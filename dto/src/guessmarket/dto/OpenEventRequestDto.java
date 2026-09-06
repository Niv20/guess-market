package guessmarket.dto;

/**
 * A request from a market maker to start their event.
 *
 * @param userName the user asking, who must be the market maker of the event
 * @param eventId  the event to start
 */
public record OpenEventRequestDto(String userName, int eventId) {
}
