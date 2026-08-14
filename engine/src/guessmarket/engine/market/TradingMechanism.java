package guessmarket.engine.market;

import java.io.Serializable;

/**
 * The pricing rules of an event: how much a share is worth, what a purchase costs, and how
 * much the market maker has to deposit in order to open the market.
 *
 * <p>An event owns exactly one mechanism. Exercise 1 supplies {@link LmsrMechanism} only;
 * an order book implementation can be added later without touching {@link Event}.
 *
 * <p>A mechanism is a pure calculator: it is handed the current number of shares held in each
 * option and never mutates anything.
 */
public abstract class TradingMechanism implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * @return the amount the market maker deposits into the event account when the event is
     *         opened, so that participants have something to trade against.
     */
    public abstract double initialSubsidy();

    /**
     * @param quantities  the number of shares bought so far in each option
     * @param optionIndex the option to price, starting at 0
     * @return the current value of one share of that option, between 0 and 1
     */
    public abstract double optionValue(long[] quantities, int optionIndex);

    /**
     * @param quantities  the number of shares bought so far in each option
     * @param optionIndex the option being bought, starting at 0
     * @param shares      how many shares are being bought
     * @return the price of that purchase, before any commission
     */
    public abstract double purchaseCost(long[] quantities, int optionIndex, long shares);
}
