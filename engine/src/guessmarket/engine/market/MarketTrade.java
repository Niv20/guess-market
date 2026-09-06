package guessmarket.engine.market;

import guessmarket.dto.TradeKind;

import java.io.Serial;
import java.io.Serializable;

/**
 * One completed transaction in an event, kept for good.
 *
 * <p>A trade is immutable and is never taken out of the history, so the history of an event is a
 * true record of everything that has happened in it, oldest first, and can be read backwards to
 * show the newest first.
 *
 * <p>The same record covers every way shares can come to somebody, which is why the seller may be
 * absent: under LMSR the shares are handed out by the event itself, and in a mint they are created
 * on the spot. In both of those cases the money goes into the event's account rather than to
 * another participant, and there is nobody to name as the seller.
 */
public final class MarketTrade implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int serialNumber;
    private final TradeKind kind;
    private final int optionIndex;
    private final String optionName;
    private final long shares;
    private final double totalPrice;
    private final double commission;
    private final String buyerName;
    private final String sellerName;

    MarketTrade(int serialNumber, TradeKind kind, int optionIndex, String optionName, long shares,
                double totalPrice, double commission, String buyerName, String sellerName) {
        this.serialNumber = serialNumber;
        this.kind = kind;
        this.optionIndex = optionIndex;
        this.optionName = optionName;
        this.shares = shares;
        this.totalPrice = totalPrice;
        this.commission = commission;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public TradeKind getKind() {
        return kind;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public String getOptionName() {
        return optionName;
    }

    public long getShares() {
        return shares;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public double getCommission() {
        return commission;
    }

    public String getBuyerName() {
        return buyerName;
    }

    /** @return who gave the shares up, or null when the event or a mint produced them. */
    public String getSellerName() {
        return sellerName;
    }

    /**
     * @return what one share went for. Under LMSR this is an average: the cost function prices a
     *         purchase as a whole, and the shares inside it are not all worth the same.
     */
    public double getPricePerShare() {
        return shares == 0 ? 0 : totalPrice / shares;
    }

    public double getTotalPaidByBuyer() {
        return totalPrice + commission;
    }

    /** @return whether this user was on either side of the trade. */
    public boolean involves(String userName) {
        return userName.equalsIgnoreCase(buyerName) || userName.equalsIgnoreCase(sellerName);
    }
}
