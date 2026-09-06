package guessmarket.dto;

/**
 * What starting an event cost its market maker.
 *
 * <p>The two trading methods spend the money differently. LMSR pays a subsidy into the event
 * account and receives nothing back for it until the event is settled; an order book pays for the
 * first pairs of shares and receives those shares, which the market maker is then free to offer
 * for sale.
 *
 * @param eventName        the event that was started
 * @param amountPaid       what left the market maker's account
 * @param sharesPerOption  how many shares of each option they received, which is zero under LMSR
 * @param balanceAfter     what their account holds now
 * @param statusAfterOpen  the event as it stands now that it is running
 */
public record OpenEventResultDto(String eventName,
                                 double amountPaid,
                                 long sharesPerOption,
                                 double balanceAfter,
                                 EventTradingStatusDto statusAfterOpen) {
}
