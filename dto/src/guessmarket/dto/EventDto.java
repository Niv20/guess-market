package guessmarket.dto;

import java.util.List;

/**
 * Everything that describes one event, as any screen listing events needs it.
 *
 * <p>Exactly one of {@link #lmsr()} and {@link #orderBook()} is present, according to
 * {@link #tradingMethod()}. Keeping the settings of the two methods in separate records rather
 * than in one record full of fields that only sometimes mean anything is what stops a screen from
 * showing a liquidity parameter for an event that has no such thing.
 *
 * @param id                       the number the file gave the event
 * @param name                     its name
 * @param description              what it asks and how it will be decided
 * @param commissionPercent        the commission rate, as a whole percentage
 * @param commissionType           whether that commission is taken on every purchase or at the end
 * @param optionNames              the two possible outcomes, in file order
 * @param status                   whether it has started, is running, or has been decided
 * @param tradingMethod            how trading in it works
 * @param marketMakerName          the user who finances, opens and closes it
 * @param accountBalance           what the event's own account holds at the moment
 * @param totalCommissionCollected what its commission has earned the market maker so far
 * @param lmsr                     the LMSR settings, or null for an order book event
 * @param orderBook                the order book settings, or null for an LMSR event
 * @param winningOptionName        the option that won, or null while the event is not closed
 */
public record EventDto(int id,
                       String name,
                       String description,
                       int commissionPercent,
                       CommissionType commissionType,
                       List<String> optionNames,
                       EventStatus status,
                       TradingMethod tradingMethod,
                       String marketMakerName,
                       double accountBalance,
                       double totalCommissionCollected,
                       LmsrSettingsDto lmsr,
                       OrderBookSettingsDto orderBook,
                       String winningOptionName) {

    public EventDto {
        optionNames = List.copyOf(optionNames);
    }

    public boolean isLmsr() {
        return tradingMethod == TradingMethod.LMSR;
    }

    public boolean isOrderBook() {
        return tradingMethod == TradingMethod.ORDER_BOOK;
    }

    public boolean isActive() {
        return status == EventStatus.ACTIVE;
    }

    public boolean isClosed() {
        return status == EventStatus.CLOSED;
    }

    /** @return what one share of the winning option pays out; LMSR events always pay one. */
    public double baseValue() {
        return orderBook == null ? 1 : orderBook.baseValue();
    }
}
