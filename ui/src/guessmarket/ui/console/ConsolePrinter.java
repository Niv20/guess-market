package guessmarket.ui.console;

import guessmarket.dto.CloseEventResultDto;
import guessmarket.dto.EventDto;
import guessmarket.dto.EventTradingStatusDto;
import guessmarket.dto.LoadResultDto;
import guessmarket.dto.OptionStateDto;
import guessmarket.dto.PurchaseResultDto;
import guessmarket.dto.StateFileResultDto;
import guessmarket.dto.TradeRecordDto;

import java.util.List;

/**
 * Everything this program shows on the screen.
 *
 * <p>All printing is gathered here, so the whole appearance of the program can be changed in
 * one file, and the rest of the user interface can stay busy with what to ask and what to do.
 * The text is plain: no colours and no clearing of the screen, so the output behaves the same
 * on every terminal and the whole session stays readable by scrolling back through it.
 */
class ConsolePrinter {

    private static final String LINE =
            "--------------------------------------------------------------------------";
    private static final String INDENT = "     ";
    private static final int LABEL_WIDTH = 38;

    void printWelcome() {
        System.out.println(LINE);
        System.out.println("Welcome to Guess Market");
        System.out.println(LINE);
        System.out.println("Load a system details file to begin, then trade in the events it holds.");
    }

    void printMenu() {
        System.out.println();
        System.out.println(LINE);
        System.out.println("Guess Market - Main Menu");
        System.out.println(LINE);
        for (MenuCommand command : MenuCommand.values()) {
            System.out.printf("  %d. %s%n", command.ordinal() + 1, command.getTitle());
        }
    }

    void printGoodbye() {
        System.out.println("Thank you for using Guess Market. Goodbye.");
    }

    void printMessage(String message) {
        System.out.println(message);
    }

    void printHeading(String heading) {
        System.out.println();
        System.out.println(LINE);
        System.out.println(heading);
        System.out.println(LINE);
    }

    /** Prints a numbered list of events, with everything that is known about each of them. */
    void printEventList(List<EventDto> events) {
        for (int i = 0; i < events.size(); i++) {
            EventDto event = events.get(i);
            System.out.printf("%d. %s%n", i + 1, event.name());
            printEventField("Event number", String.valueOf(event.id()));
            printEventField("Description", event.description());
            printEventField("Commission", ConsoleFormat.percent(event.commissionPercent())
                    + ", collected " + event.commissionType().getDisplayName().toLowerCase());
            printEventField("Options", numberedOptionNames(event.optionNames()));
            printEventField("Status", event.status().getDisplayName());
            System.out.println();
        }
    }

    /** Prints the value and the holdings of every option of an event. */
    void printCurrentState(EventTradingStatusDto status) {
        System.out.println("Current state:");
        int nameWidth = widestOptionName(status.optionStates());
        List<OptionStateDto> optionStates = status.optionStates();
        for (int i = 0; i < optionStates.size(); i++) {
            OptionStateDto option = optionStates.get(i);
            System.out.printf("%s%d. %-" + nameWidth + "s   value: %s   bought so far: %s%n",
                    INDENT, i + 1, option.name(),
                    ConsoleFormat.shareValue(option.value()),
                    ConsoleFormat.shares(option.sharesPurchased()));
        }
    }

    /** Prints the whole trading picture of an event: state, money, history and settlement. */
    void printTradingStatus(EventTradingStatusDto status) {
        EventDto event = status.event();
        printHeading("Trading status of event number " + event.id() + " - \"" + event.name() + "\"");
        System.out.println("Status: " + event.status().getDisplayName()
                + "   |   Trading method: LMSR with b = " + event.liquidityParameter());
        System.out.println();
        printCurrentState(status);
        System.out.println();
        printAccount(status);
        System.out.println();
        printTradeHistory(status.tradeHistory());
        if (status.isClosed()) {
            System.out.println();
            printSettledSummary(status);
        }
    }

    void printLoadResult(LoadResultDto result) {
        System.out.println("The file was found valid and was loaded into the system in full.");
        printField("File", result.filePath());
        printField("Events loaded", String.valueOf(result.eventCount()));
        printField("Subsidy deposited into the accounts",
                ConsoleFormat.money(result.totalSubsidy()));
    }

    void printPurchaseResult(PurchaseResultDto result) {
        printHeading("The purchase was completed");
        printField("Option bought", result.optionName());
        printField("Shares bought", ConsoleFormat.shares(result.shares()));
        printField("Paid for the shares",
                ConsoleFormat.money(result.amountPaidForShares()));
        if (result.commissionPaid() > 0) {
            printField("Commission added to the purchase",
                    ConsoleFormat.money(result.commissionPaid()));
        } else {
            printField("Commission added to the purchase",
                    "none, this event collects its commission when it is closed");
        }
        printField("Total paid", ConsoleFormat.money(result.totalPaid()));
    }

    void printCloseResult(CloseEventResultDto result) {
        printHeading("The event was closed");
        printField("Winning option", result.winningOptionName());
        printField("Shares held in the winning option",
                ConsoleFormat.shares(result.winningShares()));
        printField("Owed to the winners, $1 per share",
                ConsoleFormat.money(result.grossPayoutPot()));
        printField("Commission taken out of that amount",
                ConsoleFormat.money(result.commissionCollectedNow()));
        printField("Paid to the winners",
                ConsoleFormat.money(result.amountPaidToWinners()));
        printField("Worth of one winning share",
                ConsoleFormat.money(result.payoutPerShare()));
    }

    void printStateSaved(StateFileResultDto result) {
        System.out.println("The whole system, including everything traded so far, was saved.");
        printField("Saved to", result.filePath());
        printField("Events saved", String.valueOf(result.eventCount()));
    }

    void printStateLoaded(StateFileResultDto result) {
        System.out.println("The saved system was read back and is now the running system.");
        printField("Read from", result.filePath());
        printField("Events loaded", String.valueOf(result.eventCount()));
    }

    /** Prints a failure exactly as the engine described it, plus its details when it has any. */
    void printError(String message, List<String> details) {
        System.out.println();
        System.out.println("Sorry, that did not work.");
        System.out.println(message);
        for (String detail : details) {
            System.out.println(INDENT + "- " + detail);
        }
    }

    private void printAccount(EventTradingStatusDto status) {
        printField("Event account balance",
                ConsoleFormat.money(status.accountBalance()));
        printField("Total commission collected",
                ConsoleFormat.money(status.totalCommissionCollected()));
        printField("Subsidy deposited to open the event",
                ConsoleFormat.money(status.subsidy()));
        printField("Market maker result (without subsidy)",
                ConsoleFormat.money(status.marketMakerNetResult()));
    }

    private void printTradeHistory(List<TradeRecordDto> history) {
        if (history.isEmpty()) {
            System.out.println("Trading history: nothing has been bought in this event yet.");
            return;
        }
        System.out.println("Trading history, most recent first:");
        for (TradeRecordDto trade : history) {
            System.out.printf("%s#%d  %s  -  %s for %s%s%n",
                    INDENT, trade.serialNumber(), trade.optionName(),
                    ConsoleFormat.shares(trade.shares()),
                    ConsoleFormat.money(trade.amountPaidForShares()),
                    commissionSuffix(trade));
        }
    }

    private static String commissionSuffix(TradeRecordDto trade) {
        if (trade.commissionPaid() <= 0) {
            return "";
        }
        return " plus " + ConsoleFormat.money(trade.commissionPaid()) + " commission, "
                + ConsoleFormat.money(trade.totalPaid()) + " in total";
    }

    private void printSettledSummary(EventTradingStatusDto status) {
        System.out.println("This event has been closed.");
        printField("Winning option", status.winningOptionName());
        System.out.println(INDENT + "Total shares purchased in each option:");
        int nameWidth = widestOptionName(status.optionStates());
        for (OptionStateDto option : status.optionStates()) {
            System.out.printf("%s%s%-" + nameWidth + "s   %s%n", INDENT, INDENT,
                    option.name(), ConsoleFormat.shares(option.sharesPurchased()));
        }
    }

    private void printEventField(String label, String value) {
        System.out.printf("%s%-14s: %s%n", INDENT, label, value);
    }

    /** Prints one indented "label : value" line, with every label lined up to the same column. */
    private void printField(String label, String value) {
        System.out.printf("%s%-" + LABEL_WIDTH + "s: %s%n", INDENT, label, value);
    }

    private static String numberedOptionNames(List<String> optionNames) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < optionNames.size(); i++) {
            if (i > 0) {
                text.append("   |   ");
            }
            text.append(i + 1).append(". ").append(optionNames.get(i));
        }
        return text.toString();
    }

    private static int widestOptionName(List<OptionStateDto> optionStates) {
        int widest = 1;
        for (OptionStateDto option : optionStates) {
            widest = Math.max(widest, option.name().length());
        }
        return widest;
    }
}
