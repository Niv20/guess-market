package guessmarket.engine;

import guessmarket.dto.CloseEventRequestDto;
import guessmarket.dto.CloseEventResultDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.LoadResultDto;
import guessmarket.dto.PurchaseRequestDto;
import guessmarket.dto.PurchaseResultDto;
import guessmarket.engine.exception.EventNotActiveException;
import guessmarket.engine.exception.EventNotFoundException;
import guessmarket.engine.exception.FileLoadException;
import guessmarket.engine.exception.InvalidOptionSelectionException;
import guessmarket.engine.exception.InvalidShareQuantityException;
import guessmarket.engine.exception.SystemNotLoadedException;

import java.util.List;

/**
 * Everything the Guess Market system can do, as seen from outside it.
 *
 * <p>This interface is the only thing a user interface needs to know about the engine. The
 * engine is passive: it never starts anything by itself, it has no idea who is calling it, and
 * it answers only in data. Nothing it returns is a piece of formatted text and nothing it
 * returns is one of its own internal objects, so any user interface, console or otherwise, is
 * free to present the information in whatever way suits it.
 *
 * <p>All options are addressed by their position inside their event, starting at 0. Presenting
 * those positions to a person as numbers starting at 1 is the job of the user interface.
 *
 * <p>Every failure is reported by throwing one of the engine exceptions, all of which are
 * unchecked and carry a message written for the person using the program.
 */
public interface GuessMarketEngine {

    /**
     * Loads a system details file, replacing everything that was loaded before it.
     *
     * <p>The file is only allowed to change the system once it has been read and found valid,
     * so a rejected file leaves the previously loaded system exactly as it was. Loading resets
     * the account of every event and deposits the subsidy that opens its market.
     *
     * @param path the full path of the XML file to load
     * @return what was loaded
     * @throws FileLoadException if the path, the XML, or the system it describes is not usable
     */
    LoadResultDto loadSystemDetailsFile(String path);

    /** @return whether a valid file has already been loaded. */
    boolean isSystemLoaded();

    /**
     * @return every event in the system, in the order of the loaded file
     * @throws SystemNotLoadedException if no valid file has been loaded yet
     */
    List<EventDto> getAllEvents();

    /**
     * @return only the events that are still open for trading, in the order of the loaded file
     * @throws SystemNotLoadedException if no valid file has been loaded yet
     */
    List<EventDto> getActiveEvents();

    /**
     * @param eventId the id of the event to inspect
     * @return the full trading picture of that event: option values and holdings, the money in
     *         its account, the commission it collected and its trading history
     * @throws SystemNotLoadedException if no valid file has been loaded yet
     * @throws EventNotFoundException   if no event carries that id
     */
    EventTradingStatusDto getEventTradingStatus(int eventId);

    /**
     * Buys shares of one option of an active event.
     *
     * @param request which event, which option and how many shares
     * @return what the purchase cost, and the state of the event right after it
     * @throws SystemNotLoadedException        if no valid file has been loaded yet
     * @throws EventNotFoundException          if no event carries that id
     * @throws EventNotActiveException         if the event has already been closed
     * @throws InvalidOptionSelectionException if the event has no such option
     * @throws InvalidShareQuantityException   if fewer than one share was requested
     */
    PurchaseResultDto purchaseShares(PurchaseRequestDto request);

    /**
     * Closes an active event with one of its options as the winner, and pays the winners.
     *
     * @param request which event, and which option won
     * @return what was paid and collected, and the state of the event afterwards
     * @throws SystemNotLoadedException        if no valid file has been loaded yet
     * @throws EventNotFoundException          if no event carries that id
     * @throws EventNotActiveException         if the event has already been closed
     * @throws InvalidOptionSelectionException if the event has no such option
     */
    CloseEventResultDto closeEvent(CloseEventRequestDto request);
}
