package guessmarket.engine.exception;

/**
 * Thrown when a requested quantity is nonpositive or would exceed the market's share capacity.
 */
public class InvalidShareQuantityException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidShareQuantityException(long shares) {
        super("The number of shares to buy must be a whole number of at least 1, but "
                + shares + " was requested.");
    }
    public InvalidShareQuantityException(String reason) {
        super(reason);
    }
}
