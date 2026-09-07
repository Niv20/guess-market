package guessmarket.ui.users;

import guessmarket.dto.UserDto;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tiles;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

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
 * <p>The two things that change what a user may do are worn as badges above the name: running an
 * event, which makes them answerable for it, and being blocked, which ends everything. A blocked
 * user's whole tile is faded, because nothing on it can move again.
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

        FlowPane badges = badges(user);
        if (!badges.getChildren().isEmpty()) {
            tile.getChildren().add(badges);
        }
        tile.getChildren().add(
                Tiles.body(Tiles.title(user.name()),
                        Tiles.meta(describeEvents(user)),
                        figures(user)));
        return tile;
    }

    /**
     * The badges above the name, and nothing when there are none: an ordinary user who is still
     * free to act is the usual case, and the usual case is worth no badge at all.
     */
    private static FlowPane badges(UserDto user) {
        FlowPane badges = Tiles.badges();
        if (user.isMarketMaker()) {
            badges.getChildren().add(Tiles.badge("MARKET MAKER", "badge-mm"));
        }
        if (user.blocked()) {
            badges.getChildren().add(Tiles.badge("BLOCKED", "badge-closed"));
        }
        return badges;
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
