package guessmarket.engine.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/** All of the users described by a system details file. */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlUsers {

    @XmlElement(name = "GM-user")
    private List<XmlUser> userList = new ArrayList<>();

    public List<XmlUser> getUserList() {
        return userList;
    }
}
