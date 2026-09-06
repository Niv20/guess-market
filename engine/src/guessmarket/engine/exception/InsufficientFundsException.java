package guessmarket.engine.exception;

/**
 * Thrown when an account does not hold enough to pay for what its owner is asking to do.
 */
public class InsufficientFundsException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String userName, double required, double available,
                                      String purpose) {
        super("\"" + userName + "\" cannot " + purpose + ", because it costs "
                + money(required) + " and the account holds only " + money(available)
                + ". Accounts cannot be topped up, so choose a smaller amount or a lower price.");
    }

    /** Money is written here rather than by the caller, so every message reads the same way. */
    private static String money(double amount) {
        return String.format(java.util.Locale.US, "%.2f", amount);
    }
}
