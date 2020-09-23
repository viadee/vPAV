/*
 * BSD 3-Clause License
 *
 * Copyright © 2020, viadee Unternehmensberatung AG
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

import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;

import java.util.Collection;

/**
 * Data flow rule that can be evaluated in two different ways.
 * Also represents last stage of rule building construction with the option to define reason and criticality.
 */
public interface DataFlowRule {
    /**
     * Method to define a reason.
     * @param reason
     * - Reason
     * @return Same step builder as this is not a stage transition.
     */
    DataFlowRule because(String reason);
    /**
     * Method to define criticality.
     * Also implying issue type for vPAV validation.
     * @param criticality
     * - Criticality of rule
     * @return Same step builder as this is not a stage transition.
     */
    DataFlowRule withCriticality(CriticalityEnum criticality);

    /**
     * Evaluates rule on the set of process variables.
     * If at least one process variable violates the rule, an AssertionError is thrown
     * including a rule description and listing rule violations.
     * @param variables
     * - process variables to evaluate rule on.
     * @throws AssertionError in case at least one violation exists.
     */
    void check(Collection<ProcessVariable> variables);
    /**
     * Evaluates rule on the set of process variables.
     * EvaluationResult for each validated process variable is returned.
     * @param variables 
     * -process variables to evaluate rule on.
     * @return Collection of EvaluationResults for variables
     */
    Collection<EvaluationResult<ProcessVariable>> evaluate(Collection<ProcessVariable> variables);
    String getRuleDescription();
    /**
     * 
     * @param result
     * - EvaluationResult
     * @return Violation message
     */
    String getViolationMessageFor(EvaluationResult<ProcessVariable> result);
    CriticalityEnum getCriticality();
}
