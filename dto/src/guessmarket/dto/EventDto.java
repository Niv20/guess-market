package guessmarket.dto;

import java.util.List;

/**
 * The identifying details of a single event, as required by the "display events" command.
 *
 * @param id                  the unique identifier of the event, as given in the loaded file
 * @param name                the name of the event
 * @param description         the free text description and resolution conditions
 * @param commissionPercent   the commission of the event, as a whole percentage
 * @param commissionType      when the commission is collected
 * @param optionNames         the names of the event options, in their file order
 * @param status              whether the event is still open for trading
 * @param liquidityParameter  the LMSR liquidity parameter (b) of the event
 */
public record EventDto(int id,
                       String name,
                       String description,
                       int commissionPercent,
                       CommissionType commissionType,
                       List<String> optionNames,
                       EventStatus status,
                       int liquidityParameter) {

    public EventDto {
        optionNames = List.copyOf(optionNames);
    }
}
