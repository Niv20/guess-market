package guessmarket.ui.console;

/**
 * Raised when there is no more input to read, for example because the input stream reached its
 * end. The main loop treats it as a request to shut the program down tidily instead of letting
 * it fail.
 */
class InputClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    InputClosedException() {
        super("There is no more input to read.");
    }
}
