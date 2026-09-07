package guessmarket.ui.common;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * The short animations that accompany the actions of the program, and the one switch that turns
 * all of them off.
 *
 * <p>They are off when the program starts. Nothing here decides what ends up on the screen, only
 * how it gets there, so with the switch off the interface shows exactly what it would show if
 * this class did not exist — it simply shows it at once. No animation is longer than
 * {@link #LONGEST}.
 */
public final class Animations {

    /** No animation in the program may last longer than this. */
    public static final Duration LONGEST = Duration.seconds(2);

    private static final Duration REVEAL = Duration.millis(260);
    private static final Duration FLASH_HALF = Duration.millis(180);
    private static final Duration SLIDE = Duration.millis(300);
    private static final Duration FADE_OUT = Duration.millis(420);

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

    /**
     * Leaves a node where it is for a while and then takes it off the screen, fading it out on the
     * way. The waiting is not animation, so only the fade itself counts against {@link #LONGEST}.
     *
     * <p>This is the one method here that still does something once the animations are switched
     * off: the node is due to go either way, and the switch only decides whether it dissolves
     * first or simply disappears.
     *
     * @param linger how long the node stays as it is before it begins to go.
     * @param hide   what actually takes the node off the screen once it can no longer be seen.
     * @return the transition, which has not been started yet, so that a caller who needs the node
     *         back before it has gone can stop it.
     */
    public static Animation hideAfter(Node node, Duration linger, Runnable hide) {
        PauseTransition wait = new PauseTransition(linger);
        Animation whole = isEnabled() ? new SequentialTransition(wait, fadeOut(node)) : wait;
        whole.setOnFinished(event -> {
            hide.run();
            node.setOpacity(1);
        });
        return whole;
    }

    private static FadeTransition fadeOut(Node node) {
        FadeTransition fade = new FadeTransition(FADE_OUT, node);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);
        return fade;
    }
}
