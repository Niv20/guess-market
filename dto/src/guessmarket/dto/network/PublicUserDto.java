package guessmarket.dto.network;

/** The only account information another user may see. */
public record PublicUserDto(String name, double balance, boolean marketMaker) {
}
