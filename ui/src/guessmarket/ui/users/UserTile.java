package guessmarket.ui.users;

import guessmarket.dto.UserDto;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tiles;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws one user as a tile for the list on the users screen.
 *
 * <p>A user is read as a name and an amount of money, and the tile is built around exactly that:
 * the name is the largest thing on it, and the balance is set apart from it in the accent colour,
 * because it is the figure every other screen in the program eventually comes back to. What the
 * user has made or lost stands beside it in green or in red — the same figure the balance came
 * from, so it is set in the same size and told apart by colour alone. Set apart means under the
 * name on a narrow tile and out along the right on a wide one, which is a list of people that can
 * be read down the money alone.
 *
 * <p>The two things that change what a user may do — running an event, which makes them
 * answerable for it, and being blocked, which ends everything — are said after the name in two or
 * three letters rather than worn as badges above it. A badge only some of the people wear leaves
 * the list two tile heights, and a list of people whose rows do not line up is read as two kinds
 * of person; said after the name they cost the tile no height at all. A blocked user's whole tile
 * is faded as well, because nothing on it can move again.
 */
public final class UserTile {

    private UserTile() {
    }

    /** @return the tile for one user. */
    public static Node of(UserDto user) {
        VBox tile = Tiles.tile();
        if (user.blocked()) {
            tile.getStyleClass().add("tile-inactive");
        }
        tile.getChildren().add(
                Tiles.body(nameRow(user),
                        Tiles.meta(describeEvents(user)),
                        figures(user)));
        return tile;
    }

    /**
     * The name, and after it what this person is: {@code MM} for somebody who runs an event of
     * their own, shortened because it is read beside a name rather than instead of one, and
     * {@code Blocked} for somebody who can do nothing further. Both, for a market maker who ran
     * out of money. Neither is the usual case, and the usual case is a name and nothing else.
     */
    private static Node nameRow(UserDto user) {
        List<Label> tags = new ArrayList<>();
        if (!user.marketMakerEventIds().isEmpty()) {
            tags.add(Tiles.tag("MM", "tile-tag-mm"));
        }
        if (user.blocked()) {
            tags.add(Tiles.tag("Blocked", "tile-tag-blocked"));
        }
        return Tiles.titleRow(Tiles.title(user.name()), tags);
    }

    /** @return where in the system this user is to be found, as one line of ordinary English. */
    private static String describeEvents(UserDto user) {
        int runs = user.marketMakerEventIds().size();
        int partOf = user.participatingEventIds().size();
        if (runs == 0 && partOf == 0) {
            return "Not in any event yet";
        }
        if (runs == 0) {
            return "Takes part in " + Formats.count(partOf, "event");
        }
        if (partOf == 0) {
            return "Runs " + Formats.count(runs, "event");
        }
        return "Runs " + Formats.count(runs, "event") + " · takes part in " + partOf;
    }

    /**
     * The money at the foot: what the account holds now, and how far that is from what the file
     * said it held to begin with.
     */
    private static FlowPane figures(UserDto user) {
        FlowPane figures = Tiles.figures();
        figures.getChildren().add(
                Tiles.figure("BALANCE", Formats.money(user.balance()), "value-accent"));
        figures.getChildren().add(
                Tiles.figure("RESULT", Formats.signedMoney(user.netResult()),
                        Formats.resultStyle(user.netResult())));
        return figures;
    }
}
