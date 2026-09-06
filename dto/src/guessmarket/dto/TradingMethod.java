package guessmarket.dto;

/**
 * The way trading is run inside an event.
 *
 * <p>The two methods are genuinely different markets rather than two settings of one market.
 * Under LMSR a participant trades against the event itself and never needs anybody on the other
 * side; under the order book two participants trade with each other and nothing happens until
 * their prices meet.
 */
public enum TradingMethod {

    LMSR("LMSR"),
    ORDER_BOOK("Order Book");

    private final String displayName;

    TradingMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Whatever shows this value to a person shows its display name, never its constant name. */
    @Override
    public String toString() {
        return displayName;
    }
}
