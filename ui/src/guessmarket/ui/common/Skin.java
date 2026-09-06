package guessmarket.ui.common;

import javafx.scene.Scene;

import java.net.URL;
import java.util.Objects;

/**
 * The colour schemes the window can be shown in.
 *
 * <p>Every skin is one stylesheet that is laid on top of the shared one. The shared stylesheet
 * holds the things that never change between skins, such as spacing, corner radii and which
 * controls sit inside which panel; the skin holds the background of the window, the look of the
 * buttons and the font family and size of the labels.
 *
 * <p>{@link #MIDNIGHT} is the one the program starts in, so the two extra schemes are only ever
 * seen once they are chosen deliberately.
 */
public enum Skin {

    MIDNIGHT("Midnight", "/guessmarket/ui/css/skin-midnight.css"),
    DAYLIGHT("Daylight", "/guessmarket/ui/css/skin-daylight.css"),
    NEON("Neon", "/guessmarket/ui/css/skin-neon.css");

    /** Everything that is common to all skins, always applied underneath the chosen one. */
    private static final String BASE_STYLESHEET = "/guessmarket/ui/css/base.css";

    /** The skin the program opens with, so that the other two count as switched on by hand. */
    public static final Skin DEFAULT = MIDNIGHT;

    private final String displayName;
    private final String stylesheet;

    Skin(String displayName, String stylesheet) {
        this.displayName = displayName;
        this.stylesheet = stylesheet;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Dresses a scene in the default skin. */
    public static void applyDefault(Scene scene) {
        DEFAULT.applyTo(scene);
    }

    /** Replaces whatever skin the scene is wearing with this one, keeping the shared stylesheet. */
    public void applyTo(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        scene.getStylesheets().setAll(locate(BASE_STYLESHEET), locate(stylesheet));
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
