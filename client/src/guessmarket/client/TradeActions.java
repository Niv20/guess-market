package guessmarket.client;

import guessmarket.dto.*;
import guessmarket.dto.network.QuoteDto;
import guessmarket.ui.common.Dialogs;
import guessmarket.ui.common.Formats;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Session-owned trading controls. A caller can never select or submit another user's identity. */
final class TradeActions extends VBox {
    private final ClientSession session;
    private final Window window;
    private final Consumer<String> message;
    private final Label title = new Label("TRADE AS YOURSELF");
    private final Label explanation = new Label();
    private final Label quote = new Label();
    private final ComboBox<String> option = new ComboBox<>();
    private final ComboBox<OrderSide> side = new ComboBox<>();
    private final TextField shares = new TextField("1");
    private final TextField price = new TextField("0.50");
    private final ComboBox<String> winner = new ComboBox<>();
    private final Button open = new Button("Open event");
    private final Button close = new Button("Close & settle event");
    private final Button buy = new Button("Buy shares");
    private final Button order = new Button("Place order");
    private final Button estimate = new Button("Get quote");
    private final FlowPane buying = new FlowPane(12, 10);
    private final FlowPane closing = new FlowPane(12, 10);
    private final VBox priceField;
    private final VBox sideField;
    private EventTradingStatusDto selected;
    private UserDto self;
    private long quoteRevision;

    TradeActions(ClientSession session, Window window, BooleanProperty busy, Consumer<String> message) {
        this.session = session;
        this.window = window;
        this.message = message;
        setSpacing(10);
        getStyleClass().add("card");
        title.getStyleClass().add("panel-title");
        explanation.getStyleClass().add("muted");
        explanation.setWrapText(true);
        quote.getStyleClass().add("value-accent");
        quote.setWrapText(true);
        option.setId("trade-option"); option.setPrefWidth(180);
        shares.setId("trade-shares"); shares.setPrefColumnCount(8);
        price.setId("trade-price"); price.setPrefColumnCount(8);
        side.setId("trade-side"); side.getItems().setAll(OrderSide.values()); side.getSelectionModel().selectFirst();
        winner.setId("winning-option"); winner.setPrefWidth(180);
        open.setId("open-event-button"); close.setId("close-event-button");
        buy.setId("buy-button"); order.setId("order-button"); estimate.setId("quote-button");
        buy.getStyleClass().add("button-primary"); order.getStyleClass().add("button-primary");
        open.getStyleClass().add("button-primary");
        sideField = field("SIDE", side);
        priceField = field("LIMIT PRICE PER SHARE", price);
        buying.getChildren().addAll(field("OPTION", option), sideField, field("SHARES", shares),
                priceField, field("", estimate), field("", buy), field("", order));
        closing.getChildren().addAll(field("WINNING OPTION", winner), field("", close));
        getChildren().addAll(title, explanation, open, buying, quote, closing);
        for (Button button : new Button[]{open, close, buy, order, estimate}) button.disableProperty().bind(busy);
        option.valueProperty().addListener((o, a, b) -> invalidateQuote());
        shares.textProperty().addListener((o, a, b) -> invalidateQuote());
        estimate.setOnAction(action -> getQuote());
        buy.setOnAction(action -> buy());
        order.setOnAction(action -> order());
        open.setOnAction(action -> open());
        close.setOnAction(action -> close());
        show(null, null);
    }

    void show(EventTradingStatusDto state, UserDto user) {
        EventDto previous = selected == null ? null : selected.event();
        if (!Objects.equals(selected, state)) invalidateQuote();
        selected = state;
        self = user;
        visible(this, state != null && user != null);
        if (state == null || user == null) return;
        EventDto event = state.event();
        boolean different = previous == null || previous.id() != event.id();
        if (different) {
            option.getItems().setAll(event.optionNames()); option.getSelectionModel().selectFirst();
            winner.getItems().setAll(event.optionNames()); winner.getSelectionModel().selectFirst();
            shares.setText("1");
            price.setText("0.50");
        }
        boolean maker = user.name().equalsIgnoreCase(event.marketMakerName());
        boolean allowed = !user.blocked();
        title.setText("ACTING AS " + user.name());
        visible(open, allowed && maker && event.status() == EventStatus.NOT_STARTED);
        visible(buying, allowed && event.isActive());
        visible(closing, allowed && maker && event.isActive());
        visible(quote, allowed && event.isActive() && event.isLmsr());
        visible(priceField, event.isOrderBook());
        visible(sideField, event.isOrderBook());
        visible(estimate, event.isLmsr()); visible(buy, event.isLmsr()); visible(order, event.isOrderBook());
        // Button parents carry their labels; hide the whole field so no empty layout slot remains.
        visible(estimate.getParent(), event.isLmsr()); visible(buy.getParent(), event.isLmsr());
        visible(order.getParent(), event.isOrderBook());
        if (user.blocked()) explanation.setText("Your account is blocked. You can still view your account and load funds; trading remains unavailable under the market's account rules.");
        else if (event.isClosed()) explanation.setText("This event is settled. The winning option is " + event.winningOptionName() + ".");
        else if (event.status() == EventStatus.NOT_STARTED) {
            double investment = event.isLmsr() ? event.lmsr().subsidy() : event.orderBook().initialInvestment();
            explanation.setText(maker ? "Opening transfers " + Formats.money(investment) + " from your account to this event."
                    : "Waiting for " + event.marketMakerName() + " to open this event.");
        } else explanation.setText(event.isLmsr()
                ? "Buy an outcome with your own balance. A quote is indicative; the current market price applies when the server executes the purchase."
                : "Buy or sell an outcome with a limit order. Unfilled shares remain in the book. Prices must be between $0.01 and "
                    + Formats.price(event.orderBook().highestAllowedPrice()) + ".");
    }

    private void getQuote() {
        try {
            EventDto event = event(); int selectedOption = optionIndex(); long quantity = quantity();
            long revision = ++quoteRevision;
            quote.setText("Requesting a quote…");
            session.perform(api -> api.get("quote?eventId=" + event.id() + "&optionIndex=" + selectedOption
                    + "&shares=" + quantity, QuoteDto.class), result -> {
                if (quoteRevision == revision) quote.setText("Estimated share cost before commission: " + Formats.money(result.cost())
                        + " · may change before purchase");
            });
        } catch (IllegalArgumentException error) { message.accept(error.getMessage()); }
    }

    private void buy() {
        try {
            int id = event().id(); int selectedOption = optionIndex(); long quantity = quantity();
            session.perform(api -> api.post("events/buy", Map.of("eventId", id, "optionIndex", selectedOption,
                    "shares", quantity), PurchaseResultDto.class), result -> message.accept("Bought "
                    + Formats.shares(result.shares()) + " shares of " + result.optionName() + " for "
                    + Formats.money(result.totalPaid()) + " (commission " + Formats.money(result.commissionPaid()) + ")."));
        } catch (IllegalArgumentException error) { message.accept(error.getMessage()); }
    }

    private void order() {
        try {
            int id = event().id(); int selectedOption = optionIndex(); long quantity = quantity();
            double limit = positiveMoney(price.getText(), "Limit price");
            OrderSide direction = side.getValue();
            session.perform(api -> api.post("events/order", Map.of("eventId", id, "optionIndex", selectedOption,
                    "side", direction, "quantity", quantity, "pricePerShare", limit), SubmitOrderResultDto.class),
                    result -> message.accept("Order #" + result.orderId() + ": " + Formats.shares(result.quantityFilled())
                            + " filled (" + Formats.shares(result.quantityMinted()) + " minted), "
                            + Formats.shares(result.quantityResting()) + " waiting. Balance: " + Formats.money(result.balanceAfter()) + "."));
        } catch (IllegalArgumentException error) { message.accept(error.getMessage()); }
    }

    private void open() {
        int id = event().id();
        session.perform(api -> api.post("events/open", Map.of("eventId", id), OpenEventResultDto.class),
                result -> message.accept("Opened " + result.eventName() + ". " + Formats.money(result.amountPaid())
                        + " invested; balance " + Formats.money(result.balanceAfter()) + "."));
    }

    private void close() {
        EventDto event = event(); int winningIndex = winner.getSelectionModel().getSelectedIndex();
        if (winningIndex < 0) { message.accept("Choose the winning option."); return; }
        if (!Dialogs.confirm(window, "Close " + event.name() + "?", "Declare " + event.optionNames().get(winningIndex)
                + " the winner and settle every participant. This event cannot be reopened.")) return;
        session.perform(api -> api.post("events/close", Map.of("eventId", event.id(), "winningOptionIndex", winningIndex),
                CloseEventResultDto.class), result -> message.accept("Settled " + result.eventName() + ": "
                        + result.winningOptionName() + " won. " + Formats.money(result.amountPaidToWinners())
                        + " paid to winners; " + Formats.money(result.returnedToMarketMaker()) + " returned to the market maker."));
    }

    private void invalidateQuote() { quoteRevision++; quote.setText(""); }
    private EventDto event() { if (selected == null || self == null) throw new IllegalArgumentException("Choose an event."); return selected.event(); }
    private int optionIndex() { int index = option.getSelectionModel().getSelectedIndex(); if (index < 0) throw new IllegalArgumentException("Choose an option."); return index; }
    private long quantity() {
        try { long count = Long.parseLong(shares.getText().trim()); if (count <= 0) throw new NumberFormatException(); return count; }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Shares must be a positive whole number."); }
    }
    static double positiveMoney(String value, String field) {
        try {
            String amount = value == null ? "" : value.trim();
            if (!amount.matches("[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+")) throw new NumberFormatException();
            double number = Double.parseDouble(amount);
            if (!Double.isFinite(number) || number <= 0) throw new NumberFormatException();
            return number;
        }
        catch (NumberFormatException error) { throw new IllegalArgumentException(field + " must be a positive amount."); }
    }
    static VBox field(String caption, Node input) {
        Label label = new Label(caption); label.getStyleClass().add("field-label");
        VBox box = new VBox(5, label, input); box.setMinWidth(0); return box;
    }
    static void visible(Node node, boolean visible) { node.setVisible(visible); node.setManaged(visible); }
}
