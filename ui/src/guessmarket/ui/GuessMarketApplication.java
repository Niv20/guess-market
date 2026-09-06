package guessmarket.ui;

import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.GuessMarketEngineFactory;
import guessmarket.ui.app.AppController;
import guessmarket.ui.common.Skin;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * The JavaFX application itself: it builds the one window of the program and hands the engine to
 * the controller that drives it.
 *
 * <p>This is the only place in which an engine is created, and it is created through the factory,
 * so nothing in the user interface ever names an implementation class. From here on the engine is
 * only ever seen as the {@link GuessMarketEngine} interface.
 */
public class GuessMarketApplication extends Application {

    private static final String APP_FXML = "/guessmarket/ui/app/app.fxml";
    private static final String WINDOW_TITLE = "Guess Market";

    /** Small enough to fit a laptop screen, large enough for the whole layout to make sense. */
    private static final double INITIAL_WIDTH = 1360;
    private static final double INITIAL_HEIGHT = 820;

    /**
     * The window may be made as small as this. Everything inside it lives in scroll panes, so at
     * that size the layout is scrolled rather than clipped, and every control can still be reached.
     */
    private static final double MINIMUM_WIDTH = 720;
    private static final double MINIMUM_HEIGHT = 520;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(resource(APP_FXML));
        Parent root = loader.load();

        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
        Skin.applyDefault(scene);

        stage.setTitle(WINDOW_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(MINIMUM_WIDTH);
        stage.setMinHeight(MINIMUM_HEIGHT);

        GuessMarketEngine engine = GuessMarketEngineFactory.createEngine();
        AppController controller = loader.getController();
        controller.start(engine, stage);

        stage.show();
    }

    /**
     * @param path an absolute resource path inside the jar
     * @return the resource
     * @throws IllegalStateException if the jar was built without it, which is a packaging fault
     *                               rather than something the person using the program can fix
     */
    private URL resource(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            throw new IllegalStateException("The application file \"" + path
                    + "\" is missing from the program. The program was not packaged correctly.");
        }
        return url;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
