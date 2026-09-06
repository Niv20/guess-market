package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/** One event a user claims to be the market maker of: nothing but its id. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMarketMakerEvent {

    @XmlAttribute(name = "id")
    private Integer id;

    public Integer getId() {
        return id;
    }
}
