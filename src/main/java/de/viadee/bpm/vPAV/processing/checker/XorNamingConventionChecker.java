package de.viadee.bpm.vPAV.processing.checker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
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

                    final Collection<ElementConvention> elementConventions = rule.getElementConventions();

                    if (elementConventions == null || elementConventions.size() < 1 || elementConventions.size() > 1) {
                        throw new ProcessingException(
                                "xor naming convention checker must have one element convention!");
                    }

                    final String patternString = elementConventions.iterator().next().getPattern();
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
                }

            } catch (ParserConfigurationException | SAXException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return issues;
    }

}
