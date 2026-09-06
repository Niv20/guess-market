package guessmarket.dto;

/**
 * One point on the price graph of an option: what a share of it was worth after a given
 * transaction in its event.
 *
 * <p>The horizontal axis counts transactions rather than clock time on purpose. The system has no
 * notion of when anything happened, and in a market it is the trades that move a price, so
 * plotting against them shows exactly the same shape without inventing timestamps.
 *
 * @param tradeNumber which transaction of the event this is, starting at 0 for the opening state
 * @param optionName  the option being priced
 * @param price       what one share was worth at that point
 */
public record PricePointDto(int tradeNumber, String optionName, double price) {
}
