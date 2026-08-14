package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The root element of a system details file.
 *
 * <p>The classes in this package mirror the structure of the file one to one and hold nothing
 * but the raw values that were read from it. Numbers are bound to wrapper types on purpose, so
 * that a missing element arrives as {@code null} and can be reported as missing instead of
 * quietly turning into a zero. Turning these raw values into events, and rejecting the ones
 * that do not make sense, is the job of the loader and the validator.
 */
@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlGuessMarket {

    @XmlElement(name = "GM-events")
    private XmlEvents events;

    public XmlEvents getEvents() {
        return events;
    }
}
