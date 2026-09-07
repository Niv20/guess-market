package guessmarket.ui.common;

import javafx.geometry.Orientation;
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
 *   <li>the figures last, each under a faint caption naming it.</li>
 * </ol>
 *
 * <p>Last means underneath on a tile too narrow to say two things at once, and along the right
 * on a tile wide enough for a column of its own — the same order either way, since a column read
 * down is read after the name beside it. {@link #body} is what decides between them.
 *
 * <p>A list can also be laid on its side, as a strip of cards read across rather than a column
 * read down. It says the same things in the same order; what changes is that the list then costs
 * a fixed height whatever is in it, which is what a list that shares a screen with other panels
 * has to do.
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

    /**
     * Draws every item of a list as a card of its own, side by side, read across instead of down.
     *
     * <p>A column of tiles gives every item all the width there is and asks for as much height as
     * it likes, which is right for the list a screen is about and wrong for a list that is only
     * one panel among several on a screen somebody has to scroll. Turned on its side the list
     * costs one card's height however many events there are, and the ones that do not fit are
     * behind a scroll bar of the list's own rather than further down the screen.
     *
     * <p>Every card is given the same width, because a strip of cards of different widths is read
     * as a row of different things; and the height of the strip, so that the row has one edge
     * along the top and one along the bottom.
     *
     * @param cardWidth how wide each card is drawn, in pixels
     */
    public static <T> void renderStrip(ListView<T> list, Function<T, Node> draw, double cardWidth) {
        list.setOrientation(Orientation.HORIZONTAL);
        list.setCellFactory(ignored -> new ListCell<>() {
            {
                setPrefWidth(cardWidth);
                // The other way round from a column, and for the same reason: a cell that asks
                // for no height of its own is given the height of the strip, so every card is
                // exactly as tall as the strip. A cell that asks for a height is given the
                // largest height ever asked for by any of them, which a list that is measured
                // once before it is given its own size never lets go of again.
                setPrefHeight(0);
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
     * @return the card one item of a {@linkplain #renderStrip strip} is drawn on, stretched to the
     *         full height of the strip so that the cards beside it end where it ends
     */
    public static VBox stripTile() {
        VBox tile = tile();
        tile.setMaxHeight(Double.MAX_VALUE);
        return tile;
    }

    /**
     * @return the empty space that pushes whatever follows it down to the foot of a strip card
     *
     * <p>A card in a strip is given the height of the strip whether it has that much to say or
     * not, so without this the figures of a card with a short name sit higher than the figures of
     * the card beside it. Pushed to the foot they line up along the bottom of the row and can be
     * read across it.
     */
    public static Region stripSpacer() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
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

    /**
     * @return the name, the line under it and the figures, laid out in two columns when the tile
     *         is wide enough to hold them side by side and stacked when it is not
     *
     * <p>A tile in a panel whose divider can be dragged is sometimes half a screen wide and
     * sometimes a ribbon, and one arrangement cannot be right at both sizes: stacked, a wide tile
     * is a narrow tile with an empty half beside it; side by side, a narrow tile is a name cut
     * into single words. So the tile is given both and chooses between them from the width it is
     * actually handed — see {@link TileBody} for which and why.
     */
    public static Node body(Node title, Node meta, FlowPane figures) {
        return new TileBody(new VBox(4, title, meta), figures);
    }

    /**
     * @return the row of figures a tile ends with, which wraps instead of overflowing
     *
     * <p>It keeps its height when a card has less room than it wants, so that a long name is
     * shortened rather than the money being squeezed off the card altogether. A tile without its
     * figures is not a shorter tile, it is the wrong tile.
     *
     * <p>It is a row wherever it is put, under the words or beside them; which way round it is
     * set, and therefore whether it is read from the left or from the right, is decided by
     * {@link #body} once the tile knows how wide it is.
     */
    public static FlowPane figures() {
        FlowPane row = new FlowPane(18, 6);
        row.getStyleClass().add("tile-figures");
        row.setMinHeight(Region.USE_PREF_SIZE);
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
