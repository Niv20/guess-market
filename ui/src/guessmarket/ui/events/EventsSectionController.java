package guessmarket.ui.events;

import guessmarket.dto.CommissionType;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventStatus;
import guessmarket.dto.TradingMethod;
import guessmarket.ui.app.AppContext;
import guessmarket.ui.app.AppSection;
import guessmarket.ui.common.Filters;
import guessmarket.ui.common.Filters.Choice;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tables;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;

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
 * <p>Each of the three filters is a dropdown whose first line is "All", so exactly one thing is
 * chosen in each of them at any moment, the filters cannot contradict each other, and the row of
 * filters stays the same size however many values a filter has to offer.
 */
public class EventsSectionController implements AppSection {

    @FXML private ComboBox<Choice<TradingMethod>> typeChooser;
    @FXML private ComboBox<Choice<EventStatus>> statusChooser;
    @FXML private ComboBox<Choice<CommissionType>> commissionChooser;

    @FXML private TableView<EventDto> eventsTable;
    @FXML private Label countLabel;
    @FXML private Label placeholderLabel;
    @FXML private ScrollPane detailsScroll;
    @FXML private EventDetailsController eventDetailsController;

    private AppContext context;

    /** Every event the engine last reported, before any filter is applied. */
    private final List<EventDto> loadedEvents = new ArrayList<>();

    @FXML
    private void initialize() {
        buildEventsTable();
        buildFilters();
        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, selected) -> showDetailsOf(selected));
        Tables.emptyMessage(eventsTable, "No event matches the filters that are chosen.");
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
     * Offers every value of each of the three things an event can be filtered by, with an "All"
     * above them. Every filter starts on its "All", so the screen opens showing the whole system.
     */
    private void buildFilters() {
        Filters.fill(typeChooser, TradingMethod.values(), this::applyFilters);
        Filters.fill(statusChooser, EventStatus.values(), this::applyFilters);
        Filters.fill(commissionChooser, CommissionType.values(), this::applyFilters);
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
            showPlaceholder("No event matches the filters that are chosen.");
        }
        reselect(previouslySelected);
    }

    private boolean matchesFilters(EventDto event) {
        return Filters.allows(typeChooser, event.tradingMethod())
                && Filters.allows(statusChooser, event.status())
                && Filters.allows(commissionChooser, event.commissionType());
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
