package guessmarket.ui.users;

import guessmarket.dto.HoldingDto;
import guessmarket.dto.OptionStateDto;
import guessmarket.dto.OrderDto;
import guessmarket.dto.TradeRecordDto;
import guessmarket.dto.UserEventInvolvementDto;
import guessmarket.dto.UserLmsrInvolvementDto;
import guessmarket.dto.UserOrderBookInvolvementDto;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tables;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * What one user has to do with one event.
 *
 * <p>This is the part of the users screen the assignment describes item by item, and the two
 * trading methods are asked for different things, so the component shows one of two sets of tables
 * according to how the event is traded. Everything it shows is handed to it; it asks the engine
 * nothing and changes nothing.
 */
public class UserInvolvementController {

    @FXML private VBox rootPane;
    @FXML private Label titleLabel;
    @FXML private Label roleBadge;
    @FXML private Label commissionLabel;
    @FXML private Label holdingsValueLabel;
    @FXML private VBox outcomeTile;
    @FXML private Label outcomeCaption;
    @FXML private Label outcomeLabel;
    @FXML private Label emptyLabel;

    @FXML private VBox holdingsBox;
    @FXML private TableView<HoldingDto> holdingsTable;
    @FXML private VBox tradesBox;
    @FXML private TableView<TradeRecordDto> tradesTable;
    @FXML private VBox ordersBox;
    @FXML private TableView<OrderDto> ordersTable;
    @FXML private VBox finalTotalsBox;
    @FXML private TableView<OptionStateDto> finalTotalsTable;

    @FXML
    private void initialize() {
        Tables.columns(holdingsTable,
                Tables.text("OPTION", HoldingDto::optionName),
                Tables.number("SHARES", holding -> Formats.shares(holding.shares())),
                Tables.number("PAID", holding -> Formats.money(holding.amountPaid())),
                Tables.number("WORTH NOW", holding -> Formats.money(holding.currentValue())));

        Tables.columns(tradesTable,
                Tables.number("#", trade -> String.valueOf(trade.serialNumber())),
                Tables.text("OPTION", TradeRecordDto::optionName),
                Tables.number("SHARES", trade -> Formats.shares(trade.shares())),
                Tables.number("PRICE PAID",
                        trade -> Formats.money(trade.amountPaidForShares())),
                Tables.number("COMMISSION", trade -> Formats.money(trade.commissionPaid())),
                Tables.number("TOTAL", trade -> Formats.money(trade.totalPaid())));

        Tables.columns(ordersTable,
                Tables.text("SIDE", order -> order.side().getDisplayName()),
                Tables.number("SHARES LEFT", order -> Formats.shares(order.remaining())),
                Tables.number("OF", order -> Formats.shares(order.quantity())),
                Tables.number("PRICE", order -> Formats.price(order.pricePerShare())),
                Tables.number("WORTH", order -> Formats.money(order.remainingValue())));

        Tables.columns(finalTotalsTable,
                Tables.text("OPTION", OptionStateDto::name),
                Tables.number("SHARES BOUGHT ALTOGETHER",
                        option -> Formats.shares(option.sharesPurchased())));

        Tables.emptyMessage(holdingsTable, "No shares held.");
        Tables.emptyMessage(tradesTable, "Nothing bought in this event yet.");
        Tables.emptyMessage(ordersTable, "No orders waiting.");
        Tables.emptyMessage(finalTotalsTable, "");
        clear();
    }

    public void clear() {
        show(rootPane, false);
    }

    /** Shows what this user has going on in this event. */
    public void show(UserEventInvolvementDto involvement) {
        show(rootPane, true);
        titleLabel.setText("IN \"" + involvement.event().name().toUpperCase() + "\"");
        roleBadge.setText(describeRole(involvement));
        roleBadge.getStyleClass().setAll("badge",
                involvement.marketMaker() ? "badge-mm" : "badge-neutral");

        boolean lmsr = involvement.event().isLmsr();
        show(tradesBox, lmsr);
        show(finalTotalsBox, false);
        show(ordersBox, !lmsr);

        if (lmsr) {
            showLmsr(involvement.lmsr(), involvement.event().isClosed());
        } else {
            showOrderBook(involvement.orderBook(), involvement.event().isClosed());
        }
        show(emptyLabel, !involvement.participant());
        show(holdingsBox, involvement.participant());
    }

    // ------------------------------------------------------------------ the two kinds

    private void showLmsr(UserLmsrInvolvementDto lmsr, boolean closed) {
        commissionLabel.setText(Formats.money(lmsr.totalCommissionPaid()));
        fillHoldings(lmsr.holdings());
        tradesTable.setItems(FXCollections.observableArrayList(lmsr.trades()));

        show(finalTotalsBox, closed && !lmsr.finalOptionTotals().isEmpty());
        finalTotalsTable.setItems(FXCollections.observableArrayList(lmsr.finalOptionTotals()));

        showOutcome(closed && lmsr.amountWon() != null, "WON AT THE SETTLEMENT",
                lmsr.amountWon() == null ? 0 : lmsr.amountWon(), false);
    }

    private void showOrderBook(UserOrderBookInvolvementDto orderBook, boolean closed) {
        commissionLabel.setText(Formats.money(orderBook.totalCommissionPaid()));
        fillHoldings(orderBook.holdings());
        ordersTable.setItems(FXCollections.observableArrayList(orderBook.openOrders()));

        showOutcome(closed && orderBook.profitOrLoss() != null, "PROFIT OR LOSS",
                orderBook.profitOrLoss() == null ? 0 : orderBook.profitOrLoss(), true);
    }

    private void fillHoldings(List<HoldingDto> holdings) {
        holdingsTable.setItems(FXCollections.observableArrayList(holdings));
        double worth = 0;
        for (HoldingDto holding : holdings) {
            worth += holding.currentValue();
        }
        holdingsValueLabel.setText(Formats.money(worth));
    }

    /** The last tile only appears once the event has been decided and there is a result to show. */
    private void showOutcome(boolean visible, String caption, double amount, boolean signed) {
        show(outcomeTile, visible);
        outcomeCaption.setText(caption);
        outcomeLabel.setText(signed ? Formats.signedMoney(amount) : Formats.money(amount));
        outcomeLabel.getStyleClass().setAll("value",
                signed && amount < 0 ? "value-negative" : "value-accent");
    }

    private static String describeRole(UserEventInvolvementDto involvement) {
        if (involvement.marketMaker()) {
            return "MARKET MAKER";
        }
        return involvement.participant() ? "TAKING PART" : "NOT TAKING PART";
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
