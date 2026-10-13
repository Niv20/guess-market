package guessmarket.client;

import guessmarket.dto.*;
import guessmarket.dto.network.*;
import guessmarket.ui.common.*;
import guessmarket.ui.common.Skin;
import guessmarket.ui.events.EventDetailsController;
import guessmarket.ui.events.EventTile;
import guessmarket.ui.users.UserInvolvementController;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Exercise 3's private session UI. All displayed market/account data are server DTOs. The event
 * details, option books, participants, history, charts and private involvement components are the
 * same engine-independent FXML components used by Exercise 2.
 */
public final class GuessMarketClient extends Application implements ClientSession.Listener {
    private static final double DEFAULT_HORIZONTAL_DIVIDER_POSITION = 0.36;

    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final BorderPane root = new BorderPane();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final TextField username = new TextField();
    private final TextField server = new TextField(ApiClient.DEFAULT_SERVER);
    private final Button login = new Button("Join Guess Market");
    private final ListView<EventDto> events = new ListView<>();
    private final TextField search = new TextField();
    private final ComboBox<EventStatus> status = new ComboBox<>();
    private final ComboBox<TradingMethod> method = new ComboBox<>();
    private final ComboBox<CommissionType> commission = new ComboBox<>();
    private final TextField uploadPath = new TextField();
    private final Label eventCount = label("", "faint");
    private final Label chooseEvent = label("Choose an event to see its market and trade.", "lead");
    private final ComboBox<EventDto> myEvents = new ComboBox<>();
    private final Label accountName = label("", "screen-title");
    private final Label accountBalance = label("—", "value", "value-hero", "value-accent");
    private final Label accountState = label("", "muted");
    private final TableView<AccountEntryDto> ledger = new TableView<>();
    private final TabPane tabs = new TabPane();
    private final NumberAxis balanceAxis = new NumberAxis();
    private final NumberAxis stepAxis = new NumberAxis();
    private final LineChart<Number, Number> balanceChart = new LineChart<>(stepAxis, balanceAxis);
    private Stage stage;
    private Scene scene;
    private ClientSession session;
    private Parent signedIn;
    private EventDetailsController details;
    private UserInvolvementController accountInvolvement;
    private EventDetailsController accountDetails;
    private TradeActions accountTrade;
    private EventTradingStatusDto lastAccountDetails;
    private List<PricePointDto> lastAccountPrices;
    private TradeActions trade;
    private SnapshotDto snapshot;
    private EventTradingStatusDto lastDetails;
    private List<PricePointDto> lastPrices;
    private UserEventInvolvementDto lastInvolvement;
    private boolean rebuilding;
    private String signedName;
    private ChatDrawer chatDrawer;
    private ProfileMenu profileMenu;
    private boolean connectionInterrupted;

    @Override public void start(Stage primaryStage) throws IOException {
        stage = primaryStage;
        session = new ClientSession(this);
        Map<String, String> parameters = getParameters() == null ? Map.of() : getParameters().getNamed();
        server.setText(parameters.getOrDefault("server", System.getProperty("guessmarket.server", ApiClient.DEFAULT_SERVER)));
        scene = new Scene(root, 1240, 850);
        Skin.applyDefault(scene);
        signedIn = buildSignedIn();
        showLogin("");
        stage.setTitle("Guess Market · Exercise 3");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(650);
        stage.setOnCloseRequest(event -> {
            if (signedName != null && !confirmLeave()) event.consume();
        });
        stage.show();
    }

    private void showLogin(String reason) {
        chatDrawer.reset();
        profileMenu.reset();
        connectionInterrupted = false;
        snapshot = null;
        signedName = null;
        lastDetails = null;
        lastPrices = null;
        lastInvolvement = null;
        lastAccountDetails = null; lastAccountPrices = null;
        events.getItems().clear(); myEvents.getItems().clear(); myEvents.setValue(null);
        search.clear(); status.setValue(null); method.setValue(null); commission.setValue(null);
        ledger.getItems().clear(); balanceChart.getData().clear();
        accountName.setText(""); accountBalance.setText("—"); accountState.setText("");
        details.clear(); accountDetails.clear(); accountInvolvement.clear(); trade.show(null, null); accountTrade.show(null, null);
        uploadPath.clear();
        username.clear();
        username.setId("login-username"); username.setPromptText("Choose a username");
        server.setId("login-server");
        login.setId("login-button"); login.getStyleClass().setAll("button", "button-primary");
        login.disableProperty().unbind(); login.disableProperty().bind(busy);
        username.disableProperty().unbind(); username.disableProperty().bind(busy);
        server.disableProperty().unbind(); server.disableProperty().bind(busy);
        login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(action -> logIn()); username.setOnAction(action -> logIn());

        VBox info = new VBox(22,
                label("Welcome to Guess Market", "app-title", "login-hero-title"),
                label("Trade together in one shared, live market. Create prediction events, take positions, and watch every trade reshape the market.", "lead", "login-hero-paragraph"),
                label("Follow balances and results in real time, stay part of the shared conversation, and see where the crowd is leaning.", "lead", "login-hero-paragraph"));
        info.setId("login-hero");
        info.setMaxWidth(520);

        VBox form = new VBox(16,
                TradeActions.field("USERNAME", CharacterCount.wrap(username, 40)),
                TradeActions.field("SERVER", server), login);
        form.setId("login-form");
        form.setMaxWidth(420);

        login.setPadding(new Insets(10, 14, 10, 14));
        for (Node child : info.getChildren()) {
            if (child instanceof Label label) {
                label.setWrapText(true);
            }
        }

        VBox formSide = new VBox(form);
        formSide.setId("login-form-side");
        formSide.setPadding(new Insets(36));
        formSide.setAlignment(Pos.CENTER);
        VBox heroSide = new VBox(info);
        heroSide.setId("login-hero-side");
        heroSide.setPadding(new Insets(36));
        heroSide.setAlignment(Pos.CENTER);

        SplitPane loginSplit = new SplitPane(heroSide, formSide);
        loginSplit.setId("login-split");
        loginSplit.setDividerPositions(0.5);
        loginSplit.setOrientation(Orientation.HORIZONTAL);
        loginSplit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setCenter(loginSplit);
        stage.setTitle("Guess Market · Sign in");
        javafx.application.Platform.runLater(() -> {
            username.requestFocus();
            if (!reason.isBlank()) Dialogs.information(stage, "Signed out", reason);
        });
    }

    private void logIn() {
        if (busy.get()) return;
        String name = username.getText().strip();
        if (name.isBlank()) { error("Enter a username to join."); return; }
        session.login(server.getText(), name);
    }

    private Parent buildSignedIn() throws IOException {
        BorderPane shell = new BorderPane();
        Label title = label("Guess Market", "app-title");
        progress.setMaxSize(20, 20); progress.setPrefSize(20, 20);
        progress.visibleProperty().bind(busy); progress.managedProperty().bind(busy);
        profileMenu = new ProfileMenu(scene, busy, this::logOut);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox heading = new HBox(18, title, spacer, progress, profileMenu.button());
        heading.setAlignment(Pos.CENTER_LEFT);
        Button upload = new Button("Upload events"); upload.setId("upload-button");
        upload.setGraphic(Icons.openFolder(Icons.SMALL)); upload.getStyleClass().add("button-primary");
        upload.disableProperty().bind(busy); upload.setOnAction(action -> upload());
        Button saveState = new Button("Save state"); saveState.setId("save-state-button");
        saveState.setGraphic(Icons.floppyDisk(Icons.SMALL));
        saveState.disableProperty().bind(busy); saveState.setOnAction(action -> saveState());
        Button restoreState = new Button("Restore state"); restoreState.setId("restore-state-button");
        restoreState.setGraphic(Icons.openFolder(Icons.SMALL));
        restoreState.disableProperty().bind(busy); restoreState.setOnAction(action -> restoreState());
        uploadPath.setId("upload-path"); uploadPath.setEditable(false); uploadPath.setPromptText("No file uploaded in this session");
        FlowPane fileActions = new FlowPane(10, 8, upload, saveState, restoreState);
        fileActions.setAlignment(Pos.CENTER_LEFT);
        HBox uploadRow = new HBox(10, fileActions, uploadPath); HBox.setHgrow(uploadPath, Priority.ALWAYS);
        VBox header = new VBox(10, heading, uploadRow); header.getStyleClass().add("header-bar");
        shell.setTop(header);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().setAll(new Tab("Events", buildEvents()), new Tab("My account", buildAccount()));
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, old, selected) -> {
            if (selected != null) javafx.application.Platform.runLater(() -> {
                // Hidden tabs have not resolved their fonts when a snapshot first fills them.
                // Measure shared tables again once the selected tab has its actual CSS and size.
                Node content = selected.getContent();
                content.applyCss();
                Animations.slideIn(content, tabs.getTabs().indexOf(selected) > tabs.getTabs().indexOf(old)
                        ? HorizontalDirection.LEFT : HorizontalDirection.RIGHT);
                for (Node node : content.lookupAll(".table-view")) {
                    if (node instanceof TableView<?> table) Tables.fit(table);
                }
            });
        });
        shell.setCenter(tabs);
        chatDrawer = new ChatDrawer(session, stage, busy);
        shell.disableProperty().bind(chatDrawer.openProperty().or(profileMenu.settingsOpenProperty()));
        chatDrawer.view().disableProperty().bind(profileMenu.settingsOpenProperty());
        return new StackPane(shell, chatDrawer.view(), profileMenu.settingsView());
    }

    private Node buildEvents() throws IOException {
        FXMLLoader detailLoader = new FXMLLoader(getClass().getResource("/guessmarket/ui/events/event-details.fxml"));
        Parent detailNode = detailLoader.load(); details = detailLoader.getController();
        trade = new TradeActions(session, stage, busy, this::message);
        chooseEvent.setWrapText(true);
        VBox content = new VBox(12, chooseEvent, trade, detailNode); content.setPadding(new Insets(16)); content.setMinWidth(0);
        ScrollPane marketScroll = scroll(content); marketScroll.setId("market-scroll");
        events.setId("events-list"); events.getStyleClass().add("tile-list"); events.setMinWidth(240);
        Tiles.render(events, EventTile::of);
        events.setPlaceholder(new Region());
        events.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> { if (!rebuilding) renderSelectedEvent(); });
        search.setId("event-filter"); search.setPromptText("Search event, description or market maker");
        search.textProperty().addListener((o, old, value) -> filterEvents());
        status.setId("event-status-filter"); status.getItems().add(null); status.getItems().addAll(EventStatus.values());
        status.setConverter(new StringConverter<>() {
            @Override public String toString(EventStatus value) { return value == null ? "All states" : value.getDisplayName(); }
            @Override public EventStatus fromString(String value) { return null; }
        });
        method.setId("event-method-filter"); method.getItems().add(null); method.getItems().addAll(TradingMethod.values());
        method.setConverter(new StringConverter<>() {
            @Override public String toString(TradingMethod value) { return value == null ? "All methods" : value.getDisplayName(); }
            @Override public TradingMethod fromString(String value) { return null; }
        });
        status.setPromptText("All states"); method.setPromptText("All methods");
        status.setOnAction(action -> filterEvents()); method.setOnAction(action -> filterEvents());
        commission.setId("event-commission-filter"); commission.getItems().add(null); commission.getItems().addAll(CommissionType.values());
        commission.setConverter(new StringConverter<>() {
            @Override public String toString(CommissionType value) { return value == null ? "All commissions" : value.getDisplayName(); }
            @Override public CommissionType fromString(String value) { return null; }
        });
        commission.setPromptText("All commissions"); commission.setOnAction(action -> filterEvents());
        for (ComboBox<?> filter : List.of(status, method)) {
            filter.setMinWidth(0);
            filter.setPrefWidth(124);
        }
        commission.setMinWidth(0); commission.setPrefWidth(150);
        FlowPane filters = new FlowPane(8, 8, status, method, commission);
        VBox sidebar = new VBox(10, search, filters, eventCount, events);
        sidebar.setId("event-sidebar");
        sidebar.setPadding(new Insets(16, 8, 16, 16)); sidebar.setMinWidth(270); VBox.setVgrow(events, Priority.ALWAYS);
        SplitPane split = new SplitPane(sidebar, marketScroll); split.setId("events-split");
        split.setDividerPositions(DEFAULT_HORIZONTAL_DIVIDER_POSITION);
        // Both signed-in tabs use the same initial horizontal divider position. Ordinary resizes
        // still preserve any adjustment the user makes on this tab.
        boolean[] initialized = {false};
        boolean[] horizontalInitialized = {false};
        double[] horizontalPosition = {DEFAULT_HORIZONTAL_DIVIDER_POSITION};
        split.widthProperty().addListener((o, old, width) -> {
            if (width.doubleValue() <= 0) return;
            Orientation orientation = width.doubleValue() < 950 ? Orientation.VERTICAL : Orientation.HORIZONTAL;
            if (!initialized[0]) {
                initialized[0] = true;
                if (orientation == Orientation.HORIZONTAL) {
                    horizontalInitialized[0] = true;
                }
                split.setOrientation(orientation);
                sidebar.setMinHeight(210);
                split.setDividerPositions(orientation == Orientation.VERTICAL ? 0.32 : horizontalPosition[0]);
            } else if (split.getOrientation() != orientation) {
                if (split.getOrientation() == Orientation.HORIZONTAL) horizontalPosition[0] = split.getDividerPositions()[0];
                if (orientation == Orientation.HORIZONTAL && !horizontalInitialized[0]) {
                    horizontalInitialized[0] = true;
                }
                split.setOrientation(orientation);
                split.setDividerPositions(orientation == Orientation.VERTICAL ? 0.32 : horizontalPosition[0]);
            }
        });
        return split;
    }

    private Node buildAccount() throws IOException {
        accountName.setId("account-name"); accountBalance.setId("account-balance"); accountState.setWrapText(true);
        FlowPane statistics = new FlowPane(30, 12,
                TradeActions.field("AVAILABLE BALANCE", accountBalance), TradeActions.field("ACCOUNT STATUS", accountState));
        VBox identityCard = card(accountName, statistics,
                label("Every deposit, purchase, sale, commission and settlement appears in the ledger below.", "muted"));
        myEvents.setId("my-events"); myEvents.setMinWidth(0); myEvents.setMaxWidth(Double.MAX_VALUE); myEvents.setPromptText("Your events will appear here");
        myEvents.setConverter(new StringConverter<>() {
            @Override public String toString(EventDto event) { return event == null ? "" : "#" + event.id() + " · " + event.name() + " · " + event.status(); }
            @Override public EventDto fromString(String text) { return null; }
        });
        myEvents.valueProperty().addListener((o, old, value) -> { if (!rebuilding) renderInvolvement(); });
        Button view = new Button("View this market"); view.setMinWidth(Region.USE_PREF_SIZE); view.setOnAction(action -> selectMyMarket());
        HBox picker = new HBox(10, myEvents, view); HBox.setHgrow(myEvents, Priority.ALWAYS);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/guessmarket/ui/users/user-involvement.fxml"));
        Parent involvement = loader.load(); accountInvolvement = loader.getController();
        VBox participation = card(label("MY EVENTS", "panel-title"), picker,
                label("Events you run or participate in. Select one for your holdings, orders and results.", "muted"), involvement);
        ledger.setId("account-ledger");
        Tables.columns(ledger, Tables.number("#", row -> String.valueOf(row.sequence() + 1)),
                Tables.text("OPERATION", AccountEntryDto::description),
                Tables.number("AMOUNT", row -> Formats.signedMoney(row.amount())),
                Tables.number("BEFORE", row -> Formats.money(row.balanceBefore())),
                Tables.number("AFTER", row -> Formats.money(row.balanceAfter())));
        Tables.emptyMessage(ledger, "Your account operations will appear here. Load funds to begin.");
        stepAxis.setLabel("Account operation"); stepAxis.setMinorTickCount(0); balanceAxis.setLabel("Balance ($)");
        balanceChart.animatedProperty().bind(Animations.enabledProperty()); balanceChart.setLegendVisible(false); balanceChart.setPrefHeight(260);
        balanceChart.setMinHeight(220); balanceChart.setCreateSymbols(true);
        TextField funds = new TextField(); funds.setId("account-funds-amount"); funds.setPromptText("Amount to load"); funds.setPrefColumnCount(10);
        Button load = new Button("Load funds"); load.setId("account-funds-button"); load.disableProperty().bind(busy);
        load.setOnAction(action -> deposit(funds)); funds.setOnAction(action -> deposit(funds));
        identityCard.getChildren().add(new FlowPane(8, 8, funds, load));
        VBox account = new VBox(14, identityCard,
                card(label("ACCOUNT LEDGER", "panel-title"), ledger), card(label("BALANCE HISTORY", "panel-title"), balanceChart));
        account.setPadding(new Insets(16, 8, 16, 16)); account.setMinWidth(0);
        FXMLLoader detailsLoader = new FXMLLoader(getClass().getResource("/guessmarket/ui/events/event-details.fxml"));
        Parent eventDetails = detailsLoader.load(); accountDetails = detailsLoader.getController();
        accountTrade = new TradeActions(session, stage, busy, this::message);
        prefixIds(accountTrade, "account-");
        // Each screen hosts the very same reusable components; only the selected event differs.
        VBox eventPane = new VBox(14, participation, accountTrade, eventDetails);
        eventPane.setPadding(new Insets(16)); eventPane.setMinWidth(0);
        SplitPane split = new SplitPane(scroll(account), scroll(eventPane));
        split.setId("account-split");
        split.setDividerPositions(DEFAULT_HORIZONTAL_DIVIDER_POSITION);
        double[] horizontalPosition = {DEFAULT_HORIZONTAL_DIVIDER_POSITION};
        split.widthProperty().addListener((o, old, width) -> {
            Orientation orientation = width.doubleValue() < 1100 ? Orientation.VERTICAL : Orientation.HORIZONTAL;
            if (split.getOrientation() != orientation) {
                if (split.getOrientation() == Orientation.HORIZONTAL) {
                    horizontalPosition[0] = split.getDividerPositions()[0];
                }
                split.setOrientation(orientation);
                split.setDividerPositions(orientation == Orientation.VERTICAL ? 0.42 : horizontalPosition[0]);
            }
        });
        return split;
    }

    private boolean confirmLeave() {
        return Dialogs.confirmLeave(stage, "Leave " + signedName + "?",
                "Your name stays reserved until the server restarts. You will not be able to return to this account after leaving.");
    }

    private void logOut() {
        if (!busy.get() && Dialogs.confirmLogout(stage, "Logout " + signedName + "?",
                "Your name stays reserved until the server restarts. You will not be able to return to this account after logging out.")) {
            session.logout();
        }
    }

    private void upload() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Upload Exercise 3 events");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML event files", "*.xml"));
        File file = chooser.showOpenDialog(stage); if (file == null) return;
        session.perform(api -> api.upload(file.toPath()), result -> {
            uploadPath.setText(file.getAbsolutePath());
            message("Uploaded " + Formats.count(result.eventCount(), "event") + " from " + file.getName() + ". You are their market maker.");
        });
    }

    private void saveState() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save the Exercise 3 system state");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Guess Market saved state", "*.gmstate"));
        chooser.setInitialFileName("guess-market.gmstate");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        session.perform(api -> api.downloadState(file.toPath()), saved ->
                message("Saved the registered users, generated event IDs, balances, orders, trades and histories to "
                        + saved.toAbsolutePath() + "."));
    }

    private void restoreState() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Restore an Exercise 3 system state");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Guess Market saved state", "*.gmstate"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        if (!Dialogs.confirm(stage, "Replace the shared market?",
                "This replaces the market for every connected client. Your current username must exist in the saved state. "
                        + "Exercise 3 XML uploads remain separate and contain no users or event IDs.")) return;
        session.perform(api -> api.uploadState(file.toPath()), result -> {
            uploadPath.clear();
            message("Restored " + Formats.count(result.eventCount(), "event") + " and "
                    + Formats.count(result.userCount(), "user") + " from " + result.fileName() + ".");
        });
    }

    private void deposit(TextField field) {
        if (busy.get()) return;
        try {
            double amount = TradeActions.positiveMoney(field.getText(), "Amount");
            session.perform(api -> api.post("funds", Map.of("amount", amount), UserDto.class), user -> {
                field.clear(); message("Loaded " + Formats.money(amount) + ". Your balance is " + Formats.money(user.balance()) + ".");
            });
        } catch (IllegalArgumentException error) { error(error.getMessage()); }
    }

    private void filterEvents() {
        if (snapshot == null) return;
        Integer selectedId = events.getSelectionModel().getSelectedItem() == null ? null : events.getSelectionModel().getSelectedItem().id();
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        List<EventDto> matches = snapshot.events().stream().map(EventTradingStatusDto::event)
                .filter(event -> status.getValue() == null || event.status() == status.getValue())
                .filter(event -> method.getValue() == null || event.tradingMethod() == method.getValue())
                .filter(event -> commission.getValue() == null || event.commissionType() == commission.getValue())
                .filter(event -> (event.name() + " " + event.description() + " " + event.marketMakerName() + " " + event.id()).toLowerCase(Locale.ROOT).contains(query)).toList();
        if (!events.getItems().equals(matches)) {
            rebuilding = true;
            events.getItems().setAll(matches);
            if (selectedId != null) matches.stream().filter(event -> event.id() == selectedId).findFirst().ifPresent(event -> events.getSelectionModel().select(event));
            if (events.getSelectionModel().getSelectedItem() == null && !matches.isEmpty()) events.getSelectionModel().selectFirst();
            rebuilding = false;
        }
        eventCount.setText(matches.size() + " of " + Formats.count(snapshot.events().size(), "event"));
        renderSelectedEvent();
    }

    private void renderSelectedEvent() {
        EventDto selected = events.getSelectionModel().getSelectedItem();
        EventTradingStatusDto state = selected == null || snapshot == null ? null : snapshot.events().stream()
                .filter(value -> value.event().id() == selected.id()).findFirst().orElse(null);
        TradeActions.visible(chooseEvent, state == null);
        if (state == null) { details.clear(); trade.show(null, null); lastDetails = null; lastPrices = null; return; }
        List<PricePointDto> prices = snapshot.priceHistories().getOrDefault(state.event().id(), List.of());
        if (!Objects.equals(lastDetails, state) || !Objects.equals(lastPrices, prices)) {
            details.show(state, prices, snapshot.self().name()); lastDetails = state; lastPrices = prices;
        }
        trade.show(state, snapshot.self());
    }

    private void renderInvolvement() {
        EventDto event = myEvents.getValue();
        UserEventInvolvementDto involvement = event == null || snapshot == null ? null : snapshot.involvements().stream()
                .filter(value -> value.event().id() == event.id()).findFirst().orElse(null);
        if (involvement == null) accountInvolvement.clear();
        else if (!Objects.equals(involvement, lastInvolvement)) accountInvolvement.show(involvement);
        lastInvolvement = involvement;
        EventTradingStatusDto market = event == null || snapshot == null ? null : snapshot.events().stream()
                .filter(value -> value.event().id() == event.id()).findFirst().orElse(null);
        if (market == null) { accountDetails.clear(); accountTrade.show(null, null); lastAccountDetails = null; lastAccountPrices = null; }
        else {
            List<PricePointDto> prices = snapshot.priceHistories().getOrDefault(event.id(), List.of());
            if (!Objects.equals(market, lastAccountDetails) || !Objects.equals(prices, lastAccountPrices)) {
                accountDetails.show(market, prices, snapshot.self().name()); lastAccountDetails = market; lastAccountPrices = prices;
            }
            accountTrade.show(market, snapshot.self());
        }
    }

    private void selectMyMarket() {
        EventDto chosen = myEvents.getValue(); if (chosen == null) return;
        search.clear(); status.setValue(null); method.setValue(null); commission.setValue(null); filterEvents();
        events.getItems().stream().filter(event -> event.id() == chosen.id()).findFirst().ifPresent(event -> events.getSelectionModel().select(event));
        tabs.getSelectionModel().select(0);
    }

    @Override public void snapshotReceived(SnapshotDto next) {
        SnapshotDto previous = snapshot; snapshot = next;
        profileMenu.setUser(next.self().name()); profileMenu.setBalance(next.self().balance());
        accountName.setText(next.self().name() + " · My account"); accountBalance.setText(Formats.money(next.self().balance()));
        accountState.setText(next.self().blocked() ? "Blocked" : "Available to trade");
        if (previous != null && previous.self().balance() != next.self().balance()) {
            Animations.flash(accountBalance);
        }
        filterEvents();
        if (previous == null || !previous.involvements().equals(next.involvements())) {
            Integer chosen = myEvents.getValue() == null ? null : myEvents.getValue().id();
            rebuilding = true;
            myEvents.setValue(null);
            myEvents.getItems().setAll(next.involvements().stream().map(UserEventInvolvementDto::event).toList());
            if (chosen != null) myEvents.getItems().stream().filter(event -> event.id() == chosen).findFirst().ifPresent(myEvents::setValue);
            if (myEvents.getValue() == null && !myEvents.getItems().isEmpty()) myEvents.getSelectionModel().selectFirst();
            rebuilding = false;
        }
        renderInvolvement();
        if (previous == null || !previous.accountEntries().equals(next.accountEntries())) {
            Tables.setItemsPreservingSelection(ledger, next.accountEntries(), AccountEntryDto::sequence);
        }
        if (previous == null || !previous.balanceHistory().equals(next.balanceHistory())) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            for (BalancePointDto point : next.balanceHistory()) series.getData().add(new XYChart.Data<>(point.step(), point.balance()));
            balanceChart.getData().setAll(List.of(series)); Charts.countAlong(stepAxis, List.of(series));
        }
        chatDrawer.updateMessages(next.chat());
    }

    @Override public void loggedIn(String username) {
        signedName = username; root.setCenter(signedIn); tabs.getSelectionModel().selectFirst();
        profileMenu.setUser(username); chatDrawer.setUser(username);
        stage.setTitle("Guess Market · " + username);
        Animations.switchIn(signedIn);
    }
    @Override public void loggedOut(String reason) { showLogin(reason); }
    @Override public void busyChanged(boolean value) {
        busy.set(value);
        login.setText(value && signedName == null ? "Joining…" : "Join Guess Market");
    }
    @Override public void connectionChanged(boolean connected, String text) {
        if (connected) { connectionInterrupted = false; return; }
        if (signedName == null || connectionInterrupted) return;
        connectionInterrupted = true;
        Dialogs.error(stage, "Connection interrupted", text);
    }
    @Override public void error(String text) {
        Dialogs.error(stage, signedName == null ? "Unable to sign in" : "Operation could not be completed", text);
    }
    private void message(String text) { Dialogs.information(stage, "Guess Market", text); }
    @Override public void stop() { if (session != null) session.close(); }

    private static void prefixIds(Node node, String prefix) {
        if (node.getId() != null) node.setId(prefix + node.getId());
        if (node instanceof Parent parent) for (Node child : parent.getChildrenUnmodifiable()) prefixIds(child, prefix);
    }
    private static Label label(String text, String... styles) {
        Label label = new Label(text); label.getStyleClass().addAll(styles); label.setWrapText(true); return label;
    }
    private static VBox card(Node... children) {
        VBox card = new VBox(10, children); card.getStyleClass().add("card"); card.setMinWidth(0); return card;
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinWidth(0); scroll.setMinHeight(0); return scroll;
    }
}
