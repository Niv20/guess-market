package guessmarket.ui.console;

import java.util.Locale;

/**
 * Turns the numbers that come back from the engine into the text this program prints.
 *
 * <p>Every number is formatted with {@link Locale#US} on purpose. Without it, the decimal
 * separator would follow whatever regional settings the computer happens to use, and amounts
 * would be printed as {@code 62,01} on some machines and {@code 62.01} on others.
 */
final class ConsoleFormat {

    /** Amounts below this are the leftovers of floating point arithmetic, not real money. */
    private static final double NEGLIGIBLE_AMOUNT = 0.005;

    private static final String DECIMAL_PATTERN = "%.2f";

    private ConsoleFormat() {
    }

    /** @return an amount of money, always with two decimals, for example {@code $62.01}. */
    static String money(double amount) {
        double displayedAmount = Math.abs(amount) < NEGLIGIBLE_AMOUNT ? 0 : amount;
        String digits = String.format(Locale.US, DECIMAL_PATTERN, Math.abs(displayedAmount));
        return displayedAmount < 0 ? "-$" + digits : "$" + digits;
    }

    /** @return the value of a share, between 0 and 1, with two decimals. */
    static String shareValue(double value) {
        return String.format(Locale.US, DECIMAL_PATTERN, value);
    }

    /** @return a whole percentage, for example {@code 50%}. */
    static String percent(int percent) {
        return percent + "%";
    }

    /** @return a count followed by the right form of the word, for example {@code 1 share}. */
    static String shares(long shares) {
        return shares + (shares == 1 ? " share" : " shares");
    }
}
