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
import javafx.geometry.HorizontalDirection;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
 * <p>Two of them answer something new arriving on the screen, and which one is used says what kind
 * of arrival it was. A panel that appears where nothing was fades in, {@link #switchIn}: it is
 * already where it belongs, so it has no reason to travel there, and a movement that only changes
 * the strength of what is on the screen stays out of the way of somebody reading it. A screen that
 * takes the place of another one fades in as well, {@link #slideIn}, but leaning a little way in
 * from the side the person came from, because there the two screens sit beside each other in a
 * fixed order and the lean is what says which way they went.
 *
 * <p>Neither of them is repeated for a panel that is merely being refilled. Choosing a second
 * event after a first one is not an arrival — the panel is already on the screen and only its
 * contents change — and animating that would put a movement between the person and every single
 * thing they click on.
 */
public final class Animations {

    /** No animation in the program may last longer than this. */
    public static final Duration LONGEST = Duration.seconds(2);

    private static final Duration SWITCH = Duration.millis(320);
    private static final Duration SLIDE = Duration.millis(360);
    private static final Duration FLASH_HALF = Duration.millis(180);
    private static final Duration FADE_OUT = Duration.millis(420);

    /** How faint a panel begins before it comes up to full strength. */
    private static final double SWITCH_FROM_OPACITY = 0;

    /**
     * How far a screen leans out of its place before it settles into it, in pixels.
     *
     * <p>Small on purpose, and fixed rather than measured from the width of the window: this is a
     * hint about which way the person went and not a journey across the screen, and it should read
     * the same in a small window as in a large one.
     */
    private static final double SLIDE_LEAN = 56;

    /** How faint a screen begins before it leans into place. Not nothing, so it is never a blink. */
    private static final double SLIDE_FROM_OPACITY = 0.15;

    /**
     * Whether the animations are on when the program starts. They are not: an interface that has
     * only just appeared should show what it has to show, and a movement is something the person
     * asks for once they have seen it standing still. It is named here rather than written into
     * the property below because the settings sheet offers to put everything back the way it
     * started, and has to know what that was.
     */
    public static final boolean ENABLED_AT_START = false;

    private static final BooleanProperty ENABLED = new SimpleBooleanProperty(ENABLED_AT_START);

    /** The panels that are fading in at this moment, so that a panel inside one of them can be left alone. */
    private static final Set<Node> SWITCHING_IN = new HashSet<>();

    /** The screens that are sliding in at this moment, so that one can be called off part way. */
    private static final Map<Node, Animation> SLIDING = new HashMap<>();

    private Animations() {
    }

    /** The switch the settings sheet binds to. */
    public static BooleanProperty enabledProperty() {
        return ENABLED;
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    /**
     * Brings a panel up from nothing to full strength, without moving it, for a panel that has just
     * been filled with something other than what it held a moment ago.
     *
     * <p>It is meant for the first time a panel appears and not for every subject after that. The
     * side of the screen a panel occupies is empty until something is chosen on the other side, so
     * the first choice really does put something on the screen where there was nothing, and the
     * fade is what says so. The second choice does not: the panel is already there and the person
     * is now reading it, and a fade before every event they look at would be a delay rather than
     * an answer.
     *
     * <p>A panel inside a panel that is already fading in is left alone. It is being carried along
     * by its parent, whose strength its own is measured against, so fading it as well would only
     * make it arrive twice as slowly as everything around it.
     */
    public static void switchIn(Node node) {
        if (node == null || !isEnabled() || isCarriedByParent(node)) {
            return;
        }
        FadeTransition fade = new FadeTransition(SWITCH, node);
        fade.setFromValue(SWITCH_FROM_OPACITY);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        SWITCHING_IN.add(node);
        fade.setOnFinished(event -> {
            SWITCHING_IN.remove(node);
            node.setOpacity(1);
        });
        fade.playFromStart();
    }

    /**
     * Brings a screen in from a little way off to one side, fading it up as it comes, for a screen
     * that has just taken the place of another one.
     *
     * <p>This is what the tabs use. The screen leans the way the person moved along them: going to
     * the tab on the right it starts a little to the left and settles rightwards, and going back
     * to the tab on the left it starts a little to the right and settles leftwards.
     *
     * <p>It only ever moves {@link #SLIDE_LEAN}, which is a small step and nowhere near the width
     * of the screen. A whole screen thrown across the window is a journey, and there is nothing to
     * be learned from watching it that the first moment of it has not already said; a short lean
     * says the same thing and then gets out of the way. The fade is what actually carries the
     * screen in, and the movement only points, which is why the two run together and end together
     * rather than the panel arriving and then filling in.
     *
     * <p>The screen that is leaving is not animated. A tab pane takes it off the screen the moment
     * the choice is made and there is no honest way to hold it there afterwards, so the arrival is
     * animated alone rather than reaching around the control to fake a departure.
     *
     * @param travel the direction the screen leans in as it comes to rest, so {@code RIGHT} starts
     *               a little to the left and {@code LEFT} a little to the right.
     */
    public static void slideIn(Node node, HorizontalDirection travel) {
        if (node == null || !isEnabled()) {
            return;
        }
        // A screen the person is clicking through quickly is still on its way in when it is asked
        // to leave, and the movement it was making has to be called off before another one starts
        // on the same node, or the two fight over where it sits and it lands anywhere but home.
        stopSliding(node);

        // Eased at both ends rather than only at the finish: the screen is meant to gather itself
        // and settle again, and a movement that starts at full speed reads as a jump however
        // gently it lands.
        TranslateTransition lean = new TranslateTransition(SLIDE, node);
        lean.setFromX(travel == HorizontalDirection.RIGHT ? -SLIDE_LEAN : SLIDE_LEAN);
        lean.setToX(0);
        lean.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition fade = new FadeTransition(SLIDE, node);
        fade.setFromValue(SLIDE_FROM_OPACITY);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition arrival = new ParallelTransition(node, lean, fade);

        SLIDING.put(node, arrival);
        arrival.setOnFinished(event -> {
            SLIDING.remove(node);
            settle(node);
        });
        arrival.playFromStart();
    }

    /** Calls off a slide this node is in the middle of and puts it back where it belongs. */
    private static void stopSliding(Node node) {
        Animation running = SLIDING.remove(node);
        if (running != null) {
            running.stop();
            settle(node);
        }
    }

    /** Puts a screen back exactly where it belongs, at full strength, however it got there. */
    private static void settle(Node node) {
        node.setTranslateX(0);
        node.setOpacity(1);
    }

    /** @return whether an ancestor of this node is fading in and taking the node with it. */
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
