package guessmarket.ui.common;

import javafx.scene.Node;
import javafx.scene.Scene;

import java.net.URL;
import java.util.Locale;
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
 * but the size is named here, because a skin that changes the typeface has to be free to change
 * the size that typeface is comfortable at, and the whole interface is measured against that one
 * number. It is written onto the root of the scene rather than into a stylesheet, because the
 * whole window inherits its font from that one node: nothing else has to be told, since a label
 * two panels down is already stated in multiples of it.
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
    private final double textSize;
    private final DoubleFunction<Node> icon;

    Skin(String displayName, String stylesheet, double textSize, DoubleFunction<Node> icon) {
        this.displayName = displayName;
        this.stylesheet = stylesheet;
        this.textSize = textSize;
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
     * Tells the scene how large its ordinary text is, in points, so that everything measured
     * against it follows. A style written onto a node beats every stylesheet, which is what keeps
     * this the one number the interface is laid out against.
     */
    private void applyTextSizeTo(Scene scene) {
        if (scene.getRoot() == null) {
            return;
        }
        scene.getRoot().setStyle(String.format(Locale.US, "-fx-font-size: %.2fpx;", textSize));
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
