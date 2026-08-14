package guessmarket.dto;

/**
 * A request to buy shares of one option of one event.
 *
 * @param eventId      the identifier of the event to trade in
 * @param optionIndex  the position of the chosen option inside the event, starting at 0
 * @param shares       the number of shares to buy
 */
public record PurchaseRequestDto(int eventId, int optionIndex, long shares) {
}
