package guessmarket.dto.network;

/** Stable identity used by hosted clients before they reserve a username on a local server. */
public record HealthResponseDto(String message, String service, int apiVersion) {
}
