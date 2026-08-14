package guessmarket.ui.console;

import guessmarket.dto.CloseEventRequestDto;
import guessmarket.dto.CloseEventResultDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.PurchaseRequestDto;
import guessmarket.dto.PurchaseResultDto;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.exception.GuessMarketException;
import guessmarket.engine.exception.InvalidFileContentException;
import guessmarket.engine.exception.SystemNotLoadedException;

import java.util.List;

/**
 * One run of the program: shows the menu, asks for a command, has the engine carry it out and
 * shows what came back, over and over until the user leaves.
 *
 * <p>This class is the active side of the system and the engine is the passive one. It decides
 * nothing about trading itself; it only collects what the user chose, hands it to the engine as
 * a request, and gives the answer to the printer.
 *
 * <p>Every command runs inside the same guard, so a rejected request, a bad file or an
 * unexpected failure all end the same way: an explanation, and then the menu again.
 */
public class ConsoleSession {

    private final GuessMarketEngine engine;
    private final ConsoleInput input = new ConsoleInput();
    private final ConsolePrinter printer = new ConsolePrinter();

    public ConsoleSession(GuessMarketEngine engine) {
        this.engine = engine;
    }

    /** Runs the program until the user asks to leave or the input ends. */
    public void run() {
        printer.printWelcome();
        boolean keepRunning = true;
        while (keepRunning) {
            printer.printMenu();
            keepRunning = runOneCommandSafely();
        }
        printer.printGoodbye();
    }

    /** @return whether the program should keep going after this command. */
    private boolean runOneCommandSafely() {
        try {
            return runOneCommand();
        } catch (InputClosedException e) {
            printer.printMessage("There is no more input to read, so the program is closing.");
            return false;
        } catch (SystemNotLoadedException e) {
            printer.printError(e.getMessage(),
                    List.of("Use command " + menuNumberOf(MenuCommand.LOAD_FILE)
                            + " in the main menu to load one."));
            return true;
        } catch (InvalidFileContentException e) {
            printer.printError(e.getMessage(), e.getProblems());
            return true;
        } catch (GuessMarketException e) {
            printer.printError(e.getMessage(), List.of());
            return true;
        } catch (RuntimeException e) {
            printer.printError("The system ran into an unexpected problem and stopped this "
                    + "command, but it is still running. Details: " + e, List.of());
            return true;
        }
    }

    private boolean runOneCommand() {
        MenuCommand command = MenuCommand.ofMenuNumber(input.readMenuChoice(MenuCommand.count()));
        switch (command) {
            case LOAD_FILE -> loadSystemDetailsFile();
            case DISPLAY_EVENTS -> displayEvents();
            case EVENT_TRADING_STATUS -> displayTradingStatus();
            case PARTICIPATE -> participateInEvent();
            case CLOSE_EVENT -> closeEvent();
            case EXIT -> {
                return false;
            }
        }
        return true;
    }

    private void loadSystemDetailsFile() {
        printer.printHeading(MenuCommand.LOAD_FILE.getTitle());
        String path = input.readFilePath("Enter the full path of the XML file you want to load");
        if (path.isEmpty()) {
            printer.printMessage("No path was entered, so nothing was loaded. "
                    + "The events that were already in the system were not changed.");
            return;
        }
        printer.printLoadResult(engine.loadSystemDetailsFile(path));
    }

    private void displayEvents() {
        List<EventDto> events = engine.getAllEvents();
        printer.printHeading("The events currently in the system (" + events.size() + ")");
        printer.printEventList(events);
    }

    private void displayTradingStatus() {
        printer.printHeading(MenuCommand.EVENT_TRADING_STATUS.getTitle());
        EventDto event = chooseFrom(engine.getAllEvents(),
                "Choose the event whose trading status you want to see");
        if (event == null) {
            return;
        }
        printer.printTradingStatus(engine.getEventTradingStatus(event.id()));
    }

    private void participateInEvent() {
        printer.printHeading(MenuCommand.PARTICIPATE.getTitle());
        EventDto event = chooseActiveEvent("Choose the event you want to participate in");
        if (event == null) {
            return;
        }

        EventTradingStatusDto status = engine.getEventTradingStatus(event.id());
        printer.printMessage("");
        printer.printCurrentState(status);

        int optionNumber = input.readSelection("Choose the option you believe in",
                event.optionNames().size());
        if (optionNumber == ConsoleInput.CANCEL) {
            return;
        }
        String optionName = event.optionNames().get(optionNumber - 1);
        long shares = input.readShareCount("How many shares of \"" + optionName
                + "\" would you like to buy?");
        if (shares == ConsoleInput.CANCEL) {
            return;
        }

        PurchaseResultDto result = engine.purchaseShares(
                new PurchaseRequestDto(event.id(), optionNumber - 1, shares));
        printer.printPurchaseResult(result);
        printer.printTradingStatus(result.statusAfterPurchase());
    }

    private void closeEvent() {
        printer.printHeading(MenuCommand.CLOSE_EVENT.getTitle());
        EventDto event = chooseActiveEvent("Choose the event you want to close");
        if (event == null) {
            return;
        }

        printer.printTradingStatus(engine.getEventTradingStatus(event.id()));
        printer.printMessage("");
        int optionNumber = input.readSelection("Choose the option the event ended with",
                event.optionNames().size());
        if (optionNumber == ConsoleInput.CANCEL) {
            return;
        }

        CloseEventResultDto result = engine.closeEvent(
                new CloseEventRequestDto(event.id(), optionNumber - 1));
        printer.printCloseResult(result);
        printer.printTradingStatus(result.statusAfterClose());
    }

    /** @return the chosen active event, or {@code null} if there is none or the user went back. */
    private EventDto chooseActiveEvent(String prompt) {
        List<EventDto> activeEvents = engine.getActiveEvents();
        if (activeEvents.isEmpty()) {
            printer.printMessage("Every event in the system has already been closed, so there is "
                    + "nothing to do here. Load a system details file to start over.");
            return null;
        }
        return chooseFrom(activeEvents, prompt);
    }

    /** Shows a numbered list and returns what was chosen, or {@code null} if the user went back. */
    private EventDto chooseFrom(List<EventDto> events, String prompt) {
        printer.printEventList(events);
        int selection = input.readSelection(prompt, events.size());
        return selection == ConsoleInput.CANCEL ? null : events.get(selection - 1);
    }

    private static int menuNumberOf(MenuCommand command) {
        return command.ordinal() + 1;
    }
}
