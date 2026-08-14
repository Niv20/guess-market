package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/** The possible outcomes of an event, as they are listed in the file. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlOptions {

    @XmlElement(name = "GM-option")
    private List<String> optionList = new ArrayList<>();

    public List<String> getOptionList() {
        return optionList;
    }
}
