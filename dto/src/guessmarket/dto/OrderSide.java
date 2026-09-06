package guessmarket.dto;

/**
 * Which way round an order book order goes: its owner either wants shares or wants to be rid of
 * them.
 */
public enum OrderSide {

    BUY("Buy"),
    SELL("Sell");

    private final String displayName;

    OrderSide(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public OrderSide opposite() {
        return this == BUY ? SELL : BUY;
    }
}
