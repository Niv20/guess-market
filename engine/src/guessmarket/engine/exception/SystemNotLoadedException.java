package guessmarket.engine.exception;

/**
 * Thrown when an action that needs events is requested before any valid file was loaded.
 */
public class SystemNotLoadedException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public SystemNotLoadedException() {
        super("No system details file has been loaded yet, so there are no events to work with. "
                + "Load a valid XML file first and then try this action again.");
    }
}
