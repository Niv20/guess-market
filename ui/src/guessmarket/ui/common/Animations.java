package guessmarket.ui.common;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * The three short animations that accompany the actions of the program, and the one switch that
 * turns all of them off.
 *
 * <p>They are off when the program starts. Nothing here ever changes what is on the screen, only
 * how it arrives there, so with the switch off every method returns immediately and the interface
 * behaves exactly as it would if this class did not exist. No animation is longer than
 * {@link #LONGEST}.
 */
public final class Animations {

    /** No animation in the program may last longer than this. */
    public static final Duration LONGEST = Duration.seconds(2);

    private static final Duration REVEAL = Duration.millis(260);
    private static final Duration FLASH_HALF = Duration.millis(180);
    private static final Duration SLIDE = Duration.millis(300);

    /** Off to begin with, so the animations only run once they are switched on deliberately. */
    private static final BooleanProperty ENABLED = new SimpleBooleanProperty(false);

    private Animations() {
    }

    /** The switch a check box in the header binds to. */
    public static BooleanProperty enabledProperty() {
        return ENABLED;
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    /** Fades a panel in as it is filled with the details of whatever has just been selected. */
    public static void reveal(Node node) {
        if (node == null || !isEnabled()) {
            return;
        }
        FadeTransition fade = new FadeTransition(REVEAL, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setOnFinished(event -> node.setOpacity(1));
        fade.playFromStart();
    }

    /**
     * Brightens a figure and lets it settle again, used on a balance or a price the moment the
     * action that changed it has gone through.
     */
    public static void flash(Node node) {
        if (node == null || !isEnabled()) {
            return;
        }
        ScaleTransition grow = new ScaleTransition(FLASH_HALF, node);
        grow.setFromX(1);
        grow.setFromY(1);
        grow.setToX(1.14);
        grow.setToY(1.14);
        grow.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(FLASH_HALF, node);
        settle.setToX(1);
        settle.setToY(1);
        settle.setInterpolator(Interpolator.EASE_IN);

        SequentialTransition pulse = new SequentialTransition(node, grow, settle);
        pulse.setOnFinished(event -> {
            node.setScaleX(1);
            node.setScaleY(1);
        });
        pulse.playFromStart();
    }

    /** Slides a newly built panel in from the right, used when a tab swaps its detail area. */
    public static void slideIn(Node node) {
        if (node == null || !isEnabled()) {
            return;
        }
        TranslateTransition slide = new TranslateTransition(SLIDE, node);
        slide.setFromX(28);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        slide.setOnFinished(event -> node.setTranslateX(0));

        FadeTransition fade = new FadeTransition(SLIDE, node);
        fade.setFromValue(0.35);
        fade.setToValue(1);
        fade.setOnFinished(event -> node.setOpacity(1));

        slide.playFromStart();
        fade.playFromStart();
    }
}
