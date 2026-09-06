package guessmarket.engine.market;

import guessmarket.dto.OrderSide;

import java.io.Serial;
import java.io.Serializable;

/**
 * One order in an order book: somebody's standing offer to buy or to sell a number of shares of
 * one option at one price.
 *
 * <p>Everything about an order is fixed except how much of it is left. An order that is partly
 * filled keeps its place and its price and simply asks for less, which is what lets a large order
 * eat through several smaller ones and leave the last of them still waiting with the remainder.
 *
 * <p>The number every order carries is what settles who came first when two of them ask for the
 * same price, and it never repeats within an event.
 */
public class Order implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long id;
    private final String userName;
    private final OrderSide side;
    private final int optionIndex;
    private final long quantity;
    private final double pricePerShare;

    private long remaining;

    Order(long id, String userName, OrderSide side, int optionIndex,
          long quantity, double pricePerShare) {
        this.id = id;
        this.userName = userName;
        this.side = side;
        this.optionIndex = optionIndex;
        this.quantity = quantity;
        this.pricePerShare = pricePerShare;
        this.remaining = quantity;
    }

    public long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public OrderSide getSide() {
        return side;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public long getQuantity() {
        return quantity;
    }

    public double getPricePerShare() {
        return pricePerShare;
    }

    public long getRemaining() {
        return remaining;
    }

    public boolean isFilled() {
        return remaining <= 0;
    }

    /** Takes shares off what this order is still asking for. */
    void reduce(long filled) {
        remaining -= filled;
    }
}
