package guessmarket.engine;

import guessmarket.dto.AccountEntryDto;
import guessmarket.dto.LoadResultDto;
import guessmarket.dto.UserDto;

import java.io.InputStream;
import java.util.List;

/**
 * The in-memory server engine. Every method is serialized on this engine's monitor, including
 * inherited trading and read operations. A caller can synchronize on the engine for a snapshot
 * made up of multiple queries. Disk loading and persistence are unavailable in online mode.
 */
public interface OnlineMarketEngine extends GuessMarketEngine {

    /** Registers a unique, case-insensitive name with an initial balance of zero. */
    UserDto registerUser(String userName);

    /** Adds a positive finite amount, with one new account statement entry. */
    UserDto depositFunds(String userName, double amount);

    /**
     * Validates an XML stream in memory and adds all its events atomically. The stream remains
     * the caller's responsibility to close. Existing state is untouched on any validation failure.
     */
    LoadResultDto uploadEvents(String userName, InputStream stream, String fileName);

    /** Returns all account changes, including payments caused by other users, oldest first. */
    List<AccountEntryDto> getAccountHistory(String userName);
}
