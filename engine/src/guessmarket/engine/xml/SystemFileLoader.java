package guessmarket.engine.xml;

import guessmarket.engine.exception.FileAccessException;
import guessmarket.engine.exception.InvalidFileContentException;
import guessmarket.engine.exception.XmlParseException;
import guessmarket.engine.market.Commission;
import guessmarket.engine.market.Event;
import guessmarket.engine.market.GuessMarketSystem;
import guessmarket.engine.market.LmsrMechanism;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a system details file on disk into a ready to use {@link GuessMarketSystem}.
 *
 * <p>The work is done in three separate steps, and each of them can only fail in its own way:
 * the path is checked, the XML is read, and the result is validated. A system is built only
 * once all three have succeeded, which is what guarantees that a file the system rejects can
 * never leave the previously loaded data half replaced.
 */
public class SystemFileLoader {

    private static final String XML_EXTENSION = ".xml";

    private final LoadedFileValidator validator = new LoadedFileValidator();

    /**
     * Reads, checks and converts a system details file.
     *
     * @param path the full path of the file, as it was given by the caller
     * @return the system described by the file
     * @throws FileAccessException         if the path cannot be used as an XML file
     * @throws XmlParseException           if the XML itself cannot be read
     * @throws InvalidFileContentException if the file describes a system that cannot be run
     */
    public GuessMarketSystem load(String path) {
        File file = resolveFile(path);
        ReaderProblemCollector reader = new ReaderProblemCollector();
        XmlGuessMarket market = readMarket(file, reader);

        List<String> problems = new ArrayList<>(reader.getProblems());
        problems.addAll(validator.findProblems(market));
        if (!problems.isEmpty()) {
            throw new InvalidFileContentException(file.getPath(), problems);
        }
        return buildSystem(market);
    }

    private static File resolveFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileAccessException("No file path was given. "
                    + "Type the full path of the XML file you want to load.");
        }
        File file = new File(path.trim());
        if (!file.exists()) {
            throw new FileAccessException("There is no file at the path \"" + file.getPath()
                    + "\". Check the spelling of the path and that the file was not moved.");
        }
        if (file.isDirectory()) {
            throw new FileAccessException("The path \"" + file.getPath()
                    + "\" is a folder, not a file. Add the name of the XML file to the path.");
        }
        if (!file.getName().toLowerCase().endsWith(XML_EXTENSION)) {
            throw new FileAccessException("The file \"" + file.getName()
                    + "\" is not an XML file, because its name does not end with "
                    + XML_EXTENSION + ". Choose a file with an " + XML_EXTENSION + " extension.");
        }
        if (!file.canRead()) {
            throw new FileAccessException("The file \"" + file.getPath()
                    + "\" cannot be read. Check that you have permission to open it.");
        }
        return file;
    }

    private static XmlGuessMarket readMarket(File file, ReaderProblemCollector reader) {
        try {
            JAXBContext context = JAXBContext.newInstance(XmlGuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setEventHandler(reader);
            Object root = unmarshaller.unmarshal(file);
            if (!(root instanceof XmlGuessMarket market)) {
                throw new XmlParseException("The file \"" + file.getPath() + "\" is an XML file, "
                        + "but it does not describe a Guess Market system. "
                        + "Its root element must be Guess-Market.");
            }
            return market;
        } catch (JAXBException e) {
            throw new XmlParseException("The file \"" + file.getPath()
                    + "\" could not be read as XML. The XML reader reported: "
                    + rootCauseMessage(e) + ". Open the file and make sure it is a well formed "
                    + "Guess Market file that follows the course schema.", e);
        }
    }

    private static String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return cause.getClass().getSimpleName();
        }
        return message.trim().endsWith(".") ? message.trim().substring(0, message.trim().length() - 1)
                : message.trim();
    }

    private static GuessMarketSystem buildSystem(XmlGuessMarket market) {
        GuessMarketSystem system = new GuessMarketSystem();
        for (XmlEvent xmlEvent : market.getEvents().getEventList()) {
            system.addEvent(toEvent(xmlEvent));
        }
        return system;
    }

    /** Builds one event. Everything read here was already proved valid by the validator. */
    private static Event toEvent(XmlEvent xmlEvent) {
        Commission commission = new Commission(xmlEvent.getCommission().getPercent(),
                CommissionTypeParser.parse(xmlEvent.getCommission().getType()));
        LmsrMechanism mechanism =
                new LmsrMechanism(xmlEvent.getMethod().getLmsr().getLiquidityParameter());
        return new Event(xmlEvent.getId(),
                xmlEvent.getName().trim(),
                xmlEvent.getDescription().trim(),
                commission,
                trimmed(xmlEvent.getOptions().getOptionList()),
                mechanism);
    }

    private static List<String> trimmed(List<String> values) {
        List<String> trimmedValues = new ArrayList<>(values.size());
        for (String value : values) {
            trimmedValues.add(value.trim());
        }
        return trimmedValues;
    }
}
