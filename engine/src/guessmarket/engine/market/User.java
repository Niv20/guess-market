package guessmarket.engine.market;

import guessmarket.engine.exception.UserBlockedException;

import java.io.Serializable;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One person taking part in the system: their name, their account, the events they run and the
 * events they have acted in.
 *
 * <p>A user knows which events they are the market maker of, because that comes from the file and
 * belongs to them. What they hold inside an event does not: that is kept by the event itself, next
 * to everybody else's holdings, so that an event can show all of its participants without having
 * to walk through every user in the system looking for the ones that mention it.
 */
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private final UserAccount account;

    /** The events this user finances, opens and closes, in the order the file listed them. */
    private final Set<Integer> marketMakerEventIds = new LinkedHashSet<>();

    /** The events this user has acted in, in the order they first acted in them. */
    private final Set<Integer> participatingEventIds = new LinkedHashSet<>();

    public User(String name, double initialBalance) {
        this.name = name;
        this.account = new UserAccount(initialBalance);
    }

    public String getName() {
        return name;
    }

    public UserAccount getAccount() {
        return account;
    }

    public double getBalance() {
        return account.getBalance();
    }

    public boolean isBlocked() {
        return account.isBlocked();
    }

    public List<Integer> getMarketMakerEventIds() {
        return List.copyOf(marketMakerEventIds);
    }

    public List<Integer> getParticipatingEventIds() {
        return List.copyOf(participatingEventIds);
    }

    public boolean isMarketMakerOf(int eventId) {
        return marketMakerEventIds.contains(eventId);
    }

    /** @return every balance this user's account has held, oldest first. */
    public List<BalanceChange> getBalanceHistory() {
        return Collections.unmodifiableList(new ArrayList<>(account.getHistory()));
    }

    void addMarketMakerEvent(int eventId) {
        marketMakerEventIds.add(eventId);
    }

    void recordParticipation(int eventId) {
        participatingEventIds.add(eventId);
    }

    /**
     * Refuses to let a blocked user do anything.
     *
     * @throws UserBlockedException if this user has already been blocked
     */
    public void requireNotBlocked() {
        if (isBlocked()) {
            throw new UserBlockedException(name);
        }
    }

    boolean canAfford(double amount) {
        return account.canAfford(amount);
    }

    void deposit(double amount, String reason) {
        if (amount > 0) {
            account.deposit(amount, reason);
        }
    }

    /**
     * @return whether this withdrawal is the one that blocked the user, so that the caller can
     *         report it
     */
    boolean withdraw(double amount, String reason) {
        return amount > 0 && account.withdraw(amount, reason);
    }
}
