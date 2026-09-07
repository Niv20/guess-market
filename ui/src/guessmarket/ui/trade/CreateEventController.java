package guessmarket.ui.trade;

import guessmarket.dto.CommissionType;
import guessmarket.dto.CreateEventRequestDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.TradingMethod;
import guessmarket.dto.UserDto;
import guessmarket.engine.exception.GuessMarketException;
import guessmarket.ui.app.AppContext;
import guessmarket.ui.common.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * The form for creating an event from scratch, in a window of its own.
 *
 * <p>Whoever is chosen as market maker takes on everything that comes with it: paying to open the
 * event, receiving its commission and deciding its outcome. The event that comes out is in no way
 * special. It starts not yet running, it appears in both screens like any other, and it has to be
 * opened before anybody can trade in it.
 *
 * <p>The market maker is asked for on the form. The form is opened from the events screen, which
 * is a list of events and has nobody selected, so there is no user to assume; and asking for it
 * here puts it in the same place as everything else that has to be decided about the event.
 *
 * <p>The form checks nothing itself beyond turning what was typed into numbers. Whether those
 * numbers describe an event that can exist is the engine's decision, and its refusal is shown to
 * the person word for word.
 */
public class CreateEventController {

    private static final String LAYOUT = "/guessmarket/ui/trade/create-event.fxml";

    @FXML private VBox rootPane;
    @FXML private Label creatorLabel;
    @FXML private ComboBox<String> creatorChooser;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField firstOptionField;
    @FXML private TextField secondOptionField;
    @FXML private TextField commissionField;
    @FXML private ComboBox<CommissionType> commissionTypeChooser;
    @FXML private ComboBox<TradingMethod> methodChooser;

    @FXML private VBox lmsrRow;
    @FXML private TextField liquidityField;
    @FXML private Label lmsrHintLabel;

    @FXML private VBox orderBookRows;
    @FXML private TextField investmentField;
    @FXML private TextField baseValueField;
    @FXML private CheckBox mintCheckBox;
    @FXML private Label orderBookHintLabel;

    private AppContext context;
    private Stage window;

    /**
     * Opens the form and waits for it to be closed.
     *
     * @param context how to reach the engine and the main window
     */
    public static void open(AppContext context) {
        try {
            URL layout = CreateEventController.class.getResource(LAYOUT);
            if (layout == null) {
                throw new IOException("the layout file " + LAYOUT + " is missing");
            }
            FXMLLoader loader = new FXMLLoader(layout);
            Parent root = loader.load();
            CreateEventController controller = loader.getController();
            controller.start(context, root);
        } catch (IOException cannotOpen) {
            Dialogs.error(context.window(), "The form could not be opened",
                    "The window for creating an event could not be built: "
                            + cannotOpen.getMessage()
                            + ". The program was probably not packaged correctly.");
        }
    }

    @FXML
    private void initialize() {
        commissionTypeChooser.getItems().setAll(CommissionType.values());
        commissionTypeChooser.setValue(CommissionType.ON_PURCHASE);
        methodChooser.getItems().setAll(TradingMethod.values());
        methodChooser.valueProperty().addListener((observable, old, chosen) -> showSettingsFor(chosen));
        methodChooser.setValue(TradingMethod.LMSR);

        lmsrHintLabel.setText("The larger b is, the less each purchase moves the price, and the "
                + "more the market maker has to put up to open the event: the subsidy is b x ln 2.");
        orderBookHintLabel.setText("The opening investment buys one pair of shares for every base "
                + "value, and those shares become the market maker's. Minting lets two buyers of "
                + "opposite options create new shares between them.");
        mintCheckBox.setSelected(true);
    }

    /** Builds the window around the loaded form and shows it. */
    private void start(AppContext appContext, Parent root) {
        this.context = appContext;
        creatorLabel.setText("Whoever is chosen below becomes the market maker of this event, and "
                + "will have to open it, pay for it and decide its outcome.");
        for (UserDto user : appContext.engine().getAllUsers()) {
            // A blocked user may do nothing at all, and running an event is a great deal to do.
            if (!user.blocked()) {
                creatorChooser.getItems().add(user.name());
            }
        }

        window = new Stage();
        window.setTitle("Guess Market - create an event");
        window.initOwner(appContext.window());
        window.initModality(Modality.WINDOW_MODAL);

        Scene scene = new Scene(root);
        Window owner = appContext.window();
        if (owner != null && owner.getScene() != null) {
            // The new window has a scene of its own and does not inherit the skin, so it is dressed
            // in whatever the main window is currently wearing.
            scene.getStylesheets().setAll(owner.getScene().getStylesheets());
        }
        window.setScene(scene);
        window.showAndWait();
    }

    private void showSettingsFor(TradingMethod method) {
        show(lmsrRow, method == TradingMethod.LMSR);
        show(orderBookRows, method == TradingMethod.ORDER_BOOK);
        resizeToFit();
    }

    /** Resizes the window to whatever the chosen method needs, once it is on the screen. */
    private void resizeToFit() {
        if (window != null && window.isShowing()) {
            window.sizeToScene();
        }
    }

    @FXML
    private void onCancel() {
        window.close();
    }

    @FXML
    private void onCreate() {
        String creatorName = creatorChooser.getValue();
        if (creatorName == null) {
            Dialogs.error(context.window(), "The market maker is missing",
                    "Choose which user is creating this event. They will pay to open it and will "
                            + "decide its outcome.");
            creatorChooser.requestFocus();
            return;
        }

        Integer commission = wholeNumber(commissionField, "commission percentage");
        if (commission == null) {
            return;
        }
        boolean lmsr = methodChooser.getValue() == TradingMethod.LMSR;
        Integer liquidity = lmsr ? wholeNumber(liquidityField, "liquidity parameter b") : 0;
        Integer investment = lmsr ? 0 : wholeNumber(investmentField, "opening investment");
        Integer baseValue = lmsr ? 1 : wholeNumber(baseValueField, "base value d");
        if (liquidity == null || investment == null || baseValue == null) {
            return;
        }

        try {
            EventDto created = context.engine().createEvent(new CreateEventRequestDto(
                    creatorName,
                    nameField.getText(),
                    descriptionField.getText(),
                    commission,
                    commissionTypeChooser.getValue(),
                    List.of(text(firstOptionField), text(secondOptionField)),
                    methodChooser.getValue(),
                    liquidity,
                    investment,
                    baseValue,
                    mintCheckBox.isSelected()));
            context.reportSystemChanged();
            window.close();
            Dialogs.information(context.window(), "The event was created",
                    "\"" + created.name() + "\" was created with id " + created.id() + ", and "
                            + creatorName + " is its market maker. It has not started yet: open it "
                            + "from the users screen when you are ready to trade in it.");
        } catch (GuessMarketException refused) {
            Dialogs.error(context.window(), "The event could not be created", refused.getMessage());
        }
    }

    /**
     * @return the whole number that was typed, or null after telling the person what is wrong. The
     *         engine checks the value itself; this only turns the text into a number.
     */
    private Integer wholeNumber(TextField field, String what) {
        String typed = text(field);
        if (typed.isEmpty()) {
            Dialogs.error(context.window(), "The " + what + " is missing",
                    "Type the " + what + " as a whole number.");
            field.requestFocus();
            return null;
        }
        try {
            return Integer.valueOf(typed);
        } catch (NumberFormatException notANumber) {
            Dialogs.error(context.window(), "\"" + typed + "\" is not a number",
                    "Type the " + what + " as digits only, with no decimal point.");
            field.requestFocus();
            return null;
        }
    }

    private static String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
