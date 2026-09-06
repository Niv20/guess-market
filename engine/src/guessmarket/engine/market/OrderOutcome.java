package guessmarket.engine.market;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What became of one order the moment it reached the market.
 *
 * <p>An order can do three different things, and often does more than one of them at once: fill
 * against orders already waiting, create new shares together with a waiting buyer of the opposite
 * option, and wait in the book with whatever is left. The counts here say how much of it did each,
 * so that the person who placed it can be told what actually happened.
 *
 * <p>The money figures are written from the point of view of the person who placed the order.
 * Everybody else's money moves too - the seller on the other side of a fill is paid, the waiting
 * buyer in a mint pays - and those movements are not this order owner's business, so they are not
 * counted here. What is counted is anybody the order pushed below zero, because that is something
 * the person needs to be told about even when it happened to somebody else.
 */
public final class OrderOutcome {

    private final long orderId;
    private final String ownerName;

    private long filled;
    private long minted;
    private long resting;

    private double cashSpent;
    private double cashReceived;
    private double commissionPaid;

    private final List<MarketTrade> executions = new ArrayList<>();
    private final Set<String> blockedUsers = new LinkedHashSet<>();

    OrderOutcome(long orderId, String ownerName) {
        this.orderId = orderId;
        this.ownerName = ownerName;
    }

    public long getOrderId() {
        return orderId;
    }

    /** @return how many shares of the order were traded straight away. */
    public long getFilled() {
        return filled;
    }

    /** @return how many of the filled shares were brand new ones, created against another buyer. */
    public long getMinted() {
        return minted;
    }

    /** @return how many shares of the order are still waiting in the book. */
    public long getResting() {
        return resting;
    }

    public double getCashSpent() {
        return cashSpent;
    }

    public double getCashReceived() {
        return cashReceived;
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

    /** @return the transactions this order caused, oldest first. */
    public List<MarketTrade> getExecutions() {
        return List.copyOf(executions);
    }

    /** @return everybody this order left with a balance below zero, and therefore blocked. */
    public List<String> getBlockedUsers() {
        return List.copyOf(blockedUsers);
    }

    void addFilled(long shares) {
        filled += shares;
    }

    void addMinted(long shares) {
        minted += shares;
    }

    void setResting(long shares) {
        resting = shares;
    }

    /** Counts money leaving an account, but only when it is the order owner's account. */
    void cashOut(String userName, double amount, double commission) {
        if (ownerName.equalsIgnoreCase(userName)) {
            cashSpent += amount;
            commissionPaid += commission;
        }
    }

    /** Counts money arriving in an account, but only when it is the order owner's account. */
    void cashIn(String userName, double amount) {
        if (ownerName.equalsIgnoreCase(userName)) {
            cashReceived += amount;
        }
    }

    void addExecution(MarketTrade trade) {
        executions.add(trade);
    }

    void addBlockedUser(String userName) {
        blockedUsers.add(userName);
    }
}
