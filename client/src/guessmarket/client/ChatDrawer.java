package guessmarket.client;

import guessmarket.dto.network.ChatMessageDto;
import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Icons;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A persistent chat launcher and a modal drawer over the signed-in screen. */
final class ChatDrawer {
    private static final double DRAWER_WIDTH = 480;
    private static final Duration SLIDE_TIME = Duration.millis(240);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ClientSession session;
    private final Stage stage;
    private final BooleanProperty busy;
    private final BooleanProperty open = new SimpleBooleanProperty();
    private final StackPane overlay = new StackPane();
    private final Region backdrop = new Region();
    private final VBox drawer = new VBox();
    private final ListView<ChatMessageDto> messages = new ListView<>();
    private final TextField input = new TextField();
    private final Button send = new Button("Send");
    private final Button closeButton = new Button();
    private final Button launcher = new Button();
    private final Label badge = new Label();
    private final StackPane launcherHolder = new StackPane();
    private final EventHandler<KeyEvent> keyHandler = this::handleKey;
    private ParallelTransition transition;
    private Node previousFocus;
    private String username;
    private boolean initialized;
    private boolean closing;
    private long lastSeenId = Long.MIN_VALUE;
    private long unread;

    ChatDrawer(ClientSession session, Stage stage, BooleanProperty busy) {
        this.session = session;
        this.stage = stage;
        this.busy = busy;
        overlay.setId("chat-overlay");
        overlay.setPickOnBounds(false);
        overlay.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/guessmarket/client/chat.css")).toExternalForm());

        backdrop.setId("chat-backdrop");
        backdrop.getStyleClass().add("chat-backdrop");
        backdrop.setOnMouseClicked(event -> { close(); event.consume(); });

        launcher.setId("chat-launcher");
        launcher.getStyleClass().add("chat-launcher");
        launcher.setGraphic(messageIcon());
        launcher.setTooltip(new Tooltip("Open market chat"));
        launcher.setOnAction(event -> open());
        badge.setId("chat-unread-count");
        badge.getStyleClass().add("chat-unread-count");
        badge.setMouseTransparent(true);
        badge.setTranslateX(7);
        badge.setTranslateY(-5);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        launcherHolder.getChildren().addAll(launcher, badge);
        launcherHolder.setMinSize(58, 58);
        launcherHolder.setPrefSize(58, 58);
        launcherHolder.setMaxSize(58, 58);
        launcherHolder.setPickOnBounds(false);
        StackPane.setAlignment(launcherHolder, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(launcherHolder, new Insets(24));
        updateBadge();

        Label title = new Label("Market chat");
        title.getStyleClass().add("screen-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        closeButton.setId("chat-close");
        closeButton.getStyleClass().add("icon-button");
        closeButton.setGraphic(Icons.cross(Icons.ROW));
        closeButton.setAccessibleText("Close chat");
        closeButton.setTooltip(new Tooltip("Close chat (Esc)"));
        closeButton.setOnAction(event -> close());
        HBox header = new HBox(12, title, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("chat-header");

        messages.setId("chat-messages");
        messages.getStyleClass().add("chat-messages");
        messages.setPlaceholder(new Region());
        messages.setCellFactory(list -> new MessageCell());
        messages.setMinHeight(0);
        VBox.setVgrow(messages, Priority.ALWAYS);

        input.setId("chat-message");
        input.setPromptText("Message everyone…");
        input.setAccessibleText("Message everyone");
        input.setMinWidth(0);
        StackPane countedInput = CharacterCount.wrap(input, 1000);
        HBox.setHgrow(countedInput, Priority.ALWAYS);
        countedInput.setMinWidth(0);
        send.setId("chat-send");
        send.getStyleClass().add("button-primary");
        send.disableProperty().bind(busy.or(Bindings.createBooleanBinding(
                () -> input.getText() == null || input.getText().isBlank(), input.textProperty())));
        send.setOnAction(event -> send());
        input.setOnAction(event -> send());
        HBox composer = new HBox(10, countedInput, send);
        composer.setAlignment(Pos.CENTER);
        composer.getStyleClass().add("chat-composer");

        drawer.setId("chat-drawer");
        drawer.getStyleClass().add("chat-drawer");
        drawer.getChildren().addAll(header, messages, composer);
        drawer.setMinWidth(0);
        drawer.prefWidthProperty().bind(Bindings.min(DRAWER_WIDTH, overlay.widthProperty()));
        drawer.maxWidthProperty().bind(drawer.prefWidthProperty());
        drawer.setMaxHeight(Double.MAX_VALUE);
        StackPane.setAlignment(drawer, Pos.CENTER_RIGHT);
        overlay.getChildren().addAll(launcherHolder, backdrop, drawer);
        setDrawerVisible(false);

        overlay.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
            if (newScene != null) newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        });
        stage.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (focused && isReading()) markRead();
        });
        Animations.enabledProperty().addListener((observable, oldValue, enabled) -> {
            if (!enabled && transition != null) {
                stopTransition();
                if (closing) finishClose(true);
                else { drawer.setTranslateX(0); backdrop.setOpacity(1); }
            }
        });
    }

    Node view() { return overlay; }
    BooleanProperty openProperty() { return open; }
    boolean isOpen() { return open.get(); }

    void setUser(String name) {
        if (!Objects.equals(username, name)) reset();
        username = name;
    }

    void reset() {
        stopTransition();
        finishClose(false);
        username = null;
        initialized = false;
        lastSeenId = Long.MIN_VALUE;
        unread = 0;
        messages.getItems().clear();
        input.clear();
        updateBadge();
    }

    void updateMessages(List<ChatMessageDto> snapshot) {
        List<ChatMessageDto> next = snapshot == null ? List.of() : snapshot;
        long newest = lastSeenId;
        for (ChatMessageDto message : next) {
            // IDs are server-authored and monotonic. The watermark also survives trimming of
            // old messages on the server, and repeated polling can never count one twice.
            if (initialized && message.id() > lastSeenId && !isOwn(message) && !isReading()) unread++;
            newest = Math.max(newest, message.id());
        }
        lastSeenId = newest;
        initialized = true; // The first snapshot is history, including an empty history.
        if (isReading()) markRead();
        else updateBadge();
        if (!messages.getItems().equals(next)) {
            boolean follow = atBottom();
            messages.getItems().setAll(next);
            if (follow) scrollToLatest();
        }
    }

    private void open() {
        if (isOpen() || username == null) return;
        Scene scene = overlay.getScene();
        previousFocus = scene == null ? null : scene.getFocusOwner();
        closing = false;
        setDrawerVisible(true);
        open.set(true);
        overlay.applyCss();
        overlay.layout();
        if (isReading()) markRead();
        scrollToLatest();
        input.requestFocus();
        if (Animations.isEnabled()) animate(true);
        else { drawer.setTranslateX(0); backdrop.setOpacity(1); }
    }

    void close() {
        if (!isOpen() || closing) return;
        closing = true;
        if (Animations.isEnabled()) animate(false);
        else finishClose(true);
    }

    private void animate(boolean opening) {
        stopTransition();
        double distance = Math.max(drawer.getWidth(), drawer.getPrefWidth()) + 24;
        TranslateTransition slide = new TranslateTransition(SLIDE_TIME, drawer);
        slide.setFromX(opening ? distance : drawer.getTranslateX());
        slide.setToX(opening ? 0 : distance);
        slide.setInterpolator(Interpolator.EASE_BOTH);
        FadeTransition fade = new FadeTransition(SLIDE_TIME, backdrop);
        fade.setFromValue(opening ? 0 : backdrop.getOpacity());
        fade.setToValue(opening ? 1 : 0);
        transition = new ParallelTransition(slide, fade);
        transition.setOnFinished(event -> {
            transition = null;
            if (!opening) finishClose(true);
        });
        transition.play();
    }

    private void stopTransition() {
        if (transition != null) { transition.stop(); transition = null; }
    }

    private void finishClose(boolean restoreFocus) {
        stopTransition();
        closing = false;
        setDrawerVisible(false);
        drawer.setTranslateX(0);
        backdrop.setOpacity(1);
        open.set(false);
        Node target = previousFocus;
        previousFocus = null;
        if (restoreFocus) Platform.runLater(() -> {
            if (isOpen() || overlay.getScene() == null) return;
            if (target != null && target.getScene() == overlay.getScene()
                    && target.isVisible() && !target.isDisabled()) target.requestFocus();
            else launcher.requestFocus();
        });
    }

    private void setDrawerVisible(boolean value) {
        backdrop.setVisible(value);
        backdrop.setManaged(value);
        drawer.setVisible(value);
        drawer.setManaged(value);
        launcherHolder.setVisible(!value);
        launcherHolder.setManaged(!value);
    }

    private void handleKey(KeyEvent event) {
        if (!isOpen()) return;
        if (event.getCode() == KeyCode.ESCAPE) {
            close();
            event.consume();
        } else if (event.getCode() == KeyCode.TAB) {
            List<Node> order = new ArrayList<>(List.of(closeButton, messages, input, send));
            order.removeIf(node -> node.isDisabled() || !node.isVisible());
            Node current = overlay.getScene().getFocusOwner();
            int index = order.indexOf(current);
            int next = Math.floorMod(index + (event.isShiftDown() ? -1 : 1), order.size());
            order.get(next).requestFocus();
            event.consume();
        } else if (!insideDrawer(overlay.getScene().getFocusOwner())) {
            input.requestFocus();
            event.consume();
        }
    }

    private boolean insideDrawer(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == drawer) return true;
        }
        return false;
    }

    private boolean isReading() { return isOpen() && !closing && stage.isFocused(); }
    private boolean isOwn(ChatMessageDto message) {
        return username != null && username.equalsIgnoreCase(message.username());
    }

    private void markRead() { unread = 0; updateBadge(); }

    private void updateBadge() {
        badge.setText(Long.toString(unread));
        badge.setVisible(unread > 0);
        badge.setManaged(unread > 0);
        launcher.setAccessibleText(unread == 0 ? "Open market chat"
                : "Open market chat, " + unread + " unread " + (unread == 1 ? "message" : "messages"));
    }

    private boolean atBottom() {
        for (Node node : messages.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                return !bar.isVisible() || bar.getValue() >= bar.getMax() - (bar.getMax() - bar.getMin()) * 0.02;
            }
        }
        return true;
    }

    private void scrollToLatest() {
        Platform.runLater(() -> {
            if (!messages.getItems().isEmpty()) messages.scrollTo(messages.getItems().size() - 1);
        });
    }

    private void send() {
        String text = input.getText() == null ? "" : input.getText().strip();
        if (text.isBlank() || busy.get()) return;
        session.perform(api -> api.post("chat", Map.of("message", text), ChatMessageDto.class), result -> {
            if (input.getText().strip().equals(text)) input.clear();
        });
    }

    private static Node messageIcon() {
        SVGPath outline = new SVGPath();
        outline.setContent("M4 3 H20 Q23 3 23 6 V17 Q23 20 20 20 H10 L4 25 V20 Q1 20 1 17 V6 Q1 3 4 3 Z M7 9 H17 M7 14 H14");
        outline.getStyleClass().add("chat-message-icon");
        return outline;
    }

    private final class MessageCell extends ListCell<ChatMessageDto> {
        private final Label author = new Label();
        private final Label body = new Label();
        private final Label time = new Label();
        private final VBox bubble = new VBox(4, author, body, time);
        private final HBox row = new HBox(bubble);

        MessageCell() {
            getStyleClass().add("chat-message-cell");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setText(null);
            row.prefWidthProperty().bind(Bindings.max(0, widthProperty().subtract(28)));
            row.setMinWidth(0);
            bubble.getStyleClass().add("chat-bubble");
            bubble.setMinWidth(0);
            bubble.setPrefWidth(Region.USE_COMPUTED_SIZE);
            bubble.maxWidthProperty().bind(row.widthProperty().multiply(0.78));
            author.getStyleClass().add("chat-author");
            body.getStyleClass().add("chat-body");
            time.getStyleClass().add("chat-time");
            for (Label label : List.of(author, body, time)) {
                label.setMinWidth(0);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setWrapText(true);
            }
            time.setAlignment(Pos.CENTER_RIGHT);
        }

        @Override protected void updateItem(ChatMessageDto entry, boolean empty) {
            super.updateItem(entry, empty);
            setText(null);
            if (empty || entry == null) { setGraphic(null); return; }
            boolean own = isOwn(entry);
            row.setAlignment(own ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            bubble.getStyleClass().setAll("chat-bubble", own ? "chat-bubble-own" : "chat-bubble-other");
            author.setText(entry.username());
            author.setVisible(!own);
            author.setManaged(!own);
            body.setText(entry.message());
            time.setText(TIME.format(Instant.ofEpochMilli(entry.timestamp())));
            setAccessibleText(entry.username() + ": " + entry.message() + ", " + time.getText());
            setGraphic(row);
        }
    }
}
