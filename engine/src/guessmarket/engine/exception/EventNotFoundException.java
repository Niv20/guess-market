package guessmarket.engine.exception;

/**
 * Thrown when an action refers to an event identifier that does not exist in the system.
 */
public class EventNotFoundException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventNotFoundException(int eventId) {
        super("There is no event with id " + eventId + " in the system. "
                + "Choose an event from the list that is displayed before the selection.");
    }
}
