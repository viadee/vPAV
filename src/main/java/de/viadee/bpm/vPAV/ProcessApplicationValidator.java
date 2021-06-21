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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.beans.BeanMappingGenerator;
import de.viadee.bpm.vPAV.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class ProcessApplicationValidator {

    private ProcessApplicationValidator() {

    }

    private static Collection<DataFlowRule> dataFlowRules = new ArrayList<>();

    public static void setDataFlowRules(Collection<DataFlowRule> dataFlowRules) {
        ProcessApplicationValidator.dataFlowRules = dataFlowRules;
    }

    /**
     * Find model errors without spring context but manual bean map
     *
     * @param beanMap Map to resolve beans
     * @return all issues
     */
    public static Collection<CheckerIssue> findModelInconsistencies(final Map<String, String> beanMap) {
        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());
        RuntimeConfig.getInstance().setBeanMapping(beanMap);
        Runner runner = createRunner();

        return runner.getFilteredIssues();
    }

    /**
     * Find model errors without spring context
     *
     * @return all issues
     */
    public static Collection<CheckerIssue> findModelInconsistencies() {
        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());
        Runner runner = createRunner();

        return runner.getFilteredIssues();
    }

    /**
     * Generate a project summary from other vPAV reports
     */
    public static void createMultiProjectReport() {
        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());
        Runner runner = new Runner();
        runner.viadeeProcessApplicationValidator(true);
    }

    /**
     * Find issues with given ApplicationContext (Spring)
     *
     * @param ctx - Spring context
     * @return all issues
     */
    public static Collection<CheckerIssue> findModelInconsistencies(ApplicationContext ctx) {
        RuntimeConfig.getInstance().setApplicationContext(ctx);
        RuntimeConfig.getInstance().setBeanMapping(BeanMappingGenerator.generateBeanMappingFile(ctx));
        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());
        Runner runner = createRunner();
        return runner.getFilteredIssues();
    }

    /**
     * Find model errors with given ApplicationContext (Spring)
     *
     * @param ctx - Spring context
     * @return all issues
     */
    public static Collection<CheckerIssue> findModelErrors(ApplicationContext ctx) {
        return filterErrors(findModelInconsistencies(ctx));
    }

    /**
     * Find model errors without spring context but manual bean map
     *
     * @param beanMap Map to resolve beans
     * @return issues with status error
     */
    public static Collection<CheckerIssue> findModelErrors(final Map<String, String> beanMap) {
        return filterErrors(findModelInconsistencies(beanMap));
    }

    /**
     * Filter an issue collection by status
     *
     * @param filteredIssues - Filtered issues
     * @return issues with status
     */
    private static Collection<CheckerIssue> filterErrors(Collection<CheckerIssue> filteredIssues) {
        Collection<CheckerIssue> filteredErrors = new ArrayList<>();

        for (CheckerIssue issue : filteredIssues) {
            if (issue.getClassification().equals(CriticalityEnum.ERROR)) {
                filteredErrors.add(issue);
            }
        }
        return filteredErrors;
    }

    /**
     * Creates a new runner and returns it
     *
     * @return Runner
     */
    private static Runner createRunner() {
        Runner runner = new Runner();
        runner.setDataFlowRules(dataFlowRules);
        dataFlowRules = new ArrayList<>();
        runner.viadeeProcessApplicationValidator();
        return runner;
    }
}
