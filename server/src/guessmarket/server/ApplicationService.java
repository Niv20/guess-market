package guessmarket.server;

import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.PricePointDto;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * The single server-owned market, authentication boundary and chat room. Every operation uses
 * the engine monitor, including login and complete snapshots, so polling cannot see half a trade.
 * Nothing here is persisted; a logged-out name remains reserved until this application stops.
 */
public final class ApplicationService {
    static final String CONTEXT_ATTRIBUTE = ApplicationService.class.getName();
    private static final String USER_ATTRIBUTE = ApplicationService.class.getName() + ".username";
    private static final int MAX_CHAT_MESSAGES = 500;
    private final OnlineMarketEngine engine = GuessMarketEngineFactory.createOnlineEngine();
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
                return new LoginResponseDto(existingSessionName);
            }
            try {
                engine.getUser(name);
                throw new ApiException(409, "That username already exists. Please choose another username.");
            } catch (UserNotFoundException available) {
                UserDto user = engine.registerUser(name);
                session.setAttribute(USER_ATTRIBUTE, user.name());
                return new LoginResponseDto(user.name());
            }
        }
    }

    void requireAuthenticated(HttpSession session) {
        authenticated(session, (market, username) -> null);
    }

    <T> T authenticated(HttpSession session, BiFunction<OnlineMarketEngine, String, T> operation) {
        synchronized (engine) {
            String username = sessionUsername(session);
            if (username == null) {
                throw new ApiException(401, "Your session has ended. Please log in.");
            }
            return operation.apply(engine, username);
        }
    }

    void logout(HttpSession session) {
        authenticated(session, (market, username) -> {
            session.invalidate();
            return null;
        });
    }

    SnapshotDto snapshot(HttpSession session) {
        return authenticated(session, (market, username) -> {
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

    ChatMessageDto sendChat(HttpSession session, String text) {
        String message = text.strip();
        if (message.isEmpty() || message.length() > 1000) {
            throw new ApiException(400, "Chat messages must contain 1–1000 characters.");
        }
        if (message.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t')) {
            throw new ApiException(400, "Chat messages cannot contain control characters.");
        }
        return authenticated(session, (market, username) -> {
            ChatMessageDto entry = new ChatMessageDto(nextChatId++, username, message, System.currentTimeMillis());
            chat.add(entry);
            if (chat.size() > MAX_CHAT_MESSAGES) {
                chat.remove(0);
            }
            return entry;
        });
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
}
