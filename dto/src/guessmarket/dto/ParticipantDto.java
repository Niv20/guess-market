package guessmarket.dto;

import java.util.List;

/**
 * One participant of an event, as the event screen shows them.
 *
 * <p>Somebody counts as a participant from the moment they act in the event, so a person whose
 * order is still waiting in the book and has never been filled appears here as well, holding
 * nothing.
 *
 * @param userName     who they are
 * @param marketMaker  whether they are the market maker of this event
 * @param holdings     what they hold, one entry per option of the event
 * @param openOrders   how many of their orders are still waiting in the books
 * @param totalValue   what their holdings are worth altogether at current prices
 */
public record ParticipantDto(String userName,
                             boolean marketMaker,
                             List<HoldingDto> holdings,
                             int openOrders,
                             double totalValue) {

    public ParticipantDto {
        holdings = List.copyOf(holdings);
    }
}
