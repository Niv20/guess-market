package guessmarket.dto;

/**
 * A request to resolve an event with one of its options as the winner.
 *
 * @param eventId             the identifier of the event to close
 * @param winningOptionIndex  the position of the winning option inside the event, starting at 0
 */
public record CloseEventRequestDto(int eventId, int winningOptionIndex) {
}
