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

/**
 * Second step builder for building predicates to define constraints and condition.
 * @param <T> Captures return type after predicate is constructed and represents the stage of rule building.
 */
public interface OperationBasedPredicateBuilder<T> {
    /**
     * Returns predicate based on exact operation quantity.
     * @param n
     * - Operation quantity
     * @return Step builder of next rule building stage
     */
    T exactly(int n);
    /**
     * Returns predicate based on minimal operation quantity.
     * @param n
     * - Operation quantity
     * @return Step builder of next rule building stage
     */
    T atLeast(int n);
    /**
     * Returns predicate based on maximal operation quantity.
     * @param n
     * - Operation quantity
     * @return Step builder of next rule building stage
     */
    T atMost(int n);
    /**
     * Specifies that predicate filters operations based on elements.
     * This method is inclusive making one element fulfilling the following element based predicate
     * also fulfill the entire predicate.
     * @return next predicate step builder
     */
    ElementBasedPredicateBuilder<T> byModelElements();
    /**
     * Specifies that predicate filters operations based on elements.
     * This method is exclusive so that all elements fulfilling the following element based predicate
     * need to be fulfilled for the predicate to be true.
     * @return next predicate step builder
     */
    ElementBasedPredicateBuilder<T> onlyByModelElements();
}
