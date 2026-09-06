package guessmarket.dto;

import java.util.List;

/**
 * The order book of one option, together with the figures a market is usually read by.
 *
 * <p>The four price figures can disagree with each other, and that is the point of showing all of
 * them: the last trade says what the market agreed on some time ago, the best bid and best ask say
 * what it is willing to do right now, and how far apart those two are says how likely it is that
 * anything happens at all. Every one of them is empty until there is something to base it on.
 *
 * @param optionIndex    the position of the option inside its event, starting at 0
 * @param optionName     the name of the option this book trades
 * @param bids           orders waiting to buy, highest price first
 * @param asks           orders waiting to sell, lowest price first
 * @param lastTradePrice the price of the most recent trade, or null if nothing has traded
 * @param bestBid        the highest price anybody is currently willing to pay, or null
 * @param bestAsk        the lowest price anybody is currently willing to accept, or null
 * @param midPrice       the average of the two, or null unless both sides have an order
 * @param spread         the distance between the two, or null unless both sides have an order
 */
public record OptionBookDto(int optionIndex,
                            String optionName,
                            List<OrderDto> bids,
                            List<OrderDto> asks,
                            Double lastTradePrice,
                            Double bestBid,
                            Double bestAsk,
                            Double midPrice,
                            Double spread) {

    public OptionBookDto {
        bids = List.copyOf(bids);
        asks = List.copyOf(asks);
    }

    /** @return how many shares are on offer to buy in total. */
    public long totalBidShares() {
        return bids.stream().mapToLong(OrderDto::remaining).sum();
    }

    /** @return how many shares are on offer to sell in total. */
    public long totalAskShares() {
        return asks.stream().mapToLong(OrderDto::remaining).sum();
    }
}
