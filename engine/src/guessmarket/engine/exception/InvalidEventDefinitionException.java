package guessmarket.engine.exception;

/**
 * Thrown when the details given for a brand new event do not describe an event that can exist.
 */
public class InvalidEventDefinitionException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidEventDefinitionException(String message) {
        super(message);
    }
}
