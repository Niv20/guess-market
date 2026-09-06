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
import guessmarket.ui.common.Tables;
import guessmarket.ui.events.EventDetailsController;
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
import javafx.scene.control.TableView;
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
 */
public class UsersSectionController implements AppSection {

    @FXML private TableView<UserDto> usersTable;
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
    @FXML private TableView<EventDto> eventsTable;

    @FXML private UserInvolvementController userInvolvementController;
    @FXML private TradePanelController tradePanelController;
    @FXML private EventDetailsController eventDetailsController;

    private final ToggleGroup eventScope = new ToggleGroup();

    private AppContext context;

    /** Every event the engine last reported, before the "only mine" filter is applied. */
    private final List<EventDto> loadedEvents = new ArrayList<>();

    @FXML
    private void initialize() {
        buildUsersTable();
        buildEventsTable();

        allEventsButton.setToggleGroup(eventScope);
        myEventsButton.setToggleGroup(eventScope);
        eventScope.selectedToggleProperty().addListener((observable, previous, chosen) -> {
            if (chosen == null) {
                allEventsButton.setSelected(true);
            } else {
                showEventsOfSelectedUser();
            }
        });

        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, selected) -> showUser(selected));
        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, selected) -> showEvent(selected));

        Tables.emptyMessage(usersTable, "No users have been loaded.");
        Tables.emptyMessage(eventsTable, "This user does not run or take part in any event yet.");
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

        usersTable.setItems(FXCollections.observableArrayList(context.engine().getAllUsers()));
        userCountLabel.setText(describeCount(usersTable.getItems().size()));
        loadedEvents.clear();
        loadedEvents.addAll(context.engine().getAllEvents());

        reselectUser(previousUser);
        reselectEvent(previousEvent);
    }

    @Override
    public void clear() {
        loadedEvents.clear();
        usersTable.getItems().clear();
        eventsTable.getItems().clear();
        userCountLabel.setText("");
        showDetails(false);
        placeholderLabel.setText("Load a system details file to see the users.");
        showNode(placeholderLabel, true);
    }

    // ------------------------------------------------------------------ the list of users

    private void buildUsersTable() {
        Tables.columns(usersTable,
                Tables.text("NAME", 120, UserDto::name),
                Tables.number("BALANCE", 100, user -> Formats.money(user.balance())),
                Tables.number("RESULT", 95, user -> Formats.signedMoney(user.netResult())),
                Tables.text("STATUS", 80, user -> user.blocked() ? "Blocked" : "Active"));
        // A blocked user is greyed out, because nothing about them can change any more.
        usersTable.setRowFactory(table -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(UserDto user, boolean empty) {
                super.updateItem(user, empty);
                getStyleClass().remove("row-inactive");
                if (!empty && user != null && user.blocked()) {
                    getStyleClass().add("row-inactive");
                }
            }
        });
    }

    private static String describeCount(int users) {
        return users == 0 ? "" : users + (users == 1 ? " user" : " users");
    }

    // ------------------------------------------------------------------ one user

    private void showUser(UserDto user) {
        if (user == null) {
            showDetails(false);
            placeholderLabel.setText(usersTable.getItems().isEmpty()
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
        netResultLabel.getStyleClass().setAll("value",
                user.netResult() < 0 ? "value-negative" : "value-positive");
        runsLabel.setText(countOf(user.marketMakerEventIds().size(), "event"));
        participatesLabel.setText(countOf(user.participatingEventIds().size(), "event"));

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
        UserDto user = usersTable.getSelectionModel().getSelectedItem();
        if (user != null) {
            CreateEventController.open(context, user.name());
        }
    }

    private static String countOf(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
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

    private void buildEventsTable() {
        Tables.columns(eventsTable,
                Tables.number("ID", 44, event -> String.valueOf(event.id())),
                Tables.text("NAME", 180, EventDto::name),
                Tables.text("STATUS", 95, event -> event.status().getDisplayName()),
                Tables.text("TYPE", 100, event -> event.tradingMethod().getDisplayName()),
                Tables.text("ROLE", 115, this::describeRole));
    }

    /** @return what the selected user is to an event: its market maker, a participant, or nothing. */
    private String describeRole(EventDto event) {
        UserDto user = usersTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            return Formats.NOTHING;
        }
        if (event.marketMakerName().equalsIgnoreCase(user.name())) {
            return "Market maker";
        }
        return user.participatingEventIds().contains(event.id()) ? "Taking part" : Formats.NOTHING;
    }

    /**
     * Fills the events table for whoever is selected.
     *
     * <p>Every event is offered, not only the ones this user is already in, because taking part in
     * a new event has to start somewhere. The "Only mine" button narrows it to what the assignment
     * asks to be shown: the events they run and the events they are already taking part in.
     */
    private void showEventsOfSelectedUser() {
        UserDto user = usersTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            eventsTable.getItems().clear();
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
        eventsTable.setItems(shown);
        reselectEvent(previousEvent);
    }

    private static boolean isInvolvedIn(UserDto user, EventDto event) {
        return event.marketMakerName().equalsIgnoreCase(user.name())
                || user.participatingEventIds().contains(event.id());
    }

    private void showEvent(EventDto event) {
        UserDto user = usersTable.getSelectionModel().getSelectedItem();
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
        UserDto selected = usersTable.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.name();
    }

    private int selectedEventId() {
        EventDto selected = eventsTable.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.id();
    }

    /** Puts the selection back on the same user, now that every row is a new object. */
    private void reselectUser(String userName) {
        if (userName == null) {
            showUser(null);
            return;
        }
        for (UserDto user : usersTable.getItems()) {
            if (user.name().equalsIgnoreCase(userName)) {
                usersTable.getSelectionModel().select(user);
                showUser(user);
                return;
            }
        }
        usersTable.getSelectionModel().clearSelection();
        showUser(null);
    }

    private void reselectEvent(int eventId) {
        if (eventId < 0) {
            return;
        }
        for (EventDto event : eventsTable.getItems()) {
            if (event.id() == eventId) {
                eventsTable.getSelectionModel().select(event);
                showEvent(event);
                return;
            }
        }
        eventsTable.getSelectionModel().clearSelection();
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
