package guessmarket.engine.xml;

import guessmarket.dto.CommissionType;

/**
 * Translates the {@code type} attribute of a commission element into a {@link CommissionType}.
 *
 * <p>The comparison ignores letter case, so a file that writes {@code On-Purchase} is accepted
 * rather than rejected over something that cannot change its meaning.
 */
final class CommissionTypeParser {

    static final String ON_PURCHASE_VALUE = "on-purchase";
    static final String ON_CLOSE_VALUE = "on-close";

    private CommissionTypeParser() {
    }

    /** @return the matching type, or {@code null} when the text is missing or unknown. */
    static CommissionType parse(String rawType) {
        if (rawType == null) {
            return null;
        }
        String normalized = rawType.trim().toLowerCase();
        if (ON_PURCHASE_VALUE.equals(normalized)) {
            return CommissionType.ON_PURCHASE;
        }
        if (ON_CLOSE_VALUE.equals(normalized)) {
            return CommissionType.ON_CLOSE;
        }
        return null;
    }
}
