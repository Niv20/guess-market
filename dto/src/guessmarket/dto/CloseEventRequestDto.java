package guessmarket.dto;

/**
 * A request from a market maker to close their event and declare the outcome.
 *
 * @param userName           the user asking, who must be the market maker of the event
 * @param eventId            the event to close
 * @param winningOptionIndex the option that turned out to be right, counted from 0
 */
public record CloseEventRequestDto(String userName, int eventId, int winningOptionIndex) {
}
