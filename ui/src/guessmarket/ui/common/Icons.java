package guessmarket.ui.common;

import javafx.scene.Node;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

/**
 * The small drawings the interface puts beside a piece of text.
 *
 * <p>They are drawn rather than loaded from an image file, so that they stay sharp at any screen
 * scale and take their colour from the skin the window is wearing like everything else does.
 * Every call returns a new node, because one node cannot hang in two places at once.
 */
public final class Icons {

    /**
     * A warning sign fourteen points wide and twelve tall: the outline of the triangle, then the
     * bar and the dot of the exclamation mark inside it.
     *
     * <p>The three pieces are one shape filled by the even-odd rule, so the bar and the dot are
     * holes punched through the triangle rather than marks painted on top of it. That way the
     * whole sign is a single colour and reads the same on a light skin and on a dark one.
     */
    private static final String WARNING_TRIANGLE =
            "M7 0 L14 12 L0 12 Z "
            + "M6.2 4.2 H7.8 V8.4 H6.2 Z "
            + "M6.2 9.3 H7.8 V10.9 H6.2 Z";

    private Icons() {
    }

    /** The sign that goes in front of a message saying something went wrong. */
    public static Node warningTriangle() {
        SVGPath sign = new SVGPath();
        sign.setContent(WARNING_TRIANGLE);
        sign.setFillRule(FillRule.EVEN_ODD);
        sign.getStyleClass().add("warning-icon");
        return sign;
    }
}
