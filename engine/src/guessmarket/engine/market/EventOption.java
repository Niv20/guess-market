package guessmarket.engine.market;

import java.io.Serial;
import java.io.Serializable;

/**
 * One of the two possible outcomes of an event, together with how many of its shares exist.
 *
 * <p>Shares only ever come into being, never disappear: under LMSR they are issued as they are
 * bought, and in an order book they are minted, either by the market maker when the event opens or
 * by two buyers of opposite options between them. Shares changing hands afterwards does not change
 * this figure, which is why it can be read as how much of the market believes in this outcome.
 */
public class EventOption implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private long sharesOutstanding;

    EventOption(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** @return how many shares of this option exist altogether, in everybody's hands together. */
    public long getSharesOutstanding() {
        return sharesOutstanding;
    }

    /** Records that more shares of this option have been created. Only the owning event may do this. */
    void issueShares(long shares) {
        sharesOutstanding += shares;
    }
}
