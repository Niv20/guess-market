package guessmarket.dto;

/**
 * The settlement details produced when an event is closed.
 *
 * @param winningOptionName        the option that was declared the winner
 * @param winningShares            the number of shares held in the winning option
 * @param grossPayoutPot           the money owed to the winners before commission, one
 *                                 dollar for every winning share
 * @param commissionCollectedNow   the commission taken out of that pot, zero when the event
 *                                 collected its commission on every purchase
 * @param amountPaidToWinners      the money actually distributed among the winners
 * @param payoutPerShare           what a single winning share was worth after commission
 * @param statusAfterClose         the trading state of the event once it was settled
 */
public record CloseEventResultDto(String winningOptionName,
                                  long winningShares,
                                  double grossPayoutPot,
                                  double commissionCollectedNow,
                                  double amountPaidToWinners,
                                  double payoutPerShare,
                                  EventTradingStatusDto statusAfterClose) {
}
