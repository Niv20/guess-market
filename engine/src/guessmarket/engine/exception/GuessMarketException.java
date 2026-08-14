package guessmarket.engine.exception;

/**
 * Base class of every error the engine reports to its caller.
 *
 * <p>All engine exceptions are unchecked, so that a caller is free to handle the whole family
 * in one place instead of being forced to declare every individual failure. The message of an
 * exception is always written for a person who is not a programmer: it says what happened,
 * why, and where possible how to fix it.
 */
public abstract class GuessMarketException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected GuessMarketException(String message) {
        super(message);
    }

    protected GuessMarketException(String message, Throwable cause) {
        super(message, cause);
    }
}
