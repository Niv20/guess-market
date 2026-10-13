package guessmarket.dto;

/**
 * Describes a complete system state imported by the online server.
 *
 * @param fileName   the client-side display name of the imported state file
 * @param eventCount how many events the restored system contains
 * @param userCount  how many users the restored system contains
 */
public record StateTransferResultDto(String fileName, int eventCount, int userCount) {
}
