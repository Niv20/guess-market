package guessmarket.engine.exception;

/**
 * Thrown when a chosen option does not exist in the event it was chosen for.
 */
public class InvalidOptionSelectionException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidOptionSelectionException(int optionIndex, int optionCount, String eventName) {
        super("Option number " + (optionIndex + 1) + " does not exist in the event \"" + eventName
                + "\", which has " + optionCount + " options. Choose a number between 1 and "
                + optionCount + ".");
    }
}
