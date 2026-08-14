package guessmarket.engine.market;

import java.io.Serializable;

/**
 * The trading account of a single event, held by its market maker.
 *
 * <p>It is opened empty, receives the subsidy that makes the market tradable, then takes in
 * the money paid by buyers together with the commissions, and finally pays the winners when
 * the event is closed. It is never reset after a settlement: whatever is left simply stays in
 * it.
 *
 * <p>Two figures are worth telling apart. The balance is the money the account holds, and it
 * includes the subsidy the market maker put in. {@link #getMarketMakerNetResult()} removes
 * that subsidy again, and therefore says whether the market maker actually earned money on
 * the event or ended up paying for it out of their own pocket.
 */
public class EventAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private double balance;
    private double subsidy;
    private double totalCommissionCollected;

    public double getBalance() {
        return balance;
    }

    public double getSubsidy() {
        return subsidy;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    /** @return the balance without the subsidy; a negative value means the event cost the market maker money. */
    public double getMarketMakerNetResult() {
        return balance - subsidy;
    }

    /** Puts the money that opens the market into the account, and remembers how much it was. */
    void depositSubsidy(double amount) {
        subsidy += amount;
        balance += amount;
    }

    void deposit(double amount) {
        balance += amount;
    }

    void withdraw(double amount) {
        balance -= amount;
    }

    /**
     * Records commission that the event earned. On a purchase the money arrives through
     * {@link #deposit(double)}; when an event is closed the money is already in the account and
     * is simply kept back from the winners, so only the running total changes here.
     */
    void recordCommission(double amount) {
        totalCommissionCollected += amount;
    }
}
