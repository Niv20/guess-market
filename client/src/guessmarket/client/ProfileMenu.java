package guessmarket.client;

import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Icons;
import guessmarket.ui.common.Formats;
import guessmarket.ui.common.Skin;
import guessmarket.ui.common.ToggleSwitch;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;

/** Account actions beneath the avatar, and the online client's appearance settings. */
final class ProfileMenu {
    private final Scene scene;
    private final MenuButton profile = new MenuButton();
    private final Label name = new Label();
    private final Label balance = new Label();
    private final StackPane settings = new StackPane();
    private final BooleanProperty settingsOpen = new SimpleBooleanProperty(false);
    private final Button closeSettings = new Button();
    private final ComboBox<Skin> theme = new ComboBox<>();
    private final ToggleSwitch animations = new ToggleSwitch();
    private Node previousFocus;

    ProfileMenu(Scene scene, BooleanProperty busy, Runnable logout) {
        this.scene = Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(busy, "busy");
        Objects.requireNonNull(logout, "logout");
        String stylesheet = Objects.requireNonNull(getClass().getResource("profile.css"),
                "profile.css").toExternalForm();
        profile.getStylesheets().add(stylesheet);
        settings.getStylesheets().add(stylesheet);

        profile.setId("profile-button");
        profile.getStyleClass().add("profile-menu-button");
        profile.setGraphic(avatar());
        profile.setPopupSide(Side.BOTTOM);
        profile.setAccessibleText("User profile");
        profile.setTooltip(new Tooltip("User profile"));

        name.setId("profile-name");
        name.getStyleClass().add("profile-name");
        name.setWrapText(true);
        name.setMaxWidth(190);
        CustomMenuItem identity = new CustomMenuItem(name, false);
        identity.setDisable(true);
        identity.getStyleClass().add("profile-name-item");

        balance.setId("profile-balance");
        balance.getStyleClass().add("profile-balance");
        balance.setWrapText(true);
        balance.setMaxWidth(190);
        CustomMenuItem balanceItem = new CustomMenuItem(balance, false);
        balanceItem.setDisable(true);
        balanceItem.getStyleClass().add("profile-balance-item");

        MenuItem preferences = new MenuItem("Settings");
        preferences.setId("profile-settings");
        preferences.getStyleClass().add("profile-menu-item");
        preferences.setOnAction(event -> settingsOpen.set(true));
        MenuItem signout = new MenuItem("Logout");
        signout.setId("profile-logout");
        signout.getStyleClass().addAll("profile-menu-item", "profile-logout-item");
        signout.disableProperty().bind(busy);
        signout.setOnAction(event -> logout.run());
        profile.getItems().setAll(identity, balanceItem, new SeparatorMenuItem(), preferences, signout);

        buildSettings();
        settings.visibleProperty().bind(settingsOpen);
        settings.managedProperty().bind(settingsOpen);
        settingsOpen.addListener((observable, wasOpen, isOpen) -> {
            if (isOpen) {
                previousFocus = scene.getFocusOwner();
                profile.hide();
                settings.toFront();
                Animations.switchIn(settings);
                Platform.runLater(() -> {
                    if (settingsOpen.get()) closeSettings.requestFocus();
                });
            } else {
                theme.hide();
                Platform.runLater(() -> {
                    if (settingsOpen.get()) return;
                    Node target = previousFocus;
                    if (target != null && target.getScene() == scene && target.isVisible()
                            && !target.isDisabled()) target.requestFocus();
                    else if (profile.getScene() == scene && !profile.isDisabled()) profile.requestFocus();
                });
            }
        });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSettingsKeys);

        profile.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) Platform.runLater(() -> positionPopup(2));
        });
    }

    Node button() { return profile; }

    void setBalance(double amount) {
        setBalance("Balance " + Formats.money(amount));
    }

    void setBalance(String text) {
        balance.setText(text == null || text.isBlank() ? "—" : text);
    }

    void setUser(String username) {
        name.setText(username == null ? "" : username);
        profile.setAccessibleText(username == null || username.isBlank()
                ? "User profile" : "User profile: " + username);
    }

    Node settingsView() { return settings; }

    BooleanProperty settingsOpenProperty() { return settingsOpen; }

    /** Clears account-specific UI while preserving this window's appearance preferences. */
    void reset() {
        profile.hide();
        settingsOpen.set(false);
        setUser(null);
        setBalance("—");
    }

    private void positionPopup(int retries) {
        Bounds buttonBounds = profile.localToScreen(profile.getBoundsInLocal());
        if (buttonBounds == null) return;

        PopupWindow popup = Window.getWindows().stream()
                .filter(PopupWindow.class::isInstance)
                .map(PopupWindow.class::cast)
                .filter(PopupWindow::isShowing)
                .filter(window -> window.getOwnerWindow() == scene.getWindow())
                .filter(window -> window.getScene() != null)
                .filter(window -> {
                    Node popupRoot = window.getScene().getRoot();
                    return popupRoot.getStyleClass().contains("context-menu")
                            || popupRoot.lookup(".menu-item") != null;
                })
                .filter(window -> isNearButton(window, buttonBounds))
                .findFirst()
                .orElse(null);
        if (popup == null) {
            retryPositioning(retries);
            return;
        }

        popup.getScene().setFill(Color.TRANSPARENT);
        popup.getScene().getRoot().toFront();

        double popupWidth = popup.getWidth() > 0 ? popup.getWidth() : popup.getScene().getWidth();
        if (popupWidth <= 0) {
            popupWidth = popup.getScene().getRoot().getLayoutBounds().getWidth();
        }
        double popupHeight = popup.getHeight() > 0 ? popup.getHeight() : popup.getScene().getHeight();
        if (popupHeight <= 0) {
            popupHeight = popup.getScene().getRoot().getLayoutBounds().getHeight();
        }

        if (popupWidth <= 0 || popupHeight <= 0) {
            retryPositioning(retries);
            return;
        }

        List<Screen> screens = Screen.getScreensForRectangle(
                buttonBounds.getMinX(), buttonBounds.getMinY(),
                Math.max(1, buttonBounds.getWidth()), Math.max(1, buttonBounds.getHeight()));
        Rectangle2D bounds = screens.isEmpty() ? Screen.getPrimary().getVisualBounds() : screens.get(0).getVisualBounds();

        double minimumX = bounds.getMinX() + 8;
        double maximumX = Math.max(minimumX, bounds.getMaxX() - popupWidth - 8);
        double targetX = clamp(buttonBounds.getMaxX() - popupWidth, minimumX, maximumX);

        double minimumY = bounds.getMinY() + 8;
        double maximumY = Math.max(minimumY, bounds.getMaxY() - popupHeight - 8);
        double below = buttonBounds.getMaxY() + 4;
        double above = buttonBounds.getMinY() - popupHeight - 4;
        double targetY = below <= maximumY || above < minimumY ? below : above;
        targetY = clamp(targetY, minimumY, maximumY);

        popup.setX(targetX);
        popup.setY(targetY);
        popup.getScene().getRoot().toFront();
    }

    private void retryPositioning(int retries) {
        if (retries > 0 && profile.isShowing()) {
            Platform.runLater(() -> positionPopup(retries - 1));
        }
    }

    private static boolean isNearButton(PopupWindow popup, Bounds buttonBounds) {
        return popup.getX() >= buttonBounds.getMinX() - 240 && popup.getX() <= buttonBounds.getMaxX() + 160
                && popup.getY() >= buttonBounds.getMinY() - 2 && popup.getY() <= buttonBounds.getMaxY() + 260;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private void buildSettings() {
        settings.setId("settings-overlay");
        settings.getStyleClass().add("profile-settings-overlay");
        settings.setPickOnBounds(true);
        settings.setPadding(new Insets(24));
        settings.setFocusTraversable(true);

        Region backdrop = new Region();
        backdrop.getStyleClass().add("profile-settings-backdrop");
        backdrop.setOnMouseClicked(event -> {
            settingsOpen.set(false);
            event.consume();
        });
        // The scrim reaches the window edges, including the padding around the sheet.
        StackPane.setMargin(backdrop, new Insets(-24));

        Label title = new Label("Settings");
        title.getStyleClass().add("section-title");
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        closeSettings.setId("settings-close");
        closeSettings.setGraphic(Icons.cross(Icons.SMALL));
        closeSettings.getStyleClass().addAll("icon-button", "profile-settings-close");
        closeSettings.setAccessibleText("Close settings");
        closeSettings.setTooltip(new Tooltip("Close settings (Esc)"));
        closeSettings.setOnAction(event -> settingsOpen.set(false));
        HBox heading = new HBox(12, title, space, closeSettings);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.getStyleClass().add("settings-sheet-header");

        theme.setId("theme-selector");
        theme.setAccessibleText("Theme");
        theme.getItems().setAll(Skin.values());
        theme.setCellFactory(list -> skinCell());
        theme.setButtonCell(skinCell());
        theme.setValue(Skin.DEFAULT);
        theme.setPrefWidth(174);
        theme.setMinWidth(150);
        theme.setOnAction(event -> {
            if (theme.getValue() != null) theme.getValue().applyTo(scene);
        });

        animations.setId("animations-toggle");
        animations.setAccessibleHelp("Enable or disable interface animations");
        animations.setSelected(Animations.isEnabled());
        animations.selectedProperty().bindBidirectional(Animations.enabledProperty());

        Label appearance = new Label("APPEARANCE");
        appearance.getStyleClass().add("settings-group");
        VBox body = new VBox(appearance,
                setting("Theme", "Choose how Guess Market looks.",
                        Icons.palette(Icons.ROW), theme),
                setting("Animations", "Animate panels and controls as you use them.",
                        Icons.sparkles(Icons.ROW), animations));
        body.getStyleClass().add("settings-body");
        VBox sheet = new VBox(heading, body);
        sheet.setId("settings-sheet");
        sheet.getStyleClass().addAll("settings-sheet", "profile-settings-sheet");
        sheet.setMaxWidth(590);
        sheet.setMinWidth(0);
        sheet.setMaxHeight(Region.USE_PREF_SIZE);
        settings.getChildren().setAll(backdrop, sheet);
    }

    private HBox setting(String title, String explanation, Node icon, Node control) {
        StackPane badge = new StackPane(icon);
        badge.getStyleClass().add("icon-badge");
        Label label = new Label(title);
        label.getStyleClass().add("settings-title");
        label.setLabelFor(control);
        Label note = new Label(explanation);
        note.getStyleClass().add("settings-note");
        note.setWrapText(true);
        VBox words = new VBox(4, label, note);
        words.setMinWidth(0);
        HBox.setHgrow(words, Priority.ALWAYS);
        HBox row = new HBox(13, badge, words, control);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("settings-row");
        return row;
    }

    private ListCell<Skin> skinCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Skin value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.getDisplayName());
                setGraphic(empty || value == null ? null : value.icon(Icons.SMALL));
            }
        };
    }

    private void handleSettingsKeys(KeyEvent event) {
        if (!settingsOpen.get()) return;
        if (event.getCode() == KeyCode.ESCAPE) {
            if (theme.isShowing()) theme.hide();
            else settingsOpen.set(false);
            event.consume();
        } else if (event.getCode() == KeyCode.TAB && !theme.isShowing()) {
            List<Node> order = List.of(closeSettings, theme, animations);
            int current = order.indexOf(scene.getFocusOwner());
            int next = current < 0 ? (event.isShiftDown() ? order.size() - 1 : 0)
                    : Math.floorMod(current + (event.isShiftDown() ? -1 : 1), order.size());
            order.get(next).requestFocus();
            event.consume();
        }
    }

    private static Node avatar() {
        SVGPath drawing = new SVGPath();
        drawing.setContent("M 20 21 V 19 A 7 7 0 0 0 4 19 V 21 M 16 7 A 4 4 0 1 1 8 7 A 4 4 0 1 1 16 7 Z");
        drawing.getStyleClass().add("app-icon");
        StackPane icon = new StackPane(drawing);
        icon.setMinSize(24, 24);
        icon.setPrefSize(24, 24);
        icon.setMaxSize(24, 24);
        icon.setMouseTransparent(true);
        return icon;
    }
}
