package guessmarket.ui.users;

import guessmarket.ui.app.AppContext;
import guessmarket.ui.app.AppSection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * The users screen. Placeholder until the users screen is built.
 */
public class UsersSectionController implements AppSection {

    @FXML private Label placeholderLabel;

    private AppContext context;

    @Override
    public void connect(AppContext appContext) {
        this.context = appContext;
    }

    @Override
    public void refresh() {
        placeholderLabel.setText("The users screen is not built yet. "
                + "The engine reports " + context.engine().isSystemLoaded() + " for a loaded system.");
    }

    @Override
    public void clear() {
        placeholderLabel.setText("Load a system details file to see the users.");
    }
}
