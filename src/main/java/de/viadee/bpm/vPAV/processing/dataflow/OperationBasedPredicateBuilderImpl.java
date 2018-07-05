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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class OperationBasedPredicateBuilderImpl<T> implements OperationBasedPredicateBuilder<T> {

    private final Function<DescribedPredicateEvaluator<ProcessVariable>, T> conditionSetter;
    private final Function<ProcessVariable, List<ProcessVariableOperation>> operationProvider;
    private final String operationDescription;

    OperationBasedPredicateBuilderImpl(
            Function<DescribedPredicateEvaluator<ProcessVariable>, T> conditionSetter,
            Function<ProcessVariable, List<ProcessVariableOperation>> operationProvider,
            String operationDescription) {
        this.conditionSetter = conditionSetter;
        this.operationProvider = operationProvider;
        this.operationDescription = operationDescription;
    }

    @Override
    public T exactly(int n) {
        final String times = n == 1 ? "time" : "times";
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p -> {
            Integer operationsCount = operationProvider.apply(p).size();
            return new EvaluationResult<>(operationsCount == n, p, operationsCount.toString());
        };
        final String description = String.format("%s exactly %s %s", operationDescription, n, times);
        return conditionSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T atLeast(int n) {
        final String times = n == 1 ? "time" : "times";
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p -> {
            Integer operationsCount = operationProvider.apply(p).size();
            return new EvaluationResult<>(operationsCount >= n, p, operationsCount.toString());
        };
        final String description = String.format("%s at least %s %s", operationDescription, n, times);
        return conditionSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T atMost(int n) {
        final String times = n == 1 ? "time" : "times";
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p -> {
            Integer operationsCount = operationProvider.apply(p).size();
            return new EvaluationResult<>(operationsCount <= n, p, operationsCount.toString());
        };
        final String description = String.format("%s at most %s %s", operationDescription, n, times);
        return conditionSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public ElementBasedPredicateBuilder<T> byModelElements() {
        final Function<ProcessVariable, List<BpmnElement>> elementProvider = createElementProvider();
        return new ElementBasedPredicateBuilderImpl<>(conditionSetter, elementProvider, false,
                operationDescription + " by model elements");
    }

    @Override
    public ElementBasedPredicateBuilder<T> onlyByModelElements() {
        final Function<ProcessVariable, List<BpmnElement>> elementProvider = createElementProvider();
        return new ElementBasedPredicateBuilderImpl<>(conditionSetter, elementProvider, true,
                operationDescription + " by model elements");
    }

    private Function<ProcessVariable, List<BpmnElement>> createElementProvider() {
        return p -> operationProvider.apply(p).stream()
                .map(ProcessVariableOperation::getElement)
                .collect(Collectors.toList());
    }
}
