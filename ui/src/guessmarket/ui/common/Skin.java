package guessmarket.ui.common;

import javafx.scene.Node;
import javafx.scene.Scene;

import java.net.URL;
import java.util.Objects;
import java.util.function.DoubleFunction;

/**
 * The colour schemes the window can be shown in.
 *
 * <p>Every skin is one stylesheet that is laid on top of the shared one. The shared stylesheet
 * holds the things that never change between skins, such as spacing, corner radii and which
 * controls sit inside which panel; the skin holds the background of the window, the look of the
 * buttons and the font of the labels.
 *
 * <p>The font is the one part of a skin that is not entirely in its stylesheet. The family is,
 * but the size is named here, because the person may put a multiplier on top of it from the
 * settings and the two have to be multiplied together somewhere. Stating it once here rather
 * than in the stylesheet as well is what keeps the two from ever disagreeing. See
 * {@link TextSize}.
 *
 * <p>{@link #MIDNIGHT} is the one the program starts in, so the two extra schemes are only ever
 * seen once they are chosen deliberately.
 */
public enum Skin {

    MIDNIGHT("Midnight", "/guessmarket/ui/css/skin-midnight.css", 13, Icons::moon),
    DAYLIGHT("Daylight", "/guessmarket/ui/css/skin-daylight.css", 14.5, Icons::sun),
    NEON("Neon", "/guessmarket/ui/css/skin-neon.css", 15, Icons::lightning);

    /** Everything that is common to all skins, always applied underneath the chosen one. */
    private static final String BASE_STYLESHEET = "/guessmarket/ui/css/base.css";

    /** The skin the program opens with, so that the other two count as switched on by hand. */
    public static final Skin DEFAULT = MIDNIGHT;

    private final String displayName;
    private final String stylesheet;
    private final double baseTextSize;
    private final DoubleFunction<Node> icon;

    Skin(String displayName, String stylesheet, double baseTextSize, DoubleFunction<Node> icon) {
        this.displayName = displayName;
        this.stylesheet = stylesheet;
        this.baseTextSize = baseTextSize;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * A drawing of what this skin is like, for a chooser that offers all three at once: night,
     * day, and a current running through it.
     *
     * @param size how many points across the drawing should be.
     */
    public Node icon(double size) {
        return icon.apply(size);
    }

    /** Dresses a scene in the default skin. */
    public static void applyDefault(Scene scene) {
        DEFAULT.applyTo(scene);
    }

    /** Replaces whatever skin the scene is wearing with this one, keeping the shared stylesheet. */
    public void applyTo(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        scene.getStylesheets().setAll(locate(BASE_STYLESHEET), locate(stylesheet));
        applyTextSizeTo(scene);
    }

    /**
     * States the size of the writing again without touching the stylesheets, for when the person
     * has moved the slider rather than changed the skin. Reloading two stylesheets for every step
     * of a slider being dragged is a great deal of work to arrive at the same two stylesheets.
     */
    public void applyTextSizeTo(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        TextSize.applyTo(scene, baseTextSize);
    }

    @Override
    public String toString() {
        return displayName;
    }

    private static String locate(String path) {
        URL url = Skin.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("The stylesheet \"" + path
                    + "\" is missing from the program. The program was not packaged correctly.");
        }
        return url.toExternalForm();
    }
}
