package guessmarket.engine.exception;

/**
 * Thrown when an action is asked of an event whose trading method does not have it, such as placing an order in an LMSR event.
 */
public class UnsupportedTradingActionException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public UnsupportedTradingActionException(String eventName, String method, String action) {
        super("The event \"" + eventName + "\" is traded by " + method + ", and " + action
                + " is not something that method supports.");
    }
}
