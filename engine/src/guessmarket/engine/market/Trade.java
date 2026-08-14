package guessmarket.engine.market;

import java.io.Serializable;

/**
 * One completed purchase inside an event.
 *
 * <p>Trades are immutable, and are kept in the order they happened so that the history of an
 * event can be replayed or displayed newest first.
 */
public final class Trade implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int serialNumber;
    private final String optionName;
    private final long shares;
    private final double amountPaidForShares;
    private final double commissionPaid;

    Trade(int serialNumber, String optionName, long shares,
          double amountPaidForShares, double commissionPaid) {
        this.serialNumber = serialNumber;
        this.optionName = optionName;
        this.shares = shares;
        this.amountPaidForShares = amountPaidForShares;
        this.commissionPaid = commissionPaid;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public String getOptionName() {
        return optionName;
    }

    public long getShares() {
        return shares;
    }

    public double getAmountPaidForShares() {
        return amountPaidForShares;
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

    public double getTotalPaid() {
        return amountPaidForShares + commissionPaid;
    }
}
