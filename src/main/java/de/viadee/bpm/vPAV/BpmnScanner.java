/*
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.constants.BpmnConstants;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.camunda.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BpmnScanner {

    /**
     * Return the Implementation of an specific element (sendTask, ServiceTask or
     * BusinessRuleTask)
     *
     * @param element Bpmn element
     * @return return_implementation contains implementation
     */
    public static Map.Entry<String, String> getImplementation(BaseElement element) {
        String camundaClass = null, delegateExpression = null,
                expression = null, decisionRef = null, type = null;
        if (element instanceof ServiceTask) {
            camundaClass = ((ServiceTask) element).getCamundaClass();
            delegateExpression = ((ServiceTask) element).getCamundaDelegateExpression();
            expression = ((ServiceTask) element).getCamundaExpression();
            type = ((ServiceTask) element).getCamundaType();
        } else if (element instanceof BusinessRuleTask) {
            camundaClass = ((BusinessRuleTask) element).getCamundaClass();
            delegateExpression = ((BusinessRuleTask) element).getCamundaDelegateExpression();
            expression = ((BusinessRuleTask) element).getCamundaExpression();
            decisionRef = ((BusinessRuleTask) element).getCamundaDecisionRef();
            type = ((BusinessRuleTask) element).getCamundaType();
        } else if (element instanceof SendTask) {
            camundaClass = ((SendTask) element).getCamundaClass();
            delegateExpression = ((SendTask) element).getCamundaDelegateExpression();
            expression = ((SendTask) element).getCamundaExpression();
            type = ((SendTask) element).getCamundaType();
        }

        HashMap<String, String> values = new HashMap<>();
        if (camundaClass != null) {
            values.put(BpmnConstants.CAMUNDA_CLASS, camundaClass);
        } else if (delegateExpression != null) {
            values.put(BpmnConstants.CAMUNDA_DEXPRESSION, delegateExpression);
        } else if (expression != null) {
            values.put(BpmnConstants.CAMUNDA_EXPRESSION, expression);
        } else if (decisionRef != null) {
            values.put(BpmnConstants.CAMUNDA_DMN, decisionRef);
        } else if (type != null) {
            values.put(BpmnConstants.CAMUNDA_EXT, type);
        } else {
            values.put(BpmnConstants.IMPLEMENTATION, "");
        }

        return values.entrySet().iterator().next();
    }

    /**
     * Return the Implementation of an specific element (endEvent and/or
     * intermediateThrowEvent)
     *
     * @param element Element
     * @return return_implementation contains implementation
     */
    public static Map.Entry<String, String> getEventImplementation(BaseElement element) {
        Collection<EventDefinition> eventDefinitions = getEventDefinitions(element);
        HashMap<String, String> value = new HashMap<>();

        if (eventDefinitions != null) {
            for (EventDefinition eventDefinition : eventDefinitions) {
                if (eventDefinition instanceof MessageEventDefinition) {
                    MessageEventDefinition messageEventDefinition = (MessageEventDefinition) eventDefinition;
                    if (messageEventDefinition.getCamundaExpression() != null) {
                        value.put(BpmnConstants.CAMUNDA_EXPRESSION, messageEventDefinition.getCamundaExpression());
                    } else if (messageEventDefinition.getCamundaDelegateExpression() != null) {
                        value.put(BpmnConstants.CAMUNDA_DEXPRESSION,
                                messageEventDefinition.getCamundaDelegateExpression());
                    } else if (messageEventDefinition.getCamundaClass() != null) {
                        value.put(BpmnConstants.CAMUNDA_CLASS, messageEventDefinition.getCamundaClass());
                    } else if (messageEventDefinition.getCamundaType() != null) {
                        value.put(BpmnConstants.CAMUNDA_EXT, messageEventDefinition.getCamundaType());
                    } else {
                        value.put(BpmnConstants.IMPLEMENTATION, "");
                    }
                }
            }
        }

        if (value.isEmpty()) {
            return null;
        }
        return value.entrySet().iterator().next();
    }

    public static Collection<EventDefinition> getEventDefinitions(BaseElement element) {
        if (element instanceof StartEvent) {
            return ((StartEvent) element).getEventDefinitions();
        } else if (element instanceof EndEvent) {
            return ((EndEvent) element).getEventDefinitions();
        } else if (element instanceof ThrowEvent) {
            return ((ThrowEvent) element).getEventDefinitions();
        } else if (element instanceof CatchEvent) {
            return ((CatchEvent) element).getEventDefinitions();
        } else {
            return null;
        }
    }

    /**
     * @param element Element
     * @param extType Type of Listener
     * @return value of Listener
     */
    public static ArrayList<ModelElementInstance> getListener(BaseElement element, String extType) {
        ExtensionElements extensions = element.getExtensionElements();
        ArrayList<ModelElementInstance> listener = new ArrayList<>();

        if (extensions != null) {
            for (ModelElementInstance el : extensions.getElements()) {
                if (extType.equals(BpmnConstants.CAMUNDA_EXECUTION_LISTENER) &&
                        el instanceof CamundaExecutionListener) {
                    listener.add(el);

                } else if (extType.equals(BpmnConstants.CAMUNDA_TASK_LISTENER) && el instanceof CamundaTaskListener) {
                    listener.add(el);
                }
            }
        }

        return listener;
    }

    /**
     * Check if model has an scriptTag
     *
     * @param element Element
     * @return scriptPlaces contains script type
     */
    public static ArrayList<String> getScriptTypes(BaseElement element) {
        // bool to hold return values
        ArrayList<String> returnScriptType = new ArrayList<>();

        Collection<CamundaScript> scripts = element.getModelInstance().getModelElementsByType(CamundaScript.class);

        for (CamundaScript script : scripts) {
            if (isChildOf(element, script)) {
                returnScriptType.add(script.getParentElement().getElementType().getTypeName());
            }
        }

        return returnScriptType;
    }

    private static boolean isChildOf(BaseElement element, ModelElementInstance child) {
        if (child == null || child.getParentElement() == null) {
            return false;
        }
        if (child.getParentElement().equals(element)) {
            return true;
        }
        return isChildOf(element, child.getParentElement());
    }

    /**
     * Returns whether the given element is part of a subprocess.
     *
     * @param element Element to check
     * @return true if element is in subprocess
     */
    public static boolean isSubprocess(ModelElementInstance element) {
        if (element.getParentElement() instanceof Process) {
            return false;
        }
        if (element.getParentElement() instanceof SubProcess) {
            return true;
        }
        return isSubprocess(element.getParentElement());
    }

    /**
     * check if sequenceFlow has an Script (value in language attribute) in
     * conditionalExpression
     *
     * @param element SequenceFlow
     * @return true if script is used
     */
    public static boolean hasScriptInCondExp(SequenceFlow element) {
        if (element.getConditionExpression() != null) {
            return element.getConditionExpression().getLanguage() != null && !element.getConditionExpression()
                    .getLanguage().isEmpty();
        }
        return false;
    }

    /**
     * get ids and timer definition for all timer event types
     *
     * @param element Element
     * @return Map with timerEventDefinition-Node and his child
     */
    public static ArrayList<TimerEventDefinition> getTimerImplementation(BaseElement element) {
        ArrayList<TimerEventDefinition> timers = new ArrayList<>();
        Collection<EventDefinition> eventDefinitions = getEventDefinitions(element);
        if (eventDefinitions != null) {
            for (EventDefinition ed : eventDefinitions) {
                if (ed instanceof TimerEventDefinition) {
                    timers.add(((TimerEventDefinition) ed));
                }
            }
        }
        return timers;
    }

    /**
     * get value of expression
     *
     * @param element element
     * @return value of expression
     */
    public static ArrayList<String> getFieldInjectionExpression(BaseElement element) {
        ArrayList<String> varNames = new ArrayList<>();
        if (element.getExtensionElements() != null) {
            for (ModelElementInstance extension : element.getExtensionElements().getElements()) {
                if (extension instanceof CamundaField) {
                    varNames.add(((CamundaField) extension).getCamundaExpression());
                }
            }
        }
        return varNames;
    }

    /**
     * get names of variable in fieldInjection
     *
     * @param element Element
     * @return names of variable
     */
    public static ArrayList<String> getFieldInjectionVarName(BaseElement element) {
        ArrayList<String> varNames = new ArrayList<>();
        if (element.getExtensionElements() != null) {
            for (ModelElementInstance extension : element.getExtensionElements().getElements()) {
                if (extension instanceof CamundaField) {
                    varNames.add(((CamundaField) extension).getCamundaName());
                }
            }
        }
        return varNames;
    }

    /**
     * Returns errorEventDefinition of Boundary event if existing.
     *
     * @param event Boundary event
     * @return ErrorEventDefinition
     */
    public static ErrorEventDefinition getErrorEventDefinition(BoundaryEvent event) {
        for (EventDefinition ed : event.getEventDefinitions()) {
            if (ed instanceof ErrorEventDefinition) {
                return (ErrorEventDefinition) ed;
            }
        }
        return null;
    }

    /**
     * Returns properties of element.
     *
     * @param element Bpmn element
     * @return map with properties (name - value) of given element
     */
    public static Map<String, String> getProperties(BaseElement element) {
        final Map<String, String> properties = new HashMap<>();
        if (element.getExtensionElements() != null) {
            for (ModelElementInstance el : element.getExtensionElements().getElements()) {
                if (el instanceof CamundaProperties) {
                    for (CamundaProperty property : ((CamundaProperties) el).getCamundaProperties()) {
                        properties.put(property.getCamundaName(), property.getCamundaValue());
                    }
                }
            }
        }

        return properties;
    }
}
