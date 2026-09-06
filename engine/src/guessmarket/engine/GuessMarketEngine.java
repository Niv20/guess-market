package guessmarket.engine;

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
import guessmarket.engine.exception.EventNotFoundException;
import guessmarket.engine.exception.FileLoadException;
import guessmarket.engine.exception.InsufficientFundsException;
import guessmarket.engine.exception.NotMarketMakerException;
import guessmarket.engine.exception.SystemNotLoadedException;
import guessmarket.engine.exception.SystemStateFileException;
import guessmarket.engine.exception.UnsupportedTradingActionException;
import guessmarket.engine.exception.UserBlockedException;
import guessmarket.engine.exception.UserNotFoundException;

import java.util.List;

/**
 * Everything the Guess Market system can do, as seen from outside it.
 *
 * <p>This interface is the only thing a user interface needs to know about the engine. The engine
 * is passive: it never starts anything by itself, it has no idea who is calling it, and it answers
 * only in data. Nothing it returns is a piece of formatted text and nothing it returns is one of
 * its own internal objects, so any user interface, graphical or otherwise, is free to present the
 * information in whatever way suits it.
 *
 * <p>Nothing here is a property and nothing here calls back. A caller that wants to know whether
 * something has changed asks again; the engine never reaches out. That is what keeps it usable
 * from a window today and from the far end of a network tomorrow.
 *
 * <p>Options are addressed by their position inside their event, starting at 0. Presenting those
 * positions to a person as numbers starting at 1 is the job of the user interface. Users are
 * addressed by name, and letter case never matters.
 *
 * <p>Every failure is reported by throwing one of the engine exceptions, all of which are unchecked
 * and carry a message written for the person using the program.
 */
public interface GuessMarketEngine {

    // ------------------------------------------------------------------ loading

    /**
     * Loads a system details file, replacing everything that was loaded before it.
     *
     * <p>The file is only allowed to change the system once it has been read and found valid, so a
     * rejected file leaves the previously loaded system exactly as it was. Events arrive not yet
     * started: nothing is subsidised and nothing is bought until a market maker opens them.
     *
     * @param path the full path of the XML file to load
     * @return what was loaded
     * @throws FileLoadException if the path, the XML, or the system it describes is not usable
     */
    LoadResultDto loadSystemDetailsFile(String path);

    /** @return whether a valid file has already been loaded. */
    boolean isSystemLoaded();

    // ------------------------------------------------------------------ looking at the system

    /**
     * @return every event in the system, in the order of the loaded file
     * @throws SystemNotLoadedException if no valid file has been loaded yet
     */
    List<EventDto> getAllEvents();

    /**
     * @param eventId the id of the event to inspect
     * @return the full picture of that event: its options or its order books, the money in its
     *         account, everything that has happened in it and everybody taking part
     * @throws EventNotFoundException if no event carries that id
     */
    EventTradingStatusDto getEventTradingStatus(int eventId);

    /**
     * @return every user in the system, in the order of the loaded file
     * @throws SystemNotLoadedException if no valid file has been loaded yet
     */
    List<UserDto> getAllUsers();

    /**
     * @param userName the name of the user, in any letter case
     * @return that user
     * @throws UserNotFoundException if the system has no such user
     */
    UserDto getUser(String userName);

    /**
     * @param userName the name of the user
     * @return what that user has to do with every event they run or have acted in, in event order
     * @throws UserNotFoundException if the system has no such user
     */
    List<UserEventInvolvementDto> getUserInvolvements(String userName);

    /**
     * @param userName the name of the user
     * @param eventId  the event to look at
     * @return what that user has to do with that one event, even if the answer is nothing at all
     */
    UserEventInvolvementDto getUserInvolvement(String userName, int eventId);

    // ------------------------------------------------------------------ running an event

    /**
     * Starts an event at its market maker's expense: an LMSR event receives its subsidy, and an
     * order book event receives the first pairs of shares, which become the market maker's.
     *
     * @throws NotMarketMakerException    if the user asking does not run the event
     * @throws InsufficientFundsException if they cannot pay for it
     * @throws UserBlockedException       if they have been blocked
     */
    OpenEventResultDto openEvent(OpenEventRequestDto request);

    /**
     * Closes an event, declares the outcome and pays the winners out of the event account.
     *
     * @throws NotMarketMakerException if the user asking does not run the event
     */
    CloseEventResultDto closeEvent(CloseEventRequestDto request);

    // ------------------------------------------------------------------ taking part

    /**
     * @param eventId     an LMSR event
     * @param optionIndex the option that would be bought
     * @param shares      how many shares would be bought
     * @return what that purchase would cost right now, before commission. Nothing is bought and
     *         nothing changes, so a screen can show the price before the person commits to it.
     * @throws UnsupportedTradingActionException if the event is not traded by LMSR
     */
    double quotePurchaseCost(int eventId, int optionIndex, long shares);

    /**
     * Buys shares of one option of an LMSR event, against the event itself.
     *
     * @throws UnsupportedTradingActionException if the event is traded by an order book
     * @throws InsufficientFundsException        if the buyer cannot pay for it
     */
    PurchaseResultDto purchaseShares(PurchaseRequestDto request);

    /**
     * Places an order in one of the books of an order book event and lets the market do whatever
     * the order makes possible: fill against what is waiting, mint new shares with a buyer of the
     * other option, and rest with whatever is left.
     *
     * @throws UnsupportedTradingActionException if the event is traded by LMSR
     */
    SubmitOrderResultDto submitOrder(SubmitOrderRequestDto request);

    // ------------------------------------------------------------------ extras

    /**
     * Creates a brand new event, whose creator becomes its market maker.
     *
     * <p>The event is exactly like one that came from the file: it starts not yet running, and its
     * creator has to open it before anybody can trade in it.
     */
    EventDto createEvent(CreateEventRequestDto request);

    /**
     * @param eventId the event to chart
     * @return what every option of it was worth after every transaction, oldest first
     */
    List<PricePointDto> getEventPriceHistory(int eventId);

    /**
     * @param userName the user to chart
     * @return every balance their account has held, oldest first
     */
    List<BalancePointDto> getUserBalanceHistory(String userName);

    // ------------------------------------------------------------------ saving and restoring

    /**
     * Writes the whole system, including everything that has been traded so far, to a file.
     *
     * @param pathWithoutExtension the full path and file name to save to, with no extension
     * @return the file that was written
     * @throws SystemNotLoadedException if there is nothing to save yet
     * @throws SystemStateFileException if the file cannot be written
     */
    StateFileResultDto saveSystemState(String pathWithoutExtension);

    /**
     * Reads a system that was saved earlier, replacing whatever is loaded now.
     *
     * <p>As with a system details file, the running system is only replaced once the saved one has
     * been read successfully.
     *
     * @param pathWithoutExtension the full path and file name that was saved to, with no extension
     * @return the file that was read
     * @throws SystemStateFileException if the file is missing or is not a saved system
     */
    StateFileResultDto loadSystemState(String pathWithoutExtension);
}
