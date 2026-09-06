package guessmarket.engine.market;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The money side of one user: what their account holds, what it has held, and whether they are
 * still allowed to use it.
 *
 * <p>The account does not refuse a withdrawal that would take it below zero. That is deliberate,
 * and it is what the assignment asks for: every action is checked before it is allowed through, so
 * a balance can only go negative in the one case those checks cannot catch, which is an order that
 * was affordable when it was placed and is filled later, after the money has been spent elsewhere.
 * When that happens the account is left overdrawn and its owner is blocked from then on, which is
 * exactly the situation the exercise describes. Accounts are never topped up.
 */
public class UserAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double initialBalance;
    private double balance;
    private boolean blocked;

    /** Every balance this account has held, in order, which is what the balance graph draws. */
    private final List<BalanceChange> history = new ArrayList<>();

    UserAccount(double initialBalance) {
        this.initialBalance = initialBalance;
        this.balance = initialBalance;
        history.add(new BalanceChange("Opening balance", initialBalance));
    }

    public double getBalance() {
        return balance;
    }

    public double getInitialBalance() {
        return initialBalance;
    }

    public boolean isBlocked() {
        return blocked;
    }

    /** @return every balance this account has held, oldest first. */
    public List<BalanceChange> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /** @return whether the account can pay the given amount without going below zero. */
    boolean canAfford(double amount) {
        // Half a cent of slack, so that a purchase priced at exactly the balance is not refused
        // over the last bit of a double that no screen would ever show.
        return balance + 0.005 >= amount;
    }

    void deposit(double amount, String reason) {
        balance += amount;
        history.add(new BalanceChange(reason, balance));
    }

    /**
     * Takes money out, even if there is not enough.
     *
     * @return whether this withdrawal is what pushed the account below zero, so that the caller
     *         can tell the person that their user has just been blocked
     */
    boolean withdraw(double amount, String reason) {
        balance -= amount;
        history.add(new BalanceChange(reason, balance));
        if (!blocked && balance < 0) {
            blocked = true;
            return true;
        }
        return false;
    }
}
