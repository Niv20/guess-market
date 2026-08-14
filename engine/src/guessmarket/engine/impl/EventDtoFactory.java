package guessmarket.engine.impl;

import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.OptionStateDto;
import guessmarket.dto.TradeRecordDto;
import guessmarket.engine.market.Event;
import guessmarket.engine.market.EventAccount;
import guessmarket.engine.market.EventOption;
import guessmarket.engine.market.LmsrMechanism;
import guessmarket.engine.market.Trade;

import java.util.ArrayList;
import java.util.List;

/**
 * Copies the state of a domain event into data transfer objects.
 *
 * <p>The conversion goes all the way down: nothing that leaves this class holds a reference to
 * a domain object, so a caller can read an answer as long as it likes without seeing the system
 * change underneath it, and without any way of changing the system itself.
 *
 * <p>A fresh set of objects is built on every call. They describe the system as it was at the
 * moment of the request and are never kept in step with it afterwards.
 */
final class EventDtoFactory {

    private EventDtoFactory() {
    }

    static List<EventDto> toEventDtos(Iterable<Event> events) {
        List<EventDto> eventDtos = new ArrayList<>();
        for (Event event : events) {
            eventDtos.add(toEventDto(event));
        }
        return eventDtos;
    }

    static EventDto toEventDto(Event event) {
        List<String> optionNames = new ArrayList<>();
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
                liquidityParameterOf(event));
    }

    static EventTradingStatusDto toTradingStatusDto(Event event) {
        EventAccount account = event.getAccount();
        return new EventTradingStatusDto(toEventDto(event),
                toOptionStateDtos(event),
                account.getBalance(),
                account.getTotalCommissionCollected(),
                account.getSubsidy(),
                account.getMarketMakerNetResult(),
                toTradeHistoryNewestFirst(event),
                event.getWinningOptionName());
    }

    private static List<OptionStateDto> toOptionStateDtos(Event event) {
        List<EventOption> options = event.getOptions();
        List<OptionStateDto> optionStates = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            EventOption option = options.get(i);
            optionStates.add(new OptionStateDto(option.getName(),
                    event.optionValue(i), option.getSharesPurchased()));
        }
        return optionStates;
    }

    private static List<TradeRecordDto> toTradeHistoryNewestFirst(Event event) {
        List<Trade> trades = event.getTrades();
        List<TradeRecordDto> history = new ArrayList<>(trades.size());
        for (int i = trades.size() - 1; i >= 0; i--) {
            Trade trade = trades.get(i);
            history.add(new TradeRecordDto(trade.getSerialNumber(),
                    trade.getOptionName(),
                    trade.getShares(),
                    trade.getAmountPaidForShares(),
                    trade.getCommissionPaid(),
                    trade.getTotalPaid()));
        }
        return history;
    }

    /**
     * LMSR is the only trading method of this exercise, so the liquidity parameter is part of
     * what describes an event. A method that has no such parameter reports 0.
     */
    private static int liquidityParameterOf(Event event) {
        return event.getMechanism() instanceof LmsrMechanism lmsr ? lmsr.getLiquidityParameter() : 0;
    }
}
