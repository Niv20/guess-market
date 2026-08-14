package guessmarket.engine.exception;

import java.util.List;

/**
 * Thrown when a file was read successfully but the system it describes is not usable.
 *
 * <p>The engine deliberately keeps checking after the first problem it finds, so that the user
 * is told about everything that is wrong with the file in one go rather than fixing one
 * mistake only to be sent back for the next one.
 */
public class InvalidFileContentException extends FileLoadException {

    private static final long serialVersionUID = 1L;

    private final List<String> problems;

    public InvalidFileContentException(String filePath, List<String> problems) {
        super(buildMessage(filePath, problems));
        this.problems = List.copyOf(problems);
    }

    /** @return every problem that was found in the file, in the order they were detected. */
    public List<String> getProblems() {
        return problems;
    }

    private static String buildMessage(String filePath, List<String> problems) {
        return "The file \"" + filePath + "\" is not a valid Guess Market file. "
                + problems.size() + (problems.size() == 1 ? " problem was" : " problems were")
                + " found in it, and nothing was loaded from it.";
    }
}
