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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter CHAT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final BorderPane root = new BorderPane();
    private final Label notice = label("", "muted");
    private final Label connection = label("Connecting…", "faint");
    private final Label balance = label("—", "value", "value-accent");
    private final Label identity = label("", "muted");
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
    private final TableView<PublicUserDto> users = new TableView<>();
    private final ListView<ChatMessageDto> chat = new ListView<>();
    private final TextField chatInput = new TextField();
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
        stage.show();
    }

    private void showLogin(String reason) {
        snapshot = null;
        signedName = null;
        lastDetails = null;
        lastPrices = null;
        lastInvolvement = null;
        lastAccountDetails = null; lastAccountPrices = null;
        events.getItems().clear(); myEvents.getItems().clear(); myEvents.setValue(null); chat.getItems().clear();
        search.clear(); status.setValue(null); method.setValue(null); commission.setValue(null);
        ledger.getItems().clear(); users.getItems().clear(); balanceChart.getData().clear();
        accountName.setText(""); accountBalance.setText("—"); accountState.setText("");
        identity.setText(""); balance.setText("—"); chatInput.clear();
        connection.setText("Connecting…");
        details.clear(); accountDetails.clear(); accountInvolvement.clear(); trade.show(null, null); accountTrade.show(null, null);
        uploadPath.clear();
        username.clear();
        username.setId("login-username"); username.setPromptText("Choose a unique username (up to 40 characters)");
        if (username.getTextFormatter() == null) username.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().length() <= 40 ? change : null));
        server.setId("login-server");
        login.setId("login-button"); login.getStyleClass().setAll("button", "button-primary");
        login.disableProperty().unbind(); login.disableProperty().bind(busy);
        username.disableProperty().unbind(); username.disableProperty().bind(busy);
        server.disableProperty().unbind(); server.disableProperty().bind(busy);
        login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(action -> logIn()); username.setOnAction(action -> logIn());
        Label intro = label("Your own account. One shared market.", "lead");
        Label explanation = label("Choose a name to join. Each name is reserved until the server restarts. Keep this window open to retain access to your account.", "muted");
        explanation.setWrapText(true);
        Label feedback = label(reason, "value-accent"); feedback.setWrapText(true); feedback.setId("login-feedback");
        notice.setText("");
        VBox card = new VBox(18, label("Guess Market", "app-title"), intro, explanation,
                TradeActions.field("USERNAME", username), TradeActions.field("SERVER", server), login, feedback, notice);
        card.getStyleClass().add("card"); card.setMaxWidth(520); card.setPadding(new Insets(30));
        VBox holder = new VBox(card); holder.setAlignment(Pos.CENTER); holder.setPadding(new Insets(36));
        ScrollPane scroll = scroll(holder); holder.minHeightProperty().bind(scroll.heightProperty().subtract(4));
        root.setCenter(scroll);
        stage.setTitle("Guess Market · Sign in");
        javafx.application.Platform.runLater(username::requestFocus);
    }

    private void logIn() {
        String name = username.getText().trim();
        if (name.isBlank()) { notice.setText("Enter a username to join."); return; }
        notice.setText("Connecting to Guess Market…");
        session.login(server.getText(), name);
    }

    private Parent buildSignedIn() throws IOException {
        BorderPane shell = new BorderPane();
        Label title = label("Guess Market", "app-title");
        progress.setMaxSize(20, 20); progress.setPrefSize(20, 20);
        progress.visibleProperty().bind(busy); progress.managedProperty().bind(busy);
        FlowPane heading = new FlowPane(18, 8, title, identity, balance, progress);
        heading.setAlignment(Pos.CENTER_LEFT);
        Button upload = new Button("Upload events"); upload.setId("upload-button");
        upload.setGraphic(Icons.openFolder(Icons.SMALL)); upload.getStyleClass().add("button-primary");
        upload.disableProperty().bind(busy); upload.setOnAction(action -> upload());
        TextField funds = new TextField(); funds.setId("funds-amount"); funds.setPromptText("Amount to load"); funds.setPrefColumnCount(12);
        Button addFunds = new Button("Load funds"); addFunds.setId("funds-button"); addFunds.disableProperty().bind(busy);
        addFunds.setOnAction(action -> deposit(funds)); funds.setOnAction(action -> deposit(funds));
        ComboBox<Skin> theme = new ComboBox<>(FXCollections.observableArrayList(Skin.values()));
        theme.setValue(Skin.DEFAULT); theme.setId("theme-selector");
        theme.setOnAction(action -> { if (theme.getValue() != null) theme.getValue().applyTo(scene); });
        Button signout = new Button("Sign out"); signout.setId("signout-button"); signout.disableProperty().bind(busy);
        signout.setOnAction(action -> {
            if (Dialogs.confirm(stage, "Sign out of " + signedName + "?",
                    "Your name stays reserved until the server restarts. You will not be able to return to this account after signing out. Keep this window open if you want to continue using it.")) session.logout();
        });
        FlowPane toolbar = new FlowPane(10, 8, funds, addFunds, theme, signout);
        uploadPath.setId("upload-path"); uploadPath.setEditable(false); uploadPath.setPromptText("No file uploaded in this session");
        HBox uploadRow = new HBox(10, upload, uploadPath); HBox.setHgrow(uploadPath, Priority.ALWAYS);
        Label actionNotice = label("", "muted"); actionNotice.setWrapText(true); actionNotice.setId("action-notice");
        actionNotice.textProperty().bind(notice.textProperty());
        connection.setWrapText(true); connection.setId("connection-status");
        VBox header = new VBox(10, heading, toolbar, uploadRow, actionNotice, connection); header.getStyleClass().add("header-bar");
        shell.setTop(header);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().setAll(new Tab("Events", buildEvents()), new Tab("My account", buildAccount()), new Tab("Chat", buildChat()));
        shell.setCenter(tabs);
        return shell;
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
        Tiles.emptyMessage(events, "No matching events. Upload an Exercise 3 XML file to add markets.");
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
        FlowPane filters = new FlowPane(8, 8, status, method, commission, eventCount);
        VBox sidebar = new VBox(10, search, filters, events);
        sidebar.setPadding(new Insets(16, 8, 16, 16)); sidebar.setMinWidth(270); VBox.setVgrow(events, Priority.ALWAYS);
        SplitPane split = new SplitPane(sidebar, marketScroll); split.setDividerPositions(0.30);
        split.widthProperty().addListener((o, old, width) -> {
            Orientation orientation = width.doubleValue() < 950 ? Orientation.VERTICAL : Orientation.HORIZONTAL;
            if (split.getOrientation() != orientation) {
                split.setOrientation(orientation); sidebar.setMinHeight(210);
                split.setDividerPositions(orientation == Orientation.VERTICAL ? 0.32 : 0.30);
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
        balanceChart.setAnimated(false); balanceChart.setLegendVisible(false); balanceChart.setPrefHeight(260);
        balanceChart.setMinHeight(220); balanceChart.setCreateSymbols(true);
        TextField funds = new TextField(); funds.setId("account-funds-amount"); funds.setPromptText("Amount to load"); funds.setPrefColumnCount(10);
        Button load = new Button("Load funds"); load.setId("account-funds-button"); load.disableProperty().bind(busy);
        load.setOnAction(action -> deposit(funds)); funds.setOnAction(action -> deposit(funds));
        identityCard.getChildren().add(new FlowPane(8, 8, funds, load));
        VBox account = new VBox(14, buildUsers(), identityCard,
                card(label("ACCOUNT LEDGER", "panel-title"), ledger), card(label("BALANCE HISTORY", "panel-title"), balanceChart));
        account.setPadding(new Insets(16, 8, 16, 16)); account.setMinWidth(0);
        FXMLLoader detailsLoader = new FXMLLoader(getClass().getResource("/guessmarket/ui/events/event-details.fxml"));
        Parent eventDetails = detailsLoader.load(); accountDetails = detailsLoader.getController();
        accountTrade = new TradeActions(session, stage, busy, this::message);
        prefixIds(accountTrade, "account-");
        // Each screen hosts the very same reusable components; only the selected event differs.
        VBox eventPane = new VBox(14, participation, accountTrade, eventDetails);
        eventPane.setPadding(new Insets(16)); eventPane.setMinWidth(0);
        SplitPane split = new SplitPane(scroll(account), scroll(eventPane)); split.setDividerPositions(0.36);
        split.widthProperty().addListener((o, old, width) -> {
            Orientation orientation = width.doubleValue() < 1100 ? Orientation.VERTICAL : Orientation.HORIZONTAL;
            if (split.getOrientation() != orientation) { split.setOrientation(orientation); split.setDividerPositions(0.42); }
        });
        return split;
    }

    private Node buildUsers() {
        users.setId("users-table");
        Tables.columns(users, Tables.text("USER", PublicUserDto::name),
                Tables.number("BALANCE", user -> Formats.money(user.balance())),
                Tables.text("MM", user -> user.marketMaker() ? "Yes" : "No"));
        Tables.emptyMessage(users, "No other users have joined this server yet.");
        VBox box = new VBox(14, label("Other users", "screen-title"),
                label("Public balances update automatically. MM means market maker.", "muted"), card(users));
        box.setMinWidth(0); return box;
    }

    private Node buildChat() {
        chat.setId("chat-messages"); chat.getStyleClass().add("tile-list");
        chat.setPlaceholder(label("Start a conversation with everyone on this server.", "faint"));
        chat.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ChatMessageDto entry, boolean empty) {
                super.updateItem(entry, empty); setText(null);
                if (empty || entry == null) { setGraphic(null); return; }
                Label heading = label(entry.username() + "  ·  " + CHAT_TIME.format(Instant.ofEpochMilli(entry.timestamp())), "field-label");
                Label body = label(entry.message(), "lead"); body.setWrapText(true);
                VBox bubble = card(heading, body); bubble.prefWidthProperty().bind(chat.widthProperty().subtract(48));
                setGraphic(bubble);
            }
        });
        chatInput.setId("chat-message"); chatInput.setPromptText("Message everyone… (up to 1,000 characters)");
        chatInput.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().length() <= 1000 ? change : null));
        Button send = new Button("Send"); send.setId("chat-send"); send.getStyleClass().add("button-primary"); send.disableProperty().bind(busy);
        send.setOnAction(action -> sendChat()); chatInput.setOnAction(action -> sendChat());
        HBox composer = new HBox(10, chatInput, send); HBox.setHgrow(chatInput, Priority.ALWAYS);
        VBox box = new VBox(12, label("Market chat", "screen-title"), label("Visible to everyone on this server.", "muted"), chat, composer);
        box.setPadding(new Insets(16)); VBox.setVgrow(chat, Priority.ALWAYS); return box;
    }

    private void upload() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Upload Exercise 3 events");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML event files", "*.xml"));
        File file = chooser.showOpenDialog(stage); if (file == null) return;
        message("Uploading " + file.getName() + "…");
        session.perform(api -> api.upload(file.toPath()), result -> {
            uploadPath.setText(file.getAbsolutePath());
            message("Uploaded " + Formats.count(result.eventCount(), "event") + " from " + file.getName() + ". You are their market maker.");
        });
    }

    private void deposit(TextField field) {
        if (busy.get()) return;
        try {
            double amount = TradeActions.positiveMoney(field.getText(), "Amount");
            session.perform(api -> api.post("funds", Map.of("amount", amount), UserDto.class), user -> {
                field.clear(); message("Loaded " + Formats.money(amount) + ". Your balance is " + Formats.money(user.balance()) + ".");
            });
        } catch (IllegalArgumentException error) { message(error.getMessage()); }
    }

    private void sendChat() {
        String text = chatInput.getText().trim(); if (text.isBlank() || busy.get()) return;
        session.perform(api -> api.post("chat", Map.of("message", text), ChatMessageDto.class), result -> {
            if (chatInput.getText().trim().equals(text)) chatInput.clear();
            message("Message sent.");
        });
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
        identity.setText(next.self().name()); balance.setText("Balance " + Formats.money(next.self().balance()));
        accountName.setText(next.self().name() + " · My account"); accountBalance.setText(Formats.money(next.self().balance()));
        accountState.setText(next.self().blocked() ? "Blocked" : "Available to trade");
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
        if (previous == null || !previous.accountEntries().equals(next.accountEntries())) ledger.getItems().setAll(next.accountEntries());
        if (previous == null || !previous.users().equals(next.users())) users.getItems().setAll(next.users().stream()
                .filter(user -> !user.name().equalsIgnoreCase(next.self().name())).toList());
        if (previous == null || !previous.balanceHistory().equals(next.balanceHistory())) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            for (BalancePointDto point : next.balanceHistory()) series.getData().add(new XYChart.Data<>(point.step(), point.balance()));
            balanceChart.getData().setAll(List.of(series)); Charts.countAlong(stepAxis, List.of(series));
        }
        if (previous == null || !previous.chat().equals(next.chat())) {
            boolean follow = chat.getSelectionModel().getSelectedIndex() < 0
                    || chat.getSelectionModel().getSelectedIndex() >= chat.getItems().size() - 2;
            chat.getItems().setAll(next.chat()); if (follow && !chat.getItems().isEmpty()) chat.scrollTo(chat.getItems().size() - 1);
        }
    }

    @Override public void loggedIn(String username) {
        signedName = username; root.setCenter(signedIn); tabs.getSelectionModel().selectFirst();
        notice.setText("Welcome, " + username + ". Load funds to trade, or upload an Exercise 3 XML file to create your markets.");
        connection.setText("Loading the latest market…"); stage.setTitle("Guess Market · " + username);
    }
    @Override public void loggedOut(String reason) { showLogin(reason); }
    @Override public void busyChanged(boolean value) { busy.set(value); }
    @Override public void connectionChanged(boolean connected, String text) {
        connection.setText(text); connection.getStyleClass().setAll(connected ? "faint" : "value-negative");
    }
    @Override public void error(String text) {
        message(text);
        if (signedName != null) Dialogs.error(stage, "Operation could not be completed", text);
    }
    private void message(String text) { notice.setText(text); }
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
