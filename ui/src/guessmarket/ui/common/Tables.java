package guessmarket.ui.common;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
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
 */
public final class Tables {

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
    }
}
