package guessmarket.dto;

import java.util.List;

/**
 * A request from a user to create an event of their own, becoming its market maker.
 *
 * <p>The settings of both trading methods are carried here, and only the ones belonging to the
 * chosen method are read. An event created this way is exactly like one that came from the file:
 * it starts not yet running, and its creator has to open it like any other market maker.
 *
 * @param userName            the user creating the event, who becomes its market maker
 * @param name                the name of the new event
 * @param description         what it asks and how it will be decided
 * @param commissionPercent   its commission rate, as a whole percentage
 * @param commissionType      when that commission is collected
 * @param optionNames         its two possible outcomes
 * @param tradingMethod       how trading in it will work
 * @param liquidityParameter  the b of an LMSR event; ignored for an order book event
 * @param initialInvestment   what the market maker will pay to open an order book event
 * @param baseValue           the d of an order book event
 * @param mintAllowed         whether an order book event allows minting
 */
public record CreateEventRequestDto(String userName,
                                    String name,
                                    String description,
                                    int commissionPercent,
                                    CommissionType commissionType,
                                    List<String> optionNames,
                                    TradingMethod tradingMethod,
                                    int liquidityParameter,
                                    long initialInvestment,
                                    int baseValue,
                                    boolean mintAllowed) {

    public CreateEventRequestDto {
        optionNames = List.copyOf(optionNames);
    }
}
