package guessmarket.ui.common;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.function.Function;

/**
 * Builds the columns of the tables this program shows.
 *
 * <p>Every table here shows read only text taken from a data transfer object, so a column is
 * completely described by its heading, how wide it should be, and one function turning a row into
 * the text of that cell. Saying that in one line keeps the controllers about what they display
 * rather than about how a {@code TableColumn} is wired up.
 *
 * <p>Numbers get a column of their own kind: right aligned and in a fixed width font, so that a
 * list of prices lines up on the decimal point and can be read down the column.
 *
 * <p>The width a column is asked for is the width it needs to be readable, and it is kept as the
 * column's minimum: a name cut to "Earthqua" or a figure cut to "$1,2" is worse than one that has
 * to be scrolled to. What the window has over and above those minimums is shared out between the
 * columns in proportion, so a wide window fills the table rather than leaving an empty strip down
 * the right, and a narrow one keeps every column legible and scrolls sideways instead.
 */
public final class Tables {

    /** Marks a table whose width is already being shared out, so it is only ever watched once. */
    private static final String SHARING_WIDTH = "guessmarket.sharingWidth";

    /**
     * The room kept clear down the right hand side of a table, used until the table has been
     * given a skin and can say how thick a scroll bar of its own actually is. It matches the
     * thickness the stylesheet gives one.
     *
     * <p>The room is kept clear whether or not there is a bar there at the moment, because a
     * table that filled the last of its width would grow one the moment it had a row too many,
     * and then be too wide for what was left, and grow a second bar underneath its columns for
     * the sake of eleven pixels.
     */
    private static final double SCROLL_BAR_BREADTH = 11;

    private Tables() {
    }

    /** @return a column of plain text, aligned to the left. */
    public static <S> TableColumn<S, String> text(String heading, double width,
                                                  Function<S, String> value) {
        return column(heading, width, value, null);
    }

    /** @return a column of numbers, aligned to the right and set in a fixed width font. */
    public static <S> TableColumn<S, String> number(String heading, double width,
                                                    Function<S, String> value) {
        return column(heading, width, value, "numeric-column");
    }

    /**
     * @return a column of numbers that also carries a style of its own, used to colour the two
     *         sides of an order book
     */
    public static <S> TableColumn<S, String> styled(String heading, double width,
                                                    String styleClass, Function<S, String> value) {
        return column(heading, width, value, styleClass);
    }

    private static <S> TableColumn<S, String> column(String heading, double width,
                                                     Function<S, String> value, String styleClass) {
        TableColumn<S, String> column = new TableColumn<>(heading);
        column.setMinWidth(width);
        column.setPrefWidth(width);
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

    /** Says what an empty table means, instead of leaving a blank rectangle on the screen. */
    public static void emptyMessage(TableView<?> table, String message) {
        Label placeholder = new Label(message);
        placeholder.getStyleClass().add("faint");
        placeholder.setWrapText(true);
        table.setPlaceholder(placeholder);
    }

    /**
     * Replaces the columns of a table in one go.
     *
     * <p>The columns are rebuilt rather than declared in the layout file because several of them
     * are headed with the name of an option, and those names are only known once a file has been
     * loaded.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <S> void columns(TableView<S> table, TableColumn<S, String>... columns) {
        table.getColumns().setAll(columns);
        shareWidth(table);
    }

    /**
     * Makes a table give its columns whatever width it has.
     *
     * <p>Left alone, a table lays its columns out at the width each one was asked for and does
     * nothing else with the space: a wide table ends in a blank strip that belongs to no column,
     * and a narrow one cuts the last columns off. Here the columns are scaled up together to fill
     * the table, and never scaled down past the width they were asked for, which is what leaves a
     * table too narrow for them to scroll sideways rather than lose them.
     *
     * <p>Two things are watched: the width of the table, and its columns, which the participants
     * table replaces every time it is shown an event with differently named options. Asking twice
     * for the same table only shares the width out again, rather than leaving it watched twice
     * over, so a table whose columns are rebuilt on every refresh does not collect a listener per
     * refresh.
     *
     * <p>Watching those two is enough. A column is only ever made wider than its minimum by this,
     * so widening the columns cannot change the width of the table that holds them, and the two
     * cannot chase each other.
     */
    public static void shareWidth(TableView<?> table) {
        if (table.getProperties().put(SHARING_WIDTH, Boolean.TRUE) != null) {
            shareWidthNow(table);
            return;
        }
        table.widthProperty().addListener((width, was, now) -> shareWidthNow(table));
        table.getColumns().addListener((ListChangeListener<TableColumn<?, ?>>)
                change -> shareWidthNow(table));
        shareWidthNow(table);
    }

    private static void shareWidthNow(TableView<?> table) {
        double wanted = 0;
        for (TableColumn<?, ?> column : table.getVisibleLeafColumns()) {
            wanted += column.getMinWidth();
        }
        if (wanted <= 0) {
            return;
        }
        double room = table.getWidth()
                - table.getInsets().getLeft() - table.getInsets().getRight()
                - scrollBarBreadth(table);
        double scale = Math.max(1, room / wanted);
        for (TableColumn<?, ?> column : table.getVisibleLeafColumns()) {
            column.setPrefWidth(column.getMinWidth() * scale);
        }
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
}
