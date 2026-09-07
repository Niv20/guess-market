package guessmarket.ui.common;

import javafx.scene.control.ComboBox;

/**
 * Fills the dropdowns that narrow a long list down to what somebody is looking for.
 *
 * <p>A filter dropdown offers every value a column can take, and above them an "All" that stands
 * for no filter at all. The "All" is an ordinary line of the list rather than an empty selection,
 * so a filter always says out loud what it is doing and can never be left showing nothing.
 *
 * <p>A dropdown that is filtering something out is marked, because otherwise the only sign that a
 * list is not the whole list would be its length, and a list that is short for a reason looks
 * exactly like a list that is short because there is nothing in it.
 */
public final class Filters {

    /** What the line that keeps everything is called. */
    private static final String EVERYTHING = "All";

    /** Worn by a dropdown that is currently keeping something out. */
    private static final String FILTERING_STYLE = "filter-on";

    private Filters() {
    }

    /**
     * One line of a filter dropdown: the single value it keeps, or every value when there is no
     * value to keep. What it shows is its label, so the dropdown needs nothing else to draw it.
     *
     * @param label what this line reads on the screen
     * @param kept  the only value that gets through, or null to let everything through
     */
    public record Choice<T>(String label, T kept) {

        /** @return whether a value gets past this choice. */
        public boolean allows(T value) {
            return kept == null || kept.equals(value);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Fills a dropdown with "All" followed by every value given, chooses the "All", and runs the
     * given action whenever somebody chooses something else. The action is not run while filling,
     * because nothing has been filtered yet.
     */
    public static <T> void fill(ComboBox<Choice<T>> chooser, T[] values, Runnable onChange) {
        chooser.getItems().add(new Choice<>(EVERYTHING, null));
        for (T value : values) {
            chooser.getItems().add(new Choice<>(value.toString(), value));
        }
        chooser.getSelectionModel().selectFirst();
        chooser.valueProperty().addListener((observable, previous, chosen) -> {
            markFiltering(chooser, chosen);
            onChange.run();
        });
    }

    /** @return whether the choice made in a filter dropdown lets this value through. */
    public static <T> boolean allows(ComboBox<Choice<T>> chooser, T value) {
        Choice<T> chosen = chooser.getValue();
        return chosen == null || chosen.allows(value);
    }

    private static void markFiltering(ComboBox<?> chooser, Choice<?> chosen) {
        chooser.getStyleClass().remove(FILTERING_STYLE);
        if (chosen != null && chosen.kept() != null) {
            chooser.getStyleClass().add(FILTERING_STYLE);
        }
    }
}
