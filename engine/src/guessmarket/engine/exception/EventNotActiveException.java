package guessmarket.engine.exception;

/**
 * Thrown when trading in, or closing, an event that has already been resolved.
 */
public class EventNotActiveException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventNotActiveException(int eventId, String eventName) {
        super("Event " + eventId + " (\"" + eventName + "\") is already closed, so it cannot be "
                + "traded in or closed again. Only active events accept these actions.");
    }
}
