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

import java.util.Optional;

public class EvaluationResult<T> {
    private String message;
    private boolean result;

    private T evaluatedVariable;

    static <T> EvaluationResult<T> forViolation(String message, T evaluatedVariable) {
        return new EvaluationResult<>(false, evaluatedVariable, message.isEmpty() ? null : message);
    }

    static <T> EvaluationResult<T> forViolation(T evaluatedVariable) {
        return new EvaluationResult<>(false, evaluatedVariable, null);
    }

    static <T> EvaluationResult<T> forSuccess(T evaluatedVariable) {
        return new EvaluationResult<>(true, evaluatedVariable);
    }

    static <T> EvaluationResult<T> forSuccess(String message, T evaluatedVariable) {
        return new EvaluationResult<>(true, evaluatedVariable, message.isEmpty() ? null : message);
    }

    public EvaluationResult(boolean result, T evaluatedVariable, String message) {
        this.message = message;
        this.result = result;
        this.evaluatedVariable = evaluatedVariable;
    }

    public EvaluationResult(boolean result, T evaluatedVariable) {
        this.result = result;
        this.evaluatedVariable = evaluatedVariable;
    }

    public boolean isFulfilled() {
        return result;
    }

    public T getEvaluatedVariable() {
        return evaluatedVariable;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public EvaluationResult<T> inverse() {
        return new EvaluationResult<>(!result, evaluatedVariable, message);
    }
}
