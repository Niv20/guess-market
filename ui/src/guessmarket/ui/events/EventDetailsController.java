package guessmarket.ui.events;

import guessmarket.dto.EventDto;
import guessmarket.dto.EventStatus;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.HoldingDto;
import guessmarket.dto.MarketTradeDto;
import guessmarket.dto.OptionStateDto;
import guessmarket.dto.ParticipantDto;
import guessmarket.dto.PricePointDto;
import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tables;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything there is to see about one event, and nothing that acts on it.
 *
 * <p>The component is handed a complete picture of an event and shows it. It never asks the engine
 * anything and it never changes anything, which is what lets both screens use it: the events screen
 * shows it on its own, and the users screen shows the very same component underneath the controls
 * of the user who is acting, so that an action and its effect are visible in one place.
 *
 * <p>Which half of the layout is shown depends on how the event is traded. An LMSR event has one
 * value per option; an order book has no single value at all and shows its two books instead.
 */
public class EventDetailsController {

    /** The event id kept while the panel is empty, which no real event can have. */
    private static final int NOTHING_SHOWN = -1;

    /**
     * The narrowest a term box may be drawn before it is better to stack the row than to squeeze
     * it, which is about what its longest caption - {@code OPENING INVESTMENT} - needs.
     */
    private static final double NARROWEST_TERM = 152;

    @FXML private VBox rootPane;

    @FXML private Label eventNameLabel;
    @FXML private Label statusBadge;
    @FXML private Button methodButton;
    @FXML private Label winnerBadge;
    @FXML private Label descriptionLabel;

    @FXML private Label marketMakerLabel;
    @FXML private Label accountBalanceLabel;
    @FXML private Label commissionLabel;
    @FXML private Label commissionWhenLabel;
    @FXML private Label commissionCollectedLabel;
    @FXML private Label liquidityLabel;
    @FXML private Label subsidyLabel;
    @FXML private Label subsidyPaidLabel;
    @FXML private Label baseValueLabel;
    @FXML private Label investmentLabel;
    @FXML private Label investmentPairsLabel;
    @FXML private Label mintLabel;

    @FXML private GridPane termsGrid;
    @FXML private VBox commissionTile;
    @FXML private VBox liquidityTile;
    @FXML private VBox subsidyTile;
    @FXML private VBox baseValueTile;
    @FXML private VBox investmentTile;
    @FXML private VBox mintTile;

    /** Every term box there is, in the order they are laid out when the method calls for them. */
    private final List<VBox> termTiles = new ArrayList<>();

    /**
     * What the term boxes are currently laid out as, so that they are only moved when they would
     * actually land somewhere else.
     *
     * <p>The number of columns is not enough to tell one layout from another: three terms and four
     * terms both come to two columns once the panel is narrow, and stopping there would leave the
     * boxes of an LMSR event sitting where an order book's had been.
     */
    private int termColumns;
    private List<VBox> laidOutTerms = List.of();

    @FXML private VBox lmsrBox;
    @FXML private TableView<OptionStateDto> optionsTable;
    @FXML private VBox booksBox;
    @FXML private OptionBookController firstBookController;
    @FXML private OptionBookController secondBookController;

    @FXML private VBox participantsBox;
    @FXML private TableView<ParticipantDto> participantsTable;
    @FXML private VBox historyBox;
    @FXML private TableView<MarketTradeDto> historyTable;
    @FXML private VBox chartBox;
    @FXML private LineChart<Number, Number> priceChart;

    /** The option names the participants table was last built for, so it is rebuilt only when needed. */
    private List<String> participantColumnsFor = List.of();

    /**
     * The last event this panel was filled with, which is what tells a move to another event apart
     * from the same event being shown again.
     *
     * <p>It survives {@link #clear()} on purpose. Every refresh rebuilds the list beside the panel,
     * which drops the selection and empties the panel before putting the same event straight back;
     * forgetting here would make every purchase look like a move to another event and leave the
     * panel fading in and out while somebody is trying to trade in it.
     */
    private int shownEventId = NOTHING_SHOWN;

    /** The event account balance the panel last showed, so a balance that has moved can be marked. */
    private double shownAccountBalance;

    @FXML
    private void initialize() {
        termTiles.addAll(List.of(commissionTile, liquidityTile, subsidyTile,
                baseValueTile, investmentTile, mintTile));
        // The width changes during a layout pass, and moving the boxes there would leave the
        // panels above and below them already measured for the height the row used to have. So the
        // move waits for the pass to finish, and the whole card is measured again with it.
        termsGrid.widthProperty()
                .addListener((observable, was, now) -> Platform.runLater(this::layOutTerms));
        buildOptionsTable();
        buildHistoryTable();
        // The participants table has its columns rebuilt for every event, so it is the one table
        // asked here rather than where its columns are made.
        Tables.fit(participantsTable);
        Tables.emptyMessage(participantsTable, "Nobody has taken part in this event yet.");
        Tables.emptyMessage(historyTable, "Nothing has been traded in this event yet.");
        Tables.emptyMessage(optionsTable, "This event has no options.");
        clear();
    }

    /** @return the whole component, so that whoever placed it can show or hide it. */
    public Node getRoot() {
        return rootPane;
    }

    /** Empties the component, for when nothing is selected. */
    public void clear() {
        rootPane.setVisible(false);
        rootPane.setManaged(false);
    }

    /** Fills the component with one event and shows it. */
    public void show(EventTradingStatusDto status, List<PricePointDto> priceHistory) {
        EventDto event = status.event();
        boolean anotherEvent = event.id() != shownEventId;
        boolean accountMoved = !anotherEvent && event.accountBalance() != shownAccountBalance;
        shownEventId = event.id();
        shownAccountBalance = event.accountBalance();

        showIdentity(event);
        showStatistics(event, status);

        boolean lmsr = event.isLmsr();
        showOnly(lmsrBox, lmsr);
        showOnly(booksBox, !lmsr);
        if (lmsr) {
            optionsTable.setItems(FXCollections.observableArrayList(status.optionStates()));
        } else {
            firstBookController.show(status.books().get(0));
            secondBookController.show(status.books().get(1));
        }

        showParticipants(event, status.participants());
        historyTable.setItems(FXCollections.observableArrayList(status.tradeHistory()));
        showPriceChart(event, priceHistory);

        rootPane.setVisible(true);
        rootPane.setManaged(true);
        if (anotherEvent) {
            Animations.switchIn(rootPane);
        } else if (accountMoved) {
            Animations.flash(accountBalanceLabel);
        }
    }

    // ------------------------------------------------------------------ the parts of the picture

    private void showIdentity(EventDto event) {
        eventNameLabel.setText(event.name());
        descriptionLabel.setText(event.description());
        statusBadge.setText(event.status().getDisplayName().toUpperCase());
        statusBadge.getStyleClass().setAll("badge", badgeStyleOf(event.status()));

        methodButton.setText(event.tradingMethod().getDisplayName().toUpperCase());
        methodButton.setTooltip(new Tooltip(event.isLmsr()
                ? "Show what each option of this event is worth"
                : "Show the two books this event is traded through"));

        boolean closed = event.isClosed();
        winnerBadge.setText(closed ? "WINNER: " + event.winningOptionName() : "");
        showOnly(winnerBadge, closed);
    }

    private void showStatistics(EventDto event, EventTradingStatusDto status) {
        marketMakerLabel.setText(event.marketMakerName());
        accountBalanceLabel.setText(Formats.money(event.accountBalance()));
        commissionCollectedLabel.setText(Formats.money(event.totalCommissionCollected()));

        // Every term reads the same way: the figure itself, and underneath it in small print
        // whatever qualifies it. That is what lets boxes of quite different contents line up.
        commissionLabel.setText(Formats.percent(event.commissionPercent()));
        commissionWhenLabel.setText(event.commissionType().getDisplayName().toLowerCase());

        boolean lmsr = event.isLmsr();
        showOnly(liquidityTile, lmsr);
        showOnly(subsidyTile, lmsr);
        showOnly(baseValueTile, !lmsr);
        showOnly(investmentTile, !lmsr);
        showOnly(mintTile, !lmsr);

        if (lmsr) {
            liquidityLabel.setText(String.valueOf(event.lmsr().liquidityParameter()));
            subsidyLabel.setText(Formats.money(event.lmsr().subsidy()));
            subsidyPaidLabel.setText(status.subsidy() > 0 ? "paid" : "not yet paid");
        } else {
            baseValueLabel.setText(Formats.money(event.orderBook().baseValue()));
            investmentLabel.setText(Formats.money(event.orderBook().initialInvestment()));
            investmentPairsLabel.setText(
                    Formats.shares(event.orderBook().initialPairs()) + " pairs");
            mintLabel.setText(event.orderBook().mintAllowed() ? "Allowed" : "Not allowed");
        }
        layOutTerms();
    }

    /**
     * Puts the term boxes into equal columns that fill the width, however many of them the trading
     * method calls for.
     *
     * <p>A fixed number of columns would leave LMSR, which has three terms to an order book's
     * four, with a hole at the end of its row. So the row is filled by however many there are, and
     * when the panel is dragged too narrow for that many they fall into two columns instead - with
     * the odd one out stretched across the pair, so that the block still ends square.
     */
    private void layOutTerms() {
        List<VBox> shown = new ArrayList<>();
        for (VBox tile : termTiles) {
            if (tile.isManaged()) {
                shown.add(tile);
            }
        }
        // Three terms need less room than four, so what counts as too narrow is worked out from
        // how many there are rather than fixed once for both trading methods.
        double width = termsGrid.getWidth();
        double needed = shown.size() * NARROWEST_TERM
                + (shown.size() - 1) * termsGrid.getHgap();
        int columns = width > 0 && width < needed ? Math.min(2, shown.size()) : shown.size();
        if (columns == 0 || (columns == termColumns && shown.equals(laidOutTerms))) {
            return;
        }
        termColumns = columns;
        laidOutTerms = List.copyOf(shown);

        termsGrid.getColumnConstraints().clear();
        for (int column = 0; column < columns; column++) {
            ColumnConstraints sameAsTheRest = new ColumnConstraints();
            sameAsTheRest.setPercentWidth(100.0 / columns);
            sameAsTheRest.setHgrow(Priority.ALWAYS);
            sameAsTheRest.setFillWidth(true);
            termsGrid.getColumnConstraints().add(sameAsTheRest);
        }
        for (int index = 0; index < shown.size(); index++) {
            VBox tile = shown.get(index);
            int row = index / columns;
            int column = index % columns;
            int leftInThisRow = shown.size() - row * columns;
            GridPane.setRowIndex(tile, row);
            GridPane.setColumnIndex(tile, column);
            GridPane.setColumnSpan(tile, leftInThisRow < columns ? columns - column : 1);
        }
    }

    /**
     * Brings the part of the panel that this event is actually traded in into view: its two books,
     * or the values of its options.
     *
     * <p>How an event trades is not a figure to be read beside its commission. It says which half
     * of everything below applies, and so it is offered as the way of getting to that half.
     */
    @FXML
    private void onShowMarket() {
        scrollTo(booksBox.isManaged() ? booksBox : lmsrBox);
    }

    /**
     * Scrolls whatever the component was placed inside until one part of it is at the top.
     *
     * <p>Both screens put this panel in a scroll pane of their own, and neither of them hands it
     * down, which is on purpose: the panel shows an event and does not care whose screen it is on.
     * So the pane is looked for above the panel rather than asked for, and if there is none - which
     * there would not be if the panel were ever placed somewhere that does not scroll - nothing
     * happens at all.
     */
    private void scrollTo(Node target) {
        Node above = rootPane.getParent();
        while (above != null && !(above instanceof ScrollPane)) {
            above = above.getParent();
        }
        if (!(above instanceof ScrollPane scroller) || scroller.getContent() == null) {
            return;
        }
        Node content = scroller.getContent();
        double hidden = content.getBoundsInLocal().getHeight()
                - scroller.getViewportBounds().getHeight();
        if (hidden <= 0) {
            return;
        }
        double top = content.sceneToLocal(target.localToScene(target.getBoundsInLocal())).getMinY();
        scroller.setVvalue(top / hidden);
    }

    private void showParticipants(EventDto event, List<ParticipantDto> participants) {
        rebuildParticipantColumns(event.optionNames());
        participantsTable.setItems(FXCollections.observableArrayList(participants));
    }

    /**
     * Rebuilds the participants table, whose columns are headed with the names of the options and
     * therefore differ from one event to the next.
     */
    private void rebuildParticipantColumns(List<String> optionNames) {
        if (optionNames.equals(participantColumnsFor)) {
            return;
        }
        participantColumnsFor = List.copyOf(optionNames);
        participantsTable.getColumns().setAll(List.of(
                Tables.text("USER", ParticipantDto::userName),
                Tables.text("ROLE", p -> p.marketMaker() ? "Market maker" : "Participant")));
        for (int i = 0; i < optionNames.size(); i++) {
            int optionIndex = i;
            participantsTable.getColumns().add(Tables.group(optionNames.get(i),
                    Tables.number("SHARES",
                            p -> Formats.shares(holding(p, optionIndex).shares())),
                    Tables.number("VALUE",
                            p -> Formats.money(holding(p, optionIndex).currentValue())),
                    Tables.number("PAID",
                            p -> Formats.money(holding(p, optionIndex).amountPaid()))));
        }
        participantsTable.getColumns().addAll(List.of(
                Tables.number("OPEN ORDERS",
                        p -> p.openOrders() == 0 ? Formats.NOTHING : String.valueOf(p.openOrders())),
                Tables.number("TOTAL VALUE", p -> Formats.money(p.totalValue()))));
    }

    private static HoldingDto holding(ParticipantDto participant, int optionIndex) {
        return optionIndex < participant.holdings().size()
                ? participant.holdings().get(optionIndex)
                : new HoldingDto("", 0, 0, 0);
    }

    /**
     * Draws one line per option, against the number of transactions rather than against the clock:
     * the system has no notion of when anything happened, and in a market it is the transactions
     * that move a price.
     */
    private void showPriceChart(EventDto event, List<PricePointDto> priceHistory) {
        Map<String, XYChart.Series<Number, Number>> seriesByOption = new LinkedHashMap<>();
        for (String optionName : event.optionNames()) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(optionName);
            seriesByOption.put(optionName, series);
        }
        for (PricePointDto point : priceHistory) {
            XYChart.Series<Number, Number> series = seriesByOption.get(point.optionName());
            if (series != null) {
                series.getData().add(new XYChart.Data<>(point.tradeNumber(), point.price()));
            }
        }
        priceChart.getData().setAll(seriesByOption.values());
        showOnly(chartBox, !priceHistory.isEmpty());
    }

    // ------------------------------------------------------------------ table layouts

    private void buildOptionsTable() {
        Tables.columns(optionsTable,
                Tables.text("OPTION", OptionStateDto::name),
                Tables.number("VALUE", option -> Formats.decimal(option.value())),
                Tables.number("PROBABILITY", option -> Formats.probability(option.value())),
                Tables.number("SHARES BOUGHT",
                        option -> Formats.shares(option.sharesPurchased())));
    }

    private void buildHistoryTable() {
        Tables.columns(historyTable,
                Tables.number("#", trade -> String.valueOf(trade.serialNumber())),
                Tables.text("TYPE", trade -> trade.kind().getDisplayName()),
                Tables.text("OPTION", MarketTradeDto::optionName),
                Tables.number("SHARES", trade -> Formats.shares(trade.shares())),
                Tables.number("PRICE", trade -> Formats.price(trade.pricePerShare())),
                Tables.number("TOTAL", trade -> Formats.money(trade.totalPrice())),
                Tables.number("COMMISSION", trade -> Formats.money(trade.commission())),
                Tables.text("BUYER", MarketTradeDto::buyerName),
                Tables.text("SELLER", MarketTradeDto::sellerName));
    }

    // ------------------------------------------------------------------ small helpers

    private static void showOnly(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static String badgeStyleOf(EventStatus status) {
        return switch (status) {
            case NOT_STARTED -> "badge-neutral";
            case ACTIVE -> "badge-active";
            case CLOSED -> "badge-closed";
        };
    }
}
