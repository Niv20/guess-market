package guessmarket.ui.common;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * The message boxes the program uses to tell the person what happened.
 *
 * <p>They all go through this one class for two reasons. Every dialog is dressed in the skin the
 * window is currently wearing, which a dialog does not inherit by itself because it lives in a
 * window of its own; and every message is laid out the same way, with a short headline saying
 * what happened and a longer explanation underneath saying why and what to do about it.
 */
public final class Dialogs {

    /** Wide enough that a full explanation is not squeezed into a column of two words. */
    private static final double MESSAGE_WIDTH = 460;

    private Dialogs() {
    }

    /** Reports a failed action. The message is the one the engine wrote for the user. */
    public static void error(Window owner, String headline, String message) {
        show(owner, Alert.AlertType.ERROR, "Guess Market", headline, message);
    }

    /**
     * Reports a file that was rejected, listing every problem found in it rather than only the
     * first, so that the file can be corrected in one go.
     */
    public static void fileRejected(Window owner, String headline, String summary,
                                    List<String> problems) {
        StringBuilder message = new StringBuilder(summary);
        int number = 1;
        for (String problem : problems) {
            message.append(System.lineSeparator()).append(System.lineSeparator())
                    .append(number++).append(". ").append(problem);
        }
        show(owner, Alert.AlertType.ERROR, "Guess Market", headline, message.toString());
    }

    /** Reports an action that went through. */
    public static void information(Window owner, String headline, String message) {
        show(owner, Alert.AlertType.INFORMATION, "Guess Market", headline, message);
    }

    /**
     * Asks before something that cannot be undone, such as closing an event.
     *
     * @return whether the person confirmed
     */
    public static boolean confirm(Window owner, String headline, String message) {
        Alert alert = build(owner, Alert.AlertType.CONFIRMATION, "Guess Market", headline, message);
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.OK;
    }

    private static void show(Window owner, Alert.AlertType type,
                             String title, String headline, String message) {
        build(owner, type, title, headline, message).showAndWait();
    }

    private static Alert build(Window owner, Alert.AlertType type,
                               String title, String headline, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headline);
        alert.initOwner(owner);

        DialogPane pane = alert.getDialogPane();
        // A label of our own rather than the built in content text: it wraps at a sensible width
        // and does not stretch the dialog across the whole screen for a long explanation.
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(MESSAGE_WIDTH);
        pane.setContent(content);
        applySkinOf(owner, pane);
        return alert;
    }

    /** Copies the stylesheets of the main window onto a dialog, which has a scene of its own. */
    private static void applySkinOf(Window owner, DialogPane pane) {
        if (owner != null && owner.getScene() != null) {
            pane.getStylesheets().setAll(owner.getScene().getStylesheets());
        }
    }
}
