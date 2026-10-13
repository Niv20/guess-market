package guessmarket.server;

import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.PricePointDto;
import guessmarket.dto.StateTransferResultDto;
import guessmarket.dto.UserDto;
import guessmarket.dto.network.ChatMessageDto;
import guessmarket.dto.network.LoginResponseDto;
import guessmarket.dto.network.PublicUserDto;
import guessmarket.dto.network.SnapshotDto;
import guessmarket.engine.GuessMarketEngineFactory;
import guessmarket.engine.OnlineMarketEngine;
import guessmarket.engine.exception.UserNotFoundException;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The single server-owned market, authentication boundary and chat room. Every operation uses
 * the engine monitor, including login and complete snapshots, so polling cannot see half a trade.
 * Logged-out names remain reserved until this application stops. Names restored from a state file
 * can be claimed once, which lets their owners continue the restored accounts and histories.
 */
public final class ApplicationService {
    static final String CONTEXT_ATTRIBUTE = ApplicationService.class.getName();
    private static final String USER_ATTRIBUTE = ApplicationService.class.getName() + ".username";
    private static final String TOKEN_ATTRIBUTE = ApplicationService.class.getName() + ".token";
    private static final int MAX_CHAT_MESSAGES = 500;
    private final OnlineMarketEngine engine = GuessMarketEngineFactory.createOnlineEngine();
    private final AccessTokenStore tokens = new AccessTokenStore();
    private final Set<String> claimedUserNames = new HashSet<>();
    private final List<ChatMessageDto> chat = new ArrayList<>();
    private long nextChatId = 1;

    LoginResponseDto login(HttpSession session, String requestedName) {
        String name = requestedName.strip();
        if (name.isEmpty() || name.length() > 40 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "Enter a username of 1–40 characters without control characters.");
        }
        synchronized (engine) {
            String existingSessionName = sessionUsername(session);
            if (existingSessionName != null) {
                if (!existingSessionName.equalsIgnoreCase(name)) {
                    throw new ApiException(409, "You are already logged in. Log out before choosing a new username.");
                }
                try {
                    engine.getUser(existingSessionName);
                } catch (UserNotFoundException removedByRestore) {
                    expireIdentity(session, sessionToken(session), existingSessionName);
                    throw new ApiException(401, "Your account is not present in the restored system. Please log in again.");
                }
                return loginResponse(session, existingSessionName);
            }
            if (claimedUserNames.contains(userKey(name))) {
                throw new ApiException(409, "That username already exists. Please choose another username.");
            }
            UserDto user;
            try {
                // A state file may already contain this account. It is available until the first
                // client claims it during this server lifetime.
                user = engine.getUser(name);
            } catch (UserNotFoundException available) {
                user = engine.registerUser(name);
            }
            claimedUserNames.add(userKey(user.name()));
            session.setAttribute(USER_ATTRIBUTE, user.name());
            return loginResponse(session, user.name());
        }
    }

    void requireAuthenticated(HttpSession session, String bearerToken) {
        authenticated(session, bearerToken, (market, username) -> null);
    }

    <T> T authenticated(HttpSession session, String bearerToken,
                        BiFunction<OnlineMarketEngine, String, T> operation) {
        synchronized (engine) {
            String username = bearerToken == null ? sessionUsername(session) : tokens.authenticate(bearerToken);
            if (username == null) {
                throw new ApiException(401, "Your session has ended. Please log in.");
            }
            try {
                engine.getUser(username);
            } catch (UserNotFoundException removedByRestore) {
                expireIdentity(session, bearerToken, username);
                throw new ApiException(401, "Your account is not present in the restored system. Please log in again.");
            }
            return operation.apply(engine, username);
        }
    }

    StateTransferResultDto importState(HttpSession session, String bearerToken,
                                       java.io.InputStream input, String fileName) {
        return authenticated(session, bearerToken, (market, username) -> {
            StateTransferResultDto result = market.importSystemState(username, input, fileName);
            // Chat is a transient communication channel rather than market/account history.
            // Clearing it prevents messages from the replaced market lingering beside new users.
            chat.clear();
            nextChatId = 1;
            return result;
        });
    }

    void logout(HttpSession session, String bearerToken) {
        authenticated(session, bearerToken, (market, username) -> {
            tokens.revoke(bearerToken);
            String sessionName = sessionUsername(session);
            if (sessionName != null && sessionName.equals(username)) {
                tokens.revoke(sessionToken(session));
                session.invalidate();
            }
            return null;
        });
    }

    SnapshotDto snapshot(HttpSession session, String bearerToken) {
        return authenticated(session, bearerToken, (market, username) -> {
            List<EventTradingStatusDto> events = new ArrayList<>();
            Map<Integer, List<PricePointDto>> histories = new LinkedHashMap<>();
            for (EventDto event : market.getAllEvents()) {
                events.add(market.getEventTradingStatus(event.id()));
                histories.put(event.id(), market.getEventPriceHistory(event.id()));
            }
            List<PublicUserDto> users = market.getAllUsers().stream()
                    .map(user -> new PublicUserDto(user.name(), user.balance(),
                            !user.marketMakerEventIds().isEmpty()))
                    .toList();
            return new SnapshotDto(events, histories, users, market.getUser(username),
                    market.getUserInvolvements(username), market.getUserBalanceHistory(username),
                    market.getAccountHistory(username), chat);
        });
    }

    ChatMessageDto sendChat(HttpSession session, String bearerToken, String text) {
        String message = text.strip();
        if (message.isEmpty() || message.length() > 1000) {
            throw new ApiException(400, "Chat messages must contain 1–1000 characters.");
        }
        if (message.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t')) {
            throw new ApiException(400, "Chat messages cannot contain control characters.");
        }
        return authenticated(session, bearerToken, (market, username) -> {
            ChatMessageDto entry = new ChatMessageDto(nextChatId++, username, message, System.currentTimeMillis());
            chat.add(entry);
            if (chat.size() > MAX_CHAT_MESSAGES) {
                chat.remove(0);
            }
            return entry;
        });
    }

    private LoginResponseDto loginResponse(HttpSession session, String username) {
        tokens.revoke(sessionToken(session));
        String token = tokens.issue(username);
        session.setAttribute(TOKEN_ATTRIBUTE, token);
        return new LoginResponseDto(username, token);
    }

    private String sessionUsername(HttpSession session) {
        if (session == null) {
            return null;
        }
        try {
            Object value = session.getAttribute(USER_ATTRIBUTE);
            return value instanceof String name ? name : null;
        } catch (IllegalStateException expired) {
            return null;
        }
    }

    private String sessionToken(HttpSession session) {
        if (session == null) {
            return null;
        }
        try {
            Object value = session.getAttribute(TOKEN_ATTRIBUTE);
            return value instanceof String token ? token : null;
        } catch (IllegalStateException expired) {
            return null;
        }
    }

    private void expireIdentity(HttpSession session, String bearerToken, String username) {
        tokens.revoke(bearerToken);
        String sessionName = sessionUsername(session);
        if (session != null && sessionName != null && sessionName.equalsIgnoreCase(username)) {
            tokens.revoke(sessionToken(session));
            try {
                session.invalidate();
            } catch (IllegalStateException alreadyExpired) {
                // The identity is already gone, which is the desired result.
            }
        }
    }

    private static String userKey(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
