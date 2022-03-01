/*
 * BSD 3-Clause License
 *
 * Copyright © 2022, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vpav.processing.dataflow;

import de.viadee.bpm.vpav.processing.model.data.ProcessVariable;

/**
 * Second step builder for building data flow rules. Optionally defines additional constraint or continues with condition.
 */
public interface ConstrainedProcessVariableSet {
    /**
     * Begins a predicate construction to define an additional constraint and
     * combine it disjunctively after its creation with existing one.
     * @return First builder of predicate construction and specifying next stage of rule building with generic parameter.
     */
    ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> orThatAre();
    /**
     * Begins a predicate construction to define an additional constraint and
     * combine it conjunctively after its creation with existing one.
     * @return First builder of predicate construction and specifying next stage of rule building with generic parameter.
     */
    ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> andThatAre();
    /**
     * Method to define a custom constraint and
     * combine it disjunctively after its creation with existing one.
     * @param constraint
     * - Constraint to be appended disjunctively
     * @return Same step builder as this is not a stage transition.
     */
    ConstrainedProcessVariableSet orThatAre(DescribedPredicateEvaluator<ProcessVariable> constraint);
    /**
     * Method to define a custom constraint and
     * combine it conjunctively after its creation with existing one.
     * @param constraint
     * - Constraint to be appended conjunctively
     * @return Same step builder as this is not a stage transition.
     */
    ConstrainedProcessVariableSet andThatAre(DescribedPredicateEvaluator<ProcessVariable> constraint);
    /**
     * Begins a predicate construction to define a condition
     * @return First builder of predicate construction and specifying next stage of rule building with generic parameter.
     */
    ProcessVariablePredicateBuilder<ConditionedProcessVariableSet> shouldBe();
    /**
     * Method to define a custom condition.
     * @param condition
     * - Condition that should be fulfilled
     * @return next step builder
     */
    ConditionedProcessVariableSet shouldBe(DescribedPredicateEvaluator<ProcessVariable> condition);
}
