package guessmarket.ui.common;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The middle of a tile: what the thing is called, and what it is worth.
 *
 * <p>Those are two different questions, and a tile wide enough to answer both at once should not
 * be answering them one under the other. Stacked, the words and the figures share the left hand
 * edge and everything to the right of the longest line of them is empty; side by side, the words
 * keep the left hand edge and the figures take a column of their own along the right, where the
 * numbers of every tile in the list line up under one another and can be read straight down.
 * That is what a table gave and what a column of tiles otherwise gives up.
 *
 * <p>Which of the two it is is decided from the width the tile is actually given, not from a
 * setting: this is a list in a panel whose divider can be dragged, so the same tile has to be
 * both, and has to change while it is being dragged. It changes at one width,
 * {@link #TWO_COLUMN_WIDTH}, and not at whatever width each tile's own figures happen to stop
 * fitting at, because every tile in a list is the same width as every other: a rule read off the
 * figures would split one list into two columns for the events whose money is short and one for
 * the events whose money is long, and a list arranged two ways at once looks like an accident
 * rather than a decision. {@link #MIN_WORDS_WIDTH} is underneath that as a floor rather than a
 * rule, for the tile whose figures are so long that the name would be left a ribbon: it is worth
 * breaking the list's one arrangement for, and nothing else is.
 *
 * <p>The figures are turned round with the column they are in. Along the right they are set
 * right, so that the caption and the figure under it share the edge the tile ends at and the
 * whole list has one straight rail down it; underneath the words they are set left again, which
 * is the only way they read there.
 */
final class TileBody extends Pane {

    /**
     * The width at which a tile is worth splitting in two. Below it the tile is a column, above it
     * the figures move out to the right. It is set where the widest figures this program shows —
     * a balance and a result, side by side — still leave the name half the tile to itself.
     */
    private static final double TWO_COLUMN_WIDTH = 300;

    /**
     * The narrowest the name and the line under it may be squeezed before the figures give up and
     * go back underneath, whatever {@link #TWO_COLUMN_WIDTH} says. It is about two short words
     * across, which is enough for a name to wrap in and still be read.
     */
    private static final double MIN_WORDS_WIDTH = 140;

    /** The gap between the two columns, wide enough that neither is read as part of the other. */
    private static final double COLUMN_GAP = 18;

    /** The gap between the words and the figures underneath them, when there is no room beside. */
    private static final double ROW_GAP = 4;

    private final Region words;
    private final FlowPane figures;

    /** Which way round the tile is at the moment, or null until it has first been laid out. */
    private Boolean figuresBeside;

    TileBody(Region words, FlowPane figures) {
        this.words = words;
        this.figures = figures;
        getChildren().addAll(words, figures);
    }

    /**
     * @return whether there is room for the figures beside the words rather than under them
     *
     * <p>The figures are measured unwrapped, at the width they would like to be given: they are
     * a handful of short numbers, and a column that folded them in half would be worse than no
     * column at all.
     */
    private boolean sideBySide(double contentWidth) {
        return contentWidth >= TWO_COLUMN_WIDTH
                && contentWidth - railWidth() - COLUMN_GAP >= MIN_WORDS_WIDTH;
    }

    /**
     * @return how wide the figures are when they are all on one line
     *
     * <p>Added up here rather than asked for, because a flow pane asked how wide it would like to
     * be answers with the width it has been told to wrap at — four hundred points, since nobody
     * has told it otherwise — and not with the width of what is in it. The figures are added the
     * way the flow pane will add them when it lays them out, snapped piece by piece, so that the
     * column they are given is the column they fit on one line in.
     */
    private double railWidth() {
        double width = figures.getInsets().getLeft() + figures.getInsets().getRight();
        double gap = snapSpaceX(figures.getHgap());
        boolean first = true;
        for (Node figure : figures.getChildren()) {
            if (!figure.isManaged()) {
                continue;
            }
            width += first ? 0 : gap;
            width += snapSizeX(figure.prefWidth(-1));
            first = false;
        }
        return width;
    }

    @Override
    protected double computePrefHeight(double width) {
        double content = contentWidth(width);
        if (content <= 0) {
            content = plannedWidth();
        }
        if (content <= 0) {
            // Nothing above knows either. The question still has to be answered with a height,
            // and it is answered for the words on their own lines rather than for a width of no
            // width at all: a label told to wrap inside nothing wraps after every letter, and
            // comes back with a height of one line per letter of the name.
            return stackedHeight(-1);
        }
        if (sideBySide(content)) {
            double rail = railWidth();
            return snappedTopInset() + snappedBottomInset()
                    + Math.max(words.prefHeight(content - rail - COLUMN_GAP),
                            figures.prefHeight(rail));
        }
        return stackedHeight(content);
    }

    /** @return how tall the tile is with the figures under the words, at the given width. */
    private double stackedHeight(double width) {
        return snappedTopInset() + snappedBottomInset()
                + words.prefHeight(width) + ROW_GAP + figures.prefHeight(width);
    }

    /**
     * A tile is as tall as what is on it. Left to itself a pane says it can be as short as it
     * likes, and the list would then hand every tile a height that cuts the figures off it.
     */
    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    /**
     * How narrow the tile may be dragged. It is the words alone, because by the time it is that
     * narrow the figures are underneath them rather than beside them.
     */
    @Override
    protected double computeMinWidth(double height) {
        return snappedLeftInset() + snappedRightInset() + words.minWidth(-1);
    }

    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double contentWidth = getWidth() - left - snappedRightInset();
        double contentHeight = getHeight() - top - snappedBottomInset();
        if (contentWidth <= 0) {
            return;
        }

        if (sideBySide(contentWidth)) {
            turnFigures(true);
            double rail = snapSizeX(railWidth());
            double wordsWidth = contentWidth - rail - COLUMN_GAP;
            place(words, left, top, wordsWidth, contentHeight, HPos.LEFT);
            place(figures, left + wordsWidth + COLUMN_GAP, top, rail, contentHeight, HPos.RIGHT);
        } else {
            turnFigures(false);
            double wordsHeight = snapSizeY(words.prefHeight(contentWidth));
            place(words, left, top, contentWidth, wordsHeight, HPos.LEFT);
            place(figures, left, top + wordsHeight + ROW_GAP, contentWidth,
                    contentHeight - wordsHeight - ROW_GAP, HPos.LEFT);
        }
    }

    /**
     * Puts one of the two blocks in the space it has been given, at its own height rather than
     * stretched to fill it, so that the shorter of the two columns sits against the middle of the
     * taller one instead of floating at the top of a space it does not fill.
     */
    private void place(Node block, double x, double y, double width, double height, HPos across) {
        layoutInArea(block, x, y, width, height, 0, Insets.EMPTY, true, false, across, VPos.CENTER);
    }

    /**
     * Sets the figures the way round the column they are in asks for, and only when that has just
     * changed: this is called from every layout, and a property written on every pass would ask
     * for another one.
     */
    private void turnFigures(boolean beside) {
        if (figuresBeside != null && figuresBeside == beside) {
            return;
        }
        figuresBeside = beside;
        figures.setAlignment(beside ? Pos.CENTER_RIGHT : Pos.TOP_LEFT);
        for (Node figure : figures.getChildren()) {
            if (figure instanceof VBox box) {
                box.setAlignment(beside ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
            }
        }
    }

    /**
     * @return the width left for the two blocks, or nought or less when the tile has not been
     *         told how wide it is and has not been laid out to find out either
     */
    private double contentWidth(double width) {
        double outer = width >= 0 ? width : getWidth();
        return outer - snappedLeftInset() - snappedRightInset();
    }

    /**
     * @return the width this tile is about to be given, worked out from whatever it is inside
     *
     * <p>A list asks a tile how tall it is <em>before</em> it hands it a width, and remembers the
     * answer for as long as that tile is on the screen. A tile whose height depends on how it is
     * arranged, and whose arrangement depends on its width, therefore has to know its width one
     * question earlier than it is told it — and if it guesses the stacked height there, every
     * tile in the list is left with the empty band the other arrangement does not need.
     *
     * <p>So it is taken from the first thing above the tile that has been laid out already, which
     * in a list is the list, less the padding of everything in between. That is the width the
     * tile will be given, give or take the scroll bar the list has not yet decided to show, and
     * once it has really been given a width it is that one that decides — see
     * {@link #contentWidth}.
     */
    private double plannedWidth() {
        double taken = snappedLeftInset() + snappedRightInset();
        Parent above = getParent();
        while (above != null) {
            if (above instanceof Region region) {
                taken += region.snappedLeftInset() + region.snappedRightInset();
                if (region.getWidth() > 0) {
                    return region.getWidth() - taken;
                }
            }
            above = above.getParent();
        }
        return -1;
    }
}
