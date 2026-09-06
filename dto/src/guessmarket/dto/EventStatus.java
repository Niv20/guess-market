package guessmarket.dto;

/**
 * The three stages of an event's life.
 *
 * <p>An event is loaded from the file as {@link #NOT_STARTED}, is moved to {@link #ACTIVE} by its
 * market maker, and ends as {@link #CLOSED} when that same market maker decides the outcome. The
 * order is one way: a closed event can never be reopened.
 */
public enum EventStatus {

    NOT_STARTED("Not started"),
    ACTIVE("Active"),
    CLOSED("Closed");

    private final String displayName;

    EventStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Whatever shows this value to a person shows its display name, never its constant name. */
    @Override
    public String toString() {
        return displayName;
    }
}
