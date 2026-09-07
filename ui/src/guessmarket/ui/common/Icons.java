package guessmarket.ui.common;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

import java.util.Locale;

/**
 * The small drawings the interface puts beside a piece of text.
 *
 * <p>They are drawn rather than loaded from an image file, so that they stay sharp at any screen
 * scale and take their colour from the skin the window is wearing like everything else does.
 * Every call returns a new node, because one node cannot hang in two places at once.
 *
 * <p>All of them but the warning sign are outlines rather than solid shapes: a stroke of a
 * constant width and nothing inside it. That is what makes a row of them look like one family
 * instead of a handful of pictures collected from different places, and it is why the stroke
 * width, the round ends and the colour are stated once in the stylesheet, under
 * {@code .app-icon}, rather than by each drawing for itself.
 *
 * <p>Each drawing is described inside a square of {@value #CANVAS} units and is then scaled to
 * whatever size was asked for, so the same path serves a sixteen point icon and a twenty point
 * one, and the two are exactly the same drawing at two sizes.
 */
public final class Icons {

    /** The square every drawing below is described in, before it is scaled to the size asked for. */
    private static final double CANVAS = 24;

    /** Beside a word: inside a button whose text carries the meaning, or in front of a choice. */
    public static final double SMALL = 16;

    /** On its own: at the head of a row of settings, or as the whole of a button in the header. */
    public static final double ROW = 20;

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

    /** A cog: the toothed ring, and the hole in the middle of it. */
    private static final String[] GEAR = {
            "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08"
            + "a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51"
            + "a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08"
            + "a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18"
            + "a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39"
            + "a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09"
            + "a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25"
            + "a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
            circle(12, 12, 3)
    };

    /** An artist's palette: the outline with the thumb hole, and four blobs of paint on it. */
    private static final String[] PALETTE = {
            "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8"
            + "l.3.4a1.75 1.75 0 0 1-1.4 2.8z",
            circle(13.5, 6.5, 0.6),
            circle(17.5, 10.5, 0.6),
            circle(6.5, 12.5, 0.6),
            circle(8.5, 7.5, 0.6)
    };

    /** Three four pointed stars, one large and two small: something happening rather than being. */
    private static final String[] SPARKLES = {
            "M9.94 15.5A2 2 0 0 0 8.5 14.06l-6.14-1.58a.5.5 0 0 1 0-.96L8.5 9.94A2 2 0 0 0 9.94 8.5"
            + "l1.58-6.14a.5.5 0 0 1 .96 0L14.06 8.5A2 2 0 0 0 15.5 9.94l6.14 1.58a.5.5 0 0 1 0 .96"
            + "L15.5 14.06a2 2 0 0 0-1.44 1.44l-1.58 6.14a.5.5 0 0 1-.96 0z",
            "M20 3v4",
            "M22 5h-4",
            "M4 17v2",
            "M5 18H3"
    };

    /** A floppy disk: the case with its cut corner, the label below and the shutter above. */
    private static final String[] FLOPPY_DISK = {
            "M15.2 3a2 2 0 0 1 1.4.6l3.8 3.8a2 2 0 0 1 .6 1.4V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5"
            + "a2 2 0 0 1 2-2z",
            "M17 21v-7a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v7",
            "M7 3v4a1 1 0 0 0 1 1h7"
    };

    /** A folder with its front leaf tipped open, for going and fetching something. */
    private static final String[] OPEN_FOLDER = {
            "M6 14l1.5-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.54 6a2 2 0 0 1-1.95 1.5H4"
            + "a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18"
            + "a2 2 0 0 1 2 2v2"
    };

    /** A cross, for closing whatever it sits in the corner of. */
    private static final String[] CROSS = {
            "M18 6 6 18",
            "M6 6l12 12"
    };

    /** A crescent moon, for the dark skin. */
    private static final String[] MOON = {
            "M12 3a6.36 6.36 0 0 0 9 9 9 9 0 1 1-9-9z"
    };

    /** A sun with eight rays, for the light skin. */
    private static final String[] SUN = {
            circle(12, 12, 4),
            "M12 2v2",
            "M12 20v2",
            "M4.93 4.93l1.41 1.41",
            "M17.66 17.66l1.41 1.41",
            "M2 12h2",
            "M20 12h2",
            "M6.34 17.66l-1.41 1.41",
            "M19.07 4.93l-1.41 1.41"
    };

    /** A bolt of lightning, for the skin that is lit from within. */
    private static final String[] LIGHTNING = {
            "M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7"
            + "a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"
    };

    /** An arrow curling back on itself, for putting something back the way it was. */
    private static final String[] ARROW_BACK = {
            "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
            "M3 3v5h5"
    };

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

    /** Settings: everything about the window that the person, rather than the file, decides. */
    public static Node gear(double size) {
        return drawing(size, GEAR);
    }

    /** The skin: what the window is coloured and lettered in. */
    public static Node palette(double size) {
        return drawing(size, PALETTE);
    }

    /** The animations: the movements an action is accompanied by. */
    public static Node sparkles(double size) {
        return drawing(size, SPARKLES);
    }

    /** Writing the system as it stands to a file. */
    public static Node floppyDisk(double size) {
        return drawing(size, FLOPPY_DISK);
    }

    /** Reading a system back out of a file. */
    public static Node openFolder(double size) {
        return drawing(size, OPEN_FOLDER);
    }

    /** Closing something that was opened over the window. */
    public static Node cross(double size) {
        return drawing(size, CROSS);
    }

    /** The Midnight skin. */
    public static Node moon(double size) {
        return drawing(size, MOON);
    }

    /** The Daylight skin. */
    public static Node sun(double size) {
        return drawing(size, SUN);
    }

    /** The Neon skin. */
    public static Node lightning(double size) {
        return drawing(size, LIGHTNING);
    }

    /** Putting a set of choices back to the ones the program started with. */
    public static Node arrowBack(double size) {
        return drawing(size, ARROW_BACK);
    }

    /**
     * Builds one drawing out of the strokes it is made of.
     *
     * <p>The strokes are laid on a pane of exactly {@value #CANVAS} units square, which is then
     * scaled down as a whole. Pinning the square rather than letting the drawing decide its own
     * extent is what keeps a row of icons aligned: a moon does not fill its square and a sun very
     * nearly does, and without the square the two would be centred against each other and end up
     * at visibly different weights.
     *
     * <p>The scaled pane is then held in a box of the size that was asked for. Scaling does not
     * change how much room a node takes up in a layout, only how large it is drawn, so without
     * the box every icon would reserve the full twenty four points however small it appeared.
     */
    private static Node drawing(double size, String... strokes) {
        Pane square = new Pane();
        for (String stroke : strokes) {
            SVGPath path = new SVGPath();
            path.setContent(stroke);
            path.getStyleClass().add("app-icon");
            square.getChildren().add(path);
        }
        square.setMinSize(CANVAS, CANVAS);
        square.setPrefSize(CANVAS, CANVAS);
        square.setMaxSize(CANVAS, CANVAS);
        square.setScaleX(size / CANVAS);
        square.setScaleY(size / CANVAS);

        StackPane box = new StackPane(square);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.setMouseTransparent(true);
        return box;
    }

    /**
     * A circle, written the way a path has to write one: two half turns that meet again where
     * they began. Several of the drawings above need one, and none of them needs it badly enough
     * to be worth spelling out by hand.
     */
    private static String circle(double centreX, double centreY, double radius) {
        return String.format(Locale.US, "M%s %sa%s %s 0 1 0 %s 0a%s %s 0 1 0 -%s 0",
                centreX - radius, centreY, radius, radius, 2 * radius, radius, radius, 2 * radius);
    }
}
