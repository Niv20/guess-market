package guessmarket.ui.common;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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

    /** The gap between the mark at the head of a dialog and the headline beside it. */
    private static final double HEADER_SPACING = 10;

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
        alert.initOwner(owner);

        DialogPane pane = alert.getDialogPane();
        Node mark = markFor(type);
        if (mark == null) {
            alert.setHeaderText(headline);
        } else {
            pane.setHeader(markedHeader(mark, headline));
            // The ready made graphic belongs to the ready made header, which is no longer being
            // used; saying so as well costs nothing and is the line that would have to be found
            // if a later version of the toolkit ever drew it anyway.
            pane.setGraphic(null);
        }

        // A label of our own rather than the built in content text: it wraps at a sensible width
        // and does not stretch the dialog across the whole screen for a long explanation.
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(MESSAGE_WIDTH);
        pane.setContent(content);
        applySkinOf(owner, pane);
        return alert;
    }

    /**
     * @return the mark that goes at the head of this kind of dialog, or null for a kind that is
     *         better off with the plain headline the toolkit writes
     *
     * <p>Only the two that report an outcome are marked, and they are marked because an outcome
     * is worth knowing before the message under it has been read: a warning sign for something
     * that did not happen, a tick for something that did. A question is not an outcome - it is
     * the message itself, and a mark in front of it would only be a picture of the word "sure".
     */
    private static Node markFor(Alert.AlertType type) {
        return switch (type) {
            case ERROR -> Icons.warningTriangle();
            case INFORMATION -> Icons.checkMark(Icons.SMALL);
            default -> null;
        };
    }

    /**
     * The head of a dialog that reports an outcome, built by hand.
     *
     * <p>The ready made one hangs a large coloured disc in the top right corner, where every other
     * window on the screen keeps its close button, so that is what it looks like - and for a
     * message that went right it is the toolkit's own blue letter i, which is a colour this
     * program uses for nothing and a letter that says less than the first word of the headline
     * does. This one leaves that corner empty and marks the message instead: a small sign, and
     * the headline immediately after it.
     */
    private static Node markedHeader(Node mark, String headline) {
        Label text = new Label(headline);
        text.setWrapText(true);
        HBox header = new HBox(HEADER_SPACING, mark, text);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-header");
        return header;
    }

    /** Copies the stylesheets of the main window onto a dialog, which has a scene of its own. */
    private static void applySkinOf(Window owner, DialogPane pane) {
        if (owner != null && owner.getScene() != null) {
            pane.getStylesheets().setAll(owner.getScene().getStylesheets());
        }
    }
}
