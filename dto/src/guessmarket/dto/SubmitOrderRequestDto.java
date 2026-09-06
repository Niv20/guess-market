package guessmarket.dto;

/**
 * A request to place an order in the book of one option of an order book event.
 *
 * @param userName      the user placing it
 * @param eventId       the event
 * @param optionIndex   which option's book, counted from 0
 * @param side          whether they want to buy shares or to sell shares they hold
 * @param quantity      how many shares
 * @param pricePerShare the most they will pay, or the least they will accept, for one share
 */
public record SubmitOrderRequestDto(String userName,
                                    int eventId,
                                    int optionIndex,
                                    OrderSide side,
                                    long quantity,
                                    double pricePerShare) {
}
