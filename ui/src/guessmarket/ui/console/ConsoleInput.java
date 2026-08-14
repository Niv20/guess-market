package guessmarket.ui.console;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Reads everything this program asks the person in front of it for.
 *
 * <p>Nothing here ever gives up on the user. A value that cannot be used is explained and asked
 * for again, so a typo can never stop the program or push it into an action that was not meant.
 * Wherever a choice is being made, entering {@code 0} steps back out to the main menu, so the
 * user is never stuck inside a question they no longer want to answer.
 */
class ConsoleInput {

    /** Entering this instead of a choice abandons the current command. */
    static final int CANCEL = 0;

    private final Scanner scanner = new Scanner(System.in);

    /**
     * @param prompt what to ask
     * @return the answer, with the spaces around it removed
     * @throws InputClosedException if there is nothing left to read
     */
    String readLine(String prompt) {
        System.out.println(prompt);
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            throw new InputClosedException();
        }
    }

    /**
     * Asks for one of the numbered items of a list.
     *
     * @param prompt      what to ask
     * @param itemCount   how many items the list holds
     * @return the chosen position, starting at 1, or {@link #CANCEL} to go back
     */
    int readSelection(String prompt, int itemCount) {
        String question = prompt + " (1-" + itemCount + ", or 0 to go back to the main menu):";
        while (true) {
            String answer = readLine(question);
            Integer number = asWholeNumber(answer);
            if (number == null) {
                System.out.println("\"" + answer + "\" is not a number. "
                        + "Type the number that appears next to the item you want.");
            } else if (number == CANCEL) {
                return CANCEL;
            } else if (number < 1 || number > itemCount) {
                System.out.println("There is no item number " + number + " in this list. "
                        + "Choose a number between 1 and " + itemCount + ", or 0 to go back.");
            } else {
                return number;
            }
        }
    }

    /**
     * Asks for a whole number of shares.
     *
     * @return the number of shares to buy, or {@link #CANCEL} to go back
     */
    long readShareCount(String prompt) {
        String question = prompt + " (a whole number of at least 1, or 0 to go back):";
        while (true) {
            String answer = readLine(question);
            Long number = asWholeLong(answer);
            if (number == null) {
                System.out.println("\"" + answer + "\" is not a whole number. "
                        + "Type how many shares you want to buy, for example 100.");
            } else if (number == CANCEL) {
                return CANCEL;
            } else if (number < 0) {
                System.out.println("You cannot buy a negative number of shares. "
                        + "Type a whole number of at least 1, or 0 to go back.");
            } else {
                return number;
            }
        }
    }

    /**
     * Asks for a menu command.
     *
     * @param commandCount how many commands the menu offers
     * @return the chosen command number, starting at 1
     */
    int readMenuChoice(int commandCount) {
        while (true) {
            String answer = readLine("Enter the number of the command you want to run (1-"
                    + commandCount + "):");
            Integer number = asWholeNumber(answer);
            if (number == null || number < 1 || number > commandCount) {
                System.out.println("\"" + answer + "\" is not one of the commands. "
                        + "Type a number between 1 and " + commandCount + ".");
            } else {
                return number;
            }
        }
    }

    /**
     * Asks for a file path.
     *
     * <p>Surrounding quotation marks are removed, because copying a path out of a file explorer
     * often brings them along, and a path that keeps them would never be found.
     *
     * @return the path, or an empty string to go back
     */
    String readFilePath(String prompt) {
        String answer = readLine(prompt + " (or leave this empty to go back to the main menu):");
        if (answer.length() >= 2 && answer.startsWith("\"") && answer.endsWith("\"")) {
            answer = answer.substring(1, answer.length() - 1).trim();
        }
        return answer;
    }

    private static Integer asWholeNumber(String text) {
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long asWholeLong(String text) {
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
