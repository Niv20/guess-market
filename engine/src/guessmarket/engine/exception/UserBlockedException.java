package guessmarket.engine.exception;

/**
 * Thrown when a blocked user tries to act. A user is blocked once an action has left their balance below zero, and from that moment they can do nothing at all.
 */
public class UserBlockedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public UserBlockedException(String userName) {
        super("\"" + userName + "\" is blocked, because their balance went below zero, and a "
                + "blocked user cannot take any further action. Accounts cannot be topped up, "
                + "so nothing more can be done with this user.");
    }
}
