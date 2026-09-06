package guessmarket.ui.app;

import guessmarket.dto.LoadResultDto;
import guessmarket.engine.GuessMarketEngine;
import javafx.concurrent.Task;

/**
 * Loads a system details file on a thread of its own, so that the window stays alive while it
 * happens and can show how far along the work is.
 *
 * <p>The task lives in the user interface rather than in the engine, deliberately. Everything it
 * adds on top of the plain engine call - a progress figure, a line of text describing the current
 * step - only makes sense to something with a progress bar in it. The engine keeps the single
 * method it always had, knows nothing about threads, and would answer a console or a server in
 * exactly the same way.
 *
 * <p>Reading one of these files takes almost no time, so the steps below are paced out on purpose:
 * without that the progress bar would jump from empty to full and the person would never see that
 * anything was being checked at all.
 */
public class LoadFileTask extends Task<LoadResultDto> {

    /** How many steps the progress bar is divided into. */
    private static final int TOTAL_STEPS = 5;

    /** The pause after each step. Five of them add up to a load of about a second and a half. */
    private static final long STEP_PAUSE_MILLIS = 320;

    private final GuessMarketEngine engine;
    private final String path;

    public LoadFileTask(GuessMarketEngine engine, String path) {
        this.engine = engine;
        this.path = path;
    }

    @Override
    protected LoadResultDto call() throws InterruptedException {
        announce(0, "Opening " + fileNameOf(path));
        announce(1, "Checking that the file can be read");
        announce(2, "Reading the XML");

        // The one line that does the real work. Anything wrong with the path, the XML or the
        // system it describes leaves here as an exception, which the caller reports as a failure.
        LoadResultDto result = engine.loadSystemDetailsFile(path);

        announce(3, "Checking the events and the users");
        announce(4, "Preparing the system");
        updateProgress(TOTAL_STEPS, TOTAL_STEPS);
        updateMessage("Loaded " + result.eventCount() + " events and "
                + result.userCount() + " users");
        return result;
    }

    /** Says which step is starting, moves the bar on and then waits, so the step can be read. */
    private void announce(int completedSteps, String whatIsHappening) throws InterruptedException {
        updateProgress(completedSteps, TOTAL_STEPS);
        updateMessage(whatIsHappening + "...");
        Thread.sleep(STEP_PAUSE_MILLIS);
    }

    /** @return the file name on its own, which is all that fits next to a progress bar. */
    private static String fileNameOf(String fullPath) {
        int lastSeparator = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
        return lastSeparator < 0 ? fullPath : fullPath.substring(lastSeparator + 1);
    }
}
