package guessmarket.engine.exception;

/**
 * Thrown when somebody who is not the market maker of an event tries to open or close it.
 */
public class NotMarketMakerException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public NotMarketMakerException(String userName, String eventName, String marketMakerName) {
        super("\"" + userName + "\" cannot open or close the event \"" + eventName
                + "\", because only its market maker may do that, and its market maker is \""
                + marketMakerName + "\".");
    }
}
