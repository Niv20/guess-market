package guessmarket.engine.market;

import java.io.Serializable;

/**
 * One of the possible outcomes of an event, together with how many of its shares have been
 * bought so far.
 */
public class EventOption implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private long sharesPurchased;

    EventOption(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getSharesPurchased() {
        return sharesPurchased;
    }

    /** Records that more shares of this option were bought. Only the owning event may do this. */
    void addShares(long shares) {
        sharesPurchased += shares;
    }
}
