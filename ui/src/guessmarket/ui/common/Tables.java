package guessmarket.ui.common;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Builds the columns of the tables this program shows, and gives each of them a width.
 *
 * <p>Every table here shows read only text taken from a data transfer object, so a column is
 * completely described by its heading and one function turning a row into the text of that cell.
 * Saying that in one line keeps the controllers about what they display rather than about how a
 * {@code TableColumn} is wired up.
 *
 * <p>Numbers get a column of their own kind: right aligned and in a fixed width font, so that a
 * list of prices lines up on the decimal point and can be read down the column.
 *
 * <p>No column is given a width by hand. A column is measured against the longest thing in it,
 * heading included, and that measurement is its minimum: a column of "Yes" and "No" is as narrow
 * as the word "OPTION" above it, and takes no more of the window than it has anything to put
 * there. Whatever the table has left over is then shared out between the columns in proportion,
 * so a wide window is filled rather than ending in an empty strip, and a window too narrow even
 * for the measured widths keeps every column readable and scrolls sideways instead.
 *
 * <p>Sideways only when it really is too narrow. A column is measured twice - at what the text
 * in it needs, and at the little more it would like for the arrow of a column somebody has sorted
 * by - and a table short of width takes back what was only wanted before it will scroll. That,
 * and everything above being worked out in whole points, is what makes a bar along the bottom
 * edge mean that the columns do not fit rather than that they missed by one.
 *
 * <p>No table is given a height by hand either, and no table has a height that hides anything: it
 * is as tall as its headings and its rows together, always, so that every row it has is on the
 * screen and none of these tables ever grows a scroll bar down its side. See {@link #fitHeight}
 * for why that is worth insisting on.
 */
public final class Tables {

    /** Set on a table with no rows, so that the stylesheet can put the table itself away. */
    private static final PseudoClass NO_ROWS = PseudoClass.getPseudoClass("no-rows");

    /** Marks a table that has already been taken in hand, so it is only ever set up once. */
    private static final String ADOPTED = "guessmarket.adopted";

    /**
     * What a cell adds to the text in it: the eight points of padding the stylesheet gives it on
     * each side. The one point of border it also carries runs along the bottom of the row and
     * takes nothing away from the width.
     */
    private static final double CELL_PADDING = 16;

    /**
     * What a heading adds to the text in it: four points of padding on each side and one of
     * border down its right hand edge.
     */
    private static final double HEADING_PADDING = 9;

    /**
     * The room a heading keeps for the arrow that appears in the column somebody has sorted by.
     *
     * <p>It is wanted rather than needed, and that distinction is the whole of why a column is
     * measured twice. At most one column in a table is ever sorted, so this is space that all but
     * one of them hold open for nothing; and a table a few points short of the sum of those asks
     * used to answer with a scroll bar along its bottom edge - a bar that would have slid the
     * columns by those few points, under a table that was in truth showing every word it had.
     * {@link #shareWidth} hands this room back, as much of it as it takes, before it will let a
     * table scroll, so a bar means what it says: the text itself does not fit.
     *
     * <p>Seven and not the full width of an arrow, so that the heading of the one column that is
     * sorted is crowded rather than cut.
     */
    private static final double SORT_ARROW_ROOM = 7;

    /** Where a column keeps what it was measured at: what it needs, and what it would like. */
    private static final String NEEDED = "guessmarket.needed";
    private static final String LIKED = "guessmarket.liked";

    /** The size a heading is set in, as a fraction of the size of the rows underneath it. */
    private static final double HEADING_FONT_SCALE = 0.82;

    /** The fixed width families a numeric column asks the stylesheet for, in the same order. */
    private static final List<String> FIXED_WIDTH_FAMILIES =
            List.of("Menlo", "Consolas", "Courier New", "Monospaced");

    /**
     * How thick a scroll bar is, used to work out how much taller a table is when it has one
     * along its bottom edge. It matches the thickness the stylesheet gives one.
     *
     * <p>Nothing is kept clear down the right hand side any more. A table here never grows a bar
     * there - see {@link #fitHeight} - so eleven points were being held open for a bar that could
     * not appear, out of the width the columns had to fit inside, and a table with four points to
     * spare was given a bar along the bottom for the sake of the eleven it was not allowed to use.
     * What stops the columns from spilling over the edge instead is that they are shared out in
     * whole points and never add up to more than there are: see {@link #shareWidth}.
     */
    private static final double SCROLL_BAR_BREADTH = 11;

    /**
     * How tall one row is, used until the table has drawn a row and can be asked. It matches the
     * cell size the stylesheet gives one.
     */
    private static final double ROW_HEIGHT = 32;

    /**
     * How tall one row of headings is, used until the table has drawn them. It matches the size
     * the stylesheet gives a column header. A table whose columns sit under headings of their own
     * has two such rows, which is why the drawn headings are asked first rather than this being
     * multiplied by anything.
     */
    private static final double HEADINGS_HEIGHT = 34;

    /** The room left round the sentence a table shows in place of the rows it has not got. */
    private static final double EMPTY_TABLE_PADDING = 26;

    /** One node to measure text with, since all of this happens on the one interface thread. */
    private static final Text RULER = new Text();

    private Tables() {
    }

    /** @return a column of plain text, aligned to the left. */
    public static <S> TableColumn<S, String> text(String heading, Function<S, String> value) {
        return column(heading, value, null);
    }

    /** @return a column of numbers, aligned to the right and set in a fixed width font. */
    public static <S> TableColumn<S, String> number(String heading, Function<S, String> value) {
        return column(heading, value, "numeric-column");
    }

    /**
     * @return a column of numbers that also carries a style of its own, used to colour the two
     *         sides of an order book
     */
    public static <S> TableColumn<S, String> styled(String heading, String styleClass,
                                                    Function<S, String> value) {
        return column(heading, value, styleClass);
    }

    private static <S> TableColumn<S, String> column(String heading, Function<S, String> value,
                                                     String styleClass) {
        TableColumn<S, String> column = new TableColumn<>(heading);
        column.setCellValueFactory(cell -> read(value, cell.getValue()));
        if (styleClass != null) {
            column.getStyleClass().add(styleClass);
        }
        return column;
    }

    /**
     * Reads one cell. A row can be null while a table is being rebuilt, and a value that is not
     * there yet is shown as a dash rather than allowed to become the word "null" on the screen.
     */
    private static <S> ObservableValue<String> read(Function<S, String> value, S row) {
        if (row == null) {
            return new ReadOnlyStringWrapper(Formats.NOTHING);
        }
        String text = value.apply(row);
        return new ReadOnlyStringWrapper(text == null ? Formats.NOTHING : text);
    }

    /**
     * @return a heading with columns underneath it, used to put the name of an option above the
     *         three figures that belong to it rather than repeating that name in each of them,
     *         which would leave three headings too narrow to read
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <S> TableColumn<S, String> group(String heading,
                                                   TableColumn<S, String>... children) {
        TableColumn<S, String> parent = new TableColumn<>(heading);
        parent.getColumns().setAll(children);
        parent.setSortable(false);
        parent.setReorderable(false);
        return parent;
    }

    /**
     * Says what an empty table means, instead of leaving a blank rectangle on the screen.
     *
     * <p>A table with nothing in it is not shown as a table at all: the stylesheet takes away its
     * headings and its frame while it is empty, so what is left on the card is the sentence
     * saying why, and not a set of column names standing over nothing.
     */
    public static void emptyMessage(TableView<?> table, String message) {
        Label placeholder = new Label(message);
        placeholder.getStyleClass().add("faint");
        placeholder.setWrapText(true);
        table.setPlaceholder(placeholder);
        whenRowsChange(table, () -> markEmptiness(table));
    }

    private static void markEmptiness(TableView<?> table) {
        table.pseudoClassStateChanged(NO_ROWS,
                table.getItems() == null || table.getItems().isEmpty());
    }

    /**
     * Does something now, and again whenever the rows of a table change.
     *
     * <p>There are two ways for them to change and both matter: the table can be handed a new
     * list of rows, which is how every screen here refreshes, and the list it is already holding
     * can be added to. So the watch has to follow the table from one list to the next, which is
     * what the one place kept here remembers - a property hands out the value it now holds, never
     * the one it used to, and the old list has to be let go of by something.
     *
     * <p>The move from list to list is watched for going stale rather than for changing, because
     * one list of rows equals another with the same rows in it, and a table being handed an empty
     * list in place of the empty list it had is exactly the case that matters: it is how a table
     * that had nothing to show is told that it still has nothing to show.
     */
    private static void whenRowsChange(TableView<?> table, Runnable action) {
        ListChangeListener<Object> rows = change -> action.run();
        ObservableList<?>[] watched = new ObservableList<?>[1];
        InvalidationListener followTheRows = items -> {
            if (watched[0] != null) {
                watched[0].removeListener(rows);
            }
            watched[0] = table.getItems();
            if (watched[0] != null) {
                watched[0].addListener(rows);
            }
            action.run();
        };
        table.itemsProperty().addListener(followTheRows);
        followTheRows.invalidated(table.itemsProperty());
    }

    /**
     * Replaces the columns of a table in one go, and starts measuring them.
     *
     * <p>The columns are rebuilt rather than declared in the layout file because several of them
     * are headed with the name of an option, and those names are only known once a file has been
     * loaded.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <S> void columns(TableView<S> table, TableColumn<S, String>... columns) {
        table.getColumns().setAll(columns);
        fit(table);
    }

    /**
     * Takes away the picking of rows, leaving the table something to read rather than
     * something to choose from.
     *
     * <p>Nothing in this program asks a table which of its rows is picked, and no row has
     * anything behind it: everything a row has to say is already written across it, and
     * clicking one showed nothing further. A row that stayed lit after it was clicked was
     * therefore promising a next thing that never came, which is worse than saying nothing at
     * all, so a table is given no selection to make.
     *
     * <p>The keyboard is taken off the rows for the same reason. A table that could still walk a
     * frame from row to row with the arrow keys, while selecting none of them, would be drawing
     * the same empty promise a little more faintly, so the table is left with no row to point at
     * either. Nothing is lost with it: walking the rows was only ever a way of choosing one.
     *
     * <p>What the pointer can honestly say it still says: a row lights while it is under the
     * pointer, to keep the eye on one line of figures across a wide table, and lets go of it
     * again when the pointer moves on. The headings keep everything they could do - a column can
     * still be dragged to another place in the table, widened, and sorted by. Choosing is left to
     * the places where a choice decides something: the lists of users and of events, where
     * picking one is what the rest of the screen is about.
     */
    private static void stopPicking(TableView<?> table) {
        table.setSelectionModel(null);
        table.setFocusModel(null);
    }

    // ------------------------------------------------------------------ widths

    /**
     * Makes a table measure its columns against what is in them and then fill its width with
     * them.
     *
     * <p>Four things call for that to be worked out again: the rows changing, which is what the
     * columns are measured against; the columns themselves being replaced, which the participants
     * table does for every event; the table being made wider or narrower; and the skin being
     * changed, since the three skins are set in three different typefaces at three different
     * sizes, and a column measured in one of them is the wrong width in either of the others.
     *
     * <p>This is also where a table is taken in hand for the first time, and so where it is
     * told that its rows are not to be picked. Asking twice for the same table only measures
     * it again, rather than setting it up a second time, so a table whose columns are rebuilt
     * on every refresh does not collect a listener per refresh.
     */
    public static void fit(TableView<?> table) {
        if (table.getProperties().put(ADOPTED, Boolean.TRUE) != null) {
            measureLater(table);
            return;
        }
        stopPicking(table);
        whenRowsChange(table, () -> measureLater(table));
        table.getColumns().addListener((ListChangeListener<TableColumn<?, ?>>)
                change -> measureLater(table));
        table.widthProperty().addListener((width, was, now) -> {
            shareWidth(table);
            // A narrower table is a taller one, both because the sentence a table shows while it
            // is empty wraps and because columns that no longer fit put a bar along the bottom.
            fitHeight(table);
        });
        table.sceneProperty().addListener((scene, was, now) -> watchSkin(table, now));
        watchSkin(table, table.getScene());
        measureLater(table);
    }

    /** Watches for the skin being changed, which is the scene being given other stylesheets. */
    private static void watchSkin(TableView<?> table, Scene scene) {
        if (scene != null) {
            scene.getStylesheets().addListener((ListChangeListener<String>)
                    change -> measureLater(table));
        }
    }

    /**
     * Measures once the interface has caught up. Everything that asks for a measurement has just
     * changed something the measurement depends on - the rows, the columns, the stylesheet - and
     * measuring before that change has been laid out would measure the state before it.
     */
    private static void measureLater(TableView<?> table) {
        Platform.runLater(() -> {
            measure(table);
            shareWidth(table);
            fitHeight(table);
        });
    }

    /**
     * Measures every column against the longest thing in it, its heading included, and writes
     * down both of the widths that measurement gives: what the column needs to show its text in
     * full, and what it would like on top of that for a sort arrow. {@link #shareWidth} decides
     * between the two according to how much width the table turns out to have.
     */
    private static void measure(TableView<?> table) {
        List<? extends TableColumn<?, ?>> columns = table.getVisibleLeafColumns();
        if (columns.isEmpty()) {
            return;
        }
        // Everything below is measured against the font the table is currently written in, and a
        // skin having just been chosen is one of the things that asks for this. The stylesheet is
        // put on the table here rather than waited for, since a measurement taken between the
        // choosing and the next drawing would be a measurement in the typeface just left behind.
        if (table.getScene() != null) {
            table.applyCss();
        }
        List<Double> widths = new ArrayList<>(columns.size());
        for (TableColumn<?, ?> column : columns) {
            widths.add(neededWidth(table, column));
        }
        widenGroups(table, columns, widths);
        for (int i = 0; i < columns.size(); i++) {
            // Rounded up to a whole point, because a column is drawn on whole points whatever it
            // is told: ten columns each rounded up a fraction of a point are a table three points
            // wider than the sum it was shared out from, and three points is a scroll bar.
            double needed = Math.ceil(widths.get(i));
            TableColumn<?, ?> column = columns.get(i);
            column.getProperties().put(NEEDED, needed);
            column.getProperties().put(LIKED, needed + SORT_ARROW_ROOM);
            column.setMinWidth(needed);
        }
    }

    /** @return what one column needs to show its heading and every cell under it in full */
    private static double neededWidth(TableView<?> table, TableColumn<?, ?> column) {
        double needed = headingWidth(column);
        Font font = cellFont(table, column);
        int rows = table.getItems() == null ? 0 : table.getItems().size();
        for (int row = 0; row < rows; row++) {
            ObservableValue<?> cell = column.getCellObservableValue(row);
            Object value = cell == null ? null : cell.getValue();
            if (value != null) {
                needed = Math.max(needed, textWidth(value.toString(), font) + CELL_PADDING);
            }
        }
        return needed;
    }

    /**
     * Widens the columns that sit under a heading of their own until they are together as wide as
     * that heading, so that an option with a long name keeps its name above the figures that
     * belong to it rather than having it cut off.
     */
    private static void widenGroups(TableView<?> table, List<? extends TableColumn<?, ?>> columns,
                                    List<Double> widths) {
        for (TableColumn<?, ?> parent : table.getColumns()) {
            if (parent.getColumns().isEmpty()) {
                continue;
            }
            List<Integer> mine = new ArrayList<>();
            double have = 0;
            for (int i = 0; i < columns.size(); i++) {
                if (isUnder(columns.get(i), parent)) {
                    mine.add(i);
                    have += widths.get(i);
                }
            }
            double missing = headingWidth(parent) - have;
            if (missing > 0 && !mine.isEmpty()) {
                double each = missing / mine.size();
                for (int index : mine) {
                    widths.set(index, widths.get(index) + each);
                }
            }
        }
    }

    private static boolean isUnder(TableColumnBase<?, ?> column, TableColumnBase<?, ?> parent) {
        for (TableColumnBase<?, ?> above = column; above != null;
                above = above.getParentColumn()) {
            if (above == parent) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives every column a width, out of the width the table has to give.
     *
     * <p>There are three things that can be true of a table, and each is answered differently.
     * It can have more width than the columns asked for, and then the extra is shared between
     * them in proportion to what they asked, so that the columns with the most in them get the
     * most of it and a wide window is filled rather than ending in an empty strip. It can have
     * less, and then the columns give back the room they were holding open for a sort arrow -
     * as much of it as it takes - and fill exactly the width there is. Or it can have less even
     * than the text in them needs, and only then does the table scroll sideways.
     *
     * <p>A column is never made narrower than the text in it by any of this, so no column here is
     * ever shortened into an ellipsis, and widening the columns cannot change the width of the
     * table that holds them: the two cannot chase each other.
     */
    private static void shareWidth(TableView<?> table) {
        List<? extends TableColumn<?, ?>> columns = table.getVisibleLeafColumns();
        double needed = totalWidth(table, NEEDED);
        double liked = totalWidth(table, LIKED);
        double room = roomForColumns(table);
        if (liked <= 0 || room <= 0) {
            return;
        }
        double given = 0;
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<?, ?> column = columns.get(i);
            double share = shareFor(column, room, needed, liked);
            if (room >= needed && i == columns.size() - 1) {
                // Whatever all that rounding down left over goes to the last column, so the row
                // ends exactly where the table does rather than a few points short of it.
                share = Math.max(share, room - given);
            }
            column.setPrefWidth(share);
            given += share;
        }
    }

    /**
     * @return the width one column is to be drawn at, out of the {@code room} the table has for
     *         all of them together
     */
    private static double shareFor(TableColumn<?, ?> column, double room,
                                   double needed, double liked) {
        double mine = measurement(column, NEEDED);
        if (room < needed) {
            return mine;
        }
        // Downwards in both of what follows, so that what is handed out is never more than there
        // was to hand out. Every measurement is already a whole point, so a column can only lose
        // the part of the sharing that was fractional, and never fall under what it needs.
        if (room >= liked) {
            return Math.floor(measurement(column, LIKED) * (room / liked));
        }
        // Between the two: every column keeps the whole of what it needs, and gets the same
        // fraction of what it asked for on top, so the columns come to exactly the width there is.
        double fraction = (room - needed) / (liked - needed);
        return Math.floor(mine + (measurement(column, LIKED) - mine) * fraction);
    }

    /**
     * @return what the columns come to altogether at one of the two widths they were measured at
     *
     * <p>Nought until they have been measured, which is what tells the callers to leave the table
     * alone: a table is laid out, and so asked to share out its width, before it has been
     * measured for the first time.
     */
    private static double totalWidth(TableView<?> table, String kind) {
        double total = 0;
        for (TableColumn<?, ?> column : table.getVisibleLeafColumns()) {
            total += measurement(column, kind);
        }
        return total;
    }

    /** @return one of the two widths a column was measured at, or nought before it was measured */
    private static double measurement(TableColumn<?, ?> column, String kind) {
        Object width = column.getProperties().get(kind);
        return width instanceof Double taken ? taken : 0;
    }

    /**
     * @return how much width there is for the columns to be shared out inside, in whole points
     *
     * <p>Whole points because that is what they are drawn on. A table asked to fit its columns
     * into a width and a half will fit them into the width and then find itself half a point too
     * narrow for them.
     */
    private static double roomForColumns(TableView<?> table) {
        return Math.floor(table.getWidth()
                - table.getInsets().getLeft() - table.getInsets().getRight());
    }

    /**
     * @return how much is kept clear down the right hand side of a table for the bar it grows
     *         when it has more rows than fit
     */
    private static double scrollBarBreadth(TableView<?> table) {
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL) {
                return bar.prefWidth(-1);
            }
        }
        return SCROLL_BAR_BREADTH;
    }

    // ------------------------------------------------------------------ height

    /**
     * Gives a table the height of everything in it, so that it never has a row of its own to
     * scroll.
     *
     * <p>A table asked to be shorter than its rows keeps the rows and hides them behind a bar down
     * its right hand side, which is the one thing a table in this program must not do. Every table
     * here is a list to be read whole - what somebody holds, what has been traded, what is waiting
     * in a book - and a list read through a window three rows tall is not the list, it is a sample
     * of it. Worse, the bar is inside a panel that is itself inside a scrolling page, so a roll of
     * the wheel over one of these tables scrolls whichever of the two happens to be under the
     * pointer.
     *
     * <p>So the height is not a setting any more. It is added up from what is actually there - the
     * headings, a row apiece, and the bar along the bottom if the columns are wider than the panel
     * - and it is set as the smallest, the preferred and the largest height at once, so that
     * nothing above can stretch the table past its rows or squeeze it under them either. The page
     * these panels sit on scrolls, and that is where a long history belongs: one scroll bar for
     * the whole screen rather than one per table on it.
     */
    private static void fitHeight(TableView<?> table) {
        double needed = neededHeight(table);
        table.setMinHeight(needed);
        table.setPrefHeight(needed);
        table.setMaxHeight(needed);
    }

    /** @return the height at which every row of the table is on the screen at once. */
    private static double neededHeight(TableView<?> table) {
        double frame = table.getInsets().getTop() + table.getInsets().getBottom()
                + horizontalBarHeight(table);
        int rows = table.getItems() == null ? 0 : table.getItems().size();
        return rows == 0
                ? frame + emptyHeight(table)
                : frame + headingsHeight(table) + rows * rowHeight(table);
    }

    /**
     * @return how tall the headings are, asking the drawn ones first because a table whose columns
     *         are grouped under headings of their own has two rows of them and not one
     */
    private static double headingsHeight(TableView<?> table) {
        Node headings = table.lookup(".column-header-background");
        double drawn = headings == null ? 0 : headings.prefHeight(-1);
        return drawn > 0 ? drawn : HEADINGS_HEIGHT;
    }

    /** @return how tall one row is, measured from a drawn one where there is one to measure. */
    private static double rowHeight(TableView<?> table) {
        for (Node node : table.lookupAll(".table-row-cell")) {
            double drawn = node.prefHeight(-1);
            if (drawn > 0) {
                return drawn;
            }
        }
        return ROW_HEIGHT;
    }

    /**
     * @return how much room the sentence shown in place of the rows needs
     *
     * <p>An empty table is not drawn as a table at all - the stylesheet takes its headings and its
     * frame away - so what has to be made room for is the sentence and nothing else.
     */
    private static double emptyHeight(TableView<?> table) {
        Node saying = table.getPlaceholder();
        if (saying == null) {
            return EMPTY_TABLE_PADDING;
        }
        double room = table.getWidth() - table.getInsets().getLeft()
                - table.getInsets().getRight() - EMPTY_TABLE_PADDING;
        return saying.prefHeight(room > 0 ? room : -1) + EMPTY_TABLE_PADDING;
    }

    /**
     * @return how tall the bar along the bottom is, or nought when there is not going to be one
     *
     * <p>Worked out the same way {@link #shareWidth} works out whether the columns fit, rather
     * than by looking for a bar on the screen. The two questions are asked at different moments -
     * a table is given its height while the width it will be laid out at is still being settled -
     * and a bar that is looked for before it has been drawn is a bar whose height is left out of
     * a table that is about to have one.
     */
    private static double horizontalBarHeight(TableView<?> table) {
        double room = roomForColumns(table);
        if (room <= 0 || totalWidth(table, NEEDED) <= room) {
            return 0;
        }
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.HORIZONTAL) {
                return bar.prefHeight(-1);
            }
        }
        return SCROLL_BAR_BREADTH;
    }

    // ------------------------------------------------------------------ measuring text

    private static double headingWidth(TableColumn<?, ?> column) {
        return textWidth(column.getText(), headingFont(column)) + HEADING_PADDING;
    }

    private static double textWidth(String text, Font font) {
        RULER.setText(text == null ? "" : text);
        RULER.setFont(font);
        return RULER.getLayoutBounds().getWidth();
    }

    /** @return the font a heading is written in, which the skin decides and the stylesheet sizes */
    private static Font headingFont(TableColumn<?, ?> column) {
        Node header = column.getStyleableNode();
        if (header != null && header.lookup(".label") instanceof Label label) {
            return label.getFont();
        }
        return Font.getDefault();
    }

    /**
     * @return the font the cells of one column are written in
     *
     * <p>A rendered cell is asked first, since it is the thing being measured. Before there is
     * one - a table that has just been given its columns, or one scrolled so far sideways that
     * this column has no cells at the moment - the font is worked back from the heading above it,
     * which is the same typeface at a known fraction of the size, and a numeric column is
     * measured in the first fixed width family the stylesheet asks for that this machine has.
     */
    private static Font cellFont(TableView<?> table, TableColumn<?, ?> column) {
        for (Node node : table.lookupAll(".table-cell")) {
            if (node instanceof TableCell<?, ?> cell && cell.getTableColumn() == column) {
                return cell.getFont();
            }
        }
        Font heading = headingFont(column);
        double size = heading.getSize() / HEADING_FONT_SCALE;
        if (!column.getStyleClass().contains("numeric-column")) {
            return Font.font(heading.getFamily(), size);
        }
        for (String family : FIXED_WIDTH_FAMILIES) {
            if (Font.getFamilies().contains(family)) {
                return Font.font(family, size);
            }
        }
        return Font.font(size);
    }
}
