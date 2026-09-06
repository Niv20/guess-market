package guessmarket.engine.exception;

/**
 * Thrown when somebody offers more shares for sale than they hold, counting the shares their other orders have already promised.
 */
public class InsufficientSharesException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InsufficientSharesException(String userName, String optionName,
                                       long requested, long available) {
        super("\"" + userName + "\" cannot offer " + requested + " shares of \"" + optionName
                + "\" for sale, because only " + available + " of them are free. Shares that are "
                + "already promised by another order still waiting in the book cannot be sold "
                + "twice. Cancel that order, or offer fewer shares.");
    }
}
