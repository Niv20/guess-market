package guessmarket.dto;

import java.util.List;

/**
 * What happened to an order the moment it reached the market.
 *
 * <p>An order can do any of three things, and often does more than one of them at once: it can be
 * filled against orders already waiting, it can create new shares together with a buyer of the
 * opposite option, and whatever is left of it waits in the book. The counts below say how much of
 * it did each, so the person can be told what actually happened rather than only that the order
 * "went through".
 *
 * @param orderId          the number this order was given
 * @param quantityFilled   how many shares were traded straight away
 * @param quantityMinted   how many of those were new shares created with a buyer of the other side
 * @param quantityResting  how many shares are still waiting in the book
 * @param cashSpent        what left the account, for a buy
 * @param cashReceived     what arrived in the account, for a sell
 * @param commissionPaid   the commission paid on the filled part
 * @param executions       the trades this order caused, newest first
 * @param usersBlocked     anybody this order left with a balance below zero, and who is therefore
 *                         blocked from now on. This is the one action that can do that: an order
 *                         that was affordable when it was placed can be filled later, after its
 *                         owner has spent the money somewhere else.
 * @param balanceAfter     what the account holds now
 * @param statusAfterOrder the event as it stands after the order was processed
 */
public record SubmitOrderResultDto(long orderId,
                                   long quantityFilled,
                                   long quantityMinted,
                                   long quantityResting,
                                   double cashSpent,
                                   double cashReceived,
                                   double commissionPaid,
                                   List<MarketTradeDto> executions,
                                   List<String> usersBlocked,
                                   double balanceAfter,
                                   EventTradingStatusDto statusAfterOrder) {

    public SubmitOrderResultDto {
        executions = List.copyOf(executions);
        usersBlocked = List.copyOf(usersBlocked);
    }

    /** @return whether the order did nothing but wait. */
    public boolean restedWithoutTrading() {
        return quantityFilled == 0;
    }
}
