package guessmarket.ui.app;

/**
 * One of the two main screens of the window, as {@link AppController} sees it.
 *
 * <p>A section is told once what it may work with, and from then on is only ever asked to show
 * the system as it currently is, or to show nothing at all. It never keeps a copy of the system:
 * every refresh reads the engine again, which is what makes an action taken on one screen show up
 * on the other one without either of them knowing that the other exists.
 */
public interface AppSection {

    /** Called once, while the window is being built. */
    void connect(AppContext context);

    /** Rebuilds everything on the screen from the engine, keeping the current selection if it can. */
    void refresh();

    /** Empties the screen, because there is no system loaded to show. */
    void clear();
}
