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

import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import java.util.function.Function;
import java.util.stream.Collectors;

class ProcessVariablePredicateBuilderImpl<T> implements ProcessVariablePredicateBuilder<T> {

    private final Function<DescribedPredicateEvaluator<ProcessVariable>, T> constraintSetter;

    ProcessVariablePredicateBuilderImpl(Function<DescribedPredicateEvaluator<ProcessVariable>, T> constraintSetter) {
        this.constraintSetter = constraintSetter;
    }

    @Override
    public ProcessVariablePredicateBuilder<T> not() {
        return new ProcessVariablePredicateBuilderImpl<>(predicate -> constraintSetter.apply(predicate.inverse()));
    }

    @Override
    public OperationBasedPredicateBuilder<T> deleted() {
        return new OperationBasedPredicateBuilderImpl<>(constraintSetter, ProcessVariable::getDeletes, "deleted");
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
    public OperationBasedPredicateBuilder<T> accessed() {
        return new OperationBasedPredicateBuilderImpl<>(constraintSetter, ProcessVariable::getOperations, "accessed");
    }

    @Override
    public T prefixed(String prefix) {
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p ->
                new EvaluationResult<>(p.getName().startsWith(prefix), p);
        final String description = String.format("prefixed with '%s'", prefix);
        return constraintSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T postfixed(String postfix) {
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p ->
                new EvaluationResult<>(p.getName().endsWith(postfix), p);
        final String description = String.format("postfixed with '%s'", postfix);
        return constraintSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T matching(String regex) {
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p ->
                new EvaluationResult<>(p.getName().matches(regex), p);
        final String description = String.format("matching with '%s'", regex);
        return constraintSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }
}
