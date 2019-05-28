/**
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
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

import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;

/**
 * Third and last step builder for building predicates to define constraints and condition.
 * @param <T> Captures return type after predicate is constructed and represents the stage of rule building.
 */
public interface ElementBasedPredicateBuilder<T> {
    /**
     * Returns predicate with operation location based on type.
     * @param clazz
     * - Class 
     * @return Step builder of next rule building stage
     */
    T ofType(Class<?> clazz);
    /**
     * Returns predicate with operation location based on camunda:property.
     * @param propertyName
     * - Property name of camunda:property
     * @return Step builder of next rule building stage
     */
    T withProperty(String propertyName);
    /**
     * Returns predicate with operation location based on prefix, e.g. "ext_"
     * @param prefix
     * - Prefix to check for
     * @return Step builder of next rule building stage
     */
    T withPrefix(String prefix);
    /**
     * Returns predicate with operation location based on postfix, e.g. "_ext"
     * @param postfix
     * - Postfix to check for
     * @return Step builder of next rule building stage
     */
    T withPostfix(String postfix);
    /**
     * Returns predicate with operation location based on pattern match
     * @param regex
     * - Regex to do pattern matching
     * @return Step builder of next rule building stage
     */
    T withNameMatching(String regex);
    /**
     * Method to define a custom predicate for filtering elements.
     * @param predicate
     * - Predicate that has to be fulfilled
     * @return Step builder of next rule building stage
     */
    T thatFulfill(DescribedPredicateEvaluator<BpmnElement> predicate);
}
