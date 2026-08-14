package guessmarket.dto;

/**
 * The outcome of a successful purchase, together with the state of the event after it.
 *
 * @param optionName            the option that was bought
 * @param shares                the number of shares that were bought
 * @param amountPaidForShares   the price of the shares themselves
 * @param commissionPaid        the commission that was added on top, zero when the event
 *                              collects its commission only when it is closed
 * @param totalPaid             the total amount paid by the buyer
 * @param statusAfterPurchase   the trading state of the event once the purchase was applied
 */
public record PurchaseResultDto(String optionName,
                                long shares,
                                double amountPaidForShares,
                                double commissionPaid,
                                double totalPaid,
                                EventTradingStatusDto statusAfterPurchase) {
}
