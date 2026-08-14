package guessmarket.engine.market;

import guessmarket.dto.EventStatus;
import guessmarket.engine.exception.EventNotActiveException;
import guessmarket.engine.exception.InvalidOptionSelectionException;
import guessmarket.engine.exception.InvalidShareQuantityException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single tradable event: its details, its options, its account and everything that has been
 * traded in it.
 *
 * <p>An event is the only thing allowed to change its own state. Money never moves and shares
 * are never issued except through {@link #buyShares(int, long)} and {@link #close(int)}, both
 * of which validate their arguments before touching anything, so a rejected request leaves the
 * event exactly as it was.
 */
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Every share of the winning option is worth exactly one dollar when the event is closed. */
    public static final double PAYOUT_PER_WINNING_SHARE = 1.0;

    private final int id;
    private final String name;
    private final String description;
    private final Commission commission;
    private final List<EventOption> options;
    private final TradingMechanism mechanism;
    private final EventAccount account = new EventAccount();
    private final List<Trade> trades = new ArrayList<>();

    private EventStatus status = EventStatus.ACTIVE;
    private int winningOptionIndex = -1;

    public Event(int id, String name, String description, Commission commission,
                 List<String> optionNames, TradingMechanism mechanism) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.mechanism = mechanism;
        this.options = new ArrayList<>();
        for (String optionName : optionNames) {
            options.add(new EventOption(optionName));
        }
        this.account.depositSubsidy(mechanism.initialSubsidy());
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Commission getCommission() {
        return commission;
    }

    public TradingMechanism getMechanism() {
        return mechanism;
    }

    public EventAccount getAccount() {
        return account;
    }

    public EventStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == EventStatus.ACTIVE;
    }

    public List<EventOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public List<Trade> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    /** @return the winning option, or {@code null} while the event is still active. */
    public String getWinningOptionName() {
        return winningOptionIndex < 0 ? null : options.get(winningOptionIndex).getName();
    }

    /** @return the current value of one share of the given option, between 0 and 1. */
    public double optionValue(int optionIndex) {
        validateOptionIndex(optionIndex);
        return mechanism.optionValue(shareQuantities(), optionIndex);
    }

    /**
     * Buys shares of one option against the event account.
     *
     * <p>The price of the shares is decided by the trading mechanism. When the event collects
     * its commission on every purchase, that commission is added on top of the price and moves
     * into the event account together with it.
     *
     * @return the record of the trade that was just made
     */
    public Trade buyShares(int optionIndex, long shares) {
        requireActive();
        validateOptionIndex(optionIndex);
        if (shares < 1) {
            throw new InvalidShareQuantityException(shares);
        }

        double priceOfShares = mechanism.purchaseCost(shareQuantities(), optionIndex, shares);
        double commissionDue = commission.amountDueOnPurchase(priceOfShares);

        options.get(optionIndex).addShares(shares);
        account.deposit(priceOfShares + commissionDue);
        account.recordCommission(commissionDue);

        Trade trade = new Trade(trades.size() + 1, options.get(optionIndex).getName(),
                shares, priceOfShares, commissionDue);
        trades.add(trade);
        return trade;
    }

    /**
     * Resolves the event with the given option as the winner and pays the winners out of the
     * event account.
     *
     * <p>When the event collects its commission only at the end, that commission is taken out
     * of the winners' pot and stays in the account. Whatever is left in the account after the
     * winners have been paid stays there.
     *
     * @return what the settlement paid and collected
     */
    public Settlement close(int winningOptionIndex) {
        requireActive();
        validateOptionIndex(winningOptionIndex);

        EventOption winner = options.get(winningOptionIndex);
        long winningShares = winner.getSharesPurchased();
        double payoutPot = winningShares * PAYOUT_PER_WINNING_SHARE;
        double commissionDue = commission.amountDueOnClose(payoutPot);
        double amountPaidToWinners = payoutPot - commissionDue;

        account.withdraw(amountPaidToWinners);
        account.recordCommission(commissionDue);

        this.winningOptionIndex = winningOptionIndex;
        this.status = EventStatus.CLOSED;

        double payoutPerShare = winningShares == 0 ? 0 : amountPaidToWinners / winningShares;
        return new Settlement(winner.getName(), winningShares, payoutPot,
                commissionDue, amountPaidToWinners, payoutPerShare);
    }

    /** @return how many shares have been bought in every option, in option order. */
    private long[] shareQuantities() {
        long[] quantities = new long[options.size()];
        for (int i = 0; i < options.size(); i++) {
            quantities[i] = options.get(i).getSharesPurchased();
        }
        return quantities;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new EventNotActiveException(id, name);
        }
    }

    private void validateOptionIndex(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new InvalidOptionSelectionException(optionIndex, options.size(), name);
        }
    }
}
