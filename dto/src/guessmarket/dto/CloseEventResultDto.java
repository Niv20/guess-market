package guessmarket.dto;

import java.util.List;

/**
 * What closing an event paid out and collected.
 *
 * @param eventName            the event that was closed
 * @param winningOptionName    the option that was declared the winner
 * @param winningShares        how many shares of it were held altogether
 * @param grossPayout          what those shares were worth before commission
 * @param commissionCollected  what the market maker took out of that, for an event whose
 *                             commission is collected at the end
 * @param amountPaidToWinners  what was actually shared out among the winners
 * @param payouts              who received what
 * @param returnedToMarketMaker whatever was left in the event account and went back to the market
 *                             maker
 * @param statusAfterClose     the event as it stands now that it is closed
 */
public record CloseEventResultDto(String eventName,
                                  String winningOptionName,
                                  long winningShares,
                                  double grossPayout,
                                  double commissionCollected,
                                  double amountPaidToWinners,
                                  List<PayoutDto> payouts,
                                  double returnedToMarketMaker,
                                  EventTradingStatusDto statusAfterClose) {

    public CloseEventResultDto {
        payouts = List.copyOf(payouts);
    }
}
