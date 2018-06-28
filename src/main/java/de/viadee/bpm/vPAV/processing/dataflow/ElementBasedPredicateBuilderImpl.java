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
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ElementBasedPredicateBuilderImpl<T> implements ElementBasedPredicateBuilder<T> {

    private final Function<DescribedPredicateEvaluator<ProcessVariable>, T> conditionSetter;
    private final Function<ProcessVariable, List<BpmnElement>> elementProvider;
    private final String elementDescription;
    private boolean onlyFlag = false;

    public ElementBasedPredicateBuilderImpl(
            Function<DescribedPredicateEvaluator<ProcessVariable>, T> conditionSetter,
            Function<ProcessVariable,List<BpmnElement>> elementProvider,
            boolean onlyFlag,
            String elementDescription) {
        this.conditionSetter = conditionSetter;
        this.elementProvider = elementProvider;
        this.onlyFlag = onlyFlag;
        this.elementDescription = elementDescription;
    }

    @Override
    public T ofType(Class clazz) {
        final Function<BpmnElement, EvaluationResult<BpmnElement>> evaluator = element -> {
            return new EvaluationResult<>(clazz.isInstance(element.getBaseElement()), element,
                    element.getBaseElement().getClass().getName());
        };
        final String description = String.format("of type %s", clazz);
        return thatFulfill(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T withPrefix(String prefix) {
        final Function<BpmnElement, EvaluationResult<BpmnElement>> evaluator = element -> {
            String elementName = element.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME);
            return new EvaluationResult<>(elementName.startsWith(prefix), element, elementName);
        };
        final String description = String.format("with prefix %s", prefix);
        return thatFulfill(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T withPostfix(String postfix) {
        final Function<BpmnElement, EvaluationResult<BpmnElement>> evaluator = element -> {
            String elementName = element.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME);
            return new EvaluationResult<>(elementName.endsWith(postfix), element, elementName);
        };
        final String description = String.format("with postfix %s", postfix);
        return thatFulfill(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T withNameMatching(String regex) {
        final Function<BpmnElement, EvaluationResult<BpmnElement>> evaluator = element -> {
            String elementName = element.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME);
            return new EvaluationResult<>(Pattern.matches(regex, elementName), element, elementName);
        };
        final String description = String.format("with name matching %s", regex);
        return thatFulfill(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T thatFulfill(DescribedPredicateEvaluator<BpmnElement> predicate) {
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p -> {
            List<EvaluationResult<BpmnElement>> results = elementProvider.apply(p).stream()
                    .map(predicate::evaluate).collect(Collectors.toList());
            return isSuccess(results) ?
                    EvaluationResult.forSuccess(results.stream()
                                    .filter(EvaluationResult::isFulfilled)
                                    .filter(r -> r.getMessage().isPresent())
                                    .map(r -> r.getMessage().get())
                                    .collect(Collectors.joining(", ")), p) :
                    EvaluationResult.forViolation(results.stream()
                                    .filter(r -> !r.isFulfilled())
                                    .filter(r -> r.getMessage().isPresent())
                                    .map(r -> r.getMessage().get())
                                    .collect(Collectors.joining(", ")), p);
        };
        final String description = String.format("%s %s", elementDescription, predicate.getDescription());
        return conditionSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    private boolean isSuccess(List<EvaluationResult<BpmnElement>> results) {
        int numberOfSuccesses = results.stream().filter(EvaluationResult::isFulfilled).collect(Collectors.toList()).size();
        return onlyFlag ?
                numberOfSuccesses == results.size() :
                numberOfSuccesses > 0;
    }
}
