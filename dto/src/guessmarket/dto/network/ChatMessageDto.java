package guessmarket.dto.network;

/** A server-authored chat identity, monotonic id and UTC timestamp in milliseconds. */
public record ChatMessageDto(long id, String username, String message, long timestamp) {
}
