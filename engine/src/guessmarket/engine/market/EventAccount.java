package guessmarket.engine.market;

import java.io.Serial;
import java.io.Serializable;

/**
 * The account of a single event: the money that stands behind its shares.
 *
 * <p>It opens empty and stays empty until the market maker starts the event. From then on it holds
 * whatever backs the shares that exist. Under LMSR that is the subsidy the market maker put up plus
 * everything participants have paid for shares; in an order book it is the money paid in whenever
 * shares were minted, which is exactly one base value for every pair of shares in existence. When
 * the event is settled this account pays the winners and is then emptied, and anything still left
 * in it goes back to the market maker.
 *
 * <p>Commission is not kept here. The market maker of an event receives its commission personally,
 * into their own account, so the only thing this account records about commission is how much of it
 * the event has produced.
 */
public class EventAccount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private double balance;
    private double subsidy;
    private double totalCommissionCollected;

    public double getBalance() {
        return balance;
    }

    /** @return what the market maker paid to open the event. */
    public double getSubsidy() {
        return subsidy;
    }

    /** @return everything the commission of this event has paid its market maker so far. */
    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    /** Records the money that opens the market, and remembers how much of it there was. */
    void depositOpeningFunds(double amount) {
        subsidy += amount;
        balance += amount;
    }

    void deposit(double amount) {
        balance += amount;
    }

    void withdraw(double amount) {
        balance -= amount;
    }

    /** Records commission the event has produced. The money itself goes to the market maker. */
    void recordCommission(double amount) {
        totalCommissionCollected += amount;
    }
}
