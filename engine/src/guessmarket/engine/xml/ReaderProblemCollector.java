package guessmarket.engine.xml;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationEventLocator;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects everything the XML reader could not make sense of while a file was being read, such
 * as an element that does not belong to the schema or a number written as text.
 *
 * <p>Reading deliberately continues after such a problem instead of stopping at the first one.
 * Without that, a value the reader could not convert would quietly stay empty and the user
 * would be told only that something is missing, rather than what is actually written in the
 * file and on which line.
 */
final class ReaderProblemCollector implements ValidationEventHandler {

    private final List<String> problems = new ArrayList<>();

    @Override
    public boolean handleEvent(ValidationEvent event) {
        problems.add(describe(event));
        return true;
    }

    /** @return one readable sentence per problem the reader ran into, in reading order. */
    List<String> getProblems() {
        return problems;
    }

    private static String describe(ValidationEvent event) {
        String message = event.getMessage() == null ? "the content could not be read"
                : event.getMessage().trim();
        return "The file could not be read correctly" + position(event.getLocator()) + ": " + message;
    }

    private static String position(ValidationEventLocator locator) {
        if (locator == null || locator.getLineNumber() < 0) {
            return "";
        }
        return " at line " + locator.getLineNumber() + ", column " + locator.getColumnNumber();
    }
}
