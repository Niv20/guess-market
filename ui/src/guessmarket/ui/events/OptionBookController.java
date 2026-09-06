package guessmarket.ui.events;

import guessmarket.dto.OptionBookDto;
import guessmarket.dto.OrderDto;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tables;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/**
 * The order book of one option: the five figures the market is read by, and the orders waiting on
 * each side of it.
 *
 * <p>The component knows nothing about which event it belongs to or where it is on the screen. It
 * is handed one book and shows it, which is what lets the same layout file be used twice side by
 * side, once for each option of an event.
 */
public class OptionBookController {

    @FXML private Label optionNameLabel;
    @FXML private Label lastLabel;
    @FXML private Label bidLabel;
    @FXML private Label askLabel;
    @FXML private Label midLabel;
    @FXML private Label spreadLabel;
    @FXML private Label bidTotalLabel;
    @FXML private Label askTotalLabel;
    @FXML private TableView<OrderDto> bidsTable;
    @FXML private TableView<OrderDto> asksTable;

    @FXML
    private void initialize() {
        Tables.columns(bidsTable,
                Tables.text("USER", 120, OrderDto::userName),
                Tables.number("SHARES", 80, order -> Formats.shares(order.remaining())),
                Tables.styled("PRICE", 80, "bid-cell", order -> Formats.price(order.pricePerShare())));
        Tables.columns(asksTable,
                Tables.text("USER", 120, OrderDto::userName),
                Tables.number("SHARES", 80, order -> Formats.shares(order.remaining())),
                Tables.styled("PRICE", 80, "ask-cell", order -> Formats.price(order.pricePerShare())));

        Tables.emptyMessage(bidsTable, "Nobody is offering to buy this option.");
        Tables.emptyMessage(asksTable, "Nobody is offering to sell this option.");
    }

    /** Fills the component with one book. */
    public void show(OptionBookDto book) {
        optionNameLabel.setText(book.optionName());
        lastLabel.setText(Formats.price(book.lastTradePrice()));
        bidLabel.setText(Formats.price(book.bestBid()));
        askLabel.setText(Formats.price(book.bestAsk()));
        midLabel.setText(Formats.price(book.midPrice()));
        spreadLabel.setText(Formats.price(book.spread()));

        bidTotalLabel.setText(describeDepth(book.totalBidShares()));
        askTotalLabel.setText(describeDepth(book.totalAskShares()));
        bidsTable.setItems(FXCollections.observableArrayList(book.bids()));
        asksTable.setItems(FXCollections.observableArrayList(book.asks()));
    }

    private static String describeDepth(long shares) {
        return shares == 0 ? "" : Formats.shares(shares) + " shares";
    }
}
