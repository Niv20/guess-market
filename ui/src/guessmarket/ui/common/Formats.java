package guessmarket.ui.common;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Turns the numbers the engine returns into the text the screen shows.
 *
 * <p>Every decimal value in this system is shown with exactly two digits after the point, and
 * every format here is built on {@link Locale#US} on purpose: the program must look the same on
 * a machine whose regional settings write decimals with a comma, and a price that reads
 * {@code 0,58} instead of {@code 0.58} would be read as a different number by the person marking
 * the exercise.
 *
 * <p>This class exists in the user interface and nowhere else. The engine returns numbers; how
 * many digits of them are worth showing is a decision that belongs to the screen.
 */
public final class Formats {

    private static final DecimalFormatSymbols US_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00", US_SYMBOLS);
    private static final DecimalFormat PLAIN_DECIMAL = new DecimalFormat("0.00", US_SYMBOLS);
    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0", US_SYMBOLS);

    /** Shown wherever a number would be, but there is no number to show yet. */
    public static final String NOTHING = "—";

    private Formats() {
    }

    /**
     * @return an amount of money, with the minus sign in front of the currency symbol so that a
     *         negative balance reads as {@code -$12.50} rather than {@code $-12.50}
     */
    public static String money(double amount) {
        String digits = MONEY.format(Math.abs(amount));
        return (isNegative(amount) ? "-$" : "$") + digits;
    }

    /** @return the price of a single share, always with two decimals, or a dash for no price. */
    public static String price(Double price) {
        return price == null ? NOTHING : "$" + PLAIN_DECIMAL.format(price);
    }

    /** @return a value between 0 and 1, such as an LMSR option value, with two decimals. */
    public static String decimal(double value) {
        return PLAIN_DECIMAL.format(value);
    }

    /** @return a value between 0 and 1 written the way a market reads it, as a percentage. */
    public static String probability(double value) {
        return PLAIN_DECIMAL.format(value * 100) + "%";
    }

    /** @return a whole number of shares, grouped in thousands. */
    public static String shares(long shares) {
        return WHOLE.format(shares);
    }

    /** @return a commission rate as it is written in the file, for example {@code 15%}. */
    public static String percent(int percent) {
        return percent + "%";
    }

    /**
     * @return how many there are of something, with the noun made plural to match, for example
     *         {@code 1 event} or {@code 3 events}
     */
    public static String count(int howMany, String noun) {
        return howMany + " " + noun + (howMany == 1 ? "" : "s");
    }

    /** @return an amount of money with an explicit sign, for a profit or a loss. */
    public static String signedMoney(double amount) {
        if (isZero(amount)) {
            return "$" + MONEY.format(0);
        }
        return (isNegative(amount) ? "-$" : "+$") + MONEY.format(Math.abs(amount));
    }

    /**
     * Says how a profit or a loss should be coloured.
     *
     * <p>It lives here rather than with the screens because the question it answers is the same
     * one {@link #signedMoney} answers: an amount too small to appear in two decimal places has
     * not happened as far as anybody looking at the screen is concerned, and a figure reading
     * {@code $0.00} in the green of a profit claims something that is not so.
     *
     * @return the style class to put beside {@code value}
     */
    public static String resultStyle(double amount) {
        if (isZero(amount)) {
            return "value-neutral";
        }
        return isNegative(amount) ? "value-negative" : "value-positive";
    }

    /** Money is compared against half a cent, because anything smaller cannot be shown anyway. */
    private static boolean isNegative(double amount) {
        return amount < -0.005;
    }

    private static boolean isZero(double amount) {
        return Math.abs(amount) < 0.005;
    }
}
