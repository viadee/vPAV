package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class LinterChecker extends AbstractElementChecker {

    public LinterChecker(Rule rule) {
        super(rule);
    }

    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement bpmnElement = element.getBaseElement();
        final Map<String, Setting> settings = rule.getSettings();

        if (settings.containsKey("SETTING1")) {
            issues.addAll(checkConditionalFlows(bpmnElement, element));
        }

        return issues;
    }

    private Collection<CheckerIssue> checkConditionalFlows(BaseElement bpmnElement, BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<>();
        if (bpmnElement instanceof ExclusiveGateway && ((ExclusiveGateway) bpmnElement).getOutgoing().size() > 1) {
            final Collection<SequenceFlow> edges = ((ExclusiveGateway) bpmnElement).getOutgoing();
            for (SequenceFlow flow : edges) {
                boolean missingCondition = !hasCondition(flow) && !isDefaultFlow(bpmnElement, flow);
                if (missingCondition) {
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, flow, element.getProcessDefinition(),
                            String.format(Messages.getString("LinterChecker.1"),
                                    CheckName.checkName(bpmnElement))));
                }
            }
        }
        return issues;
    }

    private boolean hasCondition(SequenceFlow flow) {
        return flow.getConditionExpression() != null;
    }

    private boolean isDefaultFlow(BaseElement element, SequenceFlow flow) {
        return element.getAttributeValue("default") != null && element.getAttributeValue("default").equals(flow.getId());
    }
}
