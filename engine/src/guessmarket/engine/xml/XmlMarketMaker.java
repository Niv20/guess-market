package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/** The events one user is the market maker of, as they are listed in the file. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMarketMaker {

    @XmlElement(name = "event")
    private List<XmlMarketMakerEvent> events = new ArrayList<>();

    /** @return the ids listed, including any that is missing, which arrives as null. */
    public List<Integer> getEventIds() {
        List<Integer> ids = new ArrayList<>(events.size());
        for (XmlMarketMakerEvent event : events) {
            ids.add(event.getId());
        }
        return ids;
    }
}
