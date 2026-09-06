package guessmarket.engine.market;

import guessmarket.dto.EventStatus;
import guessmarket.dto.TradeKind;
import guessmarket.dto.TradingMethod;
import guessmarket.engine.exception.EventAlreadyStartedException;
import guessmarket.engine.exception.EventNotActiveException;
import guessmarket.engine.exception.EventNotStartedException;
import guessmarket.engine.exception.InsufficientFundsException;
import guessmarket.engine.exception.InvalidOptionSelectionException;
import guessmarket.engine.exception.NotMarketMakerException;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A single tradable event: what it asks, who runs it, what it holds and everything that has
 * happened in it.
 *
 * <p>The two trading methods are two subclasses rather than two settings, because almost nothing
 * about how they work is shared: LMSR prices a purchase with a formula and sells shares out of the
 * event itself, while an order book matches participants against each other and creates nothing
 * unless two of them agree. What they do have in common lives here, and it is a surprising amount:
 * an event of either kind is opened by its market maker paying for it, is settled by paying the
 * base value for every winning share, hands its commission to its market maker, and gives whatever
 * is left in its account back to that market maker at the end.
 *
 * <p>An event is the only thing that may change its own state. Money never moves and shares are
 * never created except through the methods below, every one of which checks its arguments before
 * touching anything, so a request that is refused leaves the event exactly as it was.
 */
public abstract class Event implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Anything smaller than half a cent is not money any screen could show. */
    protected static final double CENT_SLACK = 0.005;

    private final int id;
    private final String name;
    private final String description;
    private final Commission commission;
    private final List<EventOption> options;
    private final String marketMakerName;

    private final EventAccount account = new EventAccount();
    private final List<MarketTrade> trades = new ArrayList<>();

    /** Everybody who has acted in this event, keyed by their name in lower case, in arrival order. */
    private final Map<String, UserPosition> positions = new LinkedHashMap<>();

    /** What every option was worth after each transaction, oldest first, for the price graph. */
    private final List<double[]> priceHistory = new ArrayList<>();

    private EventStatus status = EventStatus.NOT_STARTED;
    private int winningOptionIndex = -1;

    protected Event(int id, String name, String description, Commission commission,
                    List<String> optionNames, String marketMakerName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.marketMakerName = marketMakerName;
        this.options = new ArrayList<>();
        for (String optionName : optionNames) {
            options.add(new EventOption(optionName));
        }
    }

    // ------------------------------------------------------------------ what the event is

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

    public String getMarketMakerName() {
        return marketMakerName;
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

    public int getOptionCount() {
        return options.size();
    }

    public String getOptionName(int optionIndex) {
        return options.get(optionIndex).getName();
    }

    /** @return the option that won, or null while the event has not been decided. */
    public String getWinningOptionName() {
        return winningOptionIndex < 0 ? null : options.get(winningOptionIndex).getName();
    }

    /** @return everything that has happened in this event, oldest first. */
    public List<MarketTrade> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    /** @return everybody who has acted in this event, in the order they first did. */
    public List<UserPosition> getPositions() {
        return List.copyOf(positions.values());
    }

    /** @return what this user has in this event, or null if they have never acted in it. */
    public UserPosition findPosition(String userName) {
        return positions.get(key(userName));
    }

    public boolean isMarketMaker(String userName) {
        return marketMakerName.equalsIgnoreCase(userName);
    }

    /** @return what every option was worth after each transaction, oldest first. */
    public List<double[]> getPriceHistory() {
        return Collections.unmodifiableList(priceHistory);
    }

    // ------------------------------------------------------------------ what the subclass decides

    public abstract TradingMethod getTradingMethod();

    /** @return what one share of the winning option pays out when the event is settled. */
    public abstract double getBaseValue();

    /** @return what the market maker has to pay in order to start this event. */
    public abstract double getOpeningCost();

    /**
     * @return what one share of the given option is worth right now, or null when nothing in the
     *         market says. Only an order book with nothing in it at all answers null.
     */
    protected abstract Double livePrice(int optionIndex);

    /** Sets the market up, once the opening money is already in the event account. */
    protected abstract void onOpened(User marketMaker);

    /** Tidies the market away, just before the event is settled. */
    protected void onClosing() {
    }

    // ------------------------------------------------------------------ prices

    /**
     * @return what one share of the given option is worth, or null when nothing says. Once the
     *         event has been decided this is no longer a matter of opinion: the winning share is
     *         worth the base value and the other one is worth nothing.
     */
    public final Double currentPrice(int optionIndex) {
        validateOptionIndex(optionIndex);
        if (status == EventStatus.CLOSED) {
            return optionIndex == winningOptionIndex ? getBaseValue() : 0.0;
        }
        return livePrice(optionIndex);
    }

    // ------------------------------------------------------------------ opening and closing

    /**
     * Starts the event, at its market maker's expense.
     *
     * @param users    how to find the market maker
     * @param userName who is asking
     * @return what it cost them
     * @throws NotMarketMakerException       if somebody else is asking
     * @throws EventAlreadyStartedException  if it has already been opened
     * @throws InsufficientFundsException    if the market maker cannot pay for it
     */
    public final double open(UserDirectory users, String userName) {
        User marketMaker = requireMarketMaker(users, userName);
        if (status != EventStatus.NOT_STARTED) {
            throw new EventAlreadyStartedException(name, status.getDisplayName());
        }
        double cost = getOpeningCost();
        if (!marketMaker.canAfford(cost)) {
            throw new InsufficientFundsException(marketMaker.getName(), cost,
                    marketMaker.getBalance(), "open the event \"" + name + "\"");
        }

        marketMaker.withdraw(cost, "Opened \"" + name + "\"");
        account.depositOpeningFunds(cost);
        status = EventStatus.ACTIVE;
        // Opening the event is itself an action in it, so the market maker takes part in it from
        // now on even under LMSR, where opening hands them no shares at all.
        position(marketMaker);
        onOpened(marketMaker);
        rememberPrices();
        return cost;
    }

    /**
     * Settles the event: every winning share is paid the base value out of the event account, the
     * commission of an event that collects it at the end is kept back from the winners and handed
     * to the market maker, and whatever is left in the account afterwards goes back to the market
     * maker as well.
     *
     * @param users              how to find the winners and the market maker
     * @param userName           who is asking, which must be the market maker
     * @param winningOptionIndex the option that turned out to be right
     * @return what was paid and to whom
     */
    public final Settlement close(UserDirectory users, String userName, int winningOptionIndex) {
        User marketMaker = requireMarketMaker(users, userName);
        requireActive();
        validateOptionIndex(winningOptionIndex);

        onClosing();
        List<Payout> payouts = payWinners(users, winningOptionIndex);

        double grossPayout = payouts.stream().mapToDouble(Payout::grossAmount).sum();
        double commissionCollected = payouts.stream().mapToDouble(Payout::commission).sum();
        double paidToWinners = payouts.stream().mapToDouble(Payout::netAmount).sum();
        if (commissionCollected > 0) {
            marketMaker.deposit(commissionCollected, "Commission from \"" + name + "\"");
            account.recordCommission(commissionCollected);
        }

        double returned = returnRemainderTo(marketMaker);
        this.winningOptionIndex = winningOptionIndex;
        this.status = EventStatus.CLOSED;
        rememberPrices();

        return new Settlement(options.get(winningOptionIndex).getName(),
                options.get(winningOptionIndex).getSharesOutstanding(),
                grossPayout, commissionCollected, paidToWinners, payouts, returned);
    }

    /** Pays every holder of the winning option out of the event account, in participant order. */
    private List<Payout> payWinners(UserDirectory users, int winningOptionIndex) {
        double perShare = getBaseValue();
        List<Payout> payouts = new ArrayList<>();
        for (UserPosition position : positions.values()) {
            long held = position.getShares(winningOptionIndex);
            if (held <= 0) {
                continue;
            }
            double gross = held * perShare;
            double commissionDue = commission.amountDueOnClose(gross);
            double net = gross - commissionDue;

            account.withdraw(gross);
            users.requireUser(position.getUserName()).deposit(net, "Won \"" + name + "\"");
            position.addWinnings(net);
            payouts.add(new Payout(position.getUserName(), held, gross, commissionDue, net));
        }
        return payouts;
    }

    /**
     * Empties the event account into the market maker's.
     *
     * <p>Under LMSR this is the part of the subsidy the event did not need, which is exactly what
     * the assignment asks to be given back. An order book should have nothing left at all, since
     * every pair of shares in it was paid for with one base value and every winning share has just
     * been paid one base value back; anything that does remain is the odd amount left over when the
     * opening investment did not divide evenly into pairs, and it belongs to the market maker too.
     *
     * @return what went back, which is negative in the case where the market maker has to make the
     *         account whole rather than being paid out of it
     */
    private double returnRemainderTo(User marketMaker) {
        double remainder = account.getBalance();
        if (Math.abs(remainder) < CENT_SLACK) {
            return 0;
        }
        account.withdraw(remainder);
        if (remainder > 0) {
            marketMaker.deposit(remainder, "Left over from \"" + name + "\"");
        } else {
            marketMaker.withdraw(-remainder, "Covered the shortfall of \"" + name + "\"");
        }
        return remainder;
    }

    // ------------------------------------------------------------------ what subclasses may use

    /** @return this user's holdings in this event, creating them the first time they act in it. */
    protected final UserPosition position(User user) {
        UserPosition existing = positions.get(key(user.getName()));
        if (existing != null) {
            return existing;
        }
        UserPosition created = new UserPosition(user.getName(), options.size());
        positions.put(key(user.getName()), created);
        user.recordParticipation(id);
        return created;
    }

    protected final EventOption option(int optionIndex) {
        return options.get(optionIndex);
    }

    /** Writes one transaction into the history and remembers what the prices were afterwards. */
    protected final MarketTrade recordTrade(TradeKind kind, int optionIndex, long shares,
                                            double totalPrice, double commissionPaid,
                                            String buyerName, String sellerName) {
        MarketTrade trade = new MarketTrade(trades.size() + 1, kind, optionIndex,
                options.get(optionIndex).getName(), shares, totalPrice, commissionPaid,
                buyerName, sellerName);
        trades.add(trade);
        rememberPrices();
        return trade;
    }

    /** Hands commission to the market maker, who is the one this event earns it for. */
    protected final void collectCommission(UserDirectory users, double amount) {
        if (amount <= 0) {
            return;
        }
        users.requireUser(marketMakerName).deposit(amount, "Commission from \"" + name + "\"");
        account.recordCommission(amount);
    }

    protected final void requireActive() {
        if (status == EventStatus.NOT_STARTED) {
            throw new EventNotStartedException(name, marketMakerName);
        }
        if (status != EventStatus.ACTIVE) {
            throw new EventNotActiveException(id, name);
        }
    }

    protected final void validateOptionIndex(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new InvalidOptionSelectionException(optionIndex, options.size(), name);
        }
    }

    /** @return how many shares of each option exist, in option order. */
    protected final long[] sharesOutstanding() {
        long[] quantities = new long[options.size()];
        for (int i = 0; i < options.size(); i++) {
            quantities[i] = options.get(i).getSharesOutstanding();
        }
        return quantities;
    }

    // ------------------------------------------------------------------ housekeeping

    private User requireMarketMaker(UserDirectory users, String userName) {
        User user = users.requireUser(userName);
        user.requireNotBlocked();
        if (!isMarketMaker(user.getName())) {
            throw new NotMarketMakerException(user.getName(), name, marketMakerName);
        }
        return user;
    }

    /**
     * Adds one point to the price graph. A price nothing can be said about is written as not a
     * number, and a graph simply leaves those points out.
     */
    private void rememberPrices() {
        double[] snapshot = new double[options.size()];
        for (int i = 0; i < options.size(); i++) {
            Double price = currentPrice(i);
            snapshot[i] = price == null ? Double.NaN : price;
        }
        priceHistory.add(snapshot);
    }

    private static String key(String userName) {
        return userName.toLowerCase(Locale.ROOT);
    }
}
