package guessmarket.dto;

/**
 * What makes an order book event what it is.
 *
 * @param initialInvestment what the market maker pays to open the event
 * @param initialPairs      how many pairs of shares that buys, which is the investment divided by
 *                          the base value
 * @param baseValue         the {@code d} of the event: what one share of the winning option pays,
 *                          and therefore the highest a share can ever be worth
 * @param mintAllowed       whether two buyers of opposite options may create new shares between
 *                          them when their prices together reach the base value
 */
public record OrderBookSettingsDto(long initialInvestment,
                                   long initialPairs,
                                   int baseValue,
                                   boolean mintAllowed) {

    /** @return the highest price an order may name, which is one cent below the base value. */
    public double highestAllowedPrice() {
        return baseValue - 0.01;
    }
}
