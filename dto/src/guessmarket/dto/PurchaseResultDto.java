package guessmarket.dto;

/**
 * What an LMSR purchase cost and what the event looked like straight afterwards.
 *
 * @param optionName          the option that was bought
 * @param shares              how many shares were bought
 * @param amountPaidForShares what the shares themselves cost
 * @param commissionPaid      the commission added on top, which is zero for an event that
 *                            collects its commission only at the end
 * @param totalPaid           the two together, which is what actually left the buyer's account
 * @param balanceAfter        what the buyer's account holds now
 * @param statusAfterPurchase the event as it stands after the purchase
 */
public record PurchaseResultDto(String optionName,
                                long shares,
                                double amountPaidForShares,
                                double commissionPaid,
                                double totalPaid,
                                double balanceAfter,
                                EventTradingStatusDto statusAfterPurchase) {
}
