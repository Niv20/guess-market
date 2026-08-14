package guessmarket.dto;

/**
 * The life cycle state of an event.
 */
public enum EventStatus {

    /** The event is open for trading. */
    ACTIVE("Active"),

    /** The event has been resolved and no further trading is possible. */
    CLOSED("Closed");

    private final String displayName;

    EventStatus(String displayName) {
        this.displayName = displayName;
    }

    /** @return a human readable name, suitable for presenting to a user. */
    public String getDisplayName() {
        return displayName;
    }
}
