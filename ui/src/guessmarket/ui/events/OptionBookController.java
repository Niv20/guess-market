package guessmarket.ui.events;

import guessmarket.dto.OptionBookDto;
import guessmarket.dto.OrderDto;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tables;
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
                Tables.text("USER", OrderDto::userName),
                Tables.number("SHARES", order -> Formats.shares(order.remaining())),
                Tables.styled("PRICE", "bid-cell", order -> Formats.price(order.pricePerShare())));
        Tables.columns(asksTable,
                Tables.text("USER", OrderDto::userName),
                Tables.number("SHARES", order -> Formats.shares(order.remaining())),
                Tables.styled("PRICE", "ask-cell", order -> Formats.price(order.pricePerShare())));

        Tables.emptyMessage(bidsTable, "Nobody is offering to buy this option.");
        Tables.emptyMessage(asksTable, "Nobody is offering to sell this option.");
    }

    /** Fills the component with one book. */
    public void show(OptionBookDto book) {
        show(book, true);
    }

    /** Fills the component while retaining an order selection only when this is the same event. */
    public void show(OptionBookDto book, boolean preserveSelection) {
        optionNameLabel.setText(book.optionName());
        lastLabel.setText(Formats.price(book.lastTradePrice()));
        bidLabel.setText(Formats.price(book.bestBid()));
        askLabel.setText(Formats.price(book.bestAsk()));
        midLabel.setText(Formats.price(book.midPrice()));
        spreadLabel.setText(Formats.price(book.spread()));

        bidTotalLabel.setText(describeDepth(book.totalBidShares()));
        askTotalLabel.setText(describeDepth(book.totalAskShares()));
        Tables.setItemsPreservingSelection(bidsTable, book.bids(), OrderDto::orderId, preserveSelection);
        Tables.setItemsPreservingSelection(asksTable, book.asks(), OrderDto::orderId, preserveSelection);
    }

    private static String describeDepth(long shares) {
        return shares == 0 ? "" : Formats.shares(shares) + " shares";
    }
}
