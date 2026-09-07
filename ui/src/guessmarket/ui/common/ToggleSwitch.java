package guessmarket.ui.common;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * A switch: a setting that is either on or off, shown as the thing itself rather than as a button
 * carrying the word for it.
 *
 * <p>A button that reads "On" is asking to be read twice - once to see what it says, and once to
 * work out whether that is the state it is in or the state it would put you in if you pressed it.
 * A switch answers both at once and without words: it is over to one side or the other, and the
 * side it is on is coloured in the accent of the skin when the answer is yes. Nothing about it
 * has to be translated, so nothing about it has to be read.
 *
 * <p>It is a {@link ToggleButton} underneath, so it is clicked, focused and worked by the space
 * bar like any other control, and anything already watching a {@code selectedProperty} goes on
 * watching the same one. Only the drawing of it is different: the button carries no text and no
 * background of its own, and what is seen is the track it hands to its graphic, with the knob
 * inside it.
 *
 * <p>The knob travels rather than jumps, so the eye can follow it from one end to the other,
 * unless the person has asked for a window that does not move - see {@link Animations}. That
 * makes the animations switch itself the one control that demonstrates what it has just been
 * asked for: turned on it glides, turned off it is simply on the other side.
 *
 * <p>It is final because its constructor builds the track and the knob and hands them to itself.
 * A subclass could be handed a half-built switch, and there is nothing here worth inheriting.
 */
public final class ToggleSwitch extends ToggleButton {

    /** How long the knob takes to cross the track, well under {@link Animations#LONGEST}. */
    private static final Duration GLIDE = Duration.millis(160);

    /** The pill the knob sits in, and the whole of what is seen. */
    private final StackPane track = new StackPane();

    /** The knob, at one end of the track or the other, and never anywhere in between at rest. */
    private final Region knob = new Region();

    private final TranslateTransition glide = new TranslateTransition(GLIDE, knob);

    public ToggleSwitch() {
        // A toggle button wears .toggle-button, which in this program is a bordered rectangle with
        // a word in it - the very thing a switch exists in order not to be. The class is replaced
        // rather than added to, so that nothing any skin says about toggle buttons reaches here.
        getStyleClass().setAll("toggle-switch");
        track.getStyleClass().add("toggle-switch-track");
        knob.getStyleClass().add("toggle-switch-knob");

        // The knob is laid out at the left end and moved from there, so that its resting place is
        // a distance from one end rather than a position that has to be worked out from both.
        //
        // Left to itself a stack pane fills itself with whatever it is holding, and a region will
        // grow as far as it is asked to, so the knob would arrive as wide as the track it is meant
        // to travel along. It is held to the size the stylesheet asks for instead, which keeps the
        // size of it stated in one place rather than being half here and half there.
        knob.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        track.setAlignment(Pos.CENTER_LEFT);
        track.getChildren().add(knob);
        setGraphic(track);

        glide.setInterpolator(Interpolator.EASE_BOTH);

        // The word this control does without is still what a screen reader has to say out loud.
        accessibleTextProperty().bind(
                Bindings.when(selectedProperty()).then("On").otherwise("Off"));

        selectedProperty().addListener((observable, previous, on) -> settle(true));

        // The track and the knob are measured in multiples of the size of the writing, so both of
        // them change size when the settings do, and the far end of the track moves with them.
        track.widthProperty().addListener((observable, previous, width) -> settle(false));
        knob.widthProperty().addListener((observable, previous, width) -> settle(false));
    }

    /**
     * Puts the knob where the answer now is: hard against the left end for off, hard against the
     * right end for on.
     *
     * @param flicked whether the answer itself has just changed, which is the only occasion the
     *                knob is allowed to travel. When the control has merely been measured again
     *                it is already showing the right answer and only has to keep showing it, and
     *                a knob that slid every time the window were resized would say that something
     *                had been switched when nothing had.
     */
    private void settle(boolean flicked) {
        double resting = isSelected() ? travel() : 0;
        glide.stop();
        if (!flicked || !Animations.isEnabled()) {
            knob.setTranslateX(resting);
            return;
        }
        glide.setFromX(knob.getTranslateX());
        glide.setToX(resting);
        glide.playFromStart();
    }

    /**
     * How far the knob has to go to cross from one end of the track to the other: the width of the
     * track, less what the border and the padding of the track take up, less the knob itself.
     */
    private double travel() {
        Insets inside = track.getInsets();
        double room = track.getWidth() - inside.getLeft() - inside.getRight() - knob.getWidth();
        return Math.max(0, room);
    }
}
