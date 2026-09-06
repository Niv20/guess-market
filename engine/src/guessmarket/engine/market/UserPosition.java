package guessmarket.engine.market;

import java.io.Serial;
import java.io.Serializable;

/**
 * What one user has going on inside one event: the shares they hold in each option, what those
 * shares have cost them, and the commission they have paid.
 *
 * <p>The amount recorded against an option is what the position has cost, not what was spent on
 * it: buying adds to it and selling takes away from it again. That is the figure that makes sense
 * next to a holding, because it says what somebody is out of pocket for the shares they still
 * have, and it is what turns into a profit or a loss once the event is settled.
 *
 * <p>A position exists from the moment its owner acts in the event, even if that action was an
 * order that has never been filled. That is what the assignment means by taking part in an event.
 */
public class UserPosition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String userName;
    private final long[] shares;
    private final double[] amountPaid;

    private double commissionPaid;
    private double amountWon;

    UserPosition(String userName, int optionCount) {
        this.userName = userName;
        this.shares = new long[optionCount];
        this.amountPaid = new double[optionCount];
    }

    public String getUserName() {
        return userName;
    }

    public long getShares(int optionIndex) {
        return shares[optionIndex];
    }

    public double getAmountPaid(int optionIndex) {
        return amountPaid[optionIndex];
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

    /** @return what the settlement of the event paid this user, or zero if it has not been settled. */
    public double getAmountWon() {
        return amountWon;
    }

    /** @return what taking part has been worth so far: what came back, less what went out. */
    public double getNetResult() {
        double spent = 0;
        for (double paid : amountPaid) {
            spent += paid;
        }
        return amountWon - spent - commissionPaid;
    }

    /** @return whether this user still holds anything at all in this event. */
    public boolean holdsAnything() {
        for (long held : shares) {
            if (held > 0) {
                return true;
            }
        }
        return false;
    }

    void addShares(int optionIndex, long quantity, double amount) {
        shares[optionIndex] += quantity;
        amountPaid[optionIndex] += amount;
    }

    void removeShares(int optionIndex, long quantity, double amountReceived) {
        shares[optionIndex] -= quantity;
        amountPaid[optionIndex] -= amountReceived;
    }

    void addCommission(double amount) {
        commissionPaid += amount;
    }

    void addWinnings(double amount) {
        amountWon += amount;
    }
}
