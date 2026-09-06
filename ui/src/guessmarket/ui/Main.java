package guessmarket.ui;

/**
 * The entry point of the program.
 *
 * <p>It exists as a class of its own, separate from {@link GuessMarketApplication}, on purpose.
 * When a jar is started with the JavaFX runtime on the module path, the launcher refuses to start
 * a main class that extends {@code Application} and reports that the JavaFX components are
 * missing. A plain launcher class that merely calls the application does not go through that
 * check, so the same jar starts the same way on every machine.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        GuessMarketApplication.main(args);
    }
}
