package guessmarket.ui.settings;

import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Icons;
import guessmarket.ui.common.Skin;
import guessmarket.ui.common.ToggleSwitch;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * The settings sheet: everything the person decides about the program rather than about a market.
 *
 * <p>It is one panel laid over the whole window rather than a screen beside the other two, because
 * nothing on it is part of reading a market. While it is open there is nothing else to be doing,
 * and while it is closed the header is a header again instead of a shelf of switches.
 *
 * <p>Both appearance settings are answered here and nowhere else. Each of them is a single value
 * that the whole window reads - the skin it is dressed in, and whether its actions are
 * accompanied by a movement - so answering one of them here is the whole of the change: the
 * control writes it into the one value the window is laid out against, and no part of the program
 * has to be told that either has changed. The two remaining rows are actions belonging to the
 * window, and are handed straight back to it through {@link SettingsActions}.
 */
public class SettingsController {

    @FXML private StackPane rootPane;

    @FXML private StackPane skinIcon;
    @FXML private StackPane animationsIcon;
    @FXML private StackPane saveStateIcon;
    @FXML private StackPane loadStateIcon;

    @FXML private Button closeSettingsButton;
    @FXML private Button saveStateButton;
    @FXML private Button loadStateButton;

    @FXML private ScrollPane settingsScroll;
    @FXML private VBox settingsBody;
    @FXML private ComboBox<Skin> skinChooser;
    @FXML private ToggleSwitch animationsToggle;

    /** The scene being dressed. Not known while the layout is being read, only once it is shown. */
    private Scene scene;

    private SettingsActions actions;

    @FXML
    private void initialize() {
        skinIcon.getChildren().setAll(Icons.palette(Icons.ROW));
        animationsIcon.getChildren().setAll(Icons.sparkles(Icons.ROW));
        saveStateIcon.getChildren().setAll(Icons.floppyDisk(Icons.ROW));
        loadStateIcon.getChildren().setAll(Icons.openFolder(Icons.ROW));

        closeSettingsButton.setGraphic(Icons.cross(Icons.SMALL));
        closeSettingsButton.setTooltip(new Tooltip("Close the settings"));

        askForExactlyTheHeightOfTheSettings();
        offerEverySkin();
        followTheAnimationsSwitch();

        // A click that lands on the darkened backing rather than on the sheet is a click outside
        // the sheet, which is the usual way of saying that one has finished with it.
        rootPane.addEventHandler(MouseEvent.MOUSE_CLICKED, click -> {
            if (click.getTarget() == rootPane) {
                close();
            }
        });
    }

    /**
     * Finishes building the sheet once there is a window to dress and something to hand its two
     * actions to. This cannot happen while the layout file is being read, because at that point
     * the panel is not yet in a scene.
     */
    public void connect(Scene hostScene, SettingsActions hostActions) {
        this.scene = Objects.requireNonNull(hostScene, "scene");
        this.actions = Objects.requireNonNull(hostActions, "actions");

        // There is nothing to write to a file until a system has been loaded, and the sheet may be
        // sitting open at the moment one is, so this is watched rather than read once.
        saveStateButton.disableProperty().bind(Bindings.not(hostActions.systemLoaded()));
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::closeOnEscape);
    }

    /** Lays the sheet over the window. */
    public void open() {
        rootPane.setVisible(true);
        rootPane.setManaged(true);
        Animations.switchIn(rootPane);
    }

    /** Takes the sheet away, leaving everything chosen on it in force. */
    public void close() {
        rootPane.setVisible(false);
        rootPane.setManaged(false);
    }

    /**
     * Makes the sheet exactly as tall as the settings on it, and no taller.
     *
     * <p>Left to itself a scroll pane asks for the height its contents would have if nothing in
     * them ever wrapped, which here is several lines short, because every row carries a sentence
     * that does wrap. The sheet is then built to that wrong height and the last row is cut off
     * inside it. So the height is worked out at the width the settings are actually laid out at,
     * which is the same question asked properly.
     *
     * <p>It is asked again whenever one of the two things that can change the answer changes: how
     * wide the sheet is, and which skin is being worn, since each skin letters the window in a
     * face of its own. It is never asked again because the sheet grew, which is what would make
     * the question chase its own answer.
     */
    private void askForExactlyTheHeightOfTheSettings() {
        settingsScroll.prefViewportHeightProperty().bind(Bindings.createDoubleBinding(
                () -> settingsBody.prefHeight(settingsBody.getWidth()),
                settingsBody.widthProperty(),
                skinChooser.valueProperty()));
    }

    /**
     * Fills the dropdown the skin is chosen from, each name with a drawing of what it is like.
     *
     * <p>The list is filled here rather than in the layout file because it is a list of whatever
     * skins exist: a fourth one added to the enum appears in the dropdown without this file or
     * that one being touched. A dropdown rather than the three names side by side because this is
     * one setting among several, and a row of the sheet is one line with one control at the end
     * of it.
     */
    private void offerEverySkin() {
        skinChooser.setCellFactory(list -> skinLine());
        skinChooser.setButtonCell(skinLine());
        skinChooser.getItems().setAll(Skin.values());
        skinChooser.setValue(Skin.DEFAULT);

        // A dropdown holds a skin at all times and offers no way of holding none, so unlike a
        // group of buttons there is no emptied chooser to guard against here.
        skinChooser.valueProperty().addListener((observable, previous, chosen) -> wear(chosen));
    }

    /**
     * One name in the skin dropdown, with the drawing that goes with it.
     *
     * <p>Two of these are made rather than one, because the closed box and the open list both show
     * the chosen skin and one node cannot hang in two places at once.
     */
    private ListCell<Skin> skinLine() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Skin skin, boolean empty) {
                super.updateItem(skin, empty);
                setText(empty || skin == null ? null : skin.getDisplayName());
                setGraphic(empty || skin == null ? null : skin.icon(Icons.SMALL));
            }
        };
    }

    /**
     * Points the switch at the one that turns every animation in the program on and off.
     *
     * <p>Nothing is written on it. A switch says which way it is by which way it is, and the row
     * it stands in has already said what it is a switch for.
     */
    private void followTheAnimationsSwitch() {
        animationsToggle.setSelected(Animations.ENABLED_AT_START);
        Animations.enabledProperty().bind(animationsToggle.selectedProperty());
    }

    /** Dresses the window in a skin, at whatever size the writing is currently set to. */
    private void wear(Skin skin) {
        if (scene != null) {
            skin.applyTo(scene);
        }
    }

    @FXML
    private void onClose() {
        close();
    }

    /**
     * Both of these close the sheet before they act. What follows is a file chooser, and then
     * either a report in the header or a window rebuilt around a different system, none of which
     * can be seen from behind a sheet laid over all of it.
     */
    @FXML
    private void onSaveState() {
        close();
        actions.saveState();
    }

    @FXML
    private void onLoadState() {
        close();
        actions.loadState();
    }

    /** Escape closes the sheet, wherever in the window the keyboard happens to be. */
    private void closeOnEscape(KeyEvent pressed) {
        if (pressed.getCode() == KeyCode.ESCAPE && rootPane.isVisible()) {
            close();
            pressed.consume();
        }
    }

}
