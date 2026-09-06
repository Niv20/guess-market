package guessmarket.engine.market;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * What happened financially when an event was closed.
 *
 * @param winningOptionName     the option that was declared the winner
 * @param winningShares         how many shares of it were held altogether
 * @param grossPayout           what those shares were worth, before any commission
 * @param commissionCollected   what the market maker took out of that pot, for an event whose
 *                              commission is collected at the end
 * @param amountPaidToWinners   what was actually shared out among the winners
 * @param payouts               who received what
 * @param returnedToMarketMaker whatever was still in the event account afterwards and went back to
 *                              the market maker, which under LMSR is the unspent part of the subsidy
 */
public record Settlement(String winningOptionName,
                         long winningShares,
                         double grossPayout,
                         double commissionCollected,
                         double amountPaidToWinners,
                         List<Payout> payouts,
                         double returnedToMarketMaker) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public Settlement {
        payouts = List.copyOf(payouts);
    }
}
