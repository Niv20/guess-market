package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/** The settings of the LMSR trading method: its liquidity parameter. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlLmsr {

    @XmlElement(name = "b")
    private Integer liquidityParameter;

    public Integer getLiquidityParameter() {
        return liquidityParameter;
    }
}
