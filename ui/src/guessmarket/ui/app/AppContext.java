package guessmarket.ui.app;

import guessmarket.engine.GuessMarketEngine;
import javafx.stage.Window;

import java.util.Objects;

/**
 * Everything a screen of the program needs in order to do its job: the engine to ask, the window
 * to hang dialogs on, and a way of saying that it has just changed something.
 *
 * <p>Handing this one object to every section keeps the wiring in a single place. It also keeps
 * the sections from talking to each other: when the users screen trades in an event, it does not
 * reach into the events screen to refresh it, it simply reports that the system moved and lets
 * {@link AppController} decide who needs to be brought up to date.
 */
public final class AppContext {

    private final GuessMarketEngine engine;
    private final Window window;
    private final Runnable systemChangedReporter;

    AppContext(GuessMarketEngine engine, Window window, Runnable systemChangedReporter) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.window = window;
        this.systemChangedReporter = Objects.requireNonNull(systemChangedReporter, "reporter");
    }

    /** @return the engine, which is the only way any screen may reach the system. */
    public GuessMarketEngine engine() {
        return engine;
    }

    /** @return the main window, used as the owner of dialogs and of the file chooser. */
    public Window window() {
        return window;
    }

    /**
     * Says that an action has just changed the system, so that every screen showing any part of
     * it is rebuilt from the engine rather than from what it happened to be holding.
     */
    public void reportSystemChanged() {
        systemChangedReporter.run();
    }
}
