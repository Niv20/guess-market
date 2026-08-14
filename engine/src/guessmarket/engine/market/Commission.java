package guessmarket.engine.market;

import guessmarket.dto.CommissionType;

import java.io.Serializable;

/**
 * The commission rule of an event: how much is charged, and at which moment.
 *
 * <p>Keeping the two moments behind their own methods means the rest of the engine never has
 * to ask which kind of commission an event uses; it simply asks for the amount due at the
 * moment it is handling, and gets zero when that moment is not the chargeable one.
 */
public final class Commission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The highest commission an event is allowed to charge, as a percentage. */
    public static final int MAX_PERCENT = 90;

    /** The lowest commission an event is allowed to charge, as a percentage. */
    public static final int MIN_PERCENT = 0;

    private final int percent;
    private final CommissionType type;

    public Commission(int percent, CommissionType type) {
        if (percent < MIN_PERCENT || percent > MAX_PERCENT) {
            throw new IllegalArgumentException(
                    "A commission must be between " + MIN_PERCENT + " and " + MAX_PERCENT + ".");
        }
        this.percent = percent;
        this.type = type;
    }

    public int getPercent() {
        return percent;
    }

    public CommissionType getType() {
        return type;
    }

    /**
     * @param purchasePrice the price of the shares themselves
     * @return the commission to add on top of that price, or zero for an event that collects
     *         its commission only when it is closed
     */
    public double amountDueOnPurchase(double purchasePrice) {
        return type == CommissionType.ON_PURCHASE ? purchasePrice * percent / 100.0 : 0;
    }

    /**
     * @param payoutPot the money owed to the winners before commission
     * @return the commission to take out of that pot, or zero for an event that already
     *         collected its commission on every purchase
     */
    public double amountDueOnClose(double payoutPot) {
        return type == CommissionType.ON_CLOSE ? payoutPot * percent / 100.0 : 0;
    }
}
