package guessmarket.engine.xml;

import guessmarket.engine.exception.FileAccessException;
import guessmarket.engine.exception.InvalidFileContentException;
import guessmarket.engine.exception.XmlParseException;
import guessmarket.engine.market.Commission;
import guessmarket.engine.market.Event;
import guessmarket.engine.market.GuessMarketSystem;
import guessmarket.engine.market.LmsrEvent;
import guessmarket.engine.market.OrderBookEvent;
import guessmarket.engine.market.User;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a system details file on disk into a ready to use {@link GuessMarketSystem}.
 *
 * <p>The work is done in three separate steps, and each of them can only fail in its own way: the
 * path is checked, the XML is read, and the result is validated. A system is built only once all
 * three have succeeded, which is what guarantees that a file the system rejects can never leave the
 * previously loaded data half replaced.
 */
public class SystemFileLoader {

    private static final String XML_EXTENSION = ".xml";
    private static final String TRUE_VALUE = "true";

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

    // ------------------------------------------------------------------ the file itself

    private static File resolveFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileAccessException("No file was chosen. "
                    + "Use the Load File button and pick the XML file you want to load.");
        }
        File file = new File(path.trim());
        if (!file.exists()) {
            throw new FileAccessException("There is no file at the path \"" + file.getPath()
                    + "\". Check that the file was not moved or renamed.");
        }
        if (file.isDirectory()) {
            throw new FileAccessException("The path \"" + file.getPath()
                    + "\" is a folder, not a file. Choose the XML file inside it.");
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
        String trimmed = message.trim();
        return trimmed.endsWith(".") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    // ------------------------------------------------------------------ building the system

    /**
     * Builds the system the file describes.
     *
     * <p>The users come first, because an event has to be told who its market maker is when it is
     * created, and the validator has already made certain that every event has exactly one.
     */
    private static GuessMarketSystem buildSystem(XmlGuessMarket market) {
        GuessMarketSystem system = new GuessMarketSystem();
        for (XmlUser xmlUser : market.getUsers().getUserList()) {
            system.addUser(new User(xmlUser.getName().trim(), xmlUser.getInitialCash()));
        }

        Map<Integer, String> marketMakerByEventId = marketMakerNames(market);
        for (XmlEvent xmlEvent : market.getEvents().getEventList()) {
            String marketMakerName = marketMakerByEventId.get(xmlEvent.getId());
            system.addEvent(toEvent(xmlEvent, marketMakerName));
            system.assignMarketMaker(marketMakerName, xmlEvent.getId());
        }
        return system;
    }

    /** @return which user runs which event, taken from the market maker element of every user. */
    private static Map<Integer, String> marketMakerNames(XmlGuessMarket market) {
        Map<Integer, String> namesByEventId = new LinkedHashMap<>();
        for (XmlUser xmlUser : market.getUsers().getUserList()) {
            for (Integer eventId : xmlUser.getMarketMakerEventIds()) {
                namesByEventId.put(eventId, xmlUser.getName().trim());
            }
        }
        return namesByEventId;
    }

    /** Builds one event. Everything read here was already proved valid by the validator. */
    private static Event toEvent(XmlEvent xmlEvent, String marketMakerName) {
        Commission commission = new Commission(xmlEvent.getCommission().getPercent(),
                CommissionTypeParser.parse(xmlEvent.getCommission().getType()));
        int id = xmlEvent.getId();
        String name = xmlEvent.getName().trim();
        String description = xmlEvent.getDescription().trim();
        List<String> optionNames = trimmed(xmlEvent.getOptions().getOptionList());

        XmlLmsr lmsr = xmlEvent.getMethod().getLmsr();
        if (lmsr != null) {
            return new LmsrEvent(id, name, description, commission, optionNames,
                    marketMakerName, lmsr.getLiquidityParameter());
        }
        XmlOrderBook book = xmlEvent.getMethod().getOrderBook();
        return new OrderBookEvent(id, name, description, commission, optionNames, marketMakerName,
                book.getInitialInvestment(), book.getBaseValue(),
                TRUE_VALUE.equalsIgnoreCase(book.getAllowMint().trim()));
    }

    private static List<String> trimmed(List<String> values) {
        List<String> trimmedValues = new ArrayList<>(values.size());
        for (String value : values) {
            trimmedValues.add(value.trim());
        }
        return trimmedValues;
    }
}
