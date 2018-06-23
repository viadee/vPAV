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

import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import java.util.function.Function;
import java.util.stream.Collectors;

class ProcessVariablePredicateBuilderImpl<T> implements ProcessVariablePredicateBuilder<T> {

    private final Function<DescribedPredicateEvaluator<ProcessVariable>, T> constraintSetter;

    ProcessVariablePredicateBuilderImpl(Function<DescribedPredicateEvaluator<ProcessVariable>, T> constraintSetter) {
        this.constraintSetter = constraintSetter;
    }

    @Override
    public OperationBasedPredicateBuilder<T> defined() {
        return new OperationBasedPredicateBuilderImpl<>(constraintSetter, ProcessVariable::getDefinitions, "defined");
    }

    @Override
    public OperationBasedPredicateBuilder<T> read() {
        return new OperationBasedPredicateBuilderImpl<>(constraintSetter, ProcessVariable::getReads, "read");
    }

    @Override
    public OperationBasedPredicateBuilder<T> written() {
        return new OperationBasedPredicateBuilderImpl<>(constraintSetter, ProcessVariable::getWrites, "written");
    }

    @Override
    public T definedByServiceTasks() {
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p -> {
            return p.getDefinitions().stream().anyMatch(o -> o.getElement().getBaseElement() instanceof ServiceTask) ?
                    EvaluationResult.forSuccess(p) :
                    EvaluationResult.forViolation("needed to be defined by ServiceTask but was defined by" +
                            p.getDefinitions().stream()
                                    .map(o -> o.getElement().getBaseElement().getClass().toString())
                                    .collect(Collectors.joining(", ")), p);
        };
        final String description = "defined by service tasks";
        return constraintSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T prefixed(String prefix) {
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p -> {
            return p.getName().startsWith(prefix) ?
                    EvaluationResult.forSuccess(p) :
                    EvaluationResult.forViolation("needed to be prefixed by " + prefix, p);
        };
        final String description = String.format("prefixed with '%s'", prefix);
        return constraintSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }
}
