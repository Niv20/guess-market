package guessmarket.ui.common;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;
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
 *   <li>a dot in the colour of how the thing is doing, in a column of its own down the left,
 *       found before anything at all is actually read;</li>
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

    /**
     * How wide across the middle the status dot is drawn, in pixels.
     *
     * <p>A fixed size rather than one measured from the text beside it, for the same reason the
     * drawings in {@link Icons} are a fixed size: it is a mark and not a letter, and a mark that
     * grew and shrank with the label font would be a different shape on each of the three skins.
     */
    private static final double DOT_RADIUS = 4;

    /**
     * How far the column a mark stands in is held off the words beside it, in pixels. Wider than
     * the gaps inside a line, so that the mark reads as a column of its own and not as the first
     * word of the name.
     */
    private static final double MARK_GAP = 11;

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
     * @param kind one of the dot styles the stylesheet offers: {@code status-dot-idle},
     *             {@code status-dot-live} or {@code status-dot-over}
     * @param meaning what the colour stands for, in the words the rest of the program uses for it
     * @return the mark that says how the thing is doing, in colour alone
     *
     * <p>It is the smallest thing on the tile and the first, and it goes in a column of its own
     * down the left hand edge — see {@link #withMark}. That is the whole of the idea: a list is
     * scanned for the rows that are still running long before any of it is read, and a colour
     * repeated at one distance from the edge is found in a single pass where a word has to be
     * read one row at a time.
     *
     * <p>A colour on its own says nothing to somebody who cannot tell two of them apart, so the
     * word the colour stands for is on the dot as well, for the pointer to find.
     */
    public static Node statusDot(String kind, String meaning) {
        Circle dot = new Circle(DOT_RADIUS);
        dot.getStyleClass().addAll("status-dot", kind);
        Tooltip.install(dot, new Tooltip(meaning));
        return dot;
    }

    /**
     * @return the number the file gave the thing, to be said in front of its name
     *
     * <p>It is set in the colour of the line under the name rather than the colour of the name,
     * so that a column of tiles is still read down the names and the numbers are found only by
     * somebody looking for one. It never shrinks: the name beside it wraps, and a number with its
     * end cut off is not a shorter number, it is the wrong one.
     */
    public static Label number(String text) {
        Label number = new Label(text);
        number.getStyleClass().add("tile-number");
        number.setMinWidth(Region.USE_PREF_SIZE);
        return number;
    }

    /** @return the name of whatever the tile is about, wrapped rather than cut off. */
    public static Label title(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("tile-title");
        title.setWrapText(true);
        return title;
    }

    /**
     * @return the name with what the thing is after it, rather than on a row of its own above it
     *
     * <p>A badge above the name is a row a tile only sometimes has, and a list whose tiles are
     * two different heights is read as two kinds of thing rather than as one list. Said after the
     * name instead, in two or three letters, it costs the tile no height at all and every tile in
     * the list ends where the one above it ended.
     *
     * <p>Each tag is held apart from the name by a dot, and the whole of what follows the name is
     * set against the middle of it: a name that has wrapped onto a second line is still one name,
     * and the tag belongs to all of it rather than to its first line.
     */
    public static Node titleRow(Node title, List<Label> tags) {
        return titleRow(List.of(), title, tags);
    }

    /**
     * @param before what stands in front of the name: how the thing is doing, and which one of
     *               them it is. Neither is separated off by a dot, because neither is a remark
     *               about the name — they are what the eye lands on before it gets to the name at
     *               all, and a dot after them would tie them to it.
     * @return the name with things said in front of it as well as after it
     */
    public static Node titleRow(List<Node> before, Node title, List<Label> tags) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(before);
        row.getChildren().add(title);
        for (Label tag : tags) {
            row.getChildren().addAll(tagSeparator(), tag);
        }
        return row;
    }

    /**
     * @param kind one of the tag styles the stylesheet offers: {@code tile-tag-mm} or
     *             {@code tile-tag-blocked}
     * @return one tag for a {@linkplain #titleRow title row}: a short word in the colour of what
     *         it means, and nothing else — no outline, no fill and no room taken from the name
     */
    public static Label tag(String text, String kind) {
        Label tag = new Label(text);
        tag.getStyleClass().addAll("tile-tag", kind);
        tag.setMinWidth(Region.USE_PREF_SIZE);
        return tag;
    }

    /** @return the dot before a tag, quiet enough that the name and the tag stay two things. */
    private static Label tagSeparator() {
        Label dot = new Label("\u00b7");
        dot.getStyleClass().add("tile-tag-dot");
        dot.setMinWidth(Region.USE_PREF_SIZE);
        return dot;
    }

    /** @return the quiet line under the name: worth reading, not worth reading first. */
    public static Label meta(String text) {
        Label meta = new Label(text);
        meta.getStyleClass().add("tile-meta");
        meta.setWrapText(true);
        return meta;
    }

    /**
     * @return the same quiet line, built out of pieces so that one part of it can be said in a
     *         colour of its own
     *
     * <p>A line and not two labels beside each other. What is coloured here is a remark about the
     * word in front of it - which person the name belongs to - and a remark set beside the block
     * of words rather than after the last of them has come loose from what it is about. Written as
     * one line the pieces stay in the order they are read in however narrow the tile gets, and the
     * line wraps between them exactly as it wraps between any two words.
     */
    public static TextFlow metaLine(Node... pieces) {
        TextFlow line = new TextFlow(pieces);
        line.getStyleClass().add("tile-meta-line");
        return line;
    }

    /**
     * @param styles what this piece means, when it means something: {@code mark-mm} for the part
     *               of the line that says the event is the reader's own
     * @return one piece of a {@linkplain #metaLine line}
     */
    public static Text metaPiece(String text, String... styles) {
        Text piece = new Text(text);
        piece.getStyleClass().add("tile-meta-text");
        piece.getStyleClass().addAll(styles);
        return piece;
    }

    /**
     * @param alignedWith the block of words the mark is held against, which is the same block that
     *                    was handed to {@link #body} or put at the head of the card
     * @return everything the tile says, with a mark standing in a narrow column of its own down
     *         the left of it
     *
     * <p>A column and not simply the first thing on the first line. The mark belongs to more than
     * the name, and put in front of the name it would be read as part of the name's line and would
     * push the line underneath out of line with it. Given a column it is at the same distance from
     * the edge on every tile in the list, so a column of tiles has one straight rail of marks down
     * it that is read in a single pass. It is the same thought as the figures out along the right,
     * at the other edge and one mark wide.
     *
     * <p>Down that rail it is set against the middle of the words rather than the middle of the
     * tile, which on a card given more height than it has anything to say in are two quite
     * different places — see {@link MarkedRow}, which is what finds the first of them.
     */
    public static Node withMark(Node mark, Region alignedWith, Node content) {
        MarkedRow row = new MarkedRow(mark, alignedWith, content, MARK_GAP);
        // For a card that is given more height than it asked for, such as one in a strip: without
        // this the row keeps the height of what is on it and the foot of the card is empty space
        // underneath, which is exactly the space the figures were meant to be pushed into.
        VBox.setVgrow(row, Priority.ALWAYS);
        return row;
    }

    /**
     * @return the quiet line with a tag held out at the far end of it, for a card that has not
     *         the width to say both after the name
     *
     * <p>The same tag as {@link #titleRow}, put somewhere else because the card is narrower. A
     * name is the one thing on a card that must not be cut short, and a tag after it takes its
     * width from the name and nothing else; the line underneath is a good deal shorter than the
     * card is wide, so the end of it is room that was going to be empty either way.
     *
     * <p>Held out at the end rather than following the line, so that a strip of cards has the
     * tags down one edge where they can be found in a single pass. They are what somebody
     * looking for their own events is looking for, and read one after another they would have to
     * be looked for a card at a time.
     */
    public static Node metaRow(Node meta, List<Label> tags) {
        HBox row = new HBox(8, meta);
        row.setAlignment(Pos.CENTER_LEFT);
        if (!tags.isEmpty()) {
            Region push = new Region();
            HBox.setHgrow(push, Priority.ALWAYS);
            row.getChildren().add(push);
            row.getChildren().addAll(tags);
        }
        return row;
    }

    /**
     * @param words the name and the line under it, kept in one block by {@link #words}
     * @return the name, the line under it and the figures, laid out in two columns when the tile
     *         is wide enough to hold them side by side and stacked when it is not
     *
     * <p>A tile in a panel whose divider can be dragged is sometimes half a screen wide and
     * sometimes a ribbon, and one arrangement cannot be right at both sizes: stacked, a wide tile
     * is a narrow tile with an empty half beside it; side by side, a narrow tile is a name cut
     * into single words. So the tile is given both and chooses between them from the width it is
     * actually handed — see {@link TileBody} for which and why.
     */
    public static Node body(Region words, FlowPane figures) {
        return new TileBody(words, figures);
    }

    /**
     * @return the name and the line under it, as the one block they are read as
     *
     * <p>Kept in one piece rather than left as two things a tile happens to put next to each
     * other, because that block is what the mark down the left is held against and what
     * {@link #body} moves about as a whole when the tile is wide enough for two columns. Both of
     * them have to be able to point at the same thing.
     */
    public static VBox words(Node title, Node meta) {
        return new VBox(4, title, meta);
    }

    /**
     * @return the same block of words, set against the middle of whatever height it is handed
     *         rather than along the top of it
     *
     * <p>For the card of a {@linkplain #renderStrip strip}, which is given the height of the strip
     * whether it has that much to say or not. Left along the top, a card whose name fits on one
     * line is that line with a third of a card of nothing underneath it, and a row of cards that
     * wrapped onto different numbers of lines is a row read across a ragged edge. Against the
     * middle every card in the row says what it has to say in the same band, and a card with less
     * to say is quieter rather than unfinished.
     *
     * <p>A column of tiles does not want this and does not get it: there the tile is only as tall
     * as what is on it, so the middle of the block and the top of it are the same place, and the
     * one thing that would move is a tile whose figures sit out to the right of the words.
     */
    public static VBox centredWords(Node title, Node meta) {
        VBox words = words(title, meta);
        words.setAlignment(Pos.CENTER_LEFT);
        return words;
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
