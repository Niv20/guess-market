package guessmarket.engine.market;

import guessmarket.dto.OrderSide;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The order book of a single option: everybody who wants to buy it, everybody who wants to sell
 * it, and the price the last trade in it went through at.
 *
 * <p>Each option of an event has its own book and the two are traded independently, so the market
 * can quite legitimately think a YES share is worth 0.70 while it thinks a NO share is worth 0.90.
 *
 * <p>Both sides are kept in the order in which they would be served. Bids are held with the
 * highest price first, because the buyer offering the most is the one a seller reaches first;
 * asks are held with the lowest price first, for the mirror image of the same reason. Two orders
 * at the same price are served in the order they arrived, which is the ordinary rule of an
 * exchange and the only fair way of choosing between them.
 */
public class OptionBook implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Prices are whole cents, so two of them that differ by less than half a cent are the same
     * price, and a comparison must not turn the last bit of a double into a refused trade.
     */
    private static final double PRICE_SLACK = 0.005;

    /** Highest price first; equal prices in arrival order. */
    private static final Comparator<Order> BID_ORDER =
            Comparator.comparingDouble(Order::getPricePerShare).reversed()
                    .thenComparingLong(Order::getId);

    /** Lowest price first; equal prices in arrival order. */
    private static final Comparator<Order> ASK_ORDER =
            Comparator.comparingDouble(Order::getPricePerShare)
                    .thenComparingLong(Order::getId);

    private final int optionIndex;
    private final List<Order> bids = new ArrayList<>();
    private final List<Order> asks = new ArrayList<>();

    private Double lastTradePrice;

    OptionBook(int optionIndex) {
        this.optionIndex = optionIndex;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    /** @return the orders waiting to buy, the best of them first. */
    public List<Order> getBids() {
        return Collections.unmodifiableList(bids);
    }

    /** @return the orders waiting to sell, the best of them first. */
    public List<Order> getAsks() {
        return Collections.unmodifiableList(asks);
    }

    /** @return what the last trade in this book went through at, or null if none has. */
    public Double getLastTradePrice() {
        return lastTradePrice;
    }

    /** @return the most anybody is currently willing to pay, or null if nobody is buying. */
    public Double getBestBidPrice() {
        return bids.isEmpty() ? null : bids.get(0).getPricePerShare();
    }

    /** @return the least anybody is currently willing to accept, or null if nobody is selling. */
    public Double getBestAskPrice() {
        return asks.isEmpty() ? null : asks.get(0).getPricePerShare();
    }

    /**
     * @return the point halfway between the two sides, or null unless both sides have an order.
     *         With no trade for a long while this is usually the better guess at what a share is
     *         actually worth, because it is the only figure that listens to both sides at once.
     */
    public Double getMidPrice() {
        Double bid = getBestBidPrice();
        Double ask = getBestAskPrice();
        return bid == null || ask == null ? null : (bid + ask) / 2;
    }

    /**
     * @return how far apart the two sides are, or null unless both sides have an order. The wider
     *         it is, the less likely anything is to happen in this book.
     */
    public Double getSpread() {
        Double bid = getBestBidPrice();
        Double ask = getBestAskPrice();
        return bid == null || ask == null ? null : ask - bid;
    }

    /**
     * @return the best order this book will sell at, provided it is not asking for more than the
     *         given limit; null when nothing on offer is cheap enough
     */
    Order bestAskAtMost(double limit) {
        return asks.isEmpty() || asks.get(0).getPricePerShare() > limit + PRICE_SLACK
                ? null : asks.get(0);
    }

    /**
     * @return the best order this book will buy at, provided it is offering at least the given
     *         limit; null when nothing on offer is dear enough
     */
    Order bestBidAtLeast(double limit) {
        return bids.isEmpty() || bids.get(0).getPricePerShare() < limit - PRICE_SLACK
                ? null : bids.get(0);
    }

    /**
     * @return the best waiting buyer whose price, together with the given one, comes to at least
     *         the base value, which is what allows new shares to be minted between them
     */
    Order bestBidCompleting(double otherPrice, double baseValue) {
        return bestBidAtLeast(baseValue - otherPrice);
    }

    /** @return how many shares this user has already promised to sell out of this book. */
    long sharesPromisedBy(String userName) {
        long promised = 0;
        for (Order ask : asks) {
            if (ask.getUserName().equalsIgnoreCase(userName)) {
                promised += ask.getRemaining();
            }
        }
        return promised;
    }

    /** @return every order of this user still waiting in this book, best first. */
    List<Order> ordersOf(String userName) {
        List<Order> mine = new ArrayList<>();
        for (Order order : bids) {
            if (order.getUserName().equalsIgnoreCase(userName)) {
                mine.add(order);
            }
        }
        for (Order order : asks) {
            if (order.getUserName().equalsIgnoreCase(userName)) {
                mine.add(order);
            }
        }
        return mine;
    }

    /** Puts an order into the side it belongs to, in the place its price and its age give it. */
    void rest(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            bids.add(order);
            bids.sort(BID_ORDER);
        } else {
            asks.add(order);
            asks.sort(ASK_ORDER);
        }
    }

    /** Takes an order out of the book once nothing is left of it. */
    void removeIfFilled(Order order) {
        if (order.isFilled()) {
            bids.remove(order);
            asks.remove(order);
        }
    }

    void setLastTradePrice(double price) {
        this.lastTradePrice = price;
    }

    /** Empties both sides. Everything still waiting when an event is closed simply lapses. */
    void cancelEverything() {
        bids.clear();
        asks.clear();
    }
}
