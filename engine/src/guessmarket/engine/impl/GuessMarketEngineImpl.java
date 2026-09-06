package guessmarket.engine.impl;

import guessmarket.dto.CloseEventRequestDto;
import guessmarket.dto.CloseEventResultDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.LoadResultDto;
import guessmarket.dto.PurchaseRequestDto;
import guessmarket.dto.PurchaseResultDto;
import guessmarket.dto.StateFileResultDto;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.exception.SystemNotLoadedException;
import guessmarket.engine.market.Event;
import guessmarket.engine.market.GuessMarketSystem;
import guessmarket.engine.market.Settlement;
import guessmarket.engine.market.Trade;
import guessmarket.engine.persistence.SystemStateStore;
import guessmarket.engine.xml.SystemFileLoader;

import java.util.List;

/**
 * The one implementation of {@link GuessMarketEngine}.
 *
 * <p>It owns the loaded system and does three things with it: it decides whether a request is
 * allowed to reach the system at all, it lets the domain objects carry the request out, and it
 * copies the answer into data transfer objects. None of the arithmetic of trading lives here;
 * that belongs to the events and to their trading mechanism.
 *
 * <p>Requests are checked here even when a caller has already checked them, because the engine
 * has no way of knowing which caller it is talking to.
 */
public class GuessMarketEngineImpl implements GuessMarketEngine {

    private final SystemFileLoader fileLoader = new SystemFileLoader();

    /** Null until a valid file has been loaded; replaced whole by every successful load. */
    private GuessMarketSystem system;

    @Override
    public LoadResultDto loadSystemDetailsFile(String path) {
        GuessMarketSystem loadedSystem = fileLoader.load(path);
        this.system = loadedSystem;
        return new LoadResultDto(path == null ? "" : path.trim(),
                loadedSystem.getEventCount(), 0);
    }

    @Override
    public boolean isSystemLoaded() {
        return system != null;
    }

    @Override
    public List<EventDto> getAllEvents() {
        return EventDtoFactory.toEventDtos(loadedSystem().getEvents());
    }

    @Override
    public List<EventDto> getActiveEvents() {
        return EventDtoFactory.toEventDtos(loadedSystem().getActiveEvents());
    }

    @Override
    public EventTradingStatusDto getEventTradingStatus(int eventId) {
        return EventDtoFactory.toTradingStatusDto(loadedSystem().getEvent(eventId));
    }

    @Override
    public PurchaseResultDto purchaseShares(PurchaseRequestDto request) {
        requireRequest(request);
        Event event = loadedSystem().getEvent(request.eventId());
        Trade trade = event.buyShares(request.optionIndex(), request.shares());
        return new PurchaseResultDto(trade.getOptionName(),
                trade.getShares(),
                trade.getAmountPaidForShares(),
                trade.getCommissionPaid(),
                trade.getTotalPaid(),
                EventDtoFactory.toTradingStatusDto(event));
    }

    @Override
    public CloseEventResultDto closeEvent(CloseEventRequestDto request) {
        requireRequest(request);
        Event event = loadedSystem().getEvent(request.eventId());
        Settlement settlement = event.close(request.winningOptionIndex());
        return new CloseEventResultDto(settlement.winningOptionName(),
                settlement.winningShares(),
                settlement.payoutPot(),
                settlement.commissionCollected(),
                settlement.amountPaidToWinners(),
                settlement.payoutPerShare(),
                EventDtoFactory.toTradingStatusDto(event));
    }

    @Override
    public StateFileResultDto saveSystemState(String pathWithoutExtension) {
        GuessMarketSystem systemToSave = loadedSystem();
        String writtenFile = SystemStateStore.save(systemToSave, pathWithoutExtension);
        return new StateFileResultDto(writtenFile, systemToSave.getEventCount());
    }

    @Override
    public StateFileResultDto loadSystemState(String pathWithoutExtension) {
        GuessMarketSystem restoredSystem = SystemStateStore.load(pathWithoutExtension);
        this.system = restoredSystem;
        return new StateFileResultDto(pathWithoutExtension.trim()
                + SystemStateStore.STATE_FILE_EXTENSION, restoredSystem.getEventCount());
    }

    /** @return the loaded system, or fails when nothing has been loaded yet. */
    private GuessMarketSystem loadedSystem() {
        if (system == null) {
            throw new SystemNotLoadedException();
        }
        return system;
    }

    private static void requireRequest(Object request) {
        if (request == null) {
            throw new IllegalArgumentException("A request must be supplied, but it was missing.");
        }
    }
}
