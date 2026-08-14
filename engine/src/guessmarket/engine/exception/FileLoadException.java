package guessmarket.engine.exception;

/**
 * Base class of every failure that can happen while a system details file is being loaded.
 *
 * <p>Whenever one of these is thrown the data that was loaded beforehand, if any, is left
 * exactly as it was.
 */
public abstract class FileLoadException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    protected FileLoadException(String message) {
        super(message);
    }

    protected FileLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
