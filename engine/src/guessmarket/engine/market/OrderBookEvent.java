package guessmarket.engine.market;

import guessmarket.dto.OrderSide;
import guessmarket.dto.TradeKind;
import guessmarket.dto.TradingMethod;
import guessmarket.engine.exception.InsufficientFundsException;
import guessmarket.engine.exception.InsufficientSharesException;
import guessmarket.engine.exception.InvalidOrderPriceException;
import guessmarket.engine.exception.InvalidShareQuantityException;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An event traded through an order book, the way an ordinary exchange works.
 *
 * <p>Each option has a book of its own and the two are traded independently, so the market can
 * think a share of one option is worth 0.70 while it thinks a share of the other is worth 0.90.
 * Nothing happens in a book until two prices meet. When they do, one of two things can:
 *
 * <ul>
 *   <li>a <b>trade</b>, where shares that already exist change hands and the buyer's money goes to
 *       the seller; or</li>
 *   <li>a <b>mint</b>, where two people who want opposite options and are between them willing to
 *       pay the base value are each given the shares they asked for, and their money goes into the
 *       event's account, which is what those brand new shares are backed by.</li>
 * </ul>
 *
 * <p>Because a pair of shares is created for exactly one base value and a winning share is paid
 * exactly one base value back, the event account always holds precisely what it owes.
 */
public class OrderBookEvent extends Event {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The cheapest an order may be priced at, and the step every price is a whole number of. */
    private static final double SMALLEST_PRICE = 0.01;

    private final long initialInvestment;
    private final int baseValue;
    private final boolean mintAllowed;
    private final List<OptionBook> books = new ArrayList<>();

    private long nextOrderId = 1;

    public OrderBookEvent(int id, String name, String description, Commission commission,
                          List<String> optionNames, String marketMakerName,
                          long initialInvestment, int baseValue, boolean mintAllowed) {
        super(id, name, description, commission, optionNames, marketMakerName);
        this.initialInvestment = initialInvestment;
        this.baseValue = baseValue;
        this.mintAllowed = mintAllowed;
        // Counted from the argument rather than from the event, because the event is not finished
        // being built yet and must not be asked anything about itself here.
        for (int i = 0; i < optionNames.size(); i++) {
            books.add(new OptionBook(i));
        }
    }

    // ------------------------------------------------------------------ what this event is

    @Override
    public TradingMethod getTradingMethod() {
        return TradingMethod.ORDER_BOOK;
    }

    @Override
    public double getBaseValue() {
        return baseValue;
    }

    public long getInitialInvestment() {
        return initialInvestment;
    }

    public boolean isMintAllowed() {
        return mintAllowed;
    }

    /**
     * @return how many pairs of shares the market maker's investment buys. One base value buys one
     *         pair, so an investment of 100 at a base value of 1 creates 100 shares of each option.
     */
    public long getInitialPairs() {
        return initialInvestment / baseValue;
    }

    /** @return the dearest an order may be priced at, one whole cent below the base value. */
    public double getHighestAllowedPrice() {
        return baseValue - SMALLEST_PRICE;
    }

    public List<OptionBook> getBooks() {
        return List.copyOf(books);
    }

    public OptionBook getBook(int optionIndex) {
        return books.get(optionIndex);
    }

    /** @return every order of this user still waiting anywhere in this event. */
    public List<Order> openOrdersOf(String userName) {
        List<Order> orders = new ArrayList<>();
        for (OptionBook book : books) {
            orders.addAll(book.ordersOf(userName));
        }
        return orders;
    }

    @Override
    public double getOpeningCost() {
        return initialInvestment;
    }

    /**
     * @return the last price this option traded at; failing that the point halfway between the two
     *         sides of its book, then whichever single side has anything in it at all. A book with
     *         nothing in it and no trade behind it answers null, because at that point there is
     *         genuinely nothing to say about what a share is worth.
     */
    @Override
    protected Double livePrice(int optionIndex) {
        OptionBook book = books.get(optionIndex);
        if (book.getLastTradePrice() != null) {
            return book.getLastTradePrice();
        }
        if (book.getMidPrice() != null) {
            return book.getMidPrice();
        }
        return book.getBestBidPrice() != null ? book.getBestBidPrice() : book.getBestAskPrice();
    }

    /**
     * Creates the shares the market maker has just paid for: one pair for every base value of the
     * investment. The money is already in the event account, which is what backs them; the shares
     * themselves are the market maker's, and they are free to offer them for sale like anybody else.
     */
    @Override
    protected void onOpened(User marketMaker) {
        long pairs = getInitialPairs();
        if (pairs <= 0) {
            return;
        }
        // The investment bought pairs, so half of it stands behind each side of every pair.
        double amountPerOption = getOpeningCost() / getOptionCount();
        UserPosition position = position(marketMaker);
        for (int optionIndex = 0; optionIndex < getOptionCount(); optionIndex++) {
            option(optionIndex).issueShares(pairs);
            position.addShares(optionIndex, pairs, amountPerOption);
            recordTrade(TradeKind.INITIAL_MINT, optionIndex, pairs, amountPerOption, 0,
                    marketMaker.getName(), null);
        }
    }

    /** Everything still waiting when the event is decided simply lapses. */
    @Override
    protected void onClosing() {
        for (OptionBook book : books) {
            book.cancelEverything();
        }
    }

    // ------------------------------------------------------------------ placing an order

    /**
     * Places an order and lets the market do whatever the order makes possible.
     *
     * <p>The order is checked, then processed against the market until nothing more can happen to
     * it, and only then does whatever is left of it go into the book to wait. Processing repeats
     * because one step can make the next one possible: a fill changes the best price, which can let
     * another fill through, and a mint empties an order out of the other book, which can change
     * things again.
     *
     * @return what became of the order
     */
    public OrderOutcome submitOrder(UserDirectory users, String userName, int optionIndex,
                                    OrderSide side, long quantity, double requestedPrice) {
        requireActive();
        validateOptionIndex(optionIndex);
        User owner = users.requireUser(userName);
        owner.requireNotBlocked();
        if (quantity < 1) {
            throw new InvalidShareQuantityException(quantity);
        }
        double price = roundToCents(requestedPrice);
        validatePrice(price, requestedPrice);
        if (side == OrderSide.SELL) {
            requireSharesToSell(owner, optionIndex, quantity);
        } else {
            requireFundsToBuy(owner, quantity * price);
        }

        Order order = new Order(nextOrderId++, owner.getName(), side, optionIndex, quantity, price);
        // Taking part in an event starts the moment an order is submitted, filled or not.
        position(owner);

        OrderOutcome outcome = new OrderOutcome(order.getId(), owner.getName());
        processUntilQuiet(order, users, outcome);
        if (!order.isFilled()) {
            books.get(optionIndex).rest(order);
        }
        outcome.setResting(order.getRemaining());
        return outcome;
    }

    /** Keeps trading and minting for as long as either of them can still do something. */
    private void processUntilQuiet(Order order, UserDirectory users, OrderOutcome outcome) {
        boolean somethingHappened = true;
        while (!order.isFilled() && somethingHappened) {
            somethingHappened = tradeOnce(order, users, outcome);
            if (!order.isFilled()) {
                somethingHappened |= mintOnce(order, users, outcome);
            }
        }
    }

    /**
     * Fills the order against the best waiting order on the other side of its own book, if there is
     * one at an acceptable price.
     *
     * <p>The trade goes through at the waiting order's price rather than the arriving one's, which
     * is the ordinary rule of an exchange and is what lets somebody selling at "0.45 or better"
     * collect 0.50 from a buyer who was already offering it.
     *
     * @return whether anything was traded
     */
    private boolean tradeOnce(Order order, UserDirectory users, OrderOutcome outcome) {
        OptionBook book = books.get(order.getOptionIndex());
        boolean buying = order.getSide() == OrderSide.BUY;
        Order waiting = buying
                ? book.bestAskAtMost(order.getPricePerShare())
                : book.bestBidAtLeast(order.getPricePerShare());
        if (waiting == null) {
            return false;
        }

        long shares = Math.min(order.getRemaining(), waiting.getRemaining());
        double price = waiting.getPricePerShare();
        String buyerName = buying ? order.getUserName() : waiting.getUserName();
        String sellerName = buying ? waiting.getUserName() : order.getUserName();

        settleTrade(users, order.getOptionIndex(), shares, price, buyerName, sellerName, outcome);

        order.reduce(shares);
        waiting.reduce(shares);
        book.removeIfFilled(waiting);
        book.setLastTradePrice(price);
        outcome.addFilled(shares);
        return true;
    }

    /**
     * Creates new shares between the arriving buyer and a waiting buyer of the other option, if the
     * two of them are together willing to pay the base value.
     *
     * <p>The waiting order is honoured at exactly the price it named, and the arriving one pays
     * whatever is left to make up the base value, which can only ever be less than or equal to what
     * it was willing to pay. Both amounts go into the event account, and each side receives the
     * shares it asked for. Only the smaller of the two quantities can be minted; whatever the
     * arriving order still wants after that carries on through the market.
     *
     * @return whether anything was minted
     */
    private boolean mintOnce(Order order, UserDirectory users, OrderOutcome outcome) {
        if (!mintAllowed || order.getSide() != OrderSide.BUY || getOptionCount() != 2) {
            return false;
        }
        int otherIndex = 1 - order.getOptionIndex();
        OptionBook otherBook = books.get(otherIndex);
        Order waiting = otherBook.bestBidCompleting(order.getPricePerShare(), baseValue);
        if (waiting == null) {
            return false;
        }

        long shares = Math.min(order.getRemaining(), waiting.getRemaining());
        double waitingPrice = waiting.getPricePerShare();
        double arrivingPrice = baseValue - waitingPrice;

        mintFor(users, waiting.getUserName(), otherIndex, shares, waitingPrice, outcome);
        mintFor(users, order.getUserName(), order.getOptionIndex(), shares, arrivingPrice, outcome);

        order.reduce(shares);
        waiting.reduce(shares);
        otherBook.removeIfFilled(waiting);
        books.get(order.getOptionIndex()).setLastTradePrice(arrivingPrice);
        otherBook.setLastTradePrice(waitingPrice);
        outcome.addFilled(shares);
        outcome.addMinted(shares);
        return true;
    }

    // ------------------------------------------------------------------ moving money and shares

    /** Shares that already exist change hands: the buyer's money goes to the seller. */
    private void settleTrade(UserDirectory users, int optionIndex, long shares, double price,
                             String buyerName, String sellerName, OrderOutcome outcome) {
        double amount = shares * price;
        double commissionDue = getCommission().amountDueOnPurchase(amount);
        User buyer = users.requireUser(buyerName);
        User seller = users.requireUser(sellerName);

        if (buyer.withdraw(amount + commissionDue, "Bought shares in \"" + getName() + "\"")) {
            outcome.addBlockedUser(buyer.getName());
        }
        seller.deposit(amount, "Sold shares in \"" + getName() + "\"");
        collectCommission(users, commissionDue);

        UserPosition buyerPosition = position(buyer);
        buyerPosition.addShares(optionIndex, shares, amount);
        buyerPosition.addCommission(commissionDue);
        position(seller).removeShares(optionIndex, shares, amount);

        outcome.cashOut(buyerName, amount + commissionDue, commissionDue);
        outcome.cashIn(sellerName, amount);
        outcome.addExecution(recordTrade(TradeKind.RESALE, optionIndex, shares, amount,
                commissionDue, buyerName, sellerName));
    }

    /** One side of a mint: brand new shares, paid for into the event's own account. */
    private void mintFor(UserDirectory users, String buyerName, int optionIndex, long shares,
                         double price, OrderOutcome outcome) {
        double amount = shares * price;
        double commissionDue = getCommission().amountDueOnPurchase(amount);
        User buyer = users.requireUser(buyerName);

        if (buyer.withdraw(amount + commissionDue, "Minted shares in \"" + getName() + "\"")) {
            outcome.addBlockedUser(buyer.getName());
        }
        getAccount().deposit(amount);
        collectCommission(users, commissionDue);

        option(optionIndex).issueShares(shares);
        UserPosition position = position(buyer);
        position.addShares(optionIndex, shares, amount);
        position.addCommission(commissionDue);

        outcome.cashOut(buyerName, amount + commissionDue, commissionDue);
        outcome.addExecution(recordTrade(TradeKind.MINT, optionIndex, shares, amount,
                commissionDue, buyerName, null));
    }

    // ------------------------------------------------------------------ checks

    /**
     * @throws InvalidOrderPriceException if the price is outside what a share of this event can be
     *         worth. A winning share pays the base value, so nobody would ever pay that much for
     *         one, and the dearest an order may be is one cent below it.
     */
    private void validatePrice(double price, double requestedPrice) {
        if (price >= SMALLEST_PRICE - CENT_SLACK
                && price <= getHighestAllowedPrice() + CENT_SLACK) {
            return;
        }
        throw new InvalidOrderPriceException("A price of " + money(requestedPrice)
                + " cannot be used in the event \"" + getName() + "\". A winning share of this "
                + "event pays " + money(baseValue) + ", so a share is worth somewhere between "
                + money(SMALLEST_PRICE) + " and " + money(getHighestAllowedPrice())
                + ". Choose a price in that range.");
    }

    /**
     * Shares already promised by another order still waiting in the book cannot be offered again,
     * because they would then have to be delivered twice.
     */
    private void requireSharesToSell(User seller, int optionIndex, long quantity) {
        UserPosition position = findPosition(seller.getName());
        long held = position == null ? 0 : position.getShares(optionIndex);
        long promised = books.get(optionIndex).sharesPromisedBy(seller.getName());
        long free = held - promised;
        if (quantity > free) {
            throw new InsufficientSharesException(seller.getName(), getOptionName(optionIndex),
                    quantity, Math.max(free, 0));
        }
    }

    private void requireFundsToBuy(User buyer, double amount) {
        double total = amount + getCommission().amountDueOnPurchase(amount);
        if (!buyer.canAfford(total)) {
            throw new InsufficientFundsException(buyer.getName(), total, buyer.getBalance(),
                    "place that order in \"" + getName() + "\"");
        }
    }

    /** Prices are whole cents, so anything else the caller sends is put onto the nearest one. */
    private static double roundToCents(double price) {
        return Math.round(price * 100.0) / 100.0;
    }

    private static String money(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}
