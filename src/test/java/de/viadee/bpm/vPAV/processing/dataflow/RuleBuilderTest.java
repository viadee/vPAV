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
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static de.viadee.bpm.vPAV.processing.dataflow.RuleBuilder.processVariables;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class RuleBuilderTest {


    @Test()
    public void testRuleWithoutVariablesSucceeds() {
        DataFlowRule rule = processVariables()
                .thatAre(constraintFrom(v -> false))
                .shouldBe(constraintFrom(v -> false));

        rule.check(new ArrayList<>());
    }

    @Test()
    public void testRulesWithBrokenConditionFails() {
        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = processVariables()
                .thatAre(constraintFrom(v -> true))
                .shouldBe(constraintFrom(v -> false));

        rule.check(processVariables);
    }

    @Test()
    public void testRulesWithNonFulfillableConstraintSucceeds() {
        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = processVariables()
                .thatAre(constraintFrom(v -> false))
                .shouldBe(constraintFrom(v -> false));

        rule.check(processVariables);
    }

    @Test()
    public void testRulesWithFullfilledConstrainedAndConditionSucceeds() {
        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = processVariables()
                .thatAre(constraintFrom(v -> true))
                .shouldBe(constraintFrom(v -> true));

        rule.check(processVariables);
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

        List<ProcessVariable> filteredVariables = filterProcessVariables(variables, processVariables()
                .that().definedByServiceTasks());

        assertThat(filteredVariables.size(), is(1));
    }

    @Test
    public void testConstraintHavePrefixFiltersCorrectProcessVariables() {
        List<ProcessVariable> variables = new ArrayList<>();
        ProcessVariable processVariable = new ProcessVariable("variable1");
        variables.add(processVariable);
        processVariable = new ProcessVariable("ext_variable2");
        variables.add(processVariable);

        List<ProcessVariable> filteredVariables = filterProcessVariables(variables, processVariables()
                .that().prefixed("ext_"));

        assertThat(filteredVariables.size(), is(1));
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

        List<ProcessVariable> filteredVariables = filterProcessVariables(variables, processVariables()
                .that().definedByServiceTasks()
                .andThatAre().prefixed("ext_"));

        assertThat(filteredVariables.size(), is(1));
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

        List<ProcessVariable> filteredVariables = filterProcessVariables(variables, processVariables()
                .that().definedByServiceTasks()
                .orThatAre().prefixed("ext_"));

        assertThat(filteredVariables.size(), is(3));
    }

    @Test
    public void testWrittenConditionIsAppliedCorrectly() {

        List<ProcessVariable> variables = new ArrayList<>();
        ProcessVariable processVariable = new ProcessVariable("ext_variable1");
        variables.add(processVariable);

        DataFlowRule rule = processVariables()
                .shouldBe().written().exactly(0);

        rule.check(variables);

        processVariable.addWrite(new ProcessVariableBuilder().build());

        rule = processVariables()
                .shouldBe().written().exactly(1);

        rule.check(variables);
    }

    private static <T> DescribedPredicateEvaluator<T> constraintFrom(Predicate<T> predicate) {
        return new DescribedPredicateEvaluator<>(v -> predicate.test(v) ? EvaluationResult.forSuccess() : EvaluationResult.forViolation(null), "");
    }

    private static List<ProcessVariable> filterProcessVariables(List<ProcessVariable> variables, ConstrainedProcessVariableSet constrainedSet) {
        DescribedPredicateEvaluator<ProcessVariable> condition = spy(new DescribedPredicateEvaluator<>(v -> EvaluationResult.forSuccess(), ""));
        constrainedSet.shouldBe(condition).check(variables);

        ArgumentCaptor<ProcessVariable> classesCaptor = ArgumentCaptor.forClass(ProcessVariable.class);
        verify(condition, atLeast(0)).evaluate(classesCaptor.capture());

        return classesCaptor.getAllValues();
    }

    private class ProcessVariableBuilder {
        private String name = "variable";
        private BpmnElement element = new BpmnElement("process1", mock(BaseElement.class));
        private VariableOperation operation = VariableOperation.WRITE;


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
}
