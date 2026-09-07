package guessmarket.ui.users;

import guessmarket.dto.BalancePointDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.UserDto;
import guessmarket.dto.UserEventInvolvementDto;
import guessmarket.ui.app.AppContext;
import guessmarket.ui.app.AppSection;
import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Tiles;
import guessmarket.ui.events.EventDetailsController;
import guessmarket.ui.events.EventTile;
import guessmarket.ui.trade.CreateEventController;
import guessmarket.ui.trade.TradePanelController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The users screen: everybody in the system, and everything one of them can see and do.
 *
 * <p>It is the only screen that changes anything, and it does so through the trade panel, which
 * always acts as the user selected on the left. Underneath that panel sits the very same event
 * details component the events screen uses, so the effect of an action appears immediately in the
 * picture the rest of the system sees.
 *
 * <p>Nothing here is kept between refreshes. Every action reports that the system has moved, the
 * application controller asks both screens to rebuild, and this screen reads the engine again and
 * puts the selection back where it was.
 *
 * <p>Both lists are columns of tiles rather than tables. A user is read as a name and a balance
 * and an event as a name and a state, and a tile can put those first and let everything else fall
 * in behind them, which is what keeps both lists readable when the window is made narrow.
 */
public class UsersSectionController implements AppSection {

    @FXML private ListView<UserDto> usersList;
    @FXML private Label userCountLabel;
    @FXML private Label placeholderLabel;
    @FXML private ScrollPane detailsScroll;
    @FXML private VBox detailsBox;

    @FXML private Label userNameLabel;
    @FXML private Label blockedBadge;
    @FXML private Label balanceLabel;
    @FXML private Label initialBalanceLabel;
    @FXML private Label netResultLabel;
    @FXML private Label runsLabel;
    @FXML private Label participatesLabel;
    @FXML private Button createEventButton;

    @FXML private VBox balanceChartBox;
    @FXML private LineChart<Number, Number> balanceChart;

    @FXML private ToggleButton allEventsButton;
    @FXML private ToggleButton myEventsButton;
    @FXML private ListView<EventDto> eventsList;

    @FXML private UserInvolvementController userInvolvementController;
    @FXML private TradePanelController tradePanelController;
    @FXML private EventDetailsController eventDetailsController;

    private final ToggleGroup eventScope = new ToggleGroup();

    private AppContext context;

    /** Every event the engine last reported, before the "only mine" filter is applied. */
    private final List<EventDto> loadedEvents = new ArrayList<>();

    @FXML
    private void initialize() {
        Tiles.render(usersList, UserTile::of);
        Tiles.render(eventsList, event -> EventTile.of(event, roleOf(event)));

        allEventsButton.setToggleGroup(eventScope);
        myEventsButton.setToggleGroup(eventScope);
        eventScope.selectedToggleProperty().addListener((observable, previous, chosen) -> {
            if (chosen == null) {
                allEventsButton.setSelected(true);
            } else {
                showEventsOfSelectedUser();
            }
        });

        usersList.getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, selected) -> showUser(selected));
        eventsList.getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, selected) -> showEvent(selected));

        Tiles.emptyMessage(usersList, "No users have been loaded.");
        Tiles.emptyMessage(eventsList, "This user does not run or take part in any event yet.");
    }

    @Override
    public void connect(AppContext appContext) {
        this.context = appContext;
        tradePanelController.connect(appContext);
    }

    @Override
    public void refresh() {
        String previousUser = selectedUserName();
        int previousEvent = selectedEventId();

        usersList.setItems(FXCollections.observableArrayList(context.engine().getAllUsers()));
        userCountLabel.setText(describeCount(usersList.getItems().size()));
        loadedEvents.clear();
        loadedEvents.addAll(context.engine().getAllEvents());

        reselectUser(previousUser);
        reselectEvent(previousEvent);
    }

    @Override
    public void clear() {
        loadedEvents.clear();
        usersList.getItems().clear();
        eventsList.getItems().clear();
        userCountLabel.setText("");
        showDetails(false);
        placeholderLabel.setText("Load a system details file to see the users.");
        showNode(placeholderLabel, true);
    }

    // ------------------------------------------------------------------ the list of users

    private static String describeCount(int users) {
        return users == 0 ? "" : users + (users == 1 ? " user" : " users");
    }

    // ------------------------------------------------------------------ one user

    private void showUser(UserDto user) {
        if (user == null) {
            showDetails(false);
            placeholderLabel.setText(usersList.getItems().isEmpty()
                    ? "Load a system details file to see the users."
                    : "Choose a user on the left to see their account and to act as them.");
            showNode(placeholderLabel, true);
            return;
        }
        showNode(placeholderLabel, false);
        showDetails(true);

        userNameLabel.setText(user.name());
        balanceLabel.setText(Formats.money(user.balance()));
        initialBalanceLabel.setText(Formats.money(user.initialBalance()));
        netResultLabel.setText(Formats.signedMoney(user.netResult()));
        netResultLabel.getStyleClass().setAll("value", Formats.resultStyle(user.netResult()));
        runsLabel.setText(Formats.count(user.marketMakerEventIds().size(), "event"));
        participatesLabel.setText(Formats.count(user.participatingEventIds().size(), "event"));

        showNode(blockedBadge, user.blocked());
        createEventButton.setDisable(user.blocked());
        showBalanceChart(user);
        showEventsOfSelectedUser();
    }

    /**
     * Opens the form for creating an event, with the selected user as its market maker. Whatever
     * comes out of it is an ordinary event that has not started yet, so nothing else here has to
     * know that it was not in the file.
     */
    @FXML
    private void onCreateEvent() {
        UserDto user = usersList.getSelectionModel().getSelectedItem();
        if (user != null) {
            CreateEventController.open(context, user.name());
        }
    }

    private void showBalanceChart(UserDto user) {
        List<BalancePointDto> history = context.engine().getUserBalanceHistory(user.name());
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(user.name());
        for (BalancePointDto point : history) {
            series.getData().add(new XYChart.Data<>(point.step(), point.balance()));
        }
        balanceChart.getData().setAll(List.of(series));
        showNode(balanceChartBox, history.size() > 1);
    }

    // ------------------------------------------------------------------ the events of that user

    /**
     * Works out what the selected user is to an event.
     *
     * <p>This is read as a tile is drawn rather than kept anywhere, so the badge on an event
     * belongs to whoever is selected at the moment the tile appears, and choosing another user
     * rebuilds the list and with it every badge on it.
     *
     * @return its market maker, one of its participants, or nothing at all
     */
    private EventTile.Role roleOf(EventDto event) {
        UserDto user = usersList.getSelectionModel().getSelectedItem();
        if (user == null) {
            return EventTile.Role.NONE;
        }
        if (event.marketMakerName().equalsIgnoreCase(user.name())) {
            return EventTile.Role.MARKET_MAKER;
        }
        return user.participatingEventIds().contains(event.id())
                ? EventTile.Role.PARTICIPANT
                : EventTile.Role.NONE;
    }

    /**
     * Fills the events table for whoever is selected.
     *
     * <p>Every event is offered, not only the ones this user is already in, because taking part in
     * a new event has to start somewhere. The "Only mine" button narrows it to what the assignment
     * asks to be shown: the events they run and the events they are already taking part in.
     */
    private void showEventsOfSelectedUser() {
        UserDto user = usersList.getSelectionModel().getSelectedItem();
        if (user == null) {
            eventsList.getItems().clear();
            return;
        }
        int previousEvent = selectedEventId();
        boolean onlyMine = myEventsButton.isSelected();
        ObservableList<EventDto> shown = FXCollections.observableArrayList();
        for (EventDto event : loadedEvents) {
            if (!onlyMine || isInvolvedIn(user, event)) {
                shown.add(event);
            }
        }
        eventsList.setItems(shown);
        reselectEvent(previousEvent);
    }

    private static boolean isInvolvedIn(UserDto user, EventDto event) {
        return event.marketMakerName().equalsIgnoreCase(user.name())
                || user.participatingEventIds().contains(event.id());
    }

    private void showEvent(EventDto event) {
        UserDto user = usersList.getSelectionModel().getSelectedItem();
        if (user == null || event == null) {
            userInvolvementController.clear();
            tradePanelController.clear();
            eventDetailsController.clear();
            return;
        }
        UserEventInvolvementDto involvement =
                context.engine().getUserInvolvement(user.name(), event.id());
        EventTradingStatusDto status = context.engine().getEventTradingStatus(event.id());

        userInvolvementController.show(involvement);
        tradePanelController.show(user, status);
        eventDetailsController.show(status, context.engine().getEventPriceHistory(event.id()));
    }

    // ------------------------------------------------------------------ keeping the selection

    private String selectedUserName() {
        UserDto selected = usersList.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.name();
    }

    private int selectedEventId() {
        EventDto selected = eventsList.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.id();
    }

    /** Puts the selection back on the same user, now that every tile is a new object. */
    private void reselectUser(String userName) {
        if (userName == null) {
            showUser(null);
            return;
        }
        for (UserDto user : usersList.getItems()) {
            if (user.name().equalsIgnoreCase(userName)) {
                usersList.getSelectionModel().select(user);
                showUser(user);
                return;
            }
        }
        usersList.getSelectionModel().clearSelection();
        showUser(null);
    }

    private void reselectEvent(int eventId) {
        if (eventId < 0) {
            return;
        }
        for (EventDto event : eventsList.getItems()) {
            if (event.id() == eventId) {
                eventsList.getSelectionModel().select(event);
                showEvent(event);
                return;
            }
        }
        eventsList.getSelectionModel().clearSelection();
        showEvent(null);
    }

    private void showDetails(boolean visible) {
        boolean wasHidden = !detailsBox.isVisible();
        showNode(detailsBox, visible);
        if (visible && wasHidden) {
            Animations.reveal(detailsBox);
        }
    }

    private static void showNode(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
