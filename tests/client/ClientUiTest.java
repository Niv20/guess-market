package guessmarket.client;

import guessmarket.dto.UserDto;
import guessmarket.dto.network.ChatMessageDto;
import guessmarket.dto.network.SnapshotDto;
import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Dialogs;
import guessmarket.ui.common.Skin;
import guessmarket.ui.common.Tables;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real JavaFX layout and interaction regressions; runs in a disposable client without HTTP writes. */
public final class ClientUiTest extends Application {
    private static Throwable failure;
    private GuessMarketClient client;
    private Stage stage;
    private Scene scene;
    private int checks;

    public static void main(String[] args) {
        launch(args);
        if (failure != null) throw new AssertionError("Client UI regression", failure);
    }

    @Override public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        client = new GuessMarketClient();
        client.start(stage);
        scene = stage.getScene();
        PauseTransition ready = new PauseTransition(Duration.millis(250));
        ready.setOnFinished(event -> Platform.runLater(this::runChecks));
        ready.play();
    }

    private void runChecks() {
        try {
            require(Animations.isEnabled(), "Animations must start enabled");
            // Avoid measuring a scene in the middle of an entrance transition.
            Animations.enabledProperty().set(false);
            verifyTableSelectionSurvivesRefresh();
            layout();
            TextField username = node("login-username", TextField.class);
            username.setText("Niv");
            require(labelText(scene.getRoot(), "3/40"), "Username counter follows edits");
            capture("login");
            client.loggedIn("Niv");
            client.snapshotReceived(snapshot(List.of(new ChatMessageDto(1, "Dana", "Earlier conversation", 1))));
            layout();
            require(scene.lookup("#action-notice") == null && scene.lookup("#connection-status") == null,
                    "Header contains no status banners");
            require(scene.lookup("#funds-amount") == null && scene.lookup("#funds-button") == null,
                    "Header funding controls removed");
            TabPane tabs = nodeByType(TabPane.class);
            require(tabs.getTabs().size() == 2 && tabs.getTabs().stream().noneMatch(t -> t.getText().equals("Chat")),
                    "Chat is no longer a tab");
            SplitPane split = node("events-split", SplitPane.class);
            require(split.getOrientation() == Orientation.HORIZONTAL, "Desktop split is horizontal");
            double defaultEventsDivider = split.getDividerPositions()[0];
            for (Skin skin : Skin.values()) {
                skin.applyTo(scene); layout();
                double y = node("event-status-filter", ComboBox.class).getLayoutY();
                require(y == node("event-method-filter", ComboBox.class).getLayoutY()
                                && y == node("event-commission-filter", ComboBox.class).getLayoutY(),
                        "All three filters share one row in " + skin);
            }
            Skin.DEFAULT.applyTo(scene); layout();
            capture("events");
            split.setDividerPositions(0.31); layout();
            double dragged = split.getDividerPositions()[0];
            layout();
            require(Math.abs(split.getDividerPositions()[0] - dragged) < 0.002, "Layout preserves manually dragged divider");
            double originalWidth = scene.getRoot().getLayoutBounds().getWidth();
            scene.getRoot().resize(760, scene.getHeight()); layout();
            require(split.getOrientation() == Orientation.VERTICAL, "Narrow window stacks panes");
            scene.getRoot().resize(originalWidth, scene.getHeight()); layout();
            require(split.getOrientation() == Orientation.HORIZONTAL, "Expanding restores horizontal split");
            require(Math.abs(split.getDividerPositions()[0] - dragged) < 0.02, "Responsive changes preserve dragged width");
            split.setDividerPositions(0.45); layout();

            ProfileMenu profile = field("profileMenu", ProfileMenu.class);
            MenuButton menu = (MenuButton) profile.button();
            require(menu.getPopupSide() == javafx.geometry.Side.BOTTOM, "Profile dropdown opens below icon");
            require(menu.getItems().stream().anyMatch(item -> "Logout".equals(item.getText())), "Profile contains Logout");
            require(menu.getItems().stream().allMatch(item -> item.getGraphic() == null),
                    "Profile dropdown items contain no icons");
            require(menu.getItems().stream().filter(CustomMenuItem.class::isInstance).map(CustomMenuItem.class::cast)
                    .anyMatch(item -> item.getContent() instanceof Label label && label.getText().equals("Niv")), "Profile shows username");
            for (Skin skin : Skin.values()) {
                skin.applyTo(scene); layout(); menu.show();
                Window popup = Window.getWindows().stream().filter(window -> window instanceof javafx.stage.PopupWindow
                        && window.isShowing()).findFirst().orElseThrow();
                Region popupRoot = (Region) popup.getScene().getRoot();
                require(popupRoot.getBackground() == null
                                || popupRoot.getBackground().getFills().stream().allMatch(fill ->
                                fill.getFill() instanceof javafx.scene.paint.Color color && color.getOpacity() == 0),
                        "Profile popup root stays transparent behind rounded corners in " + skin);
                require(popup.getScene().getRoot().localToScreen(popup.getScene().getRoot().getLayoutBounds()).getMinY()
                                >= menu.localToScreen(menu.getBoundsInLocal()).getMaxY() - 2,
                        "Profile menu sits below the avatar in " + skin);
                javafx.geometry.Bounds popupBounds = popupRoot.localToScreen(popupRoot.getLayoutBounds());
                javafx.geometry.Bounds balanceBounds = popupRoot.lookup("#profile-balance")
                        .localToScreen(popupRoot.lookup("#profile-balance").getLayoutBounds());
                require(popupBounds.contains(balanceBounds), "Profile balance stays inside menu bounds in " + skin);
                captureNode(popup.getScene().getRoot(), "profile-" + skin.name().toLowerCase());
                menu.hide();
            }
            Skin.DEFAULT.applyTo(scene); layout();
            menu.getItems().stream().filter(item -> "Settings".equals(item.getText())).findFirst().orElseThrow().fire();
            layout();
            require(profile.settingsOpenProperty().get(), "Settings opens");
            require(tabs.isDisabled(), "Settings blocks underlying controls");
            ToggleButton toggle = node("animations-toggle", ToggleButton.class);
            toggle.fire(); require(Animations.isEnabled(), "Settings restores animations");
            toggle.fire(); require(!Animations.isEnabled(), "Settings can disable animations");
            capture("settings");
            node("settings-close", Button.class).fire();
            require(!tabs.isDisabled(), "Closing settings releases underlying controls");

            ChatDrawer chat = field("chatDrawer", ChatDrawer.class);
            Label badge = node("chat-unread-count", Label.class);
            require(!badge.isVisible(), "Historical chat is not unread");
            List<ChatMessageDto> messages = new ArrayList<>();
            messages.add(new ChatMessageDto(1, "Dana", "Earlier conversation", 1));
            messages.add(new ChatMessageDto(2, "Dana", "Are you following the new market?", 2));
            messages.add(new ChatMessageDto(3, "Niv", "Yes, checking the details now.", 3));
            chat.updateMessages(messages);
            require(badge.isVisible() && badge.getText().equals("1"), "Only new incoming messages count as unread");
            chat.updateMessages(messages);
            require(badge.getText().equals("1"), "Polling does not double count unread");
            node("chat-launcher", Button.class).fire(); layout();
            require(chat.isOpen() && tabs.isDisabled(), "Chat opens as modal overlay");
            require(!stage.isFocused() || !badge.isVisible(), "Opening focused chat marks messages read");
            require(scene.getRoot().lookupAll(".chat-bubble-own").size() > 0, "Own messages have separate bubble styling");
            capture("chat");
            chat.close(); layout();
            require(!chat.isOpen() && !tabs.isDisabled(), "Closing chat restores app interaction");

            tabs.getSelectionModel().select(1); layout();
            SplitPane accountSplit = node("account-split", SplitPane.class);
            require(accountSplit.getOrientation() == Orientation.HORIZONTAL, "Desktop account split is horizontal");
            require(Math.abs(accountSplit.getDividerPositions()[0] - defaultEventsDivider) < 0.002,
                    "Events and My account use the same default left-pane width");
            require(scene.lookup("#users-table") == null && !labelText(scene.getRoot(), "Other users"),
                    "My account omits the public users table");
            TextField funds = node("account-funds-amount", TextField.class);
            Button load = node("account-funds-button", Button.class);
            for (String invalid : List.of("-1", "abc", "0", "NaN", "Infinity", "1d", "0x1p2", "")) {
                funds.setText(invalid);
                dismissNextDialog("positive amount", null);
                load.fire();
            }
            dismissNextDialog(null, "Stay");
            require(!Dialogs.confirmLeave(stage, "Leave Niv?", "Account access will end."), "Stay cancels leaving");
            dismissNextDialog(null, "Leave");
            require(Dialogs.confirmLeave(stage, "Leave Niv?", "Account access will end."), "Leave confirms leaving");
            dismissNextDialog(null, "Stay");
            require(!Dialogs.confirmLogout(stage, "Logout Niv?", "Account access will end."), "Stay cancels logout");
            dismissNextDialog(null, "Logout");
            require(Dialogs.confirmLogout(stage, "Logout Niv?", "Account access will end."), "Logout confirms logout");
            chat.reset();
            require(!badge.isVisible() && !chat.isOpen(), "Session reset clears chat state");
            client.loggedOut(""); layout();
            require(labelText(scene.getRoot(), "0/40"), "Logout resets username counter");
            System.out.println("PASS: " + checks + " client UI checks (layout, themes, profile, modal chat, unread counts, funds errors and exit choices)");
        } catch (Throwable error) {
            failure = error;
            error.printStackTrace();
        } finally {
            client.stop();
            stage.hide();
            Platform.exit();
        }
    }

    private void dismissNextDialog(String expectedText, String choice) {
        Platform.runLater(() -> {
            try {
                DialogPane pane = Window.getWindows().stream().filter(Window::isShowing)
                        .filter(window -> window != stage)
                        .map(window -> window.getScene().getRoot())
                        .filter(DialogPane.class::isInstance).map(DialogPane.class::cast).findFirst().orElseThrow();
                if (expectedText != null) require(pane.getContent() instanceof Label label
                        && label.getText().contains(expectedText), "Invalid funds shows error alert");
                if (choice != null) {
                    require(pane.getButtonTypes().stream().noneMatch(button -> "OK".equals(button.getText())), "Exit choices do not use OK");
                    Button stay = (Button) pane.lookupButton(pane.getButtonTypes().stream().filter(b -> b.getText().equals("Stay")).findFirst().orElseThrow());
                    Button action = (Button) pane.lookupButton(pane.getButtonTypes().stream()
                            .filter(button -> !"Stay".equals(button.getText())).findFirst().orElseThrow());
                    require(stay.isDefaultButton() && stay.isCancelButton() && !action.isDefaultButton(),
                            "Stay is safe default and Escape action");
                    require(action.getStyleClass().contains("button-danger"), "Exit action uses danger styling");
                    Node container = pane.lookup(".button-bar > .container");
                    require(container instanceof javafx.scene.layout.HBox box
                                    && box.getAlignment() == javafx.geometry.Pos.CENTER_RIGHT
                                    && box.getChildren().indexOf(action) > box.getChildren().indexOf(stay),
                            "Exit actions are right aligned with the destructive action last");
                    if (choice.equals("Stay")) captureNode(pane, "leave-warning");
                }
                ButtonType answer = choice == null ? pane.getButtonTypes().getFirst()
                        : pane.getButtonTypes().stream().filter(button -> choice.equals(button.getText())).findFirst().orElseThrow();
                ((Button) pane.lookupButton(answer)).fire();
            } catch (Throwable error) {
                failure = error;
                new ArrayList<>(Window.getWindows()).stream().filter(w -> w != stage).forEach(Window::hide);
            }
        });
    }

    private SnapshotDto snapshot(List<ChatMessageDto> messages) {
        return new SnapshotDto(List.of(), Map.of(), List.of(),
                new UserDto("Niv", 0, 0, false, List.of(), List.of()), List.of(), List.of(), List.of(), messages);
    }
    private void verifyTableSelectionSurvivesRefresh() {
        TableView<SelectionRow> table = new TableView<>();
        table.setItems(javafx.collections.FXCollections.observableArrayList(
                new SelectionRow(11, "selected"), new SelectionRow(12, "other")));
        table.getSelectionModel().select(0);

        Tables.setItemsPreservingSelection(table,
                List.of(new SelectionRow(12, "other updated"), new SelectionRow(11, "selected updated")),
                SelectionRow::id);
        require(table.getSelectionModel().getSelectedItem().equals(new SelectionRow(11, "selected updated")),
                "A refreshed table restores its selected row by stable key");

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().clearSelection();
        table.getSelectionModel().select(0);
        table.getSelectionModel().select(1);
        Tables.setItemsPreservingSelection(table,
                List.of(new SelectionRow(11, "selected again"), new SelectionRow(12, "other again")),
                SelectionRow::id);
        Set<Integer> selectedIds = Set.copyOf(table.getSelectionModel().getSelectedItems().stream()
                .map(SelectionRow::id).toList());
        require(selectedIds.equals(Set.of(11, 12)),
                "A refreshed table restores every selected row in multiple-selection mode");

        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getSelectionModel().clearSelection();
        table.getSelectionModel().select(0);
        Tables.setItemsPreservingSelection(table, List.of(new SelectionRow(13, "replacement")),
                SelectionRow::id);
        require(table.getSelectionModel().getSelectedItem() == null,
                "A removed row does not leave a stale table selection");

        table.getSelectionModel().select(0);
        Tables.setItemsPreservingSelection(table, List.of(new SelectionRow(13, "new context")),
                SelectionRow::id, false);
        require(table.getSelectionModel().getSelectedItem() == null,
                "Changing table context clears a selection even when a row key is reused");

        TableView<SelectionRow> readOnlyTable = new TableView<>();
        Tables.columns(readOnlyTable, Tables.text("VALUE", SelectionRow::value));
        readOnlyTable.setSelectionModel(null);
        readOnlyTable.setFocusModel(null);
        Tables.setItemsPreservingSelection(readOnlyTable,
                List.of(new SelectionRow(21, "read only")), SelectionRow::id);
        require(readOnlyTable.getItems().equals(List.of(new SelectionRow(21, "read only"))),
                "Refreshing a display-only table works when it has no selection model");
    }

    private record SelectionRow(int id, String value) {
    }

    private void layout() { scene.getRoot().applyCss(); scene.getRoot().layout(); }
    private <T> T node(String id, Class<T> type) { return type.cast(java.util.Objects.requireNonNull(scene.lookup("#" + id), id)); }
    private <T> T nodeByType(Class<T> type) { return scene.getRoot().lookupAll("*").stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow(); }
    private <T> T field(String name, Class<T> type) throws Exception { Field f = GuessMarketClient.class.getDeclaredField(name); f.setAccessible(true); return type.cast(f.get(client)); }
    private boolean labelText(Parent parent, String text) { return parent.lookupAll(".label").stream().anyMatch(node -> node instanceof Label label && label.getText().equals(text)); }
    private void require(boolean condition, String description) { checks++; if (!condition) throw new AssertionError(description); }
    private void capture(String name) throws Exception { captureNode(scene.getRoot(), name); }
    private void captureNode(Parent node, String name) throws Exception {
        node.applyCss(); node.layout();
        WritableImage image = node.snapshot(null, null);
        BufferedImage output = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) for (int x = 0; x < output.getWidth(); x++) output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        Path directory = Path.of("out", "ui-checks"); Files.createDirectories(directory);
        ImageIO.write(output, "png", directory.resolve(name + ".png").toFile());
    }
}
