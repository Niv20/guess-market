package guessmarket.client;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.util.Objects;

/** Keeps the displayed limit and all input edits, including paste and replacement, in sync. */
final class CharacterCount {
    private static final Object WRAPPER_KEY = new Object();

    private record WrappedInput(int maximum, StackPane wrapper) { }

    private CharacterCount() { }

    static StackPane wrap(TextField input, int maximum) {
        Objects.requireNonNull(input, "input");
        if (maximum < 1) throw new IllegalArgumentException("The character limit must be positive.");
        if (input.getProperties().get(WRAPPER_KEY) instanceof WrappedInput existing) {
            if (existing.maximum() != maximum) {
                throw new IllegalArgumentException("This input already has a different character limit.");
            }
            return existing.wrapper();
        }

        input.setTextFormatter(new TextFormatter<String>(change -> limit(change, maximum)));
        if (input.getLength() > maximum) input.setText(prefix(input.getText(), maximum));
        input.setMinWidth(0);
        input.setMaxWidth(Double.MAX_VALUE);

        Label counter = new Label();
        counter.getStyleClass().add("character-counter");
        counter.textProperty().bind(Bindings.createStringBinding(
                () -> input.getLength() + "/" + maximum, input.textProperty()));
        counter.setMouseTransparent(true);
        counter.setFocusTraversable(false);
        counter.setMinWidth(Region.USE_PREF_SIZE);
        counter.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        counter.disableProperty().bind(input.disabledProperty());

        // Measure the widest counter once per font change. The text never slides underneath
        // the label, and adding a digit does not move the text or shrink its editing area.
        Text measure = new Text(maximum + "/" + maximum);
        measure.fontProperty().bind(counter.fontProperty());
        input.paddingProperty().bind(Bindings.createObjectBinding(() -> {
            double scale = input.getFont().getSize() / 13.0;
            return new Insets(6 * scale, measure.getLayoutBounds().getWidth() + 24 * scale,
                    6 * scale, 10 * scale);
        }, measure.layoutBoundsProperty(), input.fontProperty()));

        StackPane wrapper = new StackPane(input, counter);
        wrapper.getStyleClass().add("character-count-wrapper");
        wrapper.setMinWidth(0);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(counter, Pos.CENTER_RIGHT);
        StackPane.setMargin(counter, new Insets(0, 12, 0, 0));
        wrapper.getStylesheets().add(Objects.requireNonNull(
                CharacterCount.class.getResource("character-count.css")).toExternalForm());
        input.getProperties().put(WRAPPER_KEY, new WrappedInput(maximum, wrapper));
        return wrapper;
    }

    private static TextFormatter.Change limit(TextFormatter.Change change, int maximum) {
        if (!change.isContentChange() || change.getControlNewText().length() <= maximum) {
            return change;
        }
        int retained = change.getControlText().length()
                - (change.getRangeEnd() - change.getRangeStart());
        String replacement = prefix(change.getText(), Math.max(0, maximum - retained));
        if (replacement.isEmpty() && change.getRangeStart() == change.getRangeEnd()) return null;
        change.setText(replacement);
        int caret = change.getRangeStart() + replacement.length();
        change.selectRange(caret, caret);
        return change;
    }

    /** Match the server's UTF-16 String length while keeping pasted supplementary letters intact. */
    private static String prefix(String value, int maximum) {
        int end = Math.min(value.length(), maximum);
        if (end > 0 && end < value.length()
                && Character.isHighSurrogate(value.charAt(end - 1))
                && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        return value.substring(0, end);
    }
}
