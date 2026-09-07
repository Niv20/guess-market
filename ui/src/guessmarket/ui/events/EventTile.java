package guessmarket.ui.events;

import guessmarket.dto.EventDto;
import guessmarket.dto.EventStatus;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tiles;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;

/**
 * Draws one event as a tile, for both of the lists of events the program shows.
 *
 * <p>The order the pieces are put in is the order they are meant to be read in. How the event is
 * doing comes first, because a person looking down a list is looking for the ones that are
 * running; the name is the largest thing on the tile, because that is what they are looking
 * <em>at</em>; who runs it and what it charges are underneath in a quieter colour, because they
 * explain the name rather than compete with it; and the money is last, set apart under a caption
 * of its own so a column of tiles can be read down the figures alone.
 *
 * <p>The number the file gave the event is put last of all in the top row and in the faintest
 * colour there is. It has to be there — it is how the event is referred to — but nobody scans a
 * list of events looking for a number.
 */
public final class EventTile {

    /**
     * What the person a list of events belongs to is to one of them.
     *
     * <p>The events screen lists events belonging to nobody in particular and uses
     * {@link #NONE} throughout; the users screen lists the same events as they stand to whoever
     * is selected, and a role is the one thing on the tile that is not in the event itself.
     */
    public enum Role {
        MARKET_MAKER,
        PARTICIPANT,
        NONE
    }

    private EventTile() {
    }

    /** @return the tile for an event listed on its own, without anybody's part in it. */
    public static Node of(EventDto event) {
        return of(event, Role.NONE);
    }

    /** @return the tile for an event, marked with what the selected user is to it. */
    public static Node of(EventDto event, Role role) {
        VBox tile = Tiles.tile();
        tile.getChildren().addAll(
                topRow(event, role, true),
                Tiles.title(event.name()),
                Tiles.meta(describeRun(event)),
                figures(event));
        return tile;
    }

    /**
     * @return the same event drawn small, for a strip read across rather than a column read down
     *
     * <p>A card in a strip is given its width and its height rather than taking what it needs, so
     * this one is the full tile with everything that can be said elsewhere left off it. What stays
     * is what somebody picking an event out of a row is picking by: how it is doing, what it is
     * called, what they are to it, and its money. What goes is how it charges — nobody chooses an
     * event by its commission, and the panel that opens underneath states it before anything can
     * be done about it.
     */
    public static Node compact(EventDto event, Role role) {
        VBox card = Tiles.stripTile();
        card.getChildren().addAll(
                topRow(event, role, false),
                Tiles.title(event.name()),
                Tiles.meta(describeMethodAndRunner(event)),
                Tiles.stripSpacer(),
                figures(event));
        return card;
    }

    /**
     * The badges that say how the event is doing, how it trades, and whose it is.
     *
     * @param withMethod whether how the event trades is worn as a badge here. A card in a strip
     *                   says it in words underneath instead: three badges and a number do not fit
     *                   across a card that narrow, and pushed onto a second line they take the
     *                   room the name needs.
     */
    private static HBox topRow(EventDto event, Role role, boolean withMethod) {
        FlowPane badges = Tiles.badges();
        badges.getChildren().add(
                Tiles.badge(shout(event.status().getDisplayName()), statusStyle(event.status())));
        if (withMethod) {
            badges.getChildren().add(
                    Tiles.badge(shout(event.tradingMethod().getDisplayName()), "badge-neutral"));
        }
        if (role != Role.NONE) {
            badges.getChildren().add(roleBadge(role));
        }

        Label id = new Label("#" + event.id());
        id.getStyleClass().add("tile-id");
        return Tiles.topRow(badges, id);
    }

    /**
     * The colour a status is worn in: the accent for the events that are running, because those
     * are the ones anything can still be done about; the outline of a loss for the ones that are
     * over; and nothing at all for the ones that have not begun.
     */
    private static String statusStyle(EventStatus status) {
        return switch (status) {
            case ACTIVE -> "badge-active";
            case CLOSED -> "badge-closed";
            case NOT_STARTED -> "badge-neutral";
        };
    }

    /** Running an event is worn in the colour of a warning: that user is answerable for it. */
    private static Label roleBadge(Role role) {
        return role == Role.MARKET_MAKER
                ? Tiles.badge("MARKET MAKER", "badge-mm")
                : Tiles.badge("TAKING PART", "badge-neutral");
    }

    /** @return who runs the event and what it charges, as one line of ordinary English. */
    private static String describeRun(EventDto event) {
        return "Run by " + event.marketMakerName() + " · "
                + Formats.percent(event.commissionPercent()) + " commission "
                + event.commissionType().getDisplayName().toLowerCase(Locale.US);
    }

    /** @return how the event trades and whose it is, all a card this small has room for. */
    private static String describeMethodAndRunner(EventDto event) {
        return shout(event.tradingMethod().getDisplayName())
                + " · Run by " + event.marketMakerName();
    }

    /**
     * The figures at the foot: what the event's own account holds, and, once it is over, which
     * option turned out to be right — the one thing about a closed event anybody asks first.
     */
    private static FlowPane figures(EventDto event) {
        FlowPane figures = Tiles.figures();
        figures.getChildren().add(
                Tiles.figure("ACCOUNT", Formats.money(event.accountBalance()), "value-accent"));
        if (event.isClosed() && event.winningOptionName() != null) {
            figures.getChildren().add(
                    Tiles.figure("WINNER", event.winningOptionName(), "value-positive"));
        }
        return figures;
    }

    /** A badge is a label rather than a sentence, so it is set in capitals like the captions. */
    private static String shout(String text) {
        return text.toUpperCase(Locale.US);
    }
}
