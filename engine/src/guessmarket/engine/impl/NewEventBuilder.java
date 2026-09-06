package guessmarket.engine.impl;

import guessmarket.dto.CreateEventRequestDto;
import guessmarket.dto.TradingMethod;
import guessmarket.engine.exception.InvalidEventDefinitionException;
import guessmarket.engine.market.Commission;
import guessmarket.engine.market.Event;
import guessmarket.engine.market.GuessMarketSystem;
import guessmarket.engine.market.LmsrEvent;
import guessmarket.engine.market.OrderBookEvent;
import guessmarket.engine.market.User;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds an event somebody creates while the program is running, and refuses the ones that cannot
 * exist.
 *
 * <p>The details of a new event arrive from a person filling in a form rather than from a file, so
 * they are checked here in exactly the same spirit as the file validator checks a file: every
 * problem is reported in one sentence that says what is wrong and what to do about it, and nothing
 * is created until all of them pass.
 *
 * <p>An event created this way is in every other respect an ordinary event. It starts not yet
 * running, its creator becomes its market maker, and that market maker has to open it, pay for it
 * and eventually close it like anybody else.
 */
final class NewEventBuilder {

    /** Every event in this system has exactly two possible outcomes. */
    private static final int REQUIRED_OPTION_COUNT = 2;

    private NewEventBuilder() {
    }

    static Event build(GuessMarketSystem system, CreateEventRequestDto request) {
        User creator = system.requireUser(request.userName());
        creator.requireNotBlocked();

        String name = required(request.name(), "The event needs a name.");
        String description = required(request.description(),
                "The event needs a description saying what it asks and how it will be decided.");
        Commission commission = commissionOf(request);
        List<String> optionNames = optionsOf(request);
        int id = system.nextFreeEventId();

        if (request.tradingMethod() == TradingMethod.LMSR) {
            return buildLmsr(request, id, name, description, commission, optionNames, creator);
        }
        return buildOrderBook(request, id, name, description, commission, optionNames, creator);
    }

    private static Event buildLmsr(CreateEventRequestDto request, int id, String name,
                                   String description, Commission commission,
                                   List<String> optionNames, User creator) {
        if (request.liquidityParameter() <= 0) {
            throw new InvalidEventDefinitionException("The liquidity parameter b is "
                    + request.liquidityParameter() + ", but it must be a whole number greater "
                    + "than 0. The larger it is, the less each purchase moves the price, and the "
                    + "more the market maker has to put up to open the event.");
        }
        return new LmsrEvent(id, name, description, commission, optionNames,
                creator.getName(), request.liquidityParameter());
    }

    private static Event buildOrderBook(CreateEventRequestDto request, int id, String name,
                                        String description, Commission commission,
                                        List<String> optionNames, User creator) {
        if (request.baseValue() <= 0) {
            throw new InvalidEventDefinitionException("The base value d is " + request.baseValue()
                    + ", but it must be a whole number greater than 0. It is what one share of the "
                    + "winning option will pay out.");
        }
        if (request.initialInvestment() < 0) {
            throw new InvalidEventDefinitionException("The initial investment is "
                    + request.initialInvestment() + ", but it cannot be less than 0.");
        }
        return new OrderBookEvent(id, name, description, commission, optionNames,
                creator.getName(), request.initialInvestment(), request.baseValue(),
                request.mintAllowed());
    }

    private static Commission commissionOf(CreateEventRequestDto request) {
        int percent = request.commissionPercent();
        if (percent < Commission.MIN_PERCENT || percent > Commission.MAX_PERCENT) {
            throw new InvalidEventDefinitionException("The commission is " + percent
                    + ", but it must be a whole percentage between " + Commission.MIN_PERCENT
                    + " and " + Commission.MAX_PERCENT + ".");
        }
        if (request.commissionType() == null) {
            throw new InvalidEventDefinitionException("Choose whether the commission is collected "
                    + "on every purchase or when the event is closed.");
        }
        return new Commission(percent, request.commissionType());
    }

    private static List<String> optionsOf(CreateEventRequestDto request) {
        List<String> names = new ArrayList<>();
        for (String optionName : request.optionNames()) {
            if (optionName != null && !optionName.trim().isEmpty()) {
                names.add(optionName.trim());
            }
        }
        if (names.size() != REQUIRED_OPTION_COUNT) {
            throw new InvalidEventDefinitionException("An event must have exactly "
                    + REQUIRED_OPTION_COUNT + " options, and both of them need a name.");
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String optionName : names) {
            distinct.add(optionName.toLowerCase(Locale.ROOT));
        }
        if (distinct.size() != names.size()) {
            throw new InvalidEventDefinitionException("The two options of an event must have "
                    + "different names, otherwise nobody can tell them apart.");
        }
        return names;
    }

    private static String required(String value, String problem) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidEventDefinitionException(problem);
        }
        return value.trim();
    }
}
