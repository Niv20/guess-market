package guessmarket.client;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises JavaFX's real editing/undo pipeline without changing the system clipboard. */
public final class CharacterCountTest {
    private CharacterCountTest() { }

    public static void main(String[] arguments) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                verifyEdits();
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                finished.countDown();
            }
        });
        try {
            if (!finished.await(20, TimeUnit.SECONDS)) throw new AssertionError("JavaFX editing checks timed out");
            if (failure.get() != null) throw new AssertionError("Character counter checks failed", failure.get());
            System.out.println("PASS: character counters, paste limits, replacement, Unicode, undo and layout");
        } finally {
            Platform.exit();
        }
    }

    private static void verifyEdits() {
        TextField field = new TextField();
        field.setId("test-input");
        StackPane wrapper = CharacterCount.wrap(field, 5);
        Label counter = (Label) wrapper.getChildren().get(1);
        require(CharacterCount.wrap(field, 5) == wrapper, "Repeated login must reuse its counter wrapper");
        require(field.getId().equals("test-input"), "Counter changed the original input identity");
        require(counter.isMouseTransparent(), "Counter blocks input clicks");
        expect(field, counter, "", "0/5");

        field.replaceSelection("abcdefghi");
        expect(field, counter, "abcde", "5/5");
        require(field.getCaretPosition() == 5, "Truncated paste left the caret outside the input");
        field.appendText("more");
        expect(field, counter, "abcde", "5/5");

        field.selectRange(1, 4);
        field.replaceSelection("WXYZ");
        expect(field, counter, "aWXYe", "5/5");
        field.undo();
        expect(field, counter, "abcde", "5/5");
        field.redo();
        expect(field, counter, "aWXYe", "5/5");
        field.deleteText(1, 4);
        expect(field, counter, "ae", "2/5");

        field.setText("1234");
        field.appendText("\uD83D\uDE00");
        expect(field, counter, "1234", "4/5");
        field.replaceText(3, 4, "\uD83D\uDE00AB");
        expect(field, counter, "123\uD83D\uDE00", "5/5");
        field.clear();
        expect(field, counter, "", "0/5");

        TextField message = new TextField("x".repeat(1002));
        StackPane messageWrapper = CharacterCount.wrap(message, 1000);
        Label messageCounter = (Label) messageWrapper.getChildren().get(1);
        expect(message, messageCounter, "x".repeat(1000), "1000/1000");
        new Scene(messageWrapper, 320, 44);
        messageWrapper.applyCss();
        messageWrapper.layout();
        require(message.getPadding().getRight() >= messageCounter.prefWidth(-1) + 12,
                "Message text can overlap its character count");
    }

    private static void expect(TextField input, Label counter, String text, String count) {
        require(input.getText().equals(text), "Unexpected input after editing: " + input.getText());
        require(counter.getText().equals(count), "Counter does not match the input: " + counter.getText());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
