package guessmarket.ui.common;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.Set;

/**
 * The short animations that accompany the actions of the program, and the one switch that turns
 * all of them off.
 *
 * <p>They are off when the program starts. Nothing here decides what ends up on the screen, only
 * how it gets there, so with the switch off the interface shows exactly what it would show if
 * this class did not exist — it simply shows it at once. No animation is longer than
 * {@link #LONGEST}.
 *
 * <p>There is exactly one animation for a panel being refilled, {@link #switchIn}, and every panel
 * in the program uses it. Choosing another user, choosing another event and moving between the two
 * screens are the same gesture as far as the person is concerned, so they are answered the same
 * way rather than each being given a movement of its own.
 */
public final class Animations {

    /** No animation in the program may last longer than this. */
    public static final Duration LONGEST = Duration.seconds(2);

    private static final Duration SWITCH = Duration.millis(300);
    private static final Duration FLASH_HALF = Duration.millis(180);
    private static final Duration FADE_OUT = Duration.millis(420);

    /** How far to the right a panel begins before it slides into its place. */
    private static final double SWITCH_OFFSET = 28;

    /** How faint a panel begins before it comes up to full strength. */
    private static final double SWITCH_FROM_OPACITY = 0.35;

    /** Off to begin with, so the animations only run once they are switched on deliberately. */
    private static final BooleanProperty ENABLED = new SimpleBooleanProperty(false);

    /** The panels that are sliding in at this moment, so that a panel inside one of them can sit still. */
    private static final Set<Node> SWITCHING_IN = new HashSet<>();

    private Animations() {
    }

    /** The switch a check box in the header binds to. */
    public static BooleanProperty enabledProperty() {
        return ENABLED;
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    /**
     * Slides a panel in from the right and brings it up to full strength, for a panel that has just
     * been filled with something other than what it held a moment ago.
     *
     * <p>This is the whole vocabulary of the program for showing a new subject: the details of a
     * user, the details of an event and the screen behind a tab all arrive this way. It is meant to
     * be called every time the subject changes and not only the first time the panel appears,
     * because the movement is what says that what is on the screen is now about something else.
     *
     * <p>A panel inside a panel that is already sliding in is left alone. It is being carried along
     * by its parent, and animating it as well would only move it twice as far.
     */
    public static void switchIn(Node node) {
        if (node == null || !isEnabled() || isCarriedByParent(node)) {
            return;
        }
        TranslateTransition slide = new TranslateTransition(SWITCH, node);
        slide.setFromX(SWITCH_OFFSET);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(SWITCH, node);
        fade.setFromValue(SWITCH_FROM_OPACITY);
        fade.setToValue(1);

        ParallelTransition whole = new ParallelTransition(node, slide, fade);
        SWITCHING_IN.add(node);
        whole.setOnFinished(event -> {
            SWITCHING_IN.remove(node);
            node.setTranslateX(0);
            node.setOpacity(1);
        });
        whole.playFromStart();
    }

    /** @return whether an ancestor of this node is sliding in and taking the node with it. */
    private static boolean isCarriedByParent(Node node) {
        for (Node above = node.getParent(); above != null; above = above.getParent()) {
            if (SWITCHING_IN.contains(above)) {
                return true;
            }
        }
        return false;
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
