package de.viadee.bpm.vPAV.processing.checker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class XorNamingConventionChecker extends AbstractElementChecker {

    final private String path;

    public XorNamingConventionChecker(final Rule rule, final String path) {
        super(rule);
        this.path = path;
    }

    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final BPMNScanner scan;

        try {
            scan = new BPMNScanner();
            String xor_gateway = scan.getXorGateWays(path, bpmnElement.getId());

            if (xor_gateway != null && !xor_gateway.isEmpty()) {
                if (!CheckName.checkName(bpmnElement).endsWith("?")
                        && scan.getOutgoing(path, bpmnElement.getId()) > 1) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "Naming convention of XOR gate " + CheckName.checkName(bpmnElement) + " not correct."));
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return issues;
    }

}
