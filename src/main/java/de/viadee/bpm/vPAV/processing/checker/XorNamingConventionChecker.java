package de.viadee.bpm.vPAV.processing.checker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.ProcessingException;
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

        if (bpmnElement instanceof ExclusiveGateway) {

            try {
                scan = new BPMNScanner();

                String xor_gateway = scan.getXorGateWays(path, bpmnElement.getId());

                if (scan.getOutgoing(path, xor_gateway) > 1) {

                    final ArrayList<ElementConvention> elementConventions = (ArrayList<ElementConvention>) rule
                            .getElementConventions();

                    if (elementConventions == null) {
                        throw new ProcessingException(
                                "xor naming convention checker must have one element convention!");
                    }

                    final String patternString = elementConventions.get(0).getPattern().trim();
                    final String taskName = bpmnElement.getAttributeValue("name");
                    if (taskName != null && taskName.trim().length() > 0) {
                        final Pattern pattern = Pattern.compile(patternString);
                        Matcher matcher = pattern.matcher(taskName);

                        if (!matcher.matches()) {
                            issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                    element.getProcessdefinition(), null, bpmnElement.getId(),
                                    bpmnElement.getAttributeValue("name"), null, null, null, "xor gateway name '"
                                            + CheckName.checkName(bpmnElement) + "' is against the naming convention"));
                        }
                    } else {
                        issues.add(
                                new CheckerIssue(rule.getName(), CriticalityEnum.ERROR, element.getProcessdefinition(),
                                        null, bpmnElement.getId(), bpmnElement.getAttributeValue("name"), null, null,
                                        null, "xor gateway name must be specified"));
                    }

                    final ArrayList<Node> edges = scan.getOutgoingEdges(path, bpmnElement.getId());
                    final String patternString2 = elementConventions.get(1).getPattern().trim();

                    for (int i = 0; i < edges.size(); i++) {
                        Element Task_Element = (Element) edges.get(i);
                        final String edgeName = Task_Element.getAttribute("name");
                        if (edgeName != null && edgeName.trim().length() > 0) {
                            final Pattern pattern = Pattern.compile(patternString2);
                            Matcher matcher = pattern.matcher(edgeName);
                            if (!matcher.matches()) {
                                issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                        element.getProcessdefinition(), null, Task_Element.getAttribute("id"),
                                        Task_Element.getAttribute("name"), null, null, null,
                                        "outgoing edges of xor gateway '"
                                                + CheckName.checkName(bpmnElement)
                                                + "' are against the naming convention"));
                            }
                        } else {
                            issues.add(
                                    new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                            element.getProcessdefinition(), null, Task_Element.getAttribute("id"),
                                            Task_Element.getAttribute("name"), null, null, null,
                                            "outgoing edges of xor gateway need to be named"));
                        }
                    }

                }

            } catch (ParserConfigurationException | SAXException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return issues;
    }

}
