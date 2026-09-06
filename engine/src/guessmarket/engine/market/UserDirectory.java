package guessmarket.engine.market;

import guessmarket.engine.exception.UserNotFoundException;

/**
 * How an event finds the people it has to move money between.
 *
 * <p>An event knows its participants only by name, because that is all an order carries. Rather
 * than handing every event a reference to the whole system, the one thing it actually needs is
 * passed in as this: a way of turning a name into the user it belongs to.
 */
public interface UserDirectory {

    /**
     * @param name the name of a user, in any letter case
     * @return that user
     * @throws UserNotFoundException if the system has no such user
     */
    User requireUser(String name);
}
