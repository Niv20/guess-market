package guessmarket.engine.impl;

import guessmarket.dto.AccountEntryDto;
import guessmarket.dto.CreateEventRequestDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.LoadResultDto;
import guessmarket.dto.StateFileResultDto;
import guessmarket.dto.UserDto;
import guessmarket.engine.OnlineMarketEngine;
import guessmarket.engine.exception.InvalidEventDefinitionException;
import guessmarket.engine.exception.InvalidFileContentException;
import guessmarket.engine.exception.UserAlreadyExistsException;
import guessmarket.engine.market.BalanceChange;
import guessmarket.engine.market.Event;
import guessmarket.engine.market.GuessMarketSystem;
import guessmarket.engine.market.User;
import guessmarket.engine.xml.OnlineEventFileLoader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Adds registration and cumulative stream uploads to the existing trading engine. */
public final class OnlineMarketEngineImpl extends GuessMarketEngineImpl implements OnlineMarketEngine {
    private final OnlineEventFileLoader uploadLoader = new OnlineEventFileLoader();

    public OnlineMarketEngineImpl() {
        super(new GuessMarketSystem());
    }

    @Override
    public synchronized UserDto registerUser(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a username before connecting.");
        }
        String name = userName.trim();
        if (name.length() > 80 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Use a username of at most 80 characters without control characters.");
        }
        GuessMarketSystem system = loadedSystem();
        if (system.hasUser(name)) {
            throw new UserAlreadyExistsException(name);
        }
        User user = new User(name, 0);
        system.addUser(user);
        return DtoFactory.toUserDto(user);
    }

    @Override
    public synchronized UserDto depositFunds(String userName, double amount) {
        User user = loadedSystem().requireUser(userName);
        user.getAccount().addFunds(amount);
        return DtoFactory.toUserDto(user);
    }

    @Override
    public synchronized LoadResultDto uploadEvents(String userName, InputStream stream, String fileName) {
        GuessMarketSystem system = loadedSystem();
        User uploader = system.requireUser(userName);
        uploader.requireNotBlocked();
        List<Event> additions = uploadLoader.load(stream, fileName, uploader.getName(),
                system.nextFreeEventId());
        List<String> problems = new ArrayList<>();
        for (Event addition : additions) {
            if (hasEventName(addition.getName())) {
                problems.add("An event named \"" + addition.getName()
                        + "\" already exists. Every event must have a unique name.");
            }
            if (system.hasEvent(addition.getId())) {
                problems.add("No free event identifier is available for this upload.");
            }
        }
        if (!problems.isEmpty()) {
            throw new InvalidFileContentException(fileName, problems);
        }
        // Nothing is added until the complete stream and all existing-name conflicts are checked.
        for (Event event : additions) {
            system.addEvent(event);
            system.assignMarketMaker(uploader.getName(), event.getId());
        }
        return new LoadResultDto(fileName, additions.size(), system.getUserCount());
    }

    @Override
    public synchronized List<AccountEntryDto> getAccountHistory(String userName) {
        List<AccountEntryDto> entries = new ArrayList<>();
        double previous = 0;
        for (BalanceChange change : loadedSystem().requireUser(userName).getBalanceHistory()) {
            entries.add(new AccountEntryDto(entries.size(), change.reason(),
                    change.balance() - previous, previous, change.balance()));
            previous = change.balance();
        }
        return List.copyOf(entries);
    }

    @Override
    public synchronized EventDto createEvent(CreateEventRequestDto request) {
        if (request != null && request.name() != null && hasEventName(request.name().trim())) {
            throw new InvalidEventDefinitionException("An event named \"" + request.name().trim()
                    + "\" already exists. Choose a different name.");
        }
        return super.createEvent(request);
    }

    @Override
    public synchronized LoadResultDto loadSystemDetailsFile(String path) {
        throw new UnsupportedOperationException("Online events must be uploaded as an XML stream.");
    }

    @Override
    public synchronized StateFileResultDto saveSystemState(String path) {
        throw new UnsupportedOperationException("The online server keeps its state in memory only.");
    }

    @Override
    public synchronized StateFileResultDto loadSystemState(String path) {
        throw new UnsupportedOperationException("The online server keeps its state in memory only.");
    }

    private boolean hasEventName(String name) {
        String comparable = name.toLowerCase(Locale.ROOT);
        return loadedSystem().getEvents().stream()
                .anyMatch(event -> event.getName().toLowerCase(Locale.ROOT).equals(comparable));
    }
}
