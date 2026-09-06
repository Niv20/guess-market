package guessmarket.engine.market;

import java.io.Serial;
import java.io.Serializable;

/**
 * What one participant received when an event was settled.
 *
 * @param userName      who was paid
 * @param winningShares how many winning shares they held
 * @param grossAmount   what those shares were worth
 * @param commission    what was kept back from them, for an event whose commission is collected
 *                      at the end
 * @param netAmount     what actually reached their account
 */
public record Payout(String userName,
                     long winningShares,
                     double grossAmount,
                     double commission,
                     double netAmount) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
