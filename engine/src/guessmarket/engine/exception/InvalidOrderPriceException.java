package guessmarket.engine.exception;

/**
 * Thrown when an order names a price a share cannot have.
 */
public class InvalidOrderPriceException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InvalidOrderPriceException(String message) {
        super(message);
    }
}
