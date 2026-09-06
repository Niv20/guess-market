package guessmarket.dto;

/**
 * What one participant holds in one option of one event.
 *
 * @param optionName   the option held
 * @param shares       how many shares of it are held
 * @param amountPaid   what was paid for them, less anything received for selling some of them
 *                     again, so that this reads as what the position has cost so far
 * @param currentValue what those shares are worth at the moment, at the option's current price
 */
public record HoldingDto(String optionName, long shares, double amountPaid, double currentValue) {
}
