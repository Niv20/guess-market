package guessmarket.engine.xml;

import guessmarket.engine.market.Commission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks that a file which was read successfully actually describes a system that can be run.
 *
 * <p>The file is guaranteed to match the course schema, but not to make sense: ids may repeat,
 * a commission may be out of range, a liquidity parameter may be zero, and so on. Every check
 * keeps going after a failure and adds one sentence to the list of problems, so that the user
 * is shown everything that is wrong with the file at once.
 */
final class LoadedFileValidator {

    /** Every event in this exercise has exactly two possible outcomes. */
    private static final int REQUIRED_OPTION_COUNT = 2;

    /** @return one readable sentence per problem, or an empty list when the file is usable. */
    List<String> findProblems(XmlGuessMarket market) {
        List<String> problems = new ArrayList<>();
        List<XmlEvent> events = eventsOf(market);

        if (events.isEmpty()) {
            problems.add("The file does not contain any event. "
                    + "A system details file must describe at least one event.");
            return problems;
        }

        Map<Integer, String> descriptionByEventId = new LinkedHashMap<>();
        for (int position = 0; position < events.size(); position++) {
            XmlEvent event = events.get(position);
            String where = describe(position + 1, event.getName());
            validateName(problems, where, event);
            validateId(problems, where, event, descriptionByEventId);
            validateDescription(problems, where, event);
            validateCommission(problems, where, event);
            validateOptions(problems, where, event);
            validateMethod(problems, where, event);
        }
        return problems;
    }

    private static List<XmlEvent> eventsOf(XmlGuessMarket market) {
        if (market == null || market.getEvents() == null || market.getEvents().getEventList() == null) {
            return List.of();
        }
        return market.getEvents().getEventList();
    }

    private static void validateName(List<String> problems, String where, XmlEvent event) {
        if (isBlank(event.getName())) {
            problems.add(where + ": the event has no name. "
                    + "Give the GM-event element a name attribute that is not empty.");
        }
    }

    private static void validateId(List<String> problems, String where, XmlEvent event,
                                   Map<Integer, String> descriptionByEventId) {
        Integer id = event.getId();
        if (id == null) {
            problems.add(where + ": the event has no id. Add an id element holding a whole number.");
            return;
        }
        String owner = descriptionByEventId.get(id);
        if (owner != null) {
            problems.add(where + ": id " + id + " is already used by " + owner
                    + ". Every event must have its own unique id.");
        } else {
            descriptionByEventId.put(id, where);
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
                    + "Add a comision element with a percentage and a type attribute.");
            return;
        }
        Integer percent = commission.getPercent();
        if (percent == null) {
            problems.add(where + ": the commission has no value. "
                    + "Write a whole percentage between " + Commission.MIN_PERCENT + " and "
                    + Commission.MAX_PERCENT + " inside the comision element.");
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

    private static void validateMethod(List<String> problems, String where, XmlEvent event) {
        if (event.getMethod() == null || event.getMethod().getLmsr() == null) {
            problems.add(where + ": the event has no LMSR trading method. "
                    + "Add a GM-method element holding a GM-LMSR element with a b value.");
            return;
        }
        Integer liquidityParameter = event.getMethod().getLmsr().getLiquidityParameter();
        if (liquidityParameter == null) {
            problems.add(where + ": the LMSR method has no liquidity parameter. "
                    + "Add a b element holding a whole number greater than 0.");
        } else if (liquidityParameter <= 0) {
            problems.add(where + ": the liquidity parameter b is " + liquidityParameter
                    + ", but it must be a whole number greater than 0.");
        }
    }

    private static boolean hasRepeatedName(List<String> optionNames) {
        List<String> seen = new ArrayList<>();
        for (String optionName : optionNames) {
            String trimmed = optionName == null ? "" : optionName.trim();
            if (seen.contains(trimmed)) {
                return true;
            }
            seen.add(trimmed);
        }
        return false;
    }

    private static String describe(int position, String name) {
        String label = "Event number " + position + " in the file";
        return isBlank(name) ? label : label + " (\"" + name.trim() + "\")";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
