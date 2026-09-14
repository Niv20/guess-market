package guessmarket.engine.exception;

/** A name remains reserved for the lifetime of the server, including after logout. */
public class UserAlreadyExistsException extends GuessMarketException {
    private static final long serialVersionUID = 1L;

    public UserAlreadyExistsException(String userName) {
        super("The username \"" + userName + "\" already exists. Choose a different name.");
    }
}
