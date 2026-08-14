package guessmarket.engine.exception;

/**
 * Thrown when the requested number of shares cannot be bought, because it is not a positive
 * whole number.
 */
public class InvalidShareQuantityException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidShareQuantityException(long shares) {
        super("The number of shares to buy must be a whole number of at least 1, but "
                + shares + " was requested.");
    }
}
