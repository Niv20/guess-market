package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

/** The commission of an event: a percentage, plus the moment at which it is collected. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlCommission {

    @XmlValue
    private Integer percent;

    @XmlAttribute(name = "type")
    private String type;

    public Integer getPercent() {
        return percent;
    }

    public String getType() {
        return type;
    }
}
