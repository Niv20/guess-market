package guessmarket.ui.common;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * A tile with its mark standing in a narrow column of its own down the left of it.
 *
 * <p>The column is the whole point: a mark at the same distance from the edge on every tile in a
 * list gives that list one straight rail down it, which is read in a single pass where a word has
 * to be read a row at a time. That is what an ordinary row of things side by side already gives,
 * and it is all this would have to be if a mark could be left anywhere down that rail.
 *
 * <p>It cannot. A mark set against the middle of the tile is against the middle of nothing in
 * particular: on a card in a strip, which is given the height of the strip whether it has that
 * much to say or not, the middle of the card is the empty space kept clear above the figures, and
 * the mark ends up floating there a line and a half below the name it belongs to. What it belongs
 * to is the name and the line under it - the block that says which thing this is - so that is what
 * it is held against, wherever on the tile that block has ended up.
 *
 * <p>Which is not known until the tile has been laid out, and is not the same twice: the block is
 * at the top of a card in a strip, at the top of a narrow tile, and against the middle of a wide
 * one, where {@link TileBody} sets it against the taller column beside it. So the row is laid out
 * the ordinary way first, the tile beside the mark is made to lay itself out, and only then is the
 * mark moved to the middle of the block - by which time there is a real answer to where that is.
 */
final class MarkedRow extends HBox {

    private final Node mark;
    private final Region alignedWith;
    private final Node content;

    /**
     * @param mark        the small thing that stands in the column, such as a status dot
     * @param alignedWith the block of the tile the mark is held against, which is somewhere inside
     *                    {@code content}
     * @param content     the whole of the tile apart from the mark
     * @param gap         how far the column is held off the tile, in pixels
     */
    MarkedRow(Node mark, Region alignedWith, Node content, double gap) {
        super(gap, mark, content);
        this.mark = mark;
        this.alignedWith = alignedWith;
        this.content = content;
        // Along the top rather than against the middle, because the mark is put where it belongs
        // below and everything else on the row is one block that fills the height either way.
        setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        // The row has just told the tile beside the mark how much room it has; the tile has not
        // yet passed that on to what is inside it, and it is inside it that the block is. Asking
        // for that now rather than waiting for the pass to reach it is what makes the answer below
        // the block's real place and not the one it was in before this row was resized.
        if (content instanceof Parent parent) {
            parent.layout();
        }
        Bounds block = sceneToLocal(alignedWith.localToScene(alignedWith.getBoundsInLocal()));
        double middleOfBlock = (block.getMinY() + block.getMaxY()) / 2;
        Bounds marked = mark.getLayoutBounds();
        double middleOfMark = mark.getLayoutY() + marked.getMinY() + marked.getHeight() / 2;
        // Moved by the difference rather than put at an absolute height, so that this says the
        // same thing for a mark whose drawing starts at its own top left corner and for one, such
        // as a circle, drawn outwards from the middle of itself.
        mark.setLayoutY(snapPositionY(mark.getLayoutY() + middleOfBlock - middleOfMark));
    }
}
