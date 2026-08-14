package guessmarket.ui.console;

/**
 * The commands the main menu offers, in the order they are shown.
 *
 * <p>Keeping them in an enum means the menu that is printed and the menu that is acted upon can
 * never drift apart: a command is added in exactly one place.
 */
enum MenuCommand {

    LOAD_FILE("Load a system details file"),
    DISPLAY_EVENTS("Display all the events in the system"),
    EVENT_TRADING_STATUS("Show the trading status of an event"),
    PARTICIPATE("Participate in an event by buying shares"),
    CLOSE_EVENT("Close an event and pay its winners"),
    EXIT("Exit the system"),
    SAVE_STATE("Save the current state of the system to a file   (bonus)"),
    LOAD_STATE("Load a state of the system from a file           (bonus)");

    private final String title;

    MenuCommand(String title) {
        this.title = title;
    }

    String getTitle() {
        return title;
    }

    /**
     * @param menuNumber the number the user typed, starting at 1
     * @return the command that number stands for
     */
    static MenuCommand ofMenuNumber(int menuNumber) {
        return values()[menuNumber - 1];
    }

    static int count() {
        return values().length;
    }
}
