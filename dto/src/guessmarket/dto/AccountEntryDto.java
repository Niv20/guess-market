package guessmarket.dto;

/**
 * One immutable account statement row. A positive amount is a credit and a negative amount is
 * a debit. Sequence zero describes the opening balance; subsequent numbers identify operations.
 */
public record AccountEntryDto(long sequence, String description, double amount,
                              double balanceBefore, double balanceAfter) {
}
