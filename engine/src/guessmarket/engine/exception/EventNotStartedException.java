package guessmarket.engine.exception;

/**
 * Thrown when somebody tries to trade in an event whose market maker has not opened it yet.
 */
public class EventNotStartedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public EventNotStartedException(String eventName, String marketMakerName) {
        super("The event \"" + eventName + "\" has not started yet, so there is nothing to trade "
                + "in it. Its market maker, \"" + marketMakerName
                + "\", has to open it before anybody can take part.");
    }
}
