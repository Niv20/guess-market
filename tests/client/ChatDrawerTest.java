package guessmarket.client;

import guessmarket.dto.network.ChatMessageDto;
import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Skin;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises unread history truncation, modal traversal and real JavaFX bubble measurement. */
public final class ChatDrawerTest {
    private ChatDrawerTest() { }

    public static void main(String[] arguments) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try { verifyDrawer(); }
            catch (Throwable error) { failure.set(error); }
            finally { finished.countDown(); }
        });
        try {
            if (!finished.await(25, TimeUnit.SECONDS)) throw new AssertionError("Chat checks timed out");
            if (failure.get() != null) throw new AssertionError("Chat drawer checks failed", failure.get());
            System.out.println("PASS: unread trimming/reset, modal keyboard traversal, and long/short chat bubbles across all skins");
        } finally { Platform.exit(); }
    }

    private static void verifyDrawer() {
        boolean animations = Animations.isEnabled();
        Animations.enabledProperty().set(false);
        Stage stage = new Stage();
        // The worker is never used here: these checks only consume server-authored snapshots.
        try (ClientSession session = new ClientSession(null)) {
            ChatDrawer chat = new ChatDrawer(session, stage, new SimpleBooleanProperty());
            Button underlying = new Button("Underlying action");
            VBox base = new VBox(underlying);
            base.disableProperty().bind(chat.openProperty());
            StackPane root = new StackPane(base, chat.view());
            Scene scene = new Scene(root, 1000, 720);
            stage.setScene(scene);
            Skin.applyDefault(scene);
            chat.setUser("Alice");
            ChatMessageDto old = message(10, "Bob", "History");
            ChatMessageDto one = message(11, "Bob", "One unread");
            ChatMessageDto own = message(12, "aLiCe", "ok");
            ChatMessageDto longText = message(13, "Bob", "x".repeat(1000));
            chat.updateMessages(List.of(old));
            chat.updateMessages(List.of(old, one, own, longText));
            Label badge = (Label) root.lookup("#chat-unread-count");
            require(badge.getText().equals("2"), "Own message contributed to unread count");
            chat.updateMessages(List.of(own, longText));
            require(badge.getText().equals("2"), "Trimming old history changed unread count");
            chat.updateMessages(List.of(own, longText, message(14, "Carol", "After trim")));
            require(badge.getText().equals("3"), "A message following history trim was lost");
            chat.updateMessages(List.of(own, longText, message(14, "Carol", "After trim")));
            require(badge.getText().equals("3"), "Repeated trimmed snapshot counted a message twice");

            ((Button) root.lookup("#chat-launcher")).fire();
            require(chat.isOpen() && base.isDisabled(), "Drawer did not isolate the base content");
            require(root.lookup("#chat-backdrop").isVisible(), "Modal backdrop is missing");
            // This Stage is intentionally not shown: opening in an unfocused window cannot
            // acknowledge unread messages that the user has not actually seen.
            require(badge.getText().equals("3"), "An unfocused drawer acknowledged unread messages");

            TextField input = (TextField) root.lookup("#chat-message");
            input.requestFocus();
            key(input, KeyCode.TAB, false);
            require(scene.getFocusOwner() == root.lookup("#chat-close"),
                    "Tab escaped the drawer when the empty Send button was disabled");
            key(root.lookup("#chat-close"), KeyCode.TAB, true);
            require(scene.getFocusOwner() == input, "Shift-Tab did not cycle inside the drawer");

            // Keep both messages in view, including a 1000-character word with no break points.
            chat.updateMessages(List.of(own, longText));
            for (Skin skin : Skin.values()) {
                skin.applyTo(scene);
                for (double width : new double[] {1000, 350}) {
                    root.resize(width, 1100);
                    root.applyCss();
                    root.layout();
                    root.layout();
                    checkBubbles(root, skin + " at " + width);
                }
            }
            key(input, KeyCode.ESCAPE, false);
            require(!chat.isOpen() && !base.isDisabled(), "Escape did not release the modal content");
            require(!root.lookup("#chat-backdrop").isVisible(), "Closed backdrop still blocks clicks");
            require(!((StackPane) chat.view()).isPickOnBounds(), "Closed overlay intercepts empty space");

            chat.reset();
            chat.setUser("Next user");
            chat.updateMessages(List.of(message(1, "Someone", "New server history")));
            require(!badge.isVisible(), "Reset preserved another user's unread state");
            chat.updateMessages(List.of(message(1, "Someone", "New server history"), message(2, "Someone", "New arrival")));
            require(badge.getText().equals("1"), "Reset did not reset the message ID watermark");
        } finally {
            stage.close();
            Animations.enabledProperty().set(animations);
        }
    }

    private static void checkBubbles(StackPane root, String context) {
        Region panel = (Region) root.lookup("#chat-drawer");
        require(panel.getWidth() <= Math.min(480, root.getWidth()) + 2,
                "Drawer overflows its viewport: " + context);
        int own = 0;
        int other = 0;
        for (Node node : root.lookupAll(".chat-bubble")) {
            VBox bubble = (VBox) node;
            if (bubble.getScene() == null || bubble.getWidth() <= 0) continue;
            HBox row = (HBox) bubble.getParent();
            require(bubble.getWidth() <= row.getWidth() * 0.78 + 1,
                    "Bubble takes more than 78% of the row: " + context);
            Label body = (Label) bubble.lookup(".chat-body");
            if (bubble.getStyleClass().contains("chat-bubble-own")) {
                own++;
                require(bubble.getWidth() < 120, "Short message fills its available width: " + context);
                require(row.getAlignment() == javafx.geometry.Pos.CENTER_RIGHT,
                        "Own message is not on the right: " + context);
            } else {
                other++;
                require(row.getAlignment() == javafx.geometry.Pos.CENTER_LEFT,
                        "Other message is not on the left: " + context);
                require(body.getHeight() > body.getFont().getSize() * 3,
                        "Long unbroken message did not wrap: " + context);
                require(body.getLayoutBounds().getWidth() <= bubble.getWidth(),
                        "Message body escapes its bubble: " + context);
            }
        }
        require(own > 0 && other > 0, "Message cells were not laid out: " + context);
    }

    private static ChatMessageDto message(long id, String username, String body) {
        return new ChatMessageDto(id, username, body, 1_700_000_000_000L + id);
    }

    private static void key(Node target, KeyCode key, boolean shift) {
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, shift, false, false, false));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
