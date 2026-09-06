package guessmarket.engine.market;

import java.io.Serial;
import java.io.Serializable;

/**
 * What one LMSR purchase cost.
 *
 * @param optionName          the option that was bought
 * @param shares              how many shares were bought
 * @param amountPaidForShares what the cost function priced them at
 * @param commissionPaid      the commission added on top, for an event that collects it on every
 *                            purchase
 * @param balanceAfter        what the buyer's account held once it had all been taken out
 */
public record Purchase(String optionName,
                       long shares,
                       double amountPaidForShares,
                       double commissionPaid,
                       double balanceAfter) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public double totalPaid() {
        return amountPaidForShares + commissionPaid;
    }
}
