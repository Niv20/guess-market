package guessmarket.engine;

import guessmarket.engine.impl.GuessMarketEngineImpl;
import guessmarket.engine.impl.OnlineMarketEngineImpl;

/**
 * The single place in which a concrete engine is created.
 *
 * <p>Callers ask this factory for a {@link GuessMarketEngine} and work with the interface from
 * then on, so no user interface ever names an implementation class and the implementation can
 * be swapped without touching a single caller.
 */
public final class GuessMarketEngineFactory {

    private GuessMarketEngineFactory() {
    }

    /** @return a new engine with nothing loaded in it yet. */
    public static GuessMarketEngine createEngine() {
        return new GuessMarketEngineImpl();
    }
    /** @return a new in-memory server engine, ready for registrations and event uploads. */
    public static OnlineMarketEngine createOnlineEngine() {
        return new OnlineMarketEngineImpl();
    }
}

