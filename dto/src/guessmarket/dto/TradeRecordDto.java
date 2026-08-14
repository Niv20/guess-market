package guessmarket.dto;

/**
 * A single purchase that was made in an event.
 *
 * @param serialNumber          the position of the trade in the event, starting at 1
 * @param optionName            the name of the option that was bought
 * @param shares                the number of shares that were bought
 * @param amountPaidForShares   the price of the shares themselves
 * @param commissionPaid        the commission that was added to that price, zero when the
 *                              event collects its commission only when it is closed
 * @param totalPaid             the total amount the buyer paid for this trade
 */
public record TradeRecordDto(int serialNumber,
                             String optionName,
                             long shares,
                             double amountPaidForShares,
                             double commissionPaid,
                             double totalPaid) {
}
