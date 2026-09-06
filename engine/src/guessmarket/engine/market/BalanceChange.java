package guessmarket.engine.market;

import java.io.Serializable;

/**
 * One balance an account has held, and what put it there.
 *
 * @param reason  a short description of the action, such as which event was traded in
 * @param balance what the account held once that action had gone through
 */
public record BalanceChange(String reason, double balance) implements Serializable {

    private static final long serialVersionUID = 1L;
}
