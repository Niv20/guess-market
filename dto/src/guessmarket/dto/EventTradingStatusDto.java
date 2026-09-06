package guessmarket.dto;

import java.util.List;

/**
 * The whole picture of one event: what it is, where its money is, who is in it and everything that
 * has happened in it.
 *
 * <p>One record serves both trading methods, and the two lists that only one of them fills are
 * simply empty for the other. An LMSR event has option values and no books; an order book event
 * has a book for each option and no option values, because under that method a share has no single
 * price, only the several figures inside {@link OptionBookDto}.
 *
 * @param event               the event itself
 * @param optionStates        the value and the shares outstanding of each option; LMSR events only
 * @param books               the order book of each option; order book events only
 * @param subsidy             what the market maker put in to open the event
 * @param tradeHistory        everything that has happened, newest first
 * @param participants        everybody who has acted in the event
 * @param totalSharesByOption how many shares of each option exist, in option order
 */
public record EventTradingStatusDto(EventDto event,
                                    List<OptionStateDto> optionStates,
                                    List<OptionBookDto> books,
                                    double subsidy,
                                    List<MarketTradeDto> tradeHistory,
                                    List<ParticipantDto> participants,
                                    List<Long> totalSharesByOption) {

    public EventTradingStatusDto {
        optionStates = List.copyOf(optionStates);
        books = List.copyOf(books);
        tradeHistory = List.copyOf(tradeHistory);
        participants = List.copyOf(participants);
        totalSharesByOption = List.copyOf(totalSharesByOption);
    }

    /** @return the event's own account balance, which is where the shares are backed from. */
    public double accountBalance() {
        return event.accountBalance();
    }

    /** @return what the commission of this event has earned its market maker so far. */
    public double totalCommissionCollected() {
        return event.totalCommissionCollected();
    }

    public boolean isClosed() {
        return event.isClosed();
    }

    /** @return the option that won, or null while the event is not closed. */
    public String winningOptionName() {
        return event.winningOptionName();
    }
}
