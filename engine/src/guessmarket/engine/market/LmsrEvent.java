package guessmarket.engine.market;

import guessmarket.dto.TradeKind;
import guessmarket.dto.TradingMethod;
import guessmarket.engine.exception.InsufficientFundsException;
import guessmarket.engine.exception.InvalidShareQuantityException;

import java.io.Serial;
import java.util.List;

/**
 * An event traded by the Logarithmic Market Scoring Rule.
 *
 * <p>Nobody has to be on the other side of a purchase here. Shares are bought from the event
 * itself, at a price the cost function works out from how many shares have already been bought, and
 * the money goes into the event's account, where it waits to pay the winners. That is what the
 * market maker's subsidy is for: it is the money that lets the first participant buy something when
 * there is nothing in the market yet, and whatever the event does not need at the end goes back.
 *
 * <p>There is no selling. A participant of an LMSR event buys shares and holds them until the event
 * is decided, exactly as in the previous exercise.
 */
public class LmsrEvent extends Event {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Every share of the winning option of an LMSR event is worth exactly one when it is settled. */
    private static final double PAYOUT_PER_WINNING_SHARE = 1.0;

    private final LmsrMechanism mechanism;

    public LmsrEvent(int id, String name, String description, Commission commission,
                     List<String> optionNames, String marketMakerName, int liquidityParameter) {
        super(id, name, description, commission, optionNames, marketMakerName);
        this.mechanism = new LmsrMechanism(liquidityParameter);
    }

    public int getLiquidityParameter() {
        return mechanism.getLiquidityParameter();
    }

    @Override
    public TradingMethod getTradingMethod() {
        return TradingMethod.LMSR;
    }

    @Override
    public double getBaseValue() {
        return PAYOUT_PER_WINNING_SHARE;
    }

    /** @return the subsidy, {@code b * ln 2}, which is what an empty two option market costs. */
    @Override
    public double getOpeningCost() {
        return mechanism.initialSubsidy();
    }

    @Override
    protected Double livePrice(int optionIndex) {
        return mechanism.optionValue(sharesOutstanding(), optionIndex);
    }

    /**
     * Nothing more to do. The subsidy is already in the event account by the time this is called,
     * and under LMSR that is the whole of what opening an event means.
     */
    @Override
    protected void onOpened(User marketMaker) {
        // Intentionally empty; see the javadoc above.
    }

    /**
     * @return what buying that many shares of that option would cost right now, before commission.
     *         Nothing is bought and nothing changes, so a screen can show the price before the
     *         person commits to it.
     */
    public double quotePurchase(int optionIndex, long shares) {
        validateOptionIndex(optionIndex);
        requirePositive(shares);
        return mechanism.purchaseCost(sharesOutstanding(), optionIndex, shares);
    }

    /**
     * Buys shares of one option from the event.
     *
     * <p>The price is the difference the purchase makes to the cost function, which is why buying
     * a lot at once costs more per share than buying a little: the price rises as the shares are
     * taken. When the event collects its commission on every purchase, that commission is added on
     * top of the price and goes to the market maker rather than into the event account.
     *
     * @throws InvalidShareQuantityException if fewer than one share was asked for
     * @throws InsufficientFundsException    if the buyer cannot pay for it
     */
    public Purchase buyShares(UserDirectory users, String userName, int optionIndex, long shares) {
        requireActive();
        validateOptionIndex(optionIndex);
        requirePositive(shares);

        User buyer = users.requireUser(userName);
        buyer.requireNotBlocked();

        double priceOfShares = mechanism.purchaseCost(sharesOutstanding(), optionIndex, shares);
        double commissionDue = getCommission().amountDueOnPurchase(priceOfShares);
        double total = priceOfShares + commissionDue;
        if (!buyer.canAfford(total)) {
            throw new InsufficientFundsException(buyer.getName(), total, buyer.getBalance(),
                    "buy " + shares + " shares of \"" + getOptionName(optionIndex) + "\"");
        }

        buyer.withdraw(total, "Bought shares in \"" + getName() + "\"");
        getAccount().deposit(priceOfShares);
        collectCommission(users, commissionDue);

        option(optionIndex).issueShares(shares);
        UserPosition position = position(buyer);
        position.addShares(optionIndex, shares, priceOfShares);
        position.addCommission(commissionDue);

        recordTrade(TradeKind.LMSR_PURCHASE, optionIndex, shares, priceOfShares, commissionDue,
                buyer.getName(), null);
        return new Purchase(getOptionName(optionIndex), shares, priceOfShares, commissionDue,
                buyer.getBalance());
    }

    private static void requirePositive(long shares) {
        if (shares < 1) {
            throw new InvalidShareQuantityException(shares);
        }
    }
}
