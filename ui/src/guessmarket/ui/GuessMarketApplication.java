package guessmarket.ui;

import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.GuessMarketEngineFactory;
import guessmarket.ui.console.ConsoleSession;

/**
 * Where the program starts.
 *
 * <p>This is the one place that brings the two halves of the system together: it asks the
 * factory for an engine, hands it to the console session, and lets the session drive. From that
 * point on the user interface knows the engine only through its interface, and the engine knows
 * nothing at all about the user interface.
 */
public final class GuessMarketApplication {

    private GuessMarketApplication() {
    }

    public static void main(String[] args) {
        GuessMarketEngine engine = GuessMarketEngineFactory.createEngine();
        new ConsoleSession(engine).run();
    }
}
