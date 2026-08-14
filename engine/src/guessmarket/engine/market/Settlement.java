package guessmarket.engine.market;

import java.io.Serializable;

/**
 * What happened financially when an event was closed.
 *
 * @param winningOptionName    the option that was declared the winner
 * @param winningShares        how many shares were held in it
 * @param payoutPot            the money owed to the winners before commission
 * @param commissionCollected  the commission taken out of that pot, if any
 * @param amountPaidToWinners  what was actually distributed among the winners
 * @param payoutPerShare       the value of a single winning share after commission
 */
public record Settlement(String winningOptionName,
                         long winningShares,
                         double payoutPot,
                         double commissionCollected,
                         double amountPaidToWinners,
                         double payoutPerShare) implements Serializable {

    private static final long serialVersionUID = 1L;
}
