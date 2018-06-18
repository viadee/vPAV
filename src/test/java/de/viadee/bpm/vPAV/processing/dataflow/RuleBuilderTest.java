/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung GmbH
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
package de.viadee.bpm.vPAV.processing.dataflow;

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static de.viadee.bpm.vPAV.processing.dataflow.RuleBuilder.processVariables;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class RuleBuilderTest {

    private static Condition conditionFrom(Predicate<ProcessVariable> predicate) {
        return new Condition(predicate, "");
    }

    private static <T> Constraint<T> constraintFrom(Predicate<T> predicate) {
        return new Constraint<>(predicate, "");
    }

    @Test()
    public void testRuleWithoutVariablesSucceeds() {
        DataFlowRule rule = processVariables()
                .that(constraintFrom(v -> false))
                .should(conditionFrom(v -> false));

        assertThat(rule.check(new ArrayList<>()), is(true));
    }

    @Test()
    public void testRulesWithBrokenConditionFails() {
        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = processVariables()
                .that(constraintFrom(v -> true))
                .should(conditionFrom(v -> false));

        assertThat(rule.check(processVariables), is(false));
    }

    @Test()
    public void testRulesWithNonFulfillableConstraintSucceeds() {
        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = processVariables()
                .that(constraintFrom(v -> false))
                .should(conditionFrom(v -> false));

        assertThat(rule.check(processVariables), is(true));
    }

    @Test()
    public void testRulesWithFullfilledConstrainedAndConditionSucceeds() {
        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = processVariables()
                .that(constraintFrom(v -> true))
                .should(conditionFrom(v -> true));

        assertThat(rule.check(processVariables), is(true));
    }

    @Test
    public void testConstraintAreDefinedByServiceTasksFiltersCorrectProcessVariables() {
        List<ProcessVariable> variables = new ArrayList<>();
        ProcessVariable processVariable = new ProcessVariable("variable1");
        processVariable.addDefinition(new ProcessVariableBuilder()
                .withElement(UserTask.class).withOperation(VariableOperation.WRITE).build());
        variables.add(processVariable);
        processVariable = new ProcessVariable("variable2");
        processVariable.addDefinition(new ProcessVariableBuilder()
                .withElement(ServiceTask.class).withOperation(VariableOperation.WRITE).build());
        processVariable.addWrite(new ProcessVariableBuilder()
                .withElement(ServiceTask.class).withOperation(VariableOperation.WRITE).build());
        processVariable.addDefinition(new ProcessVariableBuilder()
                .withElement(ExclusiveGateway.class).withOperation(VariableOperation.WRITE).build());
        variables.add(processVariable);

        Counter cnt = new Counter();
        processVariables()
                .that().areDefinedByServiceTasks()
                .should(conditionFrom(countingPredicate(cnt)))
                .check(variables);

        assertThat(cnt.value(), is(1));
    }

    @Test
    public void testConstraintHavePrefixFiltersCorrectProcessVariables() {
        List<ProcessVariable> variables = new ArrayList<>();
        ProcessVariable processVariable = new ProcessVariable("variable1");
        variables.add(processVariable);
        processVariable = new ProcessVariable("ext_variable2");
        variables.add(processVariable);

        Counter cnt = new Counter();
        processVariables()
                .that().havePrefix("ext_")
                .should(conditionFrom(countingPredicate(cnt)))
                .check(variables);

        assertThat(cnt.value(), is(1));
    }

    @Test()
    public void testAndConstraintConjunctionIsAppliedCorrectly() {
        List<ProcessVariable> variables = new ArrayList<>();
        ProcessVariable processVariable = new ProcessVariable("ext_variable1");
        variables.add(processVariable);
        processVariable = new ProcessVariable("variable1");
        processVariable.addDefinition(new ProcessVariableBuilder()
                .withElement(ServiceTask.class).withOperation(VariableOperation.WRITE).build());
        variables.add(processVariable);
        processVariable = new ProcessVariable("ext_variable3");
        processVariable.addDefinition(new ProcessVariableBuilder()
                .withElement(ServiceTask.class).withOperation(VariableOperation.WRITE).build());
        variables.add(processVariable);

        Counter cnt = new Counter();
        processVariables()
                .that().areDefinedByServiceTasks()
                .andThat().havePrefix("ext_")
                .should(conditionFrom(countingPredicate(cnt)))
                .check(variables);

        assertThat(cnt.value(), is(1));
    }

    @Test()
    public void testOrConstraintConjunctionIsAppliedCorrectly() {
        List<ProcessVariable> variables = new ArrayList<>();
        ProcessVariable processVariable = new ProcessVariable("ext_variable1");
        variables.add(processVariable);
        processVariable = new ProcessVariable("variable1");
        processVariable.addDefinition(new ProcessVariableBuilder()
                .withElement(ServiceTask.class).withOperation(VariableOperation.WRITE).build());
        variables.add(processVariable);
        processVariable = new ProcessVariable("ext_variable3");
        processVariable.addDefinition(new ProcessVariableBuilder()
                .withElement(ServiceTask.class).withOperation(VariableOperation.WRITE).build());
        variables.add(processVariable);
        processVariable = new ProcessVariable("variable3");
        processVariable.addDefinition(new ProcessVariableBuilder()
                .withElement(UserTask.class).withOperation(VariableOperation.WRITE).build());
        variables.add(processVariable);

        Counter cnt = new Counter();
        processVariables()
                .that().areDefinedByServiceTasks()
                .orThat().havePrefix("ext_")
                .should(conditionFrom(countingPredicate(cnt)))
                .check(variables);

        assertThat(cnt.value(), is(3));
    }

    private static Predicate<ProcessVariable> countingPredicate(Counter cnt) {
        return processVariable -> {
            cnt.increment();
            return true;
        };
    }

    private class ProcessVariableBuilder {
        private String name = "variable";
        private BpmnElement element = new BpmnElement("process1", mock(BaseElement.class));
        private VariableOperation operation = VariableOperation.WRITE;

        public ProcessVariableBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ProcessVariableBuilder withElement(Class<? extends BaseElement> clazz) {
            element = new BpmnElement("process1", mock(clazz));
            return this;
        }

        public ProcessVariableBuilder withOperation(VariableOperation operation) {
            this.operation = operation;
            return this;
        }

        public de.viadee.bpm.vPAV.processing.model.data.ProcessVariable build() {
            return new de.viadee.bpm.vPAV.processing.model.data.ProcessVariable(name, element, ElementChapter.Details,
                    KnownElementFieldType.Class, "", operation, "");
        }
    }

    private class Counter {
        private int c = 0;

        public void increment() {
            c++;
        }

        public void decrement() {
            c--;
        }

        public int value() {
            return c;
        }
    }
}
