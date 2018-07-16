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
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OperationBasedPredicateBuilderImplTest {
    @Test
    public void testExactlyDoesNotFilterCorrectNumberOfOperations() {
        List<ProcessVariableOperation> operations = Arrays.asList(
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(operations).exactly(2);

        assertThat(result.isFulfilled(), is(true));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString("2"));
    }

    @Test
    public void testExactlyFiltersIncorrectNumberOfOperations() {
        List<ProcessVariableOperation> operations = Arrays.asList(
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(operations).exactly(3);

        assertThat(result.isFulfilled(), is(false));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString("2"));
    }

    @Test
    public void testExactlySetsCorrectDescription() {
        List<ProcessVariableOperation> operations = Arrays.asList(
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build());

        Function<DescribedPredicateEvaluator<ProcessVariable>, Object> conditionSetter = predicate -> {
            assertThat(predicate.getDescription(), is("number of operations are exactly 2 times"));
            return new Object();
        };
        Function<ProcessVariable, List<ProcessVariableOperation>> operationsProvider = p -> operations;
        OperationBasedPredicateBuilder<Object> predicateBuilder = new OperationBasedPredicateBuilderImpl<>(
                conditionSetter, operationsProvider, "number of operations are"
        );

        predicateBuilder.exactly(2);
    }

    @Test
    public void testAtLeastDoesNotFilterCorrectNumberOfOperations() {
        List<ProcessVariableOperation> operations = Arrays.asList(
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(operations).atLeast(2);

        assertThat(result.isFulfilled(), is(true));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString("3"));
    }

    @Test
    public void testAtLeastFiltersIncorrectNumberOfOperations() {
        List<ProcessVariableOperation> operations = Arrays.asList(
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(operations).atLeast(3);

        assertThat(result.isFulfilled(), is(false));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString("2"));
    }

    @Test
    public void testAtLeastSetsCorrectDescription() {
        List<ProcessVariableOperation> operations = Arrays.asList(
                new ProcessVariableOperationBuilder().build());

        Function<DescribedPredicateEvaluator<ProcessVariable>, Object> conditionSetter = predicate -> {
            assertThat(predicate.getDescription(), is("number of operations are at least 2 times"));
            return new Object();
        };
        Function<ProcessVariable, List<ProcessVariableOperation>> operationsProvider = p -> operations;
        OperationBasedPredicateBuilder<Object> predicateBuilder = new OperationBasedPredicateBuilderImpl<>(
                conditionSetter, operationsProvider, "number of operations are"
        );

        predicateBuilder.atLeast(2);
    }
   @Test
    public void testAtMostDoesNotFilterCorrectNumberOfOperations() {
        List<ProcessVariableOperation> operations = Arrays.asList(
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(operations).atMost(4);

        assertThat(result.isFulfilled(), is(true));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString("2"));
    }

    @Test
    public void testAtMostFiltersIncorrectNumberOfOperations() {
        List<ProcessVariableOperation> operations = Arrays.asList(
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(operations).atMost(1);

        assertThat(result.isFulfilled(), is(false));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString("4"));
    }

    @Test
    public void testAtMostSetsCorrectDescription() {
        List<ProcessVariableOperation> operations = Arrays.asList(
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build(),
                new ProcessVariableOperationBuilder().build());

        Function<DescribedPredicateEvaluator<ProcessVariable>, Object> conditionSetter = predicate -> {
            assertThat(predicate.getDescription(), is("number of operations are at most 2 times"));
            return new Object();
        };
        Function<ProcessVariable, List<ProcessVariableOperation>> operationsProvider = p -> operations;
        OperationBasedPredicateBuilder<Object> predicateBuilder = new OperationBasedPredicateBuilderImpl<>(
                conditionSetter, operationsProvider, "number of operations are"
        );

        predicateBuilder.atMost(2);
    }

    private static OperationBasedPredicateBuilderImpl<EvaluationResult<ProcessVariable>> createPredicateBuilderOn(List<ProcessVariableOperation> operations) {
        Function<DescribedPredicateEvaluator<ProcessVariable>, EvaluationResult<ProcessVariable>> conditionSetter = predicate ->
                predicate.evaluate(new ProcessVariable(""));
        Function<ProcessVariable, List<ProcessVariableOperation>> operationsProvider = p -> operations;
        return new OperationBasedPredicateBuilderImpl<>(
                conditionSetter, operationsProvider,  "operation description");
    }

    private class ProcessVariableOperationBuilder {
        private VariableOperation operationType = VariableOperation.WRITE;

        ProcessVariableOperationBuilder withOperation(VariableOperation operationType) {
            this.operationType = operationType;
            return this;
        }

        ProcessVariableOperation build() {
            return new ProcessVariableOperation("name", new BpmnElement("", null), null,
                    null, "", operationType, "");
        }
    }
}
