package guessmarket.engine.exception;

/**
 * Thrown when a market maker tries to open an event that is already running, or to reopen one that has been closed.
 */
public class EventAlreadyStartedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventAlreadyStartedException(String eventName, String status) {
        super("The event \"" + eventName + "\" cannot be opened, because it is already "
                + status.toLowerCase() + ". An event can only be opened once.");
    }
}
