package guessmarket.dto;

/**
 * The current trading state of a single option of an event.
 *
 * @param name             the name of the option
 * @param value            the current value of one share, always between 0 and 1
 * @param sharesPurchased  the total number of shares of this option bought so far
 */
public record OptionStateDto(String name, double value, long sharesPurchased) {
}
