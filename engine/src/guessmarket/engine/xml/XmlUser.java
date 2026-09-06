package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;

/** A single user as they are written in a system details file. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlUser {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "initial-cash")
    private Integer initialCash;

    /** As the course schema spells it. */
    @XmlElement(name = "GM-market-maker")
    private XmlMarketMaker marketMaker;

    /** As the appendix spells it. A file written from the appendix is still a file to be read. */
    @XmlElement(name = "GM-mareket-maker")
    private XmlMarketMaker misspelledMarketMaker;

    public String getName() {
        return name;
    }

    public Integer getInitialCash() {
        return initialCash;
    }

    /** @return whichever spelling of the market maker element the file used, or null for neither. */
    public XmlMarketMaker getMarketMaker() {
        return marketMaker != null ? marketMaker : misspelledMarketMaker;
    }

    /** @return the ids of the events this user runs, in file order, empty when they run none. */
    public List<Integer> getMarketMakerEventIds() {
        XmlMarketMaker element = getMarketMaker();
        return element == null ? List.of() : element.getEventIds();
    }
}
