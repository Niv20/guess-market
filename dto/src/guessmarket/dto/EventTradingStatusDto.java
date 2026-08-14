package guessmarket.dto;

import java.util.List;

/**
 * The complete trading picture of a single event: its details, the state of every option,
 * the money held by its account and the trades that were made in it.
 *
 * @param event                     the details of the event itself
 * @param optionStates              the state of every option, in the option order of the event
 * @param accountBalance            the money currently held by the event account
 * @param totalCommissionCollected  the commission collected by the event so far
 * @param subsidy                   the amount the market maker deposited to open the event
 * @param marketMakerNetResult      the account balance less the subsidy; a negative value
 *                                  means the market maker is still funding the event
 * @param tradeHistory              the trades of the event, most recent first
 * @param winningOptionName         the winning option, or {@code null} while the event is active
 */
public record EventTradingStatusDto(EventDto event,
                                    List<OptionStateDto> optionStates,
                                    double accountBalance,
                                    double totalCommissionCollected,
                                    double subsidy,
                                    double marketMakerNetResult,
                                    List<TradeRecordDto> tradeHistory,
                                    String winningOptionName) {

    public EventTradingStatusDto {
        optionStates = List.copyOf(optionStates);
        tradeHistory = List.copyOf(tradeHistory);
    }

    /** @return {@code true} when the event has already been resolved. */
    public boolean isClosed() {
        return event.status() == EventStatus.CLOSED;
    }
}
