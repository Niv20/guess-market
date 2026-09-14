package guessmarket.server;

/** An expected HTTP failure whose message is safe to show in the client. */
final class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int status;

    ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    int status() {
        return status;
    }
}
