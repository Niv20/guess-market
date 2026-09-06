package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

/** A single event as it is written in a system details file. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlEvent {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "id")
    private Integer id;

    @XmlElement(name = "description")
    private String description;

    /** As the course schema and the exercise 2 sample files spell it. */
    @XmlElement(name = "commission")
    private XmlCommission commission;

    /** As the appendix spells it, and as the exercise 1 files were written. */
    @XmlElement(name = "comision")
    private XmlCommission misspelledCommission;

    @XmlElement(name = "GM-options")
    private XmlOptions options;

    @XmlElement(name = "GM-method")
    private XmlMethod method;

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    /** @return whichever spelling of the commission element the file used, or null for neither. */
    public XmlCommission getCommission() {
        return commission != null ? commission : misspelledCommission;
    }

    public XmlOptions getOptions() {
        return options;
    }

    public XmlMethod getMethod() {
        return method;
    }
}
