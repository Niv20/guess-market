package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/** The collection of all events described by a system details file. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlEvents {

    @XmlElement(name = "GM-event")
    private List<XmlEvent> eventList = new ArrayList<>();

    public List<XmlEvent> getEventList() {
        return eventList;
    }
}
