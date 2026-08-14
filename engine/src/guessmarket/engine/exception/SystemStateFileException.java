package guessmarket.engine.exception;

/**
 * Thrown when a saved system state cannot be written to, or read back from, disk.
 */
public class SystemStateFileException extends GuessMarketException {

    private static final long serialVersionUID = 1L;

    public SystemStateFileException(String message) {
        super(message);
    }

    public SystemStateFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
