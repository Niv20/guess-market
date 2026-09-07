package guessmarket.ui.settings;

import javafx.beans.value.ObservableBooleanValue;

/**
 * The two things on the settings sheet that are not settings at all.
 *
 * <p>Writing the system to a file and reading one back are actions rather than preferences, and
 * they belong to the window as a whole: both of them replace or record everything the two screens
 * are showing, and reading one back reports its outcome in the header where a loaded file reports
 * its own. So the sheet does not do them. It offers them, and asks the window to carry them out.
 *
 * <p>They are on the sheet because that is where everything that is about the program rather than
 * about a market now lives, and because a header with a Save and a Load button standing in it
 * suggests they are part of trading, which they are not.
 */
public interface SettingsActions {

    /** Writes everything the system currently holds to a file of its own. */
    void saveState();

    /** Reads back a system saved earlier, replacing whatever is loaded now. */
    void loadState();

    /**
     * @return whether there is a system loaded at the moment, so that the sheet can refuse to
     *         offer to save nothing. It is watched rather than asked, because it becomes true
     *         while the sheet is closed and has to be right the next time it is opened.
     */
    ObservableBooleanValue systemLoaded();
}
