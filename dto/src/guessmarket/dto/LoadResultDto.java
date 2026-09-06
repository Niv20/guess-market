package guessmarket.dto;

/**
 * What a successful load of a system details file produced.
 *
 * @param filePath   the file that was loaded, as the system now knows it
 * @param eventCount how many events came out of it
 * @param userCount  how many users came out of it
 */
public record LoadResultDto(String filePath, int eventCount, int userCount) {
}
