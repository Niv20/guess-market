package guessmarket.ui.settings;

import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Icons;
import guessmarket.ui.common.Skin;
import guessmarket.ui.common.TextSize;
import guessmarket.ui.common.ToggleSwitch;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Objects;

/**
 * The settings sheet: everything the person decides about the program rather than about a market.
 *
 * <p>It is one panel laid over the whole window rather than a screen beside the other two, because
 * nothing on it is part of reading a market. While it is open there is nothing else to be doing,
 * and while it is closed the header is a header again instead of a shelf of switches.
 *
 * <p>The three appearance settings are answered here and nowhere else. Each of them is a single
 * value that the whole window reads - the skin it is dressed in, the size of its writing, whether
 * its actions are accompanied by a movement - so the controls are bound to those values rather
 * than copied into them, and no part of the program has to be told that any of the three has
 * changed. The two remaining rows are actions belonging to the window, and are handed straight
 * back to it through {@link SettingsActions}.
 */
public class SettingsController {

    /**
     * How the multiplier is written out beside the slider. The figure is padded to three columns
     * and set in the monospaced face of {@code .value}, so that the slider beside it does not
     * shift the moment the reading goes from two figures to three.
     */
    private static final String PERCENTAGE = "%3.0f%%";

    @FXML private StackPane rootPane;

    @FXML private StackPane titleIcon;
    @FXML private StackPane skinIcon;
    @FXML private StackPane textSizeIcon;
    @FXML private StackPane animationsIcon;
    @FXML private StackPane saveStateIcon;
    @FXML private StackPane loadStateIcon;

    @FXML private Button closeSettingsButton;
    @FXML private Button resetButton;
    @FXML private Button saveStateButton;
    @FXML private Button loadStateButton;

    @FXML private ScrollPane settingsScroll;
    @FXML private VBox settingsBody;
    @FXML private HBox skinChoices;
    @FXML private Slider textSizeSlider;
    @FXML private Label textSizePercent;
    @FXML private ToggleSwitch animationsToggle;

    /** Holds the three skins together, so that exactly one of them is chosen at any moment. */
    private final ToggleGroup skins = new ToggleGroup();

    /** The scene being dressed. Not known while the layout is being read, only once it is shown. */
    private Scene scene;

    private SettingsActions actions;

    @FXML
    private void initialize() {
        titleIcon.getChildren().setAll(Icons.gear(Icons.ROW));
        skinIcon.getChildren().setAll(Icons.palette(Icons.ROW));
        textSizeIcon.getChildren().setAll(Icons.letter(Icons.ROW));
        animationsIcon.getChildren().setAll(Icons.sparkles(Icons.ROW));
        saveStateIcon.getChildren().setAll(Icons.floppyDisk(Icons.ROW));
        loadStateIcon.getChildren().setAll(Icons.openFolder(Icons.ROW));

        closeSettingsButton.setGraphic(Icons.cross(Icons.SMALL));
        closeSettingsButton.setTooltip(new Tooltip("Close the settings"));
        resetButton.setGraphic(Icons.arrowBack(Icons.SMALL));

        askForExactlyTheHeightOfTheSettings();
        offerEverySkin();
        followTheTextSizeSlider();
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
     * <p>It is asked again whenever one of the three things that can change the answer changes:
     * how wide the sheet is, how large the writing is, and which skin is being worn, since each
     * skin letters the window in a face of its own. It is never asked again because the sheet
     * grew, which is what would make the question chase its own answer.
     */
    private void askForExactlyTheHeightOfTheSettings() {
        settingsScroll.prefViewportHeightProperty().bind(Bindings.createDoubleBinding(
                () -> settingsBody.prefHeight(settingsBody.getWidth()),
                settingsBody.widthProperty(),
                TextSize.scaleProperty(),
                skins.selectedToggleProperty()));
    }

    /**
     * Offers the three skins side by side, each with a drawing of what it is like.
     *
     * <p>The chooser is built here rather than in the layout file because it is a list of whatever
     * skins exist: a fourth one added to the enum appears in the sheet without this file or that
     * one being touched.
     */
    private void offerEverySkin() {
        for (Skin skin : Skin.values()) {
            ToggleButton choice = new ToggleButton(skin.getDisplayName(), skin.icon(Icons.SMALL));
            choice.setUserData(skin);
            choice.setToggleGroup(skins);
            choice.setSelected(skin == Skin.DEFAULT);
            skinChoices.getChildren().add(choice);
        }

        skins.selectedToggleProperty().addListener((observable, previous, chosen) -> {
            // A toggle group lets the chosen one be clicked again to choose nothing. The window is
            // always wearing a skin, so that click is taken to mean "leave it as it is".
            if (chosen == null) {
                skins.selectToggle(previous);
                return;
            }
            wear((Skin) chosen.getUserData());
        });
    }

    /**
     * Points the slider at the size of the writing.
     *
     * <p>The value is bound rather than copied, so the slider is the size of the writing rather
     * than a control that has to remember to go and set it. Moving it re-states the size on the
     * scene, which every label in the window is measured against, so the whole window is laid out
     * again around the new one.
     */
    private void followTheTextSizeSlider() {
        textSizeSlider.setMin(TextSize.SMALLEST);
        textSizeSlider.setMax(TextSize.LARGEST);
        textSizeSlider.setBlockIncrement(TextSize.STEP);
        textSizeSlider.setValue(TextSize.get());
        TextSize.scaleProperty().bind(textSizeSlider.valueProperty());

        textSizePercent.textProperty().bind(Bindings.format(Locale.US, PERCENTAGE,
                textSizeSlider.valueProperty().multiply(100)));
        TextSize.scaleProperty().addListener((observable, previous, chosen) -> resize());
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

    /** States the size of the writing again, without going back through the stylesheets. */
    private void resize() {
        if (scene != null) {
            chosenSkin().applyTextSizeTo(scene);
        }
    }

    private Skin chosenSkin() {
        Toggle chosen = skins.getSelectedToggle();
        return chosen == null ? Skin.DEFAULT : (Skin) chosen.getUserData();
    }

    @FXML
    private void onClose() {
        close();
    }

    /**
     * Puts the three appearance settings back to the ones the program opens with. It says nothing
     * about the system that is loaded: nothing on this sheet is allowed to throw that away.
     */
    @FXML
    private void onReset() {
        select(Skin.DEFAULT);
        textSizeSlider.setValue(TextSize.NORMAL);
        animationsToggle.setSelected(Animations.ENABLED_AT_START);
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

    private void select(Skin skin) {
        for (Toggle choice : skins.getToggles()) {
            if (choice.getUserData() == skin) {
                skins.selectToggle(choice);
                return;
            }
        }
    }
}
