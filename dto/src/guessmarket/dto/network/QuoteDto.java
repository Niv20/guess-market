package guessmarket.dto.network;

/** Current LMSR cost before commission; the final transaction recalculates the price. */
public record QuoteDto(double cost) {
}
