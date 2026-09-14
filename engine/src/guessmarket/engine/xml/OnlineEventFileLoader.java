package guessmarket.engine.xml;

import guessmarket.engine.exception.FileAccessException;
import guessmarket.engine.exception.InvalidFileContentException;
import guessmarket.engine.exception.XmlParseException;
import guessmarket.engine.market.Event;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads event-only Exercise 3 uploads directly from a stream, never from a server-side file. */
public final class OnlineEventFileLoader {
    private static final Schema SCHEMA = loadSchema();
    private final LoadedFileValidator validator = new LoadedFileValidator();

    public List<Event> load(InputStream stream, String fileName, String ownerName, int firstEventId) {
        if (stream == null) {
            throw new FileAccessException("Choose an XML event file to upload.");
        }
        if (fileName == null || !fileName.trim().toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new FileAccessException("The uploaded file name must end with .xml.");
        }
        ReaderProblemCollector problems = new ReaderProblemCollector();
        XmlGuessMarket market = readMarket(stream, fileName, problems);
        List<String> validation = new ArrayList<>(problems.getProblems());
        validation.addAll(validator.findOnlineEventProblems(market));
        if (!validation.isEmpty()) {
            throw new InvalidFileContentException(fileName, validation);
        }
        List<XmlEvent> definitions = market.getEvents().getEventList();
        if (firstEventId <= 0 || (long) firstEventId + definitions.size() - 1 > Integer.MAX_VALUE) {
            throw new InvalidFileContentException(fileName,
                    List.of("There are no free event identifiers available for this upload."));
        }
        List<Event> result = new ArrayList<>();
        for (XmlEvent definition : definitions) {
            result.add(SystemFileLoader.toEvent(definition, ownerName, firstEventId + result.size()));
        }
        return List.copyOf(result);
    }

    private static Schema loadSchema() {
        try (InputStream schema = OnlineEventFileLoader.class.getResourceAsStream("GM-EX3-schema.xsd")) {
            if (schema == null) {
                throw new IllegalStateException("The server is missing its bundled Exercise 3 XML schema.");
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newSchema(new StreamSource(schema));
        } catch (SAXException | IOException error) {
            throw new IllegalStateException("The bundled Exercise 3 XML schema could not be loaded.", error);
        }
    }

    private static XmlGuessMarket readMarket(InputStream stream, String fileName,
                                             ReaderProblemCollector problems) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            reader.setEntityResolver((publicId, systemId) -> {
                throw new SAXException("External XML entities are not allowed in event uploads.");
            });
            Unmarshaller unmarshaller = JAXBContext.newInstance(XmlGuessMarket.class).createUnmarshaller();
            unmarshaller.setSchema(SCHEMA);
            unmarshaller.setEventHandler(problems);
            Object root = unmarshaller.unmarshal(new SAXSource(reader, new InputSource(stream)));
            if (!(root instanceof XmlGuessMarket market)) {
                throw new XmlParseException("The uploaded XML root must be Guess-Market.");
            }
            return market;
        } catch (JAXBException | SAXException | ParserConfigurationException error) {
            Throwable cause = error;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            throw new XmlParseException("The file \"" + fileName + "\" could not be read as an "
                    + "Exercise 3 event XML file: " + cause.getMessage(), error);
        }
    }
}
