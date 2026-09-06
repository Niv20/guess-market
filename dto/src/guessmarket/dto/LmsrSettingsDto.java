package guessmarket.dto;

/**
 * What makes an LMSR event what it is.
 *
 * @param liquidityParameter the {@code b} of the event: how much a single purchase moves the price
 * @param subsidy            what the market maker has to put in to open it, {@code b * ln 2}
 */
public record LmsrSettingsDto(int liquidityParameter, double subsidy) {
}
