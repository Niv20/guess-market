package guessmarket.dto.network;

/** Login result shared by cookie clients and hosted browser clients. */
public record LoginResponseDto(String username, String token) {
    /** Source-compatible convenience for consumers that only carry the cookie session. */
    public LoginResponseDto(String username) {
        this(username, null);
    }
}
