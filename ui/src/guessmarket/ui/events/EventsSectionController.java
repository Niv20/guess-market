package guessmarket.ui.events;

import guessmarket.ui.app.AppContext;
import guessmarket.ui.app.AppSection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * The events screen. Placeholder until the events screen is built.
 */
public class EventsSectionController implements AppSection {

    @FXML private Label placeholderLabel;

    private AppContext context;

    @Override
    public void connect(AppContext appContext) {
        this.context = appContext;
    }

    @Override
    public void refresh() {
        placeholderLabel.setText("The events screen is not built yet. "
                + "The engine reports " + context.engine().isSystemLoaded() + " for a loaded system.");
    }

    @Override
    public void clear() {
        placeholderLabel.setText("Load a system details file to see the events.");
    }
}
