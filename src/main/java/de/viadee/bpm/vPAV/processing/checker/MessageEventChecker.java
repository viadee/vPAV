package de.viadee.bpm.vPAV.processing.checker;

import java.util.ArrayList;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class MessageEventChecker extends AbstractElementChecker {

    public MessageEventChecker(final Rule rule) {
        super(rule);
    }

    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement baseElement = element.getBaseElement();

        if (baseElement.getElementType().getTypeName()
                .equals(BpmnModelConstants.BPMN_ELEMENT_END_EVENT)
                || baseElement.getElementType().getTypeName()
                        .equals(BpmnModelConstants.BPMN_ELEMENT_START_EVENT)
                || baseElement.getElementType().getTypeName()
                        .equals(BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_CATCH_EVENT)
                || baseElement.getElementType().getTypeName()
                        .equals(BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT)) {

            final Event event = (Event) baseElement;
            final Collection<MessageEventDefinition> messageEventDefinitions = event
                    .getChildElementsByType(MessageEventDefinition.class);
            if (messageEventDefinitions != null) {
                for (MessageEventDefinition eventDef : messageEventDefinitions) {
                    if (eventDef != null) {
                        final Message message = eventDef.getMessage();
                        if (message == null || message.getName() == null || message.getName().isEmpty()) {
                            issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                    element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                                    baseElement.getAttributeValue("name"), null, null, null,
                                    "No message has been specified for '" + CheckName.checkName(baseElement)
                                            + "'."));
                        }
                    }
                }
            }
        }
        return issues;
    }

}
