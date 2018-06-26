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

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DescribedPredicateEvaluator<T> {

    private final Function<T, EvaluationResult<T>> predicateEvaluator;
    private final String description;

    public DescribedPredicateEvaluator(Function<T, EvaluationResult<T>> predicateEvaluator, String description) {
        this.predicateEvaluator = predicateEvaluator;
        this.description = description;
    }

    public EvaluationResult<T> evaluate(T value) {
        return predicateEvaluator.apply(value);
    }

    public String getDescription() {
        return description;
    }

    public DescribedPredicateEvaluator<T> or(DescribedPredicateEvaluator<T> other) {
        Function<T, EvaluationResult<T>> orPredicateEvaluator = (T) ->
        {
            EvaluationResult<T> result1 = this.evaluate(T);
            EvaluationResult<T> result2 = other.evaluate(T);
            boolean isCombinedViolation = !result1.isFulfilled() && !result2.isFulfilled();

            return createEvaluationResult(result1, result2, isCombinedViolation);
        };
        return new DescribedPredicateEvaluator<>(orPredicateEvaluator, description + " or " + other.description);
    }


    public DescribedPredicateEvaluator<T> and(DescribedPredicateEvaluator other) {
        Function<T, EvaluationResult<T>> andPredicateEvaluator = (T) ->
        {
            EvaluationResult<T> result1 = this.evaluate(T);
            EvaluationResult<T> result2 = other.evaluate(T);
            boolean isCombinedViolation = !result1.isFulfilled() || !result2.isFulfilled();

            return createEvaluationResult(result1, result2, isCombinedViolation);
        };
        return new DescribedPredicateEvaluator<>(andPredicateEvaluator, description + " and " + other.description);
    }

    public DescribedPredicateEvaluator<T> inverse() {
        return new DescribedPredicateEvaluator<>(T -> predicateEvaluator.apply(T).inverse(), "not " + description);
    }

    private EvaluationResult<T> createEvaluationResult(EvaluationResult<T> result1, EvaluationResult<T> result2, boolean isCombinedViolation) {
        if (isCombinedViolation) {
            String violationMessage = Stream.of(result1, result2)
                    .filter(r -> !r.isFulfilled())
                    .filter(r -> r.getMessage().isPresent())
                    .map(r -> r.getMessage().get())
                    .collect(Collectors.joining(" and "));
            return EvaluationResult.forViolation(violationMessage, result1.getEvaluatedVariable());
        } else {
            String successMessage = Stream.of(result1, result2)
                    .filter(EvaluationResult::isFulfilled)
                    .filter(r -> r.getMessage().isPresent())
                    .map(r -> r.getMessage().get())
                    .collect(Collectors.joining(" and "));
            return EvaluationResult.forSuccess(successMessage, result1.getEvaluatedVariable());
        }
    }

}
