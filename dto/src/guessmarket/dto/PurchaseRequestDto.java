package guessmarket.dto;

/**
 * A request to buy shares of one option of an LMSR event.
 *
 * @param userName    the user buying
 * @param eventId     the event to buy in
 * @param optionIndex which option, counted from 0
 * @param shares      how many shares to buy
 */
public record PurchaseRequestDto(String userName, int eventId, int optionIndex, long shares) {
}
