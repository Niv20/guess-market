package guessmarket.dto;

/**
 * The two ways in which an event may collect its commission.
 */
public enum CommissionType {

    /** The commission is added to every purchase and paid by the buyer. */
    ON_PURCHASE("On purchase"),

    /** The commission is taken from the winners' pot when the event is closed. */
    ON_CLOSE("On close");

    private final String displayName;

    CommissionType(String displayName) {
        this.displayName = displayName;
    }

    /** @return a human readable name, suitable for presenting to a user. */
    public String getDisplayName() {
        return displayName;
    }
}
