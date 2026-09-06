package guessmarket.engine.market;

import java.io.Serial;
import java.io.Serializable;

/**
 * The Logarithmic Market Scoring Rule.
 *
 * <p>Trading is done against the event itself rather than against another participant, which
 * is what allows a share to be bought even when nobody is selling one. The rule is built on
 * two equations, where {@code q(i)} is the number of shares bought so far in option {@code i}
 * and {@code b} is the liquidity parameter of the event:
 *
 * <pre>
 *     cost       C(q)     = b * ln( sum over i of e^(q(i)/b) )
 *     option value p(i)   = e^(q(i)/b) / sum over j of e^(q(j)/b)
 * </pre>
 *
 * <p>The price of a purchase is the difference between the cost function after it and the cost
 * function before it, and the subsidy the market maker has to put up is simply the cost
 * function of an empty market, {@code C(0,0) = b * ln 2} for a two option event.
 *
 * <p>A large {@code b} makes the market deep: prices barely move, but the subsidy is large.
 * A small {@code b} makes it volatile: every purchase moves the price sharply.
 *
 * <p>Both equations are evaluated with the exponents shifted by their maximum. That is
 * mathematically identical to the plain formulas, but it keeps {@code Math.exp} away from the
 * value at which a {@code double} overflows to infinity, so even very large purchases are
 * priced correctly.
 */
public final class LmsrMechanism implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int liquidityParameter;

    public LmsrMechanism(int liquidityParameter) {
        if (liquidityParameter <= 0) {
            throw new IllegalArgumentException("The liquidity parameter b must be positive.");
        }
        this.liquidityParameter = liquidityParameter;
    }

    public int getLiquidityParameter() {
        return liquidityParameter;
    }

    /** @return what an empty two option market costs to open, which is {@code b * ln 2}. */
    public double initialSubsidy() {
        return cost(new long[]{0, 0});
    }

    /**
     * @param quantities  how many shares of each option have been bought so far
     * @param optionIndex the option to price, starting at 0
     * @return the value of one share of it, between 0 and 1
     */
    public double optionValue(long[] quantities, int optionIndex) {
        double[] exponents = exponents(quantities);
        double highest = highestOf(exponents);
        double total = 0;
        for (double exponent : exponents) {
            total += Math.exp(exponent - highest);
        }
        return Math.exp(exponents[optionIndex] - highest) / total;
    }

    /**
     * @param quantities  how many shares of each option have been bought so far
     * @param optionIndex the option being bought, starting at 0
     * @param shares      how many shares are being bought
     * @return what that purchase costs, before any commission
     */
    public double purchaseCost(long[] quantities, int optionIndex, long shares) {
        long[] quantitiesAfterPurchase = quantities.clone();
        quantitiesAfterPurchase[optionIndex] += shares;
        return cost(quantitiesAfterPurchase) - cost(quantities);
    }

    /** @return the total value of the pool for the given holdings, {@code C(q)}. */
    private double cost(long[] quantities) {
        double[] exponents = exponents(quantities);
        double highest = highestOf(exponents);
        double total = 0;
        for (double exponent : exponents) {
            total += Math.exp(exponent - highest);
        }
        return liquidityParameter * (highest + Math.log(total));
    }

    private double[] exponents(long[] quantities) {
        double[] exponents = new double[quantities.length];
        for (int i = 0; i < quantities.length; i++) {
            exponents[i] = (double) quantities[i] / liquidityParameter;
        }
        return exponents;
    }

    private static double highestOf(double[] values) {
        double highest = values[0];
        for (double value : values) {
            highest = Math.max(highest, value);
        }
        return highest;
    }
}
