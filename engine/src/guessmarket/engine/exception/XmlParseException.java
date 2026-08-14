package guessmarket.engine.exception;

/**
 * Thrown when the file exists and ends with {@code .xml}, but its XML content cannot be read:
 * the file is not well formed, or it does not describe a Guess Market system at all.
 */
public class XmlParseException extends FileLoadException {

    private static final long serialVersionUID = 1L;

    public XmlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public XmlParseException(String message) {
        super(message);
    }
}
