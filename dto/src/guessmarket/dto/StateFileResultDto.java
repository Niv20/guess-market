package guessmarket.dto;

/**
 * The result of saving the system to a file, or of reading it back from one.
 *
 * @param filePath   the file that was actually written or read, extension included
 * @param eventCount how many events it holds
 */
public record StateFileResultDto(String filePath, int eventCount) {
}
