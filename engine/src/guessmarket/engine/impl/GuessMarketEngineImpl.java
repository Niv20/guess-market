package guessmarket.engine.impl;

import guessmarket.dto.BalancePointDto;
import guessmarket.dto.CloseEventRequestDto;
import guessmarket.dto.CloseEventResultDto;
import guessmarket.dto.CreateEventRequestDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.LoadResultDto;
import guessmarket.dto.OpenEventRequestDto;
import guessmarket.dto.OpenEventResultDto;
import guessmarket.dto.PricePointDto;
import guessmarket.dto.PurchaseRequestDto;
import guessmarket.dto.PurchaseResultDto;
import guessmarket.dto.StateFileResultDto;
import guessmarket.dto.SubmitOrderRequestDto;
import guessmarket.dto.SubmitOrderResultDto;
import guessmarket.dto.UserDto;
import guessmarket.dto.UserEventInvolvementDto;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.exception.SystemNotLoadedException;
import guessmarket.engine.exception.UnsupportedTradingActionException;
import guessmarket.engine.market.Event;
import guessmarket.engine.market.GuessMarketSystem;
import guessmarket.engine.market.LmsrEvent;
import guessmarket.engine.market.OrderBookEvent;
import guessmarket.engine.market.OrderOutcome;
import guessmarket.engine.market.Purchase;
import guessmarket.engine.market.Settlement;
import guessmarket.engine.market.User;
import guessmarket.engine.persistence.SystemStateStore;
import guessmarket.engine.xml.SystemFileLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * The one implementation of {@link GuessMarketEngine}.
 *
 * <p>It owns the loaded system and does three things with it: it decides whether a request is
 * allowed to reach the system at all, it lets the domain objects carry the request out, and it
 * copies the answer into data transfer objects. None of the arithmetic of trading lives here; that
 * belongs to the events, to their books and to the LMSR formulas.
 *
 * <p>Requests are checked here even when a caller has already checked them, because the engine has
 * no way of knowing which caller it is talking to.
 */
public class GuessMarketEngineImpl implements GuessMarketEngine {

    private final SystemFileLoader fileLoader = new SystemFileLoader();

    /** Null until a valid file has been loaded; replaced whole by every successful load. */
    private GuessMarketSystem system;

    // ------------------------------------------------------------------ loading

    @Override
    public LoadResultDto loadSystemDetailsFile(String path) {
        GuessMarketSystem loadedSystem = fileLoader.load(path);
        this.system = loadedSystem;
        return new LoadResultDto(path == null ? "" : path.trim(),
                loadedSystem.getEventCount(), loadedSystem.getUserCount());
    }

    @Override
    public boolean isSystemLoaded() {
        return system != null;
    }

    // ------------------------------------------------------------------ looking at the system

    @Override
    public List<EventDto> getAllEvents() {
        return DtoFactory.toEventDtos(loadedSystem().getEvents());
    }

    @Override
    public EventTradingStatusDto getEventTradingStatus(int eventId) {
        return DtoFactory.toTradingStatusDto(loadedSystem().requireEvent(eventId));
    }

    @Override
    public List<UserDto> getAllUsers() {
        return DtoFactory.toUserDtos(loadedSystem().getUsers());
    }

    @Override
    public UserDto getUser(String userName) {
        return DtoFactory.toUserDto(loadedSystem().requireUser(userName));
    }

    @Override
    public List<UserEventInvolvementDto> getUserInvolvements(String userName) {
        User user = loadedSystem().requireUser(userName);
        List<UserEventInvolvementDto> involvements = new ArrayList<>();
        for (Event event : loadedSystem().getEvents()) {
            if (event.isMarketMaker(user.getName()) || event.findPosition(user.getName()) != null) {
                involvements.add(DtoFactory.toInvolvementDto(user, event));
            }
        }
        return involvements;
    }

    @Override
    public UserEventInvolvementDto getUserInvolvement(String userName, int eventId) {
        User user = loadedSystem().requireUser(userName);
        return DtoFactory.toInvolvementDto(user, loadedSystem().requireEvent(eventId));
    }

    // ------------------------------------------------------------------ running an event

    @Override
    public OpenEventResultDto openEvent(OpenEventRequestDto request) {
        requireRequest(request);
        GuessMarketSystem loaded = loadedSystem();
        Event event = loaded.requireEvent(request.eventId());

        double amountPaid = event.open(loaded, request.userName());
        User marketMaker = loaded.requireUser(request.userName());
        long sharesPerOption = event instanceof OrderBookEvent book ? book.getInitialPairs() : 0;

        return new OpenEventResultDto(event.getName(), amountPaid, sharesPerOption,
                marketMaker.getBalance(), DtoFactory.toTradingStatusDto(event));
    }

    @Override
    public CloseEventResultDto closeEvent(CloseEventRequestDto request) {
        requireRequest(request);
        GuessMarketSystem loaded = loadedSystem();
        Event event = loaded.requireEvent(request.eventId());

        Settlement settlement =
                event.close(loaded, request.userName(), request.winningOptionIndex());

        return new CloseEventResultDto(event.getName(),
                settlement.winningOptionName(),
                settlement.winningShares(),
                settlement.grossPayout(),
                settlement.commissionCollected(),
                settlement.amountPaidToWinners(),
                DtoFactory.toPayoutDtos(settlement.payouts()),
                settlement.returnedToMarketMaker(),
                DtoFactory.toTradingStatusDto(event));
    }

    // ------------------------------------------------------------------ taking part

    @Override
    public double quotePurchaseCost(int eventId, int optionIndex, long shares) {
        return requireLmsr(loadedSystem().requireEvent(eventId)).quotePurchase(optionIndex, shares);
    }

    @Override
    public PurchaseResultDto purchaseShares(PurchaseRequestDto request) {
        requireRequest(request);
        GuessMarketSystem loaded = loadedSystem();
        LmsrEvent event = requireLmsr(loaded.requireEvent(request.eventId()));

        Purchase purchase = event.buyShares(loaded, request.userName(),
                request.optionIndex(), request.shares());

        return new PurchaseResultDto(purchase.optionName(),
                purchase.shares(),
                purchase.amountPaidForShares(),
                purchase.commissionPaid(),
                purchase.totalPaid(),
                purchase.balanceAfter(),
                DtoFactory.toTradingStatusDto(event));
    }

    @Override
    public SubmitOrderResultDto submitOrder(SubmitOrderRequestDto request) {
        requireRequest(request);
        GuessMarketSystem loaded = loadedSystem();
        OrderBookEvent event = requireOrderBook(loaded.requireEvent(request.eventId()));

        OrderOutcome outcome = event.submitOrder(loaded, request.userName(), request.optionIndex(),
                request.side(), request.quantity(), request.pricePerShare());

        return new SubmitOrderResultDto(outcome.getOrderId(),
                outcome.getFilled(),
                outcome.getMinted(),
                outcome.getResting(),
                outcome.getCashSpent(),
                outcome.getCashReceived(),
                outcome.getCommissionPaid(),
                DtoFactory.toMarketTradeDtos(outcome.getExecutions()),
                outcome.getBlockedUsers(),
                loaded.requireUser(request.userName()).getBalance(),
                DtoFactory.toTradingStatusDto(event));
    }

    // ------------------------------------------------------------------ extras

    @Override
    public EventDto createEvent(CreateEventRequestDto request) {
        requireRequest(request);
        GuessMarketSystem loaded = loadedSystem();
        Event event = NewEventBuilder.build(loaded, request);
        loaded.addEvent(event);
        loaded.assignMarketMaker(event.getMarketMakerName(), event.getId());
        return DtoFactory.toEventDto(event);
    }

    @Override
    public List<PricePointDto> getEventPriceHistory(int eventId) {
        return DtoFactory.toPriceHistory(loadedSystem().requireEvent(eventId));
    }

    @Override
    public List<BalancePointDto> getUserBalanceHistory(String userName) {
        return DtoFactory.toBalanceHistory(loadedSystem().requireUser(userName));
    }

    // ------------------------------------------------------------------ saving and restoring

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

    // ------------------------------------------------------------------ checks

    /** @return the loaded system, or fails when nothing has been loaded yet. */
    private GuessMarketSystem loadedSystem() {
        if (system == null) {
            throw new SystemNotLoadedException();
        }
        return system;
    }

    private static LmsrEvent requireLmsr(Event event) {
        if (event instanceof LmsrEvent lmsr) {
            return lmsr;
        }
        throw new UnsupportedTradingActionException(event.getName(),
                event.getTradingMethod().getDisplayName(),
                "buying shares straight from the event");
    }

    private static OrderBookEvent requireOrderBook(Event event) {
        if (event instanceof OrderBookEvent book) {
            return book;
        }
        throw new UnsupportedTradingActionException(event.getName(),
                event.getTradingMethod().getDisplayName(), "placing an order in a book");
    }

    private static void requireRequest(Object request) {
        if (request == null) {
            throw new IllegalArgumentException("A request must be supplied, but it was missing.");
        }
    }
}
