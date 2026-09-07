package guessmarket.ui.common;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/**
 * Builds the lists this program shows as a column of tiles rather than as a table.
 *
 * <p>A table has to be at least as wide as the sum of its columns. Made any narrower it either
 * cuts columns off the right hand side or grows a scroll bar underneath them, and a list of
 * events read through a scroll bar is no list at all. A tile is a single column that reflows, so
 * the same information stays readable however narrow the window is made.
 *
 * <p>What a table says by the position of a column, a tile has to say by weight and colour
 * instead, and every tile built here says it in the same order:
 *
 * <ol>
 *   <li>a row of badges saying what kind of thing this is, and how it is doing;</li>
 *   <li>its name, in bold, which is the one part read first;</li>
 *   <li>a faint line of context underneath, read only once the name has been found;</li>
 *   <li>the figures at the foot, each under a faint caption naming it.</li>
 * </ol>
 *
 * <p>Only the frame is here. What goes on a tile belongs to whatever the tile is about, which is
 * why this class hands out the pieces and never assembles them.
 */
public final class Tiles {

    private Tiles() {
    }

    /**
     * Draws every item of a list as a tile of its own, one under the next.
     *
     * <p>Only the cells that can be seen are ever built, so a long list costs no more to show
     * than a short one, and a tile is rebuilt whenever its item is redrawn — which is what lets a
     * tile show something that is not in the item itself, such as what the selected user is to an
     * event.
     */
    public static <T> void render(ListView<T> list, Function<T, Node> draw) {
        list.setCellFactory(ignored -> new ListCell<>() {
            {
                // A cell that asks for no width of its own is given the width of the list, so a
                // tile is as wide as the list is however narrow the window gets, and no
                // horizontal scroll bar ever appears underneath one.
                setPrefWidth(0);
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : draw.apply(item));
            }
        });
    }

    /** Says what an empty list means, instead of leaving a blank rectangle on the screen. */
    public static void emptyMessage(ListView<?> list, String message) {
        Label placeholder = new Label(message);
        placeholder.getStyleClass().add("faint");
        placeholder.setWrapText(true);
        list.setPlaceholder(placeholder);
    }

    /** @return the card one item is drawn on, stretched to the full width of the list. */
    public static VBox tile() {
        VBox tile = new VBox(4);
        tile.getStyleClass().addAll("card", "tile");
        tile.setMaxWidth(Double.MAX_VALUE);
        return tile;
    }

    /**
     * @return the row of badges above the name, which says what the tile is before it is read
     *
     * <p>It wraps rather than sharing out what room there is. A badge is one word, and half a word
     * followed by three dots says less than nothing; a badge pushed onto a second line still says
     * what it came to say.
     */
    public static FlowPane badges() {
        FlowPane row = new FlowPane(6, 4);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * @return the badges with one thing held apart from them at the end of the line, for the sort
     *         of detail that has to be on the tile but is never scanned for
     */
    public static HBox topRow(FlowPane badges, Node trailing) {
        HBox.setHgrow(badges, Priority.ALWAYS);
        HBox row = new HBox(8, badges, trailing);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    /**
     * @param kind one of the badge styles the stylesheet offers: {@code badge-neutral},
     *             {@code badge-active}, {@code badge-closed} or {@code badge-mm}
     * @return one badge: a short word in a coloured outline
     */
    public static Label badge(String text, String kind) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("badge", kind);
        badge.setMinWidth(Region.USE_PREF_SIZE);
        return badge;
    }

    /** @return the name of whatever the tile is about, wrapped rather than cut off. */
    public static Label title(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("tile-title");
        title.setWrapText(true);
        return title;
    }

    /** @return the quiet line under the name: worth reading, not worth reading first. */
    public static Label meta(String text) {
        Label meta = new Label(text);
        meta.getStyleClass().add("tile-meta");
        meta.setWrapText(true);
        return meta;
    }

    /** @return the row of figures at the foot of a tile, which wraps instead of overflowing. */
    public static FlowPane figures() {
        FlowPane row = new FlowPane(18, 6);
        row.getStyleClass().add("tile-figures");
        return row;
    }

    /**
     * @param valueStyles what the figure means, when it means something: {@code value-accent} for
     *                    the one figure of the tile that matters most, {@code value-positive} or
     *                    {@code value-negative} for a result that went one way or the other
     * @return one figure: a faint caption naming it, and the figure itself underneath in bold
     */
    public static VBox figure(String caption, String value, String... valueStyles) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("field-label");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("value");
        valueLabel.getStyleClass().addAll(valueStyles);

        return new VBox(1, captionLabel, valueLabel);
    }
}
