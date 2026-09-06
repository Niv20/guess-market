package guessmarket.dto;

/**
 * One point on the graph of a user's account balance: what it held after a given action of theirs.
 *
 * @param step        which action of this user it was, starting at 0 for the balance they began with
 * @param description what the action was, for the tooltip of the point
 * @param balance     what the account held afterwards
 */
public record BalancePointDto(int step, String description, double balance) {
}
