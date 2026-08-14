package guessmarket.dto;

/**
 * A summary of a file that was successfully loaded into the system.
 *
 * @param filePath      the file that was read
 * @param eventCount    how many events were loaded from it
 * @param totalSubsidy  the total subsidy deposited into the accounts of those events
 */
public record LoadResultDto(String filePath, int eventCount, double totalSubsidy) {
}
