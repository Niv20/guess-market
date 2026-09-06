package guessmarket.dto;

/**
 * How a completed transaction came about, which is also what says where its money went.
 *
 * <p>The distinction matters when the history of an event is read: a resale moves money between
 * two participants, while both kinds of mint move money into the event's own account, because a
 * mint creates shares that did not exist before and the account holds what backs them.
 */
public enum TradeKind {

    /** Shares bought from the event itself, priced by the LMSR cost function. */
    LMSR_PURCHASE("Purchase"),

    /** The market maker's opening purchase, which creates the first pairs of shares. */
    INITIAL_MINT("Opening mint"),

    /** Two buyers of opposite options whose prices together reach the base value. */
    MINT("Mint"),

    /** Shares that already existed changing hands between two participants. */
    RESALE("Trade");

    private final String displayName;

    TradeKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
