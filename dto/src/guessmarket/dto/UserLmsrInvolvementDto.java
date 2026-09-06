package guessmarket.dto;

import java.util.List;

/**
 * What one user has done inside one LMSR event.
 *
 * @param trades               everything they bought, newest first
 * @param holdings             the shares they hold in each option, and what they paid for them
 * @param totalCommissionPaid  the commission they have paid in this event altogether
 * @param finalOptionTotals    once the event is closed, the shares bought in each option by
 *                             everybody; empty while it is still running
 * @param amountWon            what the settlement paid them, or null while the event is not closed
 */
public record UserLmsrInvolvementDto(List<TradeRecordDto> trades,
                                     List<HoldingDto> holdings,
                                     double totalCommissionPaid,
                                     List<OptionStateDto> finalOptionTotals,
                                     Double amountWon) {

    public UserLmsrInvolvementDto {
        trades = List.copyOf(trades);
        holdings = List.copyOf(holdings);
        finalOptionTotals = List.copyOf(finalOptionTotals);
    }
}
