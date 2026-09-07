package guessmarket.ui.events;

import guessmarket.dto.CommissionType;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventStatus;
import guessmarket.dto.TradingMethod;
import guessmarket.ui.app.AppContext;
import guessmarket.ui.app.AppSection;
import guessmarket.ui.common.Filters;
import guessmarket.ui.common.Filters.Choice;
import guessmarket.ui.common.Tiles;
import guessmarket.ui.trade.CreateEventController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;

import java.util.ArrayList;
import java.util.List;

/**
 * The events screen: every event in the system, filtered three ways, with the details of whichever
 * one is selected beside it.
 *
 * <p>The one thing done from here is creating an event, which is the one action that makes an
 * event rather than doing something to one. Everything else belongs to somebody and so belongs on
 * the users screen, where it is plain whose money is moving; what shows up here is the result of
 * those actions, which is why every refresh reads the engine again rather than keeping a copy of
 * anything.
 *
 * <p>Each of the three filters is a dropdown whose first line is "All", so exactly one thing is
 * chosen in each of them at any moment, the filters cannot contradict each other, and the row of
 * filters stays the same size however many values a filter has to offer.
 *
 * <p>The events themselves are a column of tiles rather than a table. This half of the screen is
 * the narrow one and it is meant to be dragged narrower still, and a table would answer that by
 * hiding whichever columns no longer fit, which are the columns furthest to the right rather than
 * the ones that matter least. A tile decides that question once, by how it is laid out, and then
 * gives the same answer at every width.
 */
public class EventsSectionController implements AppSection {

    @FXML private ComboBox<Choice<TradingMethod>> typeChooser;
    @FXML private ComboBox<Choice<EventStatus>> statusChooser;
    @FXML private ComboBox<Choice<CommissionType>> commissionChooser;

    @FXML private Button createEventButton;
    @FXML private ListView<EventDto> eventsList;
    @FXML private Label countLabel;
    @FXML private Label placeholderLabel;
    @FXML private ScrollPane detailsScroll;
    @FXML private EventDetailsController eventDetailsController;

    private AppContext context;

    /** Every event the engine last reported, before any filter is applied. */
    private final List<EventDto> loadedEvents = new ArrayList<>();

    @FXML
    private void initialize() {
        Tiles.render(eventsList, EventTile::of);
        buildFilters();
        eventsList.getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, selected) -> showDetailsOf(selected));
        Tiles.emptyMessage(eventsList, "No event matches the filters that are chosen.");
        createEventButton.setDisable(true);
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
        // An event must have a market maker, so there is nothing to create until there is somebody
        // who could be one.
        createEventButton.setDisable(context.engine().getAllUsers().isEmpty());
        applyFilters();
        reselect(previouslySelected);
    }

    @Override
    public void clear() {
        loadedEvents.clear();
        eventsList.getItems().clear();
        createEventButton.setDisable(true);
        countLabel.setText("");
        eventDetailsController.clear();
        showPlaceholder("Load a system details file to see the events.");
    }

    /**
     * Opens the form for creating an event. Whatever comes out of it is an ordinary event that has
     * not started yet, so nothing else here has to know that it was not in the file.
     */
    @FXML
    private void onCreateEvent() {
        CreateEventController.open(context);
    }

    // ------------------------------------------------------------------ the list and its filters

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
        eventsList.setItems(shown);
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
        EventDto selected = eventsList.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.id();
    }

    /**
     * Puts the selection back on the event it was on, now that the tiles are new objects. Without
     * this, every refresh would throw the person out of whatever they were looking at.
     */
    private void reselect(int eventId) {
        if (eventId < 0) {
            return;
        }
        for (EventDto event : eventsList.getItems()) {
            if (event.id() == eventId) {
                eventsList.getSelectionModel().select(event);
                showDetailsOf(event);
                return;
            }
        }
        eventsList.getSelectionModel().clearSelection();
        showDetailsOf(null);
    }
}
