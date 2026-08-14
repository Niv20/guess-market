package guessmarket.engine.exception;

/**
 * Thrown when the requested path cannot be used as a system details file at all: nothing
 * exists there, it is a folder rather than a file, it cannot be read, or it is not an XML file.
 */
public class FileAccessException extends FileLoadException {

    private static final long serialVersionUID = 1L;

    public FileAccessException(String message) {
        super(message);
    }

    public FileAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
