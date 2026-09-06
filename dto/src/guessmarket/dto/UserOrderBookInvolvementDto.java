package guessmarket.dto;

import java.util.List;

/**
 * What one user has going on inside one order book event.
 *
 * @param holdings            the shares they hold in each option, and what those cost them
 * @param openOrders          their orders that are still waiting in the books
 * @param totalCommissionPaid the commission they have paid in this event altogether
 * @param profitOrLoss        what taking part in the event ended up being worth to them, or null
 *                            while the event is not closed
 */
public record UserOrderBookInvolvementDto(List<HoldingDto> holdings,
                                          List<OrderDto> openOrders,
                                          double totalCommissionPaid,
                                          Double profitOrLoss) {

    public UserOrderBookInvolvementDto {
        holdings = List.copyOf(holdings);
        openOrders = List.copyOf(openOrders);
    }
}
