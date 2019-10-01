package de.viadee.bpm.vPAV.config.reader;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "variable")
@XmlType(propOrder = { "name", "process", "creationPoint", "scope" })
public class XmlVariable {

    private String name;

    private String process;

    private String creationPoint;

    private String scope;

    public XmlVariable() {
    }

    public XmlVariable(String name, String process, String creationPoint, String scope) {
        super();
        this.name = name;
        this.process = process;
        this.creationPoint = creationPoint;
        this.scope = scope;
    }

    @XmlAttribute(name = "name", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute(name = "process", required = true)
    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    @XmlAttribute(name = "creationPoint", required = false)
    public String getCreationPoint() {
        return creationPoint;
    }

    public void setCreationPoint(String creationPoint) {
        this.creationPoint = creationPoint;
    }

    @XmlAttribute(name = "scope", required = false)
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
