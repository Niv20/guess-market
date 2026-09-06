package guessmarket.dto;

/**
 * What one participant received when an event was settled.
 *
 * @param userName      who was paid
 * @param winningShares how many winning shares they held
 * @param grossAmount   what those shares were worth
 * @param commission    what was kept back from them as commission, if any
 * @param netAmount     what actually reached their account
 */
public record PayoutDto(String userName,
                        long winningShares,
                        double grossAmount,
                        double commission,
                        double netAmount) {
}
