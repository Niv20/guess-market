package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * The trading method of an event.
 *
 * <p>LMSR is the only method exercise 1 supports; later exercises add an order book here as a
 * second, alternative child element.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMethod {

    @XmlElement(name = "GM-LMSR")
    private XmlLmsr lmsr;

    public XmlLmsr getLmsr() {
        return lmsr;
    }
}
