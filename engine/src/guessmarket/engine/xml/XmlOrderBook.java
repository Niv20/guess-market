package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/** The settings of the order book trading method, as they are written in the file. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlOrderBook {

    @XmlAttribute(name = "allow-mint")
    private String allowMint;

    /** As the course schema and the sample files spell it. */
    @XmlAttribute(name = "initial")
    private Integer initial;

    /** As the appendix spells it in its example. */
    @XmlAttribute(name = "inital")
    private Integer misspelledInitial;

    @XmlAttribute(name = "d")
    private Integer baseValue;

    public String getAllowMint() {
        return allowMint;
    }

    /** @return whichever spelling of the initial investment the file used, or null for neither. */
    public Integer getInitialInvestment() {
        return initial != null ? initial : misspelledInitial;
    }

    public Integer getBaseValue() {
        return baseValue;
    }
}
