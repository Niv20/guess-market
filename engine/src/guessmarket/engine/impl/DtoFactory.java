package guessmarket.engine.impl;

import guessmarket.dto.BalancePointDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.HoldingDto;
import guessmarket.dto.LmsrSettingsDto;
import guessmarket.dto.MarketTradeDto;
import guessmarket.dto.OptionBookDto;
import guessmarket.dto.OptionStateDto;
import guessmarket.dto.OrderBookSettingsDto;
import guessmarket.dto.OrderDto;
import guessmarket.dto.ParticipantDto;
import guessmarket.dto.PayoutDto;
import guessmarket.dto.PricePointDto;
import guessmarket.dto.TradeKind;
import guessmarket.dto.TradeRecordDto;
import guessmarket.dto.UserDto;
import guessmarket.dto.UserEventInvolvementDto;
import guessmarket.dto.UserLmsrInvolvementDto;
import guessmarket.dto.UserOrderBookInvolvementDto;
import guessmarket.engine.market.BalanceChange;
import guessmarket.engine.market.Event;
import guessmarket.engine.market.EventOption;
import guessmarket.engine.market.LmsrEvent;
import guessmarket.engine.market.MarketTrade;
import guessmarket.engine.market.OptionBook;
import guessmarket.engine.market.Order;
import guessmarket.engine.market.OrderBookEvent;
import guessmarket.engine.market.Payout;
import guessmarket.engine.market.User;
import guessmarket.engine.market.UserPosition;

import java.util.ArrayList;
import java.util.List;

/**
 * Copies the state of the domain into data transfer objects.
 *
 * <p>The conversion goes all the way down: nothing that leaves this class holds a reference to a
 * domain object, so a caller can hold on to an answer for as long as it likes without seeing the
 * system change underneath it, and without any way of changing the system itself.
 *
 * <p>A fresh set of objects is built on every call. They describe the system as it was at the
 * moment of the request and are never kept in step with it afterwards, which is exactly what makes
 * the engine passive: it answers questions, and it is the caller that decides when to ask again.
 */
final class DtoFactory {

    private DtoFactory() {
    }

    // ------------------------------------------------------------------ events

    static List<EventDto> toEventDtos(List<Event> events) {
        List<EventDto> eventDtos = new ArrayList<>(events.size());
        for (Event event : events) {
            eventDtos.add(toEventDto(event));
        }
        return eventDtos;
    }

    static EventDto toEventDto(Event event) {
        List<String> optionNames = new ArrayList<>(event.getOptionCount());
        for (EventOption option : event.getOptions()) {
            optionNames.add(option.getName());
        }
        return new EventDto(event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommission().getPercent(),
                event.getCommission().getType(),
                optionNames,
                event.getStatus(),
                event.getTradingMethod(),
                event.getMarketMakerName(),
                event.getAccount().getBalance(),
                event.getAccount().getTotalCommissionCollected(),
                lmsrSettingsOf(event),
                orderBookSettingsOf(event),
                event.getWinningOptionName());
    }

    private static LmsrSettingsDto lmsrSettingsOf(Event event) {
        return event instanceof LmsrEvent lmsr
                ? new LmsrSettingsDto(lmsr.getLiquidityParameter(), lmsr.getOpeningCost())
                : null;
    }

    private static OrderBookSettingsDto orderBookSettingsOf(Event event) {
        return event instanceof OrderBookEvent book
                ? new OrderBookSettingsDto(book.getInitialInvestment(), book.getInitialPairs(),
                        (int) book.getBaseValue(), book.isMintAllowed())
                : null;
    }

    static EventTradingStatusDto toTradingStatusDto(Event event) {
        return new EventTradingStatusDto(toEventDto(event),
                toOptionStateDtos(event),
                toBookDtos(event),
                event.getAccount().getSubsidy(),
                toTradeHistoryNewestFirst(event),
                toParticipantDtos(event),
                sharesOutstanding(event));
    }

    /**
     * @return one entry per option for an LMSR event, and nothing at all for an order book, where a
     *         share has no single value and the books say everything there is to say instead
     */
    private static List<OptionStateDto> toOptionStateDtos(Event event) {
        if (!(event instanceof LmsrEvent)) {
            return List.of();
        }
        List<OptionStateDto> states = new ArrayList<>(event.getOptionCount());
        for (int i = 0; i < event.getOptionCount(); i++) {
            Double price = event.currentPrice(i);
            states.add(new OptionStateDto(event.getOptionName(i),
                    price == null ? 0 : price,
                    event.getOptions().get(i).getSharesOutstanding()));
        }
        return states;
    }

    /** @return one book per option for an order book event, and nothing at all for LMSR. */
    private static List<OptionBookDto> toBookDtos(Event event) {
        if (!(event instanceof OrderBookEvent orderBookEvent)) {
            return List.of();
        }
        List<OptionBookDto> books = new ArrayList<>(event.getOptionCount());
        for (OptionBook book : orderBookEvent.getBooks()) {
            books.add(new OptionBookDto(book.getOptionIndex(),
                    event.getOptionName(book.getOptionIndex()),
                    toOrderDtos(book.getBids()),
                    toOrderDtos(book.getAsks()),
                    book.getLastTradePrice(),
                    book.getBestBidPrice(),
                    book.getBestAskPrice(),
                    book.getMidPrice(),
                    book.getSpread()));
        }
        return books;
    }

    private static List<OrderDto> toOrderDtos(List<Order> orders) {
        List<OrderDto> orderDtos = new ArrayList<>(orders.size());
        for (Order order : orders) {
            orderDtos.add(new OrderDto(order.getId(), order.getUserName(), order.getSide(),
                    order.getQuantity(), order.getRemaining(), order.getPricePerShare()));
        }
        return orderDtos;
    }

    private static List<MarketTradeDto> toTradeHistoryNewestFirst(Event event) {
        List<MarketTrade> trades = event.getTrades();
        List<MarketTradeDto> history = new ArrayList<>(trades.size());
        for (int i = trades.size() - 1; i >= 0; i--) {
            history.add(toMarketTradeDto(trades.get(i)));
        }
        return history;
    }

    static MarketTradeDto toMarketTradeDto(MarketTrade trade) {
        return new MarketTradeDto(trade.getSerialNumber(), trade.getKind(), trade.getOptionName(),
                trade.getShares(), trade.getPricePerShare(), trade.getTotalPrice(),
                trade.getCommission(), trade.getBuyerName(), trade.getSellerName());
    }

    static List<MarketTradeDto> toMarketTradeDtos(List<MarketTrade> trades) {
        List<MarketTradeDto> tradeDtos = new ArrayList<>(trades.size());
        for (MarketTrade trade : trades) {
            tradeDtos.add(toMarketTradeDto(trade));
        }
        return tradeDtos;
    }

    private static List<ParticipantDto> toParticipantDtos(Event event) {
        List<ParticipantDto> participants = new ArrayList<>();
        for (UserPosition position : event.getPositions()) {
            List<HoldingDto> holdings = toHoldingDtos(event, position);
            double totalValue = 0;
            for (HoldingDto holding : holdings) {
                totalValue += holding.currentValue();
            }
            participants.add(new ParticipantDto(position.getUserName(),
                    event.isMarketMaker(position.getUserName()),
                    holdings,
                    openOrderCount(event, position.getUserName()),
                    totalValue));
        }
        return participants;
    }

    private static List<HoldingDto> toHoldingDtos(Event event, UserPosition position) {
        List<HoldingDto> holdings = new ArrayList<>(event.getOptionCount());
        for (int i = 0; i < event.getOptionCount(); i++) {
            Double price = event.currentPrice(i);
            long shares = position.getShares(i);
            holdings.add(new HoldingDto(event.getOptionName(i), shares,
                    position.getAmountPaid(i), price == null ? 0 : shares * price));
        }
        return holdings;
    }

    private static int openOrderCount(Event event, String userName) {
        return event instanceof OrderBookEvent book ? book.openOrdersOf(userName).size() : 0;
    }

    private static List<Long> sharesOutstanding(Event event) {
        List<Long> totals = new ArrayList<>(event.getOptionCount());
        for (EventOption option : event.getOptions()) {
            totals.add(option.getSharesOutstanding());
        }
        return totals;
    }

    // ------------------------------------------------------------------ users

    static List<UserDto> toUserDtos(List<User> users) {
        List<UserDto> userDtos = new ArrayList<>(users.size());
        for (User user : users) {
            userDtos.add(toUserDto(user));
        }
        return userDtos;
    }

    static UserDto toUserDto(User user) {
        return new UserDto(user.getName(),
                user.getBalance(),
                user.getAccount().getInitialBalance(),
                user.isBlocked(),
                user.getMarketMakerEventIds(),
                user.getParticipatingEventIds());
    }

    /**
     * @return everything this user has to do with this event: whether they run it, whether they
     *         have acted in it, and whichever of the two kinds of involvement its trading method
     *         calls for
     */
    static UserEventInvolvementDto toInvolvementDto(User user, Event event) {
        UserPosition position = event.findPosition(user.getName());
        boolean marketMaker = event.isMarketMaker(user.getName());
        boolean participant = position != null;

        if (event instanceof LmsrEvent) {
            return new UserEventInvolvementDto(toEventDto(event), marketMaker, participant,
                    toLmsrInvolvement(user, event, position), null);
        }
        return new UserEventInvolvementDto(toEventDto(event), marketMaker, participant,
                null, toOrderBookInvolvement(user, (OrderBookEvent) event, position));
    }

    private static UserLmsrInvolvementDto toLmsrInvolvement(User user, Event event,
                                                            UserPosition position) {
        List<TradeRecordDto> trades = new ArrayList<>();
        List<MarketTrade> all = event.getTrades();
        for (int i = all.size() - 1; i >= 0; i--) {
            MarketTrade trade = all.get(i);
            if (trade.getKind() == TradeKind.LMSR_PURCHASE
                    && user.getName().equalsIgnoreCase(trade.getBuyerName())) {
                trades.add(new TradeRecordDto(trade.getSerialNumber(), trade.getOptionName(),
                        trade.getShares(), trade.getTotalPrice(), trade.getCommission(),
                        trade.getTotalPaidByBuyer()));
            }
        }
        return new UserLmsrInvolvementDto(trades,
                position == null ? List.of() : toHoldingDtos(event, position),
                position == null ? 0 : position.getCommissionPaid(),
                event.getStatus() == guessmarket.dto.EventStatus.CLOSED
                        ? toOptionStateDtos(event) : List.of(),
                closedAmount(event, position == null ? null : position.getAmountWon()));
    }

    private static UserOrderBookInvolvementDto toOrderBookInvolvement(User user,
                                                                      OrderBookEvent event,
                                                                      UserPosition position) {
        return new UserOrderBookInvolvementDto(
                position == null ? List.of() : toHoldingDtos(event, position),
                toOrderDtos(event.openOrdersOf(user.getName())),
                position == null ? 0 : position.getCommissionPaid(),
                closedAmount(event, position == null ? null : position.getNetResult()));
    }

    /** @return the figure, but only once the event has actually been decided. */
    private static Double closedAmount(Event event, Double value) {
        return event.getStatus() == guessmarket.dto.EventStatus.CLOSED ? value : null;
    }

    // ------------------------------------------------------------------ settlement and graphs

    static List<PayoutDto> toPayoutDtos(List<Payout> payouts) {
        List<PayoutDto> payoutDtos = new ArrayList<>(payouts.size());
        for (Payout payout : payouts) {
            payoutDtos.add(new PayoutDto(payout.userName(), payout.winningShares(),
                    payout.grossAmount(), payout.commission(), payout.netAmount()));
        }
        return payoutDtos;
    }

    /**
     * @return the price of every option after every transaction, oldest first. A point at which an
     *         option had no price at all, which only happens in an untouched order book, is left
     *         out rather than drawn as a zero.
     */
    static List<PricePointDto> toPriceHistory(Event event) {
        List<PricePointDto> points = new ArrayList<>();
        List<double[]> snapshots = event.getPriceHistory();
        for (int step = 0; step < snapshots.size(); step++) {
            double[] prices = snapshots.get(step);
            for (int option = 0; option < prices.length; option++) {
                if (!Double.isNaN(prices[option])) {
                    points.add(new PricePointDto(step, event.getOptionName(option), prices[option]));
                }
            }
        }
        return points;
    }

    static List<BalancePointDto> toBalanceHistory(User user) {
        List<BalanceChange> changes = user.getBalanceHistory();
        List<BalancePointDto> points = new ArrayList<>(changes.size());
        for (int step = 0; step < changes.size(); step++) {
            points.add(new BalancePointDto(step, changes.get(step).reason(),
                    changes.get(step).balance()));
        }
        return points;
    }
}
