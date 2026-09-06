package guessmarket.engine.xml;

import guessmarket.engine.market.Commission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Checks that a file which was read successfully actually describes a system that can be run.
 *
 * <p>The file is guaranteed to match the course schema, but not to make sense: ids may repeat, a
 * commission may be out of range, two users may share a name, a user may claim to run an event that
 * is not in the file, and an event may have nobody running it at all.
 *
 * <p>Every check carries on after a failure and adds one sentence to the list of problems, so that
 * the person is shown everything that is wrong with the file at once instead of correcting it one
 * line at a time.
 */
final class LoadedFileValidator {

    /** Every event in this system has exactly two possible outcomes. */
    private static final int REQUIRED_OPTION_COUNT = 2;

    private static final String TRUE_VALUE = "true";
    private static final String FALSE_VALUE = "false";

    /** @return one readable sentence per problem, or an empty list when the file is usable. */
    List<String> findProblems(XmlGuessMarket market) {
        List<String> problems = new ArrayList<>();
        List<XmlEvent> events = eventsOf(market);
        List<XmlUser> users = usersOf(market);

        if (events.isEmpty()) {
            problems.add("The file does not contain any event. "
                    + "A system details file must describe at least one event under GM-events.");
        }
        if (users.isEmpty()) {
            problems.add("The file does not contain any user. "
                    + "A system details file must describe at least one user under GM-users, "
                    + "because every event needs a user to be its market maker.");
        }
        if (events.isEmpty() || users.isEmpty()) {
            return problems;
        }

        Set<Integer> eventIds = validateEvents(problems, events);
        validateUsers(problems, users, eventIds);
        validateMarketMakers(problems, events, users);
        return problems;
    }

    // ------------------------------------------------------------------ events

    /** @return the ids that were found, so that the market maker checks can be made against them. */
    private Set<Integer> validateEvents(List<String> problems, List<XmlEvent> events) {
        Map<Integer, String> ownerOfId = new LinkedHashMap<>();
        for (int position = 0; position < events.size(); position++) {
            XmlEvent event = events.get(position);
            String where = describeEvent(position + 1, event.getName());
            validateEventName(problems, where, event);
            validateEventId(problems, where, event, ownerOfId);
            validateDescription(problems, where, event);
            validateCommission(problems, where, event);
            validateOptions(problems, where, event);
            validateMethod(problems, where, event);
        }
        return ownerOfId.keySet();
    }

    private static void validateEventName(List<String> problems, String where, XmlEvent event) {
        if (isBlank(event.getName())) {
            problems.add(where + ": the event has no name. "
                    + "Give the GM-event element a name attribute that is not empty.");
        }
    }

    private static void validateEventId(List<String> problems, String where, XmlEvent event,
                                        Map<Integer, String> ownerOfId) {
        Integer id = event.getId();
        if (id == null) {
            problems.add(where + ": the event has no id. Add an id element holding a whole number.");
            return;
        }
        String owner = ownerOfId.get(id);
        if (owner != null) {
            problems.add(where + ": id " + id + " is already used by " + owner
                    + ". Every event must have its own unique id.");
        } else {
            ownerOfId.put(id, where);
        }
    }

    private static void validateDescription(List<String> problems, String where, XmlEvent event) {
        if (isBlank(event.getDescription())) {
            problems.add(where + ": the event has no description. "
                    + "Add a description element explaining the event and how it is resolved.");
        }
    }

    private static void validateCommission(List<String> problems, String where, XmlEvent event) {
        XmlCommission commission = event.getCommission();
        if (commission == null) {
            problems.add(where + ": the event has no commission. "
                    + "Add a commission element with a percentage and a type attribute.");
            return;
        }
        Integer percent = commission.getPercent();
        if (percent == null) {
            problems.add(where + ": the commission has no value. "
                    + "Write a whole percentage between " + Commission.MIN_PERCENT + " and "
                    + Commission.MAX_PERCENT + " inside the commission element.");
        } else if (percent < Commission.MIN_PERCENT || percent > Commission.MAX_PERCENT) {
            problems.add(where + ": the commission is " + percent
                    + ", but a commission must be a whole percentage between "
                    + Commission.MIN_PERCENT + " and " + Commission.MAX_PERCENT + ".");
        }
        if (CommissionTypeParser.parse(commission.getType()) == null) {
            problems.add(where + ": \"" + commission.getType() + "\" is not a known commission type. "
                    + "Use type=\"" + CommissionTypeParser.ON_PURCHASE_VALUE + "\" or type=\""
                    + CommissionTypeParser.ON_CLOSE_VALUE + "\".");
        }
    }

    private static void validateOptions(List<String> problems, String where, XmlEvent event) {
        if (event.getOptions() == null || event.getOptions().getOptionList().isEmpty()) {
            problems.add(where + ": the event has no options. "
                    + "Add a GM-options element holding exactly " + REQUIRED_OPTION_COUNT
                    + " GM-option elements.");
            return;
        }
        List<String> optionNames = event.getOptions().getOptionList();
        if (optionNames.size() != REQUIRED_OPTION_COUNT) {
            problems.add(where + ": the event has " + optionNames.size() + " options, but every "
                    + "event must have exactly " + REQUIRED_OPTION_COUNT + " of them.");
        }
        for (int i = 0; i < optionNames.size(); i++) {
            if (isBlank(optionNames.get(i))) {
                problems.add(where + ": option number " + (i + 1) + " has no name. "
                        + "Every GM-option element must hold a name that is not empty.");
            }
        }
        if (hasRepeatedName(optionNames)) {
            problems.add(where + ": two options carry the same name. "
                    + "The options of an event must be told apart by their names.");
        }
    }

    /** An event is traded either by LMSR or by an order book, and the file must say which. */
    private static void validateMethod(List<String> problems, String where, XmlEvent event) {
        XmlMethod method = event.getMethod();
        if (method == null || (method.getLmsr() == null && method.getOrderBook() == null)) {
            problems.add(where + ": the event has no trading method. Add a GM-method element "
                    + "holding either a GM-LMSR element or a GM-order-book element.");
            return;
        }
        if (!method.hasExactlyOneMethod()) {
            problems.add(where + ": the event has both an LMSR method and an order book method, "
                    + "but an event is traded in one way only. Keep one of them and remove the other.");
            return;
        }
        if (method.getLmsr() != null) {
            validateLmsr(problems, where, method.getLmsr());
        } else {
            validateOrderBook(problems, where, method.getOrderBook());
        }
    }

    private static void validateLmsr(List<String> problems, String where, XmlLmsr lmsr) {
        Integer liquidityParameter = lmsr.getLiquidityParameter();
        if (liquidityParameter == null) {
            problems.add(where + ": the LMSR method has no liquidity parameter. "
                    + "Add a b element holding a whole number greater than 0.");
        } else if (liquidityParameter <= 0) {
            problems.add(where + ": the liquidity parameter b is " + liquidityParameter
                    + ", but it must be a whole number greater than 0.");
        }
    }

    private static void validateOrderBook(List<String> problems, String where, XmlOrderBook book) {
        Integer baseValue = book.getBaseValue();
        if (baseValue == null) {
            problems.add(where + ": the order book has no base value. "
                    + "Add a d attribute holding a whole number greater than 0, which is what one "
                    + "share of the winning option will pay.");
        } else if (baseValue <= 0) {
            problems.add(where + ": the base value d is " + baseValue
                    + ", but it must be a whole number greater than 0.");
        }

        Integer initial = book.getInitialInvestment();
        if (initial == null) {
            problems.add(where + ": the order book has no initial investment. Add an initial "
                    + "attribute holding the amount the market maker pays to create the first shares.");
        } else if (initial < 0) {
            problems.add(where + ": the initial investment is " + initial
                    + ", but it cannot be less than 0.");
        }

        String allowMint = book.getAllowMint() == null ? null : book.getAllowMint().trim();
        if (allowMint == null || !(TRUE_VALUE.equalsIgnoreCase(allowMint)
                || FALSE_VALUE.equalsIgnoreCase(allowMint))) {
            problems.add(where + ": \"" + book.getAllowMint() + "\" is not a valid allow-mint value. "
                    + "Use allow-mint=\"" + TRUE_VALUE + "\" or allow-mint=\"" + FALSE_VALUE + "\".");
        }
    }

    // ------------------------------------------------------------------ users

    private void validateUsers(List<String> problems, List<XmlUser> users, Set<Integer> eventIds) {
        Set<String> namesSoFar = new LinkedHashSet<>();
        for (int position = 0; position < users.size(); position++) {
            XmlUser user = users.get(position);
            String where = describeUser(position + 1, user.getName());
            validateUserName(problems, where, user, namesSoFar);
            validateInitialCash(problems, where, user);
            validateMarketMakerReferences(problems, where, user, eventIds);
        }
    }

    private static void validateUserName(List<String> problems, String where, XmlUser user,
                                         Set<String> namesSoFar) {
        if (isBlank(user.getName())) {
            problems.add(where + ": the user has no name. "
                    + "Give the GM-user element a name attribute that is not empty.");
            return;
        }
        // Names are compared without regard to letter case, because the program treats the text a
        // person types the same way, and two users it could not tell apart would be unusable.
        String comparable = user.getName().trim().toLowerCase(Locale.ROOT);
        if (!namesSoFar.add(comparable)) {
            problems.add(where + ": there is already a user called \"" + user.getName().trim()
                    + "\". Every user must have a name of their own.");
        }
    }

    private static void validateInitialCash(List<String> problems, String where, XmlUser user) {
        Integer initialCash = user.getInitialCash();
        if (initialCash == null) {
            problems.add(where + ": the user has no initial balance. "
                    + "Add an initial-cash element holding a whole number greater than 0.");
        } else if (initialCash <= 0) {
            problems.add(where + ": the initial balance is " + initialCash
                    + ", but every user must start with more than 0 in their account.");
        }
    }

    private static void validateMarketMakerReferences(List<String> problems, String where,
                                                      XmlUser user, Set<Integer> eventIds) {
        for (Integer eventId : user.getMarketMakerEventIds()) {
            if (eventId == null) {
                problems.add(where + ": one of the events this user runs has no id. "
                        + "Every event element inside GM-market-maker needs an id attribute.");
            } else if (!eventIds.contains(eventId)) {
                problems.add(where + ": this user is set as the market maker of event " + eventId
                        + ", but there is no event with that id in the file. "
                        + "Correct the id, or add the missing event.");
            }
        }
    }

    // ------------------------------------------------------------------ market makers

    /**
     * Checks the one rule that ties the two halves of the file together: every event must be run by
     * exactly one of the users, no more and no fewer.
     */
    private void validateMarketMakers(List<String> problems, List<XmlEvent> events,
                                      List<XmlUser> users) {
        Map<Integer, List<String>> claimsByEventId = new LinkedHashMap<>();
        for (XmlUser user : users) {
            for (Integer eventId : user.getMarketMakerEventIds()) {
                if (eventId != null) {
                    claimsByEventId.computeIfAbsent(eventId, id -> new ArrayList<>())
                            .add(isBlank(user.getName()) ? "an unnamed user" : user.getName().trim());
                }
            }
        }

        for (int position = 0; position < events.size(); position++) {
            XmlEvent event = events.get(position);
            if (event.getId() == null) {
                continue;
            }
            List<String> claims = claimsByEventId.getOrDefault(event.getId(), List.of());
            String where = describeEvent(position + 1, event.getName());
            if (claims.isEmpty()) {
                problems.add(where + ": no user is the market maker of this event. "
                        + "Exactly one user must list event id " + event.getId()
                        + " inside their GM-market-maker element.");
            } else if (claims.size() > 1) {
                problems.add(where + ": " + claims.size() + " users are set as its market maker ("
                        + String.join(", ", claims) + "), but an event must have exactly one. "
                        + "Remove event id " + event.getId() + " from all but one of them.");
            }
        }
    }

    // ------------------------------------------------------------------ small helpers

    private static List<XmlEvent> eventsOf(XmlGuessMarket market) {
        if (market == null || market.getEvents() == null
                || market.getEvents().getEventList() == null) {
            return List.of();
        }
        return market.getEvents().getEventList();
    }

    private static List<XmlUser> usersOf(XmlGuessMarket market) {
        if (market == null || market.getUsers() == null
                || market.getUsers().getUserList() == null) {
            return List.of();
        }
        return market.getUsers().getUserList();
    }

    private static boolean hasRepeatedName(List<String> optionNames) {
        Set<String> seen = new LinkedHashSet<>();
        for (String optionName : optionNames) {
            if (!seen.add(optionName == null ? "" : optionName.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String describeEvent(int position, String name) {
        String label = "Event number " + position + " in the file";
        return isBlank(name) ? label : label + " (\"" + name.trim() + "\")";
    }

    private static String describeUser(int position, String name) {
        String label = "User number " + position + " in the file";
        return isBlank(name) ? label : label + " (\"" + name.trim() + "\")";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
