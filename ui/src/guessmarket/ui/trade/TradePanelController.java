package guessmarket.ui.trade;

import guessmarket.dto.CloseEventRequestDto;
import guessmarket.dto.CloseEventResultDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventStatus;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.OpenEventRequestDto;
import guessmarket.dto.OpenEventResultDto;
import guessmarket.dto.OrderSide;
import guessmarket.dto.PurchaseRequestDto;
import guessmarket.dto.PurchaseResultDto;
import guessmarket.dto.SubmitOrderRequestDto;
import guessmarket.dto.SubmitOrderResultDto;
import guessmarket.dto.UserDto;
import guessmarket.engine.exception.GuessMarketException;
import guessmarket.ui.app.AppContext;
import guessmarket.ui.common.Dialogs;
import guessmarket.ui.common.Formats;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * What the selected user may do in the selected event.
 *
 * <p>Everything a person can change about the system happens through this one component, which is
 * why it lives on the users screen: an action always belongs to somebody, and the person who is
 * acting is whoever is selected on the left. The events screen shows the same events with no
 * controls at all.
 *
 * <p>The panel decides what to offer from three things: whether the user runs the event, what stage
 * the event has reached, and how it is traded. Where there is something to explain it explains it -
 * a blocked user is told why nothing here will work for them, and a closed event says what it
 * settled on. Where there is nothing at all to offer and nothing to explain, the panel is not
 * shown: an event nobody has opened yet is not this user's to open, and the event underneath
 * already says so.
 *
 * <p>Nothing is worked out here. Every figure shown comes from the engine, including the price of a
 * purchase before it is made, and every action is a single call whose refusal is shown to the
 * person exactly as the engine phrased it.
 */
public class TradePanelController {

    @FXML private VBox rootPane;
    @FXML private Label actingAsLabel;
    @FXML private Label messageLabel;

    /** The box holding whichever of the two things only a market maker may do applies now. */
    @FXML private VBox marketMakerBox;

    @FXML private VBox openBox;
    @FXML private Button openButton;
    @FXML private Label openCostLabel;

    @FXML private VBox buyBox;
    @FXML private ComboBox<String> buyOptionChooser;
    @FXML private TextField sharesField;
    @FXML private Button buyButton;
    @FXML private Label quoteLabel;

    @FXML private VBox orderBox;
    @FXML private ComboBox<String> orderOptionChooser;
    @FXML private ComboBox<OrderSide> sideChooser;
    @FXML private TextField quantityField;
    @FXML private TextField priceField;
    @FXML private Button submitOrderButton;
    @FXML private Label orderHintLabel;

    @FXML private VBox closeBox;
    @FXML private ComboBox<String> winnerChooser;
    @FXML private Button closeButton;
    @FXML private Label closeHintLabel;

    private AppContext context;

    /** Who is acting and what they are acting on; both null when the panel is empty. */
    private UserDto user;
    private EventDto event;

    @FXML
    private void initialize() {
        sideChooser.getItems().setAll(OrderSide.BUY, OrderSide.SELL);
        sideChooser.setValue(OrderSide.BUY);
        buyOptionChooser.valueProperty().addListener((observable, old, chosen) -> updateQuote());
        sharesField.textProperty().addListener((observable, old, typed) -> updateQuote());
        clear();
    }

    public void connect(AppContext appContext) {
        this.context = appContext;
    }

    /** Empties the panel and hides it, for when no user or no event is selected. */
    public void clear() {
        user = null;
        event = null;
        show(rootPane, false);
    }

    /** Offers whatever this user may do in this event, and explains whatever they may not. */
    public void show(UserDto selectedUser, EventTradingStatusDto status) {
        this.user = selectedUser;
        this.event = status.event();
        show(rootPane, true);
        actingAsLabel.setText("Acting as " + selectedUser.name());

        boolean marketMaker = event.marketMakerName().equalsIgnoreCase(selectedUser.name());
        show(marketMakerBox, false);
        show(openBox, false);
        show(buyBox, false);
        show(orderBox, false);
        show(closeBox, false);
        show(messageLabel, false);

        // An event that has not been opened has nothing on this panel for anybody but the one
        // person who can open it, so for everybody else the panel is taken away rather than
        // filled with a sentence saying so. The sentence was the whole of what was shown - a
        // heading, a name and a rule round nothing anybody could do - and it said what the event
        // itself says underneath in the same words, where somebody who never opens this screen
        // reads it too.
        if (event.status() == EventStatus.NOT_STARTED && !marketMaker) {
            show(rootPane, false);
            return;
        }
        if (selectedUser.blocked()) {
            explain(selectedUser.name() + " is blocked, because their balance went below zero. "
                    + "A blocked user cannot take any further action, and accounts cannot be "
                    + "topped up.");
            return;
        }
        switch (event.status()) {
            case NOT_STARTED -> offerToOpen();
            case ACTIVE -> offerToTrade(marketMaker);
            case CLOSED -> explain("This event is closed. \"" + event.winningOptionName()
                    + "\" won, the winners have been paid, and nothing more can happen in it.");
        }
    }

    // ------------------------------------------------------------------ what is on offer

    /** Only ever reached by the market maker: everybody else was shown no panel at all. */
    private void offerToOpen() {
        offerAsMarketMaker(openBox);
        openCostLabel.setText(describeOpeningCost());
        openButton.setDisable(false);
    }

    /** @return what opening this event will cost, and what the market maker gets for it. */
    private String describeOpeningCost() {
        if (event.isLmsr()) {
            return "Opening this event costs " + Formats.money(event.lmsr().subsidy())
                    + ", the subsidy that lets people trade against it. Whatever the event does "
                    + "not need is returned when it is closed.";
        }
        return "Opening this event costs " + Formats.money(event.orderBook().initialInvestment())
                + " and creates " + Formats.shares(event.orderBook().initialPairs())
                + " shares of each option, which become yours to sell.";
    }

    private void offerToTrade(boolean marketMaker) {
        if (event.isLmsr()) {
            show(buyBox, true);
            fillOptions(buyOptionChooser);
            updateQuote();
        } else {
            show(orderBox, true);
            fillOptions(orderOptionChooser);
            orderHintLabel.setText("A share of this event pays "
                    + Formats.money(event.orderBook().baseValue())
                    + " if its option wins, so an order can be priced between $0.01 and "
                    + Formats.price(event.orderBook().highestAllowedPrice()) + ". "
                    + (event.orderBook().mintAllowed()
                        ? "Minting is allowed here: a buy order can also be filled by a buyer of "
                            + "the other option, if your two prices together reach "
                            + Formats.money(event.orderBook().baseValue()) + "."
                        : "Minting is switched off here, so an order can only be filled against "
                            + "an order on the other side of the same book."));
        }
        if (marketMaker) {
            offerAsMarketMaker(closeBox);
            fillOptions(winnerChooser);
            closeHintLabel.setText("Closing pays every share of the winning option "
                    + Formats.money(event.baseValue()) + " out of the event account. "
                    + "It cannot be undone.");
        }
    }

    /**
     * Shows one of the two things only a market maker may do, and with it the box that says so.
     *
     * <p>They are never both on the screen at once - an event is either waiting to be opened or
     * waiting to be closed - so the box round them is what makes them one pair rather than two
     * rows that happen to look alike. Which is worth saying: everything else on this panel is
     * offered to whoever is selected, and these two are offered to one person in the system.
     */
    private void offerAsMarketMaker(VBox action) {
        show(marketMakerBox, true);
        show(action, true);
    }

    private void explain(String message) {
        messageLabel.setText(message);
        show(messageLabel, true);
    }

    /**
     * Refills a chooser with the options of the event, keeping whatever was chosen before if it is
     * still one of them. The option lists are immutable and refuse to be searched for nothing, so
     * the empty chooser of a freshly built panel is handled before the list is asked anything.
     */
    private void fillOptions(ComboBox<String> chooser) {
        String previous = chooser.getValue();
        chooser.getItems().setAll(event.optionNames());
        boolean stillThere = previous != null && event.optionNames().contains(previous);
        chooser.setValue(stillThere ? previous : event.optionNames().get(0));
    }

    // ------------------------------------------------------------------ the actions themselves

    @FXML
    private void onOpenEvent() {
        run(() -> {
            OpenEventResultDto result = context.engine()
                    .openEvent(new OpenEventRequestDto(user.name(), event.id()));
            return "\"" + result.eventName() + "\" is now open. " + user.name() + " paid "
                    + Formats.money(result.amountPaid())
                    + (result.sharesPerOption() > 0
                        ? " and received " + Formats.shares(result.sharesPerOption())
                            + " shares of each option." : ".")
                    + System.lineSeparator() + System.lineSeparator()
                    + "Their balance is now " + Formats.money(result.balanceAfter()) + ".";
        });
    }

    @FXML
    private void onBuyShares() {
        Long shares = wholeNumber(sharesField, "shares");
        if (shares == null) {
            return;
        }
        run(() -> {
            PurchaseResultDto result = context.engine().purchaseShares(new PurchaseRequestDto(
                    user.name(), event.id(), optionIndexOf(buyOptionChooser), shares));
            sharesField.clear();
            return user.name() + " bought " + Formats.shares(result.shares()) + " shares of \""
                    + result.optionName() + "\"." + System.lineSeparator()
                    + System.lineSeparator()
                    + "Shares: " + Formats.money(result.amountPaidForShares())
                    + System.lineSeparator()
                    + "Commission: " + Formats.money(result.commissionPaid())
                    + System.lineSeparator()
                    + "Total paid: " + Formats.money(result.totalPaid())
                    + System.lineSeparator()
                    + "Balance now: " + Formats.money(result.balanceAfter());
        });
    }

    @FXML
    private void onSubmitOrder() {
        Long quantity = wholeNumber(quantityField, "shares");
        Double price = price(priceField);
        if (quantity == null || price == null) {
            return;
        }
        run(() -> {
            SubmitOrderResultDto result = context.engine().submitOrder(new SubmitOrderRequestDto(
                    user.name(), event.id(), optionIndexOf(orderOptionChooser),
                    sideChooser.getValue(), quantity, price));
            quantityField.clear();
            priceField.clear();
            return describeOrder(result);
        });
    }

    /** @return what actually became of an order: what it traded, what it minted, what is waiting. */
    private String describeOrder(SubmitOrderResultDto result) {
        StringBuilder message = new StringBuilder();
        if (result.restedWithoutTrading()) {
            message.append("Nothing matched your order, so all ")
                    .append(Formats.shares(result.quantityResting()))
                    .append(" shares of it are waiting in the book.");
        } else {
            message.append(Formats.shares(result.quantityFilled())).append(" shares went through");
            if (result.quantityMinted() > 0) {
                message.append(", ").append(Formats.shares(result.quantityMinted()))
                        .append(" of them newly minted with a buyer of the other option");
            }
            message.append('.');
            if (result.quantityResting() > 0) {
                message.append(' ').append(Formats.shares(result.quantityResting()))
                        .append(" are waiting in the book.");
            }
        }
        message.append(System.lineSeparator()).append(System.lineSeparator());
        if (result.cashSpent() > 0) {
            message.append("Paid: ").append(Formats.money(result.cashSpent()))
                    .append(", of which ").append(Formats.money(result.commissionPaid()))
                    .append(" was commission.").append(System.lineSeparator());
        }
        if (result.cashReceived() > 0) {
            message.append("Received: ").append(Formats.money(result.cashReceived()))
                    .append(System.lineSeparator());
        }
        message.append("Balance now: ").append(Formats.money(result.balanceAfter()));
        for (String blocked : result.usersBlocked()) {
            message.append(System.lineSeparator()).append(System.lineSeparator())
                    .append("Note: this order left ").append(blocked)
                    .append(" with a balance below zero. ").append(blocked)
                    .append(" is now blocked and can take no further action.");
        }
        return message.toString();
    }

    @FXML
    private void onCloseEvent() {
        String winner = winnerChooser.getValue();
        if (winner == null) {
            Dialogs.error(context.window(), "No outcome was chosen",
                    "Choose which option turned out to be right before closing the event.");
            return;
        }
        if (!Dialogs.confirm(context.window(), "Close \"" + event.name() + "\"?",
                "\"" + winner + "\" will be declared the winner, every share of it will be paid "
                        + Formats.money(event.baseValue()) + " out of the event account, and the "
                        + "event will be locked. This cannot be undone.")) {
            return;
        }
        run(() -> {
            CloseEventResultDto result = context.engine().closeEvent(new CloseEventRequestDto(
                    user.name(), event.id(), optionIndexOf(winnerChooser)));
            return "\"" + result.eventName() + "\" is closed and \"" + result.winningOptionName()
                    + "\" won." + System.lineSeparator() + System.lineSeparator()
                    + Formats.shares(result.winningShares()) + " winning shares were worth "
                    + Formats.money(result.grossPayout()) + "." + System.lineSeparator()
                    + "Paid to the winners: " + Formats.money(result.amountPaidToWinners())
                    + System.lineSeparator()
                    + "Commission to the market maker: "
                    + Formats.money(result.commissionCollected()) + System.lineSeparator()
                    + "Returned to the market maker: "
                    + Formats.money(result.returnedToMarketMaker());
        });
    }

    /**
     * Carries out one action.
     *
     * <p>Everything that can go wrong arrives as an engine exception carrying a message written for
     * the person, so it is shown as it is. Anything that goes through reports what happened and
     * then tells the rest of the program that the system has moved, which is what brings both
     * screens back into step.
     *
     * <p>The report comes first and the refresh only afterwards, so that the new figures arrive on
     * a screen the person is actually looking at. Refreshing first would move every number while a
     * modal dialog is holding the person's attention, and the animations that mark what changed
     * would have played out and finished before the dialog was dismissed.
     */
    private void run(ActionWithReport action) {
        try {
            String report = action.perform();
            Dialogs.information(context.window(), "Done", report);
            context.reportSystemChanged();
        } catch (GuessMarketException refused) {
            Dialogs.error(context.window(), "That could not be done", refused.getMessage());
        }
    }

    /** One action, and the report to show once it has gone through. */
    @FunctionalInterface
    private interface ActionWithReport {
        String perform();
    }

    // ------------------------------------------------------------------ reading what was typed

    /**
     * @return the whole number that was typed, or null after telling the person what is wrong with
     *         it. The engine checks this again; this check exists so that a typing mistake is
     *         answered by the field it was made in rather than by an exception.
     */
    private Long wholeNumber(TextField field, String what) {
        String typed = field.getText() == null ? "" : field.getText().trim();
        if (typed.isEmpty()) {
            return invalid(field, "No number of " + what + " was given",
                    "Type how many " + what + " you want, as a whole number of at least 1.");
        }
        try {
            long value = Long.parseLong(typed);
            if (value < 1) {
                return invalid(field, "\"" + typed + "\" is not a usable number of " + what,
                        "The number of " + what + " must be at least 1.");
            }
            field.getStyleClass().remove("text-field-invalid");
            return value;
        } catch (NumberFormatException notANumber) {
            return invalid(field, "\"" + typed + "\" is not a number",
                    "Type the number of " + what + " as digits only, for example 50.");
        }
    }

    /** @return the price that was typed, or null after telling the person what is wrong with it. */
    private Double price(TextField field) {
        String typed = field.getText() == null ? "" : field.getText().trim();
        if (typed.isEmpty()) {
            return invalidPrice(field, "No price was given",
                    "Type what you are willing to pay, or want to receive, for one share.");
        }
        try {
            double value = Double.parseDouble(typed);
            field.getStyleClass().remove("text-field-invalid");
            return value;
        } catch (NumberFormatException notANumber) {
            return invalidPrice(field, "\"" + typed + "\" is not a price",
                    "Type a price such as 0.62, using a full stop for the decimal point.");
        }
    }

    private Long invalid(TextField field, String headline, String explanation) {
        markInvalid(field, headline, explanation);
        return null;
    }

    private Double invalidPrice(TextField field, String headline, String explanation) {
        markInvalid(field, headline, explanation);
        return null;
    }

    private void markInvalid(TextField field, String headline, String explanation) {
        if (!field.getStyleClass().contains("text-field-invalid")) {
            field.getStyleClass().add("text-field-invalid");
        }
        field.requestFocus();
        Dialogs.error(context.window(), headline, explanation);
    }

    /** Shows what a purchase would cost before it is made, straight from the engine. */
    private void updateQuote() {
        if (user == null || event == null || !event.isLmsr() || buyOptionChooser.getValue() == null) {
            return;
        }
        String typed = sharesField.getText() == null ? "" : sharesField.getText().trim();
        if (typed.isEmpty()) {
            quoteLabel.setText("Your balance is " + Formats.money(user.balance()) + ".");
            return;
        }
        try {
            long shares = Long.parseLong(typed);
            double cost = context.engine()
                    .quotePurchaseCost(event.id(), optionIndexOf(buyOptionChooser), shares);
            double commission = cost * event.commissionPercent() / 100.0;
            boolean onPurchase = event.commissionType() == guessmarket.dto.CommissionType.ON_PURCHASE;
            quoteLabel.setText(Formats.shares(shares) + " shares would cost "
                    + Formats.money(cost)
                    + (onPurchase ? " plus " + Formats.money(commission) + " commission, "
                        + Formats.money(cost + commission) + " in all" : "")
                    + ". Your balance is " + Formats.money(user.balance()) + ".");
        } catch (NumberFormatException | GuessMarketException cannotQuote) {
            quoteLabel.setText("Your balance is " + Formats.money(user.balance()) + ".");
        }
    }

    /** @return which option of the event a chooser is on, falling back to the first one. */
    private int optionIndexOf(ComboBox<String> chooser) {
        String chosen = chooser.getValue();
        return chosen == null ? 0 : Math.max(0, event.optionNames().indexOf(chosen));
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
