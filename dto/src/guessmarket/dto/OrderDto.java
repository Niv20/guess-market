package guessmarket.dto;

/**
 * One order waiting in an order book.
 *
 * @param orderId       tells two otherwise identical orders apart, and says which arrived first
 * @param userName      who placed it
 * @param side          whether it is waiting to buy or waiting to sell
 * @param quantity      how many shares it was placed for
 * @param remaining     how many of them are still unfilled, which is what the book shows
 * @param pricePerShare what the owner is willing to pay, or wants to receive, for one share
 */
public record OrderDto(long orderId,
                       String userName,
                       OrderSide side,
                       long quantity,
                       long remaining,
                       double pricePerShare) {

    /** @return what the unfilled part of this order is worth at its own price. */
    public double remainingValue() {
        return remaining * pricePerShare;
    }
}
