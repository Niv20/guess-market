package guessmarket.ui.common;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Scene;

import java.util.Locale;

/**
 * How large the writing in the window is.
 *
 * <p>Every size in the shared stylesheet is stated as a multiple of the size of ordinary text and
 * never in points of its own, so there is exactly one number in the whole interface that decides
 * how large everything is. Each skin names that number, because a skin that changes the typeface
 * has to be free to change the size that typeface is comfortable at; this class holds the
 * multiplier the person puts on top of it, and the window is laid out again around the result.
 *
 * <p>The number is written onto the root of the scene as a style of its own rather than into a
 * stylesheet, because a style written onto a node beats every stylesheet, and because the whole
 * window inherits its font from that one node. Nothing else has to be told: a label two panels
 * down is already measured in multiples of it.
 *
 * <p>Only a {@link Skin} may apply it, so that the size of the writing is always the size that
 * skin asked for, multiplied by what was chosen here, and never one of the two on its own.
 */
public final class TextSize {

    /** As small as the writing may be made: still readable, and fits a great deal on the screen. */
    public static final double SMALLEST = 0.85;

    /** As large as the writing may be made without the layout starting to lose its shape. */
    public static final double LARGEST = 1.4;

    /** The size the skin itself asks for, which is what the program starts at. */
    public static final double NORMAL = 1;

    /** How far one step of the slider moves, so that the percentage lands on a round number. */
    public static final double STEP = 0.05;

    private static final DoubleProperty SCALE = new SimpleDoubleProperty(NORMAL);

    private TextSize() {
    }

    /** The multiplier a slider in the settings binds to. */
    public static DoubleProperty scaleProperty() {
        return SCALE;
    }

    public static double get() {
        return SCALE.get();
    }

    /**
     * Tells a scene how large its writing is, in points, so that everything measured against it
     * follows.
     *
     * @param baseSize the size the skin being worn asks for, before the multiplier is applied.
     */
    static void applyTo(Scene scene, double baseSize) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        scene.getRoot().setStyle(
                String.format(Locale.US, "-fx-font-size: %.2fpx;", baseSize * SCALE.get()));
    }
}
