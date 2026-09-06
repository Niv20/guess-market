package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * The trading method of an event.
 *
 * <p>The schema allows exactly one of the two, so a file holding both, or neither, describes an
 * event that cannot exist and is reported by the validator rather than guessed at here.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMethod {

    @XmlElement(name = "GM-LMSR")
    private XmlLmsr lmsr;

    @XmlElement(name = "GM-order-book")
    private XmlOrderBook orderBook;

    public XmlLmsr getLmsr() {
        return lmsr;
    }

    public XmlOrderBook getOrderBook() {
        return orderBook;
    }

    public boolean hasExactlyOneMethod() {
        return (lmsr == null) != (orderBook == null);
    }
}
