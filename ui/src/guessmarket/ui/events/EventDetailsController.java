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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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

    @FXML private VBox rootPane;

    @FXML private Label eventNameLabel;
    @FXML private Label statusBadge;
    @FXML private Label methodBadge;
    @FXML private Label winnerBadge;
    @FXML private Label descriptionLabel;

    @FXML private Label marketMakerLabel;
    @FXML private Label accountBalanceLabel;
    @FXML private Label commissionLabel;
    @FXML private Label commissionCollectedLabel;
    @FXML private Label liquidityLabel;
    @FXML private Label subsidyLabel;
    @FXML private Label baseValueLabel;
    @FXML private Label investmentLabel;
    @FXML private Label mintLabel;

    @FXML private VBox liquidityTile;
    @FXML private VBox subsidyTile;
    @FXML private VBox baseValueTile;
    @FXML private VBox investmentTile;
    @FXML private VBox mintTile;

    @FXML private VBox lmsrBox;
    @FXML private TableView<OptionStateDto> optionsTable;
    @FXML private HBox booksRow;
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
     * panel sliding about while somebody is trying to trade in it.
     */
    private int shownEventId = NOTHING_SHOWN;

    /** The event account balance the panel last showed, so a balance that has moved can be marked. */
    private double shownAccountBalance;

    @FXML
    private void initialize() {
        buildOptionsTable();
        buildHistoryTable();
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
        showOnly(booksRow, !lmsr);
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
        methodBadge.setText(event.tradingMethod().getDisplayName().toUpperCase());

        boolean closed = event.isClosed();
        winnerBadge.setText(closed ? "WINNER: " + event.winningOptionName() : "");
        showOnly(winnerBadge, closed);
    }

    private void showStatistics(EventDto event, EventTradingStatusDto status) {
        marketMakerLabel.setText(event.marketMakerName());
        accountBalanceLabel.setText(Formats.money(event.accountBalance()));
        commissionLabel.setText(Formats.percent(event.commissionPercent())
                + " " + event.commissionType().getDisplayName().toLowerCase());
        commissionCollectedLabel.setText(Formats.money(event.totalCommissionCollected()));

        boolean lmsr = event.isLmsr();
        showOnly(liquidityTile, lmsr);
        showOnly(subsidyTile, lmsr);
        showOnly(baseValueTile, !lmsr);
        showOnly(investmentTile, !lmsr);
        showOnly(mintTile, !lmsr);

        if (lmsr) {
            liquidityLabel.setText(String.valueOf(event.lmsr().liquidityParameter()));
            subsidyLabel.setText(Formats.money(event.lmsr().subsidy())
                    + (status.subsidy() > 0 ? " (paid)" : " (not yet paid)"));
        } else {
            baseValueLabel.setText(Formats.money(event.orderBook().baseValue()));
            investmentLabel.setText(Formats.money(event.orderBook().initialInvestment())
                    + " = " + Formats.shares(event.orderBook().initialPairs()) + " pairs");
            mintLabel.setText(event.orderBook().mintAllowed() ? "Allowed" : "Not allowed");
        }
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
                Tables.text("USER", 130, ParticipantDto::userName),
                Tables.text("ROLE", 115, p -> p.marketMaker() ? "Market maker" : "Participant")));
        for (int i = 0; i < optionNames.size(); i++) {
            int optionIndex = i;
            participantsTable.getColumns().add(Tables.group(optionNames.get(i),
                    Tables.number("SHARES", 85,
                            p -> Formats.shares(holding(p, optionIndex).shares())),
                    Tables.number("VALUE", 90,
                            p -> Formats.money(holding(p, optionIndex).currentValue())),
                    Tables.number("PAID", 90,
                            p -> Formats.money(holding(p, optionIndex).amountPaid()))));
        }
        participantsTable.getColumns().addAll(List.of(
                Tables.number("OPEN ORDERS", 100,
                        p -> p.openOrders() == 0 ? Formats.NOTHING : String.valueOf(p.openOrders())),
                Tables.number("TOTAL VALUE", 110, p -> Formats.money(p.totalValue()))));
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
                Tables.text("OPTION", 220, OptionStateDto::name),
                Tables.number("VALUE", 100, option -> Formats.decimal(option.value())),
                Tables.number("PROBABILITY", 120, option -> Formats.probability(option.value())),
                Tables.number("SHARES BOUGHT", 140,
                        option -> Formats.shares(option.sharesPurchased())));
    }

    private void buildHistoryTable() {
        Tables.columns(historyTable,
                Tables.number("#", 50, trade -> String.valueOf(trade.serialNumber())),
                Tables.text("TYPE", 100, trade -> trade.kind().getDisplayName()),
                Tables.text("OPTION", 130, MarketTradeDto::optionName),
                Tables.number("SHARES", 90, trade -> Formats.shares(trade.shares())),
                Tables.number("PRICE", 90, trade -> Formats.price(trade.pricePerShare())),
                Tables.number("TOTAL", 100, trade -> Formats.money(trade.totalPrice())),
                Tables.number("COMMISSION", 110, trade -> Formats.money(trade.commission())),
                Tables.text("BUYER", 105, MarketTradeDto::buyerName),
                Tables.text("SELLER", 105, MarketTradeDto::sellerName));
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
