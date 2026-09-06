package guessmarket.ui.app;

import guessmarket.dto.LoadResultDto;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.exception.InvalidFileContentException;
import guessmarket.ui.common.Animations;
import guessmarket.ui.common.Dialogs;
import guessmarket.ui.common.Skin;
import guessmarket.ui.events.EventsSectionController;
import guessmarket.ui.users.UsersSectionController;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * The controller of the window as a whole.
 *
 * <p>It owns the three things that do not belong to either screen: the button that loads a file,
 * the skin the window is wearing and the switch that turns the animations on. Beyond that it does
 * as little as possible. It hands each screen the same {@link AppContext} and, whenever anything
 * changes the system, asks both of them to read the engine again. Neither screen knows the other
 * one is there.
 */
public class AppController {

    private static final String FILE_CHOOSER_TITLE = "Choose a Guess Market system details file";
    private static final String XML_FILTER_NAME = "Guess Market system details file (*.xml)";
    private static final String XML_FILTER_PATTERN = "*.xml";

    @FXML private ComboBox<Skin> skinChooser;
    @FXML private CheckBox animationsToggle;
    @FXML private Button loadFileButton;
    @FXML private TextField loadedFilePath;
    @FXML private HBox progressRow;
    @FXML private ProgressBar loadProgressBar;
    @FXML private Label loadProgressPercent;
    @FXML private Label loadMessageLabel;
    @FXML private TabPane tabPane;

    /** Injected by the fx:include elements: the controller of each included layout file. */
    @FXML private EventsSectionController eventsSectionController;
    @FXML private UsersSectionController usersSectionController;

    private GuessMarketEngine engine;
    private Stage stage;

    /** The folder the file chooser opens in, which is the one the last file came from. */
    private File lastChosenFolder;

    @FXML
    private void initialize() {
        skinChooser.getItems().setAll(Skin.values());
        skinChooser.setValue(Skin.DEFAULT);
        animationsToggle.setSelected(Animations.isEnabled());
        Animations.enabledProperty().bind(animationsToggle.selectedProperty());
    }

    /**
     * Finishes building the window once the engine and the stage exist.
     *
     * <p>This cannot happen in {@code initialize()}, which runs while the layout file is still
     * being read and therefore before there is either an engine or a window to work with.
     */
    public void start(GuessMarketEngine engineToUse, Stage hostStage) {
        this.engine = engineToUse;
        this.stage = hostStage;

        Scene scene = hostStage.getScene();
        skinChooser.valueProperty().addListener((observable, oldSkin, newSkin) -> {
            if (newSkin != null) {
                newSkin.applyTo(scene);
            }
        });

        AppContext context = new AppContext(engineToUse, hostStage, this::refreshEverything);
        for (AppSection section : sections()) {
            section.connect(context);
            section.clear();
        }
    }

    /** The button in the header. Everything about loading a file starts here. */
    @FXML
    private void onLoadFile() {
        File chosenFile = chooseFile();
        if (chosenFile == null) {
            return;
        }
        lastChosenFolder = chosenFile.getParentFile();
        startLoading(chosenFile);
    }

    private File chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(FILE_CHOOSER_TITLE);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(XML_FILTER_NAME, XML_FILTER_PATTERN));
        if (lastChosenFolder != null && lastChosenFolder.isDirectory()) {
            chooser.setInitialDirectory(lastChosenFolder);
        }
        return chooser.showOpenDialog(window());
    }

    /**
     * Runs the load on a background thread and shows how far it has got.
     *
     * <p>The bar and the two labels are bound to the task rather than written to, so nothing here
     * has to remember to update them, and nothing touches the screen from the background thread.
     */
    private void startLoading(File chosenFile) {
        LoadFileTask task = new LoadFileTask(engine, chosenFile.getAbsolutePath());

        loadProgressBar.progressProperty().bind(task.progressProperty());
        loadMessageLabel.textProperty().bind(task.messageProperty());
        loadProgressPercent.textProperty().bind(Bindings.format(Locale.US, "%.0f%%",
                task.progressProperty().multiply(100)));
        showProgressRow(true);
        loadFileButton.setDisable(true);

        task.setOnSucceeded(event -> finishLoading(() -> onFileLoaded(task.getValue())));
        task.setOnFailed(event -> finishLoading(() -> onFileRejected(task.getException())));

        Thread loader = new Thread(task, "guess-market-file-load");
        loader.setDaemon(true);
        loader.start();
    }

    /** Releases the bindings, then does whatever the outcome of the load calls for. */
    private void finishLoading(Runnable outcome) {
        loadProgressBar.progressProperty().unbind();
        loadMessageLabel.textProperty().unbind();
        loadProgressPercent.textProperty().unbind();
        loadFileButton.setDisable(false);
        outcome.run();
    }

    private void onFileLoaded(LoadResultDto result) {
        loadProgressBar.setProgress(1);
        loadProgressPercent.setText("100%");
        loadMessageLabel.setText("Loaded " + result.eventCount() + " events and "
                + result.userCount() + " users");
        loadedFilePath.setText(result.filePath());
        Animations.flash(loadProgressPercent);
        refreshEverything();
    }

    /**
     * Reports a file that was not loaded. Nothing else changes: the system that was loaded before
     * it, if there was one, is still the one on the screen.
     */
    private void onFileRejected(Throwable failure) {
        loadProgressBar.setProgress(0);
        loadProgressPercent.setText("");
        loadMessageLabel.setText("The file was not loaded.");

        if (failure instanceof InvalidFileContentException invalidFile) {
            Dialogs.fileRejected(window(), "The file was not loaded",
                    invalidFile.getMessage(), invalidFile.getProblems());
        } else {
            Dialogs.error(window(), "The file was not loaded", messageOf(failure));
        }
    }

    /** Asks both screens to read the engine again. */
    private void refreshEverything() {
        boolean loaded = engine.isSystemLoaded();
        for (AppSection section : sections()) {
            if (loaded) {
                section.refresh();
            } else {
                section.clear();
            }
        }
    }

    private List<AppSection> sections() {
        return List.of(eventsSectionController, usersSectionController);
    }

    private void showProgressRow(boolean visible) {
        progressRow.setVisible(visible);
        progressRow.setManaged(visible);
    }

    private Window window() {
        return stage;
    }

    /** @return the message the engine wrote for the user, or a readable stand in for anything else. */
    private static String messageOf(Throwable failure) {
        if (failure == null) {
            return "The file could not be loaded, and no reason was reported.";
        }
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return "The file could not be loaded (" + failure.getClass().getSimpleName() + ").";
        }
        return message;
    }
}
