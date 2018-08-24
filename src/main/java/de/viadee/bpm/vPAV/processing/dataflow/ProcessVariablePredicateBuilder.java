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

/**
 * Initial step builder for building predicates to define constraints and condition.
 * @param <T> Captures return type after predicate is constructed and represents the stage of rule building.
 */
public interface ProcessVariablePredicateBuilder<T> {
    /**
     * Negates a predicate.
     * @return this as negation does not lead to next stage.
     */
    ProcessVariablePredicateBuilder<T> not();

    /**
     * Specifies operation and continues with next stages.
     * @return next predicate step builder
     */
    OperationBasedPredicateBuilder<T> deleted();
    /**
     * Specifies operation and continues with next stages.
     * @return next predicate step builder
     */
    OperationBasedPredicateBuilder<T> read();
    /**
     * Specifies operation and continues with next stages.
     * @return next predicate step builder
     */
    OperationBasedPredicateBuilder<T> written();
    /**
     * Specifies operation and continues with next stages.
     * @return next predicate step builder
     */
    OperationBasedPredicateBuilder<T> accessed();
    /**
     * Returns predicate based on process variable name prefix, e.g. "ext_".
     * @return Step builder of next rule building stage
     */
    T prefixed(String prefix);
    /**
     * Returns predicate based on process variable name prefix, e.g. "_ext".
     * @return Step builder of next rule building stage
     */
    T postfixed(String postfix);
    /**
     * Returns predicate based on process variable name matching a RegEx.
     * @return Step builder of next rule building stage
     */
    T matching(String regex);
}
