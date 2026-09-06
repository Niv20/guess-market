package guessmarket.engine.exception;

/**
 * Thrown when a request names a user the system does not have.
 */
public class UserNotFoundException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String userName) {
        super("There is no user called \"" + userName + "\" in the system. "
                + "Choose a user from the list of users that is shown before the selection.");
    }
}
