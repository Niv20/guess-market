package guessmarket.ui.events;

import guessmarket.dto.EventDto;
import guessmarket.dto.EventStatus;
import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tiles;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Locale;

/**
 * Draws one event as a tile, for both of the lists of events the program shows.
 *
 * <p>How the event is doing comes first and is a coloured dot rather than a word, and it stands
 * in a narrow column of its own down the left of everything else: it is what a list is scanned
 * for, and one colour at one distance from the edge is found in a single pass where a word has to
 * be read row by row. The colours are a set of traffic lights - red waiting, amber running, green
 * finished - which is the one arrangement of three colours that needs no key beside it. Everything else is on one line with the name or underneath it. The number
 * the file gave the event leads that line, quiet, in front of the name rather than off in a
 * corner of its own — it is how the event is referred to everywhere else in the program, so it
 * belongs where the name is looked at. Then the name, which is the largest thing on the tile
 * because it is what the person is looking at. Underneath it, in a quieter colour, is everything
 * that explains the name: how the event trades, who runs it and what it charges. The money is
 * last, set apart under a caption of its own so that a column of tiles can be read down the
 * figures alone — under the words on a narrow tile, and out to the right of them on a tile with
 * the width for a column, which is the same order and a straighter column to read down.
 *
 * <p>Nothing here is worn as a badge. A badge is a word in a box, and a box is as tall as the
 * line it is set on: a row of them above the name is a row of the tile that only some of the
 * tiles have, and a list whose rows are two different heights is read as two kinds of thing
 * rather than as one list. A dot costs no height, a number costs none, and what the event trades
 * by is a fact about the event and reads as one in the line that holds the rest of them.
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

    /**
     * @return the tile for an event listed on its own, without anybody's part in it
     *
     * <p>The events screen lists the whole system and nobody in particular is chosen there, so
     * there is no such thing as this reader's part in an event: every tile says what the event
     * is and none of them says whose it is to anybody. {@link #compact} is the tile for the list
     * that does know.
     */
    public static Node of(EventDto event) {
        VBox tile = Tiles.tile();
        VBox words = Tiles.words(nameRow(event), Tiles.meta(describeRun(event)));
        tile.getChildren().add(
                Tiles.withMark(statusDot(event), words, Tiles.body(words, figures(event))));
        return tile;
    }

    /**
     * @return the same event drawn small, for a strip read across rather than a column read down
     *
     * <p>This is the head of the tile and nothing else: how the event is doing, which one it is,
     * what it is called, how it trades, whose it is and what the reader is to it. Everything a
     * card in this strip is for is in those two lines, because the strip is not a list to be read
     * - it is a row of things to pick one of, and what is picked opens underneath it.
     *
     * <p>Which is why the money is not here. It was the foot of the card and it was the event's
     * own account, a figure nobody chooses an event by; and the panel that opens the moment a card
     * is picked states it in full, under a caption, a few inches below the card that had just
     * whispered it. A card in a row of cards should carry what tells one of them from the next,
     * and nothing that the thing it opens will say again anyway.
     *
     * <p>The one thing this card says that the event itself does not is what the reader is to it,
     * and it is not said after the name: on a card this narrow a tag there takes that width out of
     * the name, and a name cut off in the middle is the one thing a card somebody is choosing from
     * cannot afford. It goes on the line underneath instead, which is shorter than the card either
     * way - running the event inside the sentence that names the runner, and having money in it
     * held out at the far end. See {@link #runnerRow}.
     */
    public static Node compact(EventDto event, Role role) {
        VBox card = Tiles.stripTile();
        VBox words = Tiles.words(nameRow(event), runnerRow(event, role));
        card.getChildren().add(Tiles.withMark(statusDot(event), words, words));
        return card;
    }

    /** The line the tile is found by: which event it is, and what it is called. */
    private static Node nameRow(EventDto event) {
        List<Node> before = List.of(Tiles.number("#" + event.id()));
        return Tiles.titleRow(before, Tiles.title(event.name()), List.of());
    }

    /**
     * The dot that says how the event is doing, as a set of traffic lights: red for one that has
     * not been opened, in which nothing at all can happen yet; amber for one that is running;
     * green for one that is over and has paid everybody out.
     *
     * <p>Three colours nobody has to be taught, which is the whole reason for choosing them - a
     * list is scanned for its marks before a word of it is read, and a reader who has to remember
     * what the colours mean is reading it after all. What they mean here is what they mean at a
     * junction: stop, in motion, go.
     *
     * <p>The amber one beats, when the animations are on. The other two are states the event is
     * resting in and this one is not — an open event is taking trades while it is being looked
     * at — and a mark that is moving is the difference between saying so and saying it was open
     * at the moment the list was drawn.
     */
    private static Node statusDot(EventDto event) {
        Node dot = Tiles.statusDot(dotStyle(event.status()), event.status().getDisplayName());
        if (event.status() == EventStatus.ACTIVE) {
            Animations.pulse(dot);
        }
        return dot;
    }

    private static String dotStyle(EventStatus status) {
        return switch (status) {
            case NOT_STARTED -> "status-dot-idle";
            case ACTIVE -> "status-dot-live";
            case CLOSED -> "status-dot-over";
        };
    }

    /**
     * The line under the name of a card in the strip: how the event trades and who runs it, with
     * whatever the reader is to it said where it belongs.
     *
     * <p>Whose the event is is the one thing on this line the reader is looking for, and until now
     * it was answered twice over and in two different places: the runner was named on the left and
     * a tag at the far right of the same line said MM, meaning not that the runner was the market
     * maker — they could hardly be anything else — but that the runner was the reader. Two claims
     * that far apart on one line are read as being about the two names nearest them, which is
     * precisely the wrong way round.
     *
     * <p>So it is said once, where the runner is named: the reader's own events say
     * <em>Run by you</em>, with {@code (MM)} after it in the colour a market maker is worn in
     * everywhere else in the program, because being answerable for an event is worth marking and
     * the word "you" alone is not. Every other event names whoever runs it and stops there.
     */
    private static Node runnerRow(EventDto event, Role role) {
        boolean mine = role == Role.MARKET_MAKER;
        Text lead = Tiles.metaPiece(event.tradingMethod().getDisplayName() + " · Run by "
                + (mine ? "you" : event.marketMakerName()));
        Node line = mine
                ? Tiles.metaLine(lead, Tiles.metaPiece(" (MM)", "mark-mm"))
                : Tiles.metaLine(lead);
        return Tiles.metaRow(line, participationTag(role));
    }

    /**
     * Having money in an event, held out at the far end of the line under the name.
     *
     * <p>It stays a tag of its own where running the event has become part of the sentence,
     * because it is a different kind of claim: what the reader is answerable for belongs beside
     * the runner's name, and what the reader stands to win or lose belongs where the other cards'
     * tags are, down one edge of the strip, so that somebody looking for their own money finds it
     * in one pass instead of a card at a time. It is in the accent, which is the colour every
     * figure they stand to win or lose is already set in.
     *
     * <p>Neither claim is made about the usual card. The users screen offers every event there is,
     * so most of the strip is events this person has nothing to do with, and a list where the
     * usual case wears a label is a list of labels.
     */
    private static List<Label> participationTag(Role role) {
        return role == Role.PARTICIPANT
                ? List.of(Tiles.tag("Taking part", "tile-tag-part"))
                : List.of();
    }

    /** @return how the event trades, who runs it and what it charges, as one line of English. */
    private static String describeRun(EventDto event) {
        return event.tradingMethod().getDisplayName()
                + " · Run by " + event.marketMakerName() + " · "
                + Formats.percent(event.commissionPercent()) + " commission "
                + event.commissionType().getDisplayName().toLowerCase(Locale.US);
    }

    /**
     * The one figure at the foot, which is a different figure once the event is over.
     *
     * <p>While the event is running, what its own account holds: the money behind every share
     * anybody could still buy in it, and the only figure on the tile that moves. Once it is over
     * that account is a residue - the winners have been paid and the rest has gone back to the
     * market maker - and the question anybody asks about a closed event is not how much is left
     * in it but which option turned out to be right.
     *
     * <p>One or the other, and never both. They were both shown, and a closed tile therefore
     * ended in two figures of which the first was the one nobody had asked for: the eye reads a
     * row of figures from the left, so the account was answering a question about a finished
     * event ahead of the answer to the question the event was for.
     */
    private static FlowPane figures(EventDto event) {
        FlowPane figures = Tiles.figures();
        if (event.isClosed() && event.winningOptionName() != null) {
            figures.getChildren().add(
                    Tiles.figure("WINNER", event.winningOptionName(), "value-positive"));
        } else {
            figures.getChildren().add(
                    Tiles.figure("ACCOUNT", Formats.money(event.accountBalance()), "value-accent"));
        }
        return figures;
    }
}
