package de.viadee.bpm.vPAV.processing.checker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.AbstractRunner;
import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.ConstantsConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class XorNamingConventionChecker extends AbstractElementChecker {

    public XorNamingConventionChecker(final Rule rule) {
        super(rule);
    }

    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        if (element.getBaseElement() instanceof ExclusiveGateway) {
            String path;
            for (final String output : AbstractRunner.getModelPath()) {
                path = ConstantsConfig.BASEPATH + output;
                try {
                    issues.addAll(checkSingleModel(element, path));
                } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return issues;
    }

    public Collection<CheckerIssue> checkSingleModel(final BpmnElement element, String path)
            throws ParserConfigurationException, XPathExpressionException, SAXException, IOException {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();

        BPMNScanner scan = new BPMNScanner();
        String xor_gateway = scan.getXorGateWays(path, bpmnElement.getId());

        if (xor_gateway != null && !xor_gateway.isEmpty()) {
            if (!CheckName.checkName(bpmnElement).endsWith("?") && scan.getOutgoing(path, bpmnElement.getId()) > 1) {
                issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                        element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                        bpmnElement.getAttributeValue("name"), null, null, null,
                        "Naming convention of XOR gate " + CheckName.checkName(bpmnElement) + " not correct."));
            }
        }
        return issues;
    }

}
