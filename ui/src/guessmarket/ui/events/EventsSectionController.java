package guessmarket.ui.events;

import guessmarket.dto.CommissionType;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventStatus;
import guessmarket.dto.TradingMethod;
import guessmarket.ui.app.AppContext;
import guessmarket.ui.app.AppSection;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tables;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * The events screen: every event in the system, filtered three ways, with the details of whichever
 * one is selected beside it.
 *
 * <p>The screen never acts on anything. Taking part in an event happens from the users screen,
 * because an action always belongs to somebody; what shows up here is the result of those actions,
 * which is why every refresh reads the engine again rather than keeping a copy of anything.
 *
 * <p>Each of the three filters is a set of toggle buttons with an "All" among them, and each set is
 * a toggle group, so exactly one of them is chosen at any moment and the filters cannot contradict
 * each other.
 */
public class EventsSectionController implements AppSection {

    @FXML private ToggleButton allTypesButton;
    @FXML private ToggleButton lmsrButton;
    @FXML private ToggleButton orderBookButton;
    @FXML private ToggleButton allStatusesButton;
    @FXML private ToggleButton notStartedButton;
    @FXML private ToggleButton activeButton;
    @FXML private ToggleButton closedButton;
    @FXML private ToggleButton allCommissionsButton;
    @FXML private ToggleButton onPurchaseButton;
    @FXML private ToggleButton onCloseButton;

    @FXML private TableView<EventDto> eventsTable;
    @FXML private Label countLabel;
    @FXML private Label placeholderLabel;
    @FXML private ScrollPane detailsScroll;
    @FXML private EventDetailsController eventDetailsController;

    private final ToggleGroup typeFilter = new ToggleGroup();
    private final ToggleGroup statusFilter = new ToggleGroup();
    private final ToggleGroup commissionFilter = new ToggleGroup();

    private AppContext context;

    /** Every event the engine last reported, before any filter is applied. */
    private final List<EventDto> loadedEvents = new ArrayList<>();

    @FXML
    private void initialize() {
        buildEventsTable();
        buildFilters();
        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, selected) -> showDetailsOf(selected));
        Tables.emptyMessage(eventsTable, "No event matches the filters that are switched on.");
    }

    @Override
    public void connect(AppContext appContext) {
        this.context = appContext;
    }

    @Override
    public void refresh() {
        int previouslySelected = selectedEventId();
        loadedEvents.clear();
        loadedEvents.addAll(context.engine().getAllEvents());
        applyFilters();
        reselect(previouslySelected);
    }

    @Override
    public void clear() {
        loadedEvents.clear();
        eventsTable.getItems().clear();
        countLabel.setText("");
        eventDetailsController.clear();
        showPlaceholder("Load a system details file to see the events.");
    }

    // ------------------------------------------------------------------ the list and its filters

    private void buildEventsTable() {
        Tables.columns(eventsTable,
                Tables.number("ID", 44, event -> String.valueOf(event.id())),
                Tables.text("NAME", 190, EventDto::name),
                Tables.text("STATUS", 95, event -> event.status().getDisplayName()),
                Tables.text("TYPE", 100, event -> event.tradingMethod().getDisplayName()),
                Tables.text("COMMISSION", 125, EventsSectionController::describeCommission),
                Tables.number("ACCOUNT", 100, event -> Formats.money(event.accountBalance())),
                Tables.text("MARKET MAKER", 120, EventDto::marketMakerName));
    }

    /** @return the commission of an event as one phrase: how much, and when it is taken. */
    private static String describeCommission(EventDto event) {
        return Formats.percent(event.commissionPercent()) + " "
                + event.commissionType().getDisplayName().toLowerCase();
    }

    /**
     * Puts each row of filter buttons into a group of its own and remembers what each button
     * stands for. An "All" button stands for nothing in particular, which is exactly what makes it
     * match everything.
     */
    private void buildFilters() {
        group(typeFilter, allTypesButton, null);
        group(typeFilter, lmsrButton, TradingMethod.LMSR);
        group(typeFilter, orderBookButton, TradingMethod.ORDER_BOOK);

        group(statusFilter, allStatusesButton, null);
        group(statusFilter, notStartedButton, EventStatus.NOT_STARTED);
        group(statusFilter, activeButton, EventStatus.ACTIVE);
        group(statusFilter, closedButton, EventStatus.CLOSED);

        group(commissionFilter, allCommissionsButton, null);
        group(commissionFilter, onPurchaseButton, CommissionType.ON_PURCHASE);
        group(commissionFilter, onCloseButton, CommissionType.ON_CLOSE);

        keepOneChosen(typeFilter, allTypesButton);
        keepOneChosen(statusFilter, allStatusesButton);
        keepOneChosen(commissionFilter, allCommissionsButton);
    }

    private static void group(ToggleGroup toggleGroup, ToggleButton button, Object stands) {
        button.setToggleGroup(toggleGroup);
        button.setUserData(stands);
    }

    /**
     * A toggle group lets its chosen button be switched off again, which would leave a filter row
     * with nothing chosen and no way to tell what it means. Clicking the chosen button therefore
     * falls back to "All" rather than to nothing.
     */
    private void keepOneChosen(ToggleGroup toggleGroup, ToggleButton fallback) {
        toggleGroup.selectedToggleProperty().addListener((observable, previous, chosen) -> {
            if (chosen == null) {
                fallback.setSelected(true);
            } else {
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        int previouslySelected = selectedEventId();
        ObservableList<EventDto> shown = FXCollections.observableArrayList();
        for (EventDto event : loadedEvents) {
            if (matchesFilters(event)) {
                shown.add(event);
            }
        }
        eventsTable.setItems(shown);
        countLabel.setText(describeCount(shown.size(), loadedEvents.size()));
        if (loadedEvents.isEmpty()) {
            showPlaceholder("Load a system details file to see the events.");
        } else if (shown.isEmpty()) {
            eventDetailsController.clear();
            showPlaceholder("No event matches the filters that are switched on.");
        }
        reselect(previouslySelected);
    }

    private boolean matchesFilters(EventDto event) {
        return matches(typeFilter, event.tradingMethod())
                && matches(statusFilter, event.status())
                && matches(commissionFilter, event.commissionType());
    }

    /** @return whether the chosen button of a filter row lets this value through. */
    private static boolean matches(ToggleGroup toggleGroup, Object value) {
        Toggle chosen = toggleGroup.getSelectedToggle();
        Object wanted = chosen == null ? null : chosen.getUserData();
        return wanted == null || wanted.equals(value);
    }

    private static String describeCount(int shown, int total) {
        if (total == 0) {
            return "";
        }
        return shown == total
                ? shown + (shown == 1 ? " event" : " events")
                : "Showing " + shown + " of " + total + " events";
    }

    // ------------------------------------------------------------------ the details beside it

    private void showDetailsOf(EventDto event) {
        if (event == null) {
            eventDetailsController.clear();
            showPlaceholder(loadedEvents.isEmpty()
                    ? "Load a system details file to see the events."
                    : "Choose an event on the left to see everything about it.");
            return;
        }
        placeholderLabel.setVisible(false);
        placeholderLabel.setManaged(false);
        eventDetailsController.show(context.engine().getEventTradingStatus(event.id()),
                context.engine().getEventPriceHistory(event.id()));
    }

    private void showPlaceholder(String message) {
        placeholderLabel.setText(message);
        placeholderLabel.setVisible(true);
        placeholderLabel.setManaged(true);
    }

    /** @return the id of the selected event, or -1 when nothing is selected. */
    private int selectedEventId() {
        EventDto selected = eventsTable.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.id();
    }

    /**
     * Puts the selection back on the event it was on, now that the rows are new objects. Without
     * this, every refresh would throw the person out of whatever they were looking at.
     */
    private void reselect(int eventId) {
        if (eventId < 0) {
            return;
        }
        for (EventDto event : eventsTable.getItems()) {
            if (event.id() == eventId) {
                eventsTable.getSelectionModel().select(event);
                showDetailsOf(event);
                return;
            }
        }
        eventsTable.getSelectionModel().clearSelection();
        showDetailsOf(null);
    }
}
