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
package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.BpmnModelDispatcher;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariableReader;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class DataFlowChecker extends AbstractModelChecker {

    private Rule rule;
    private Collection<DataFlowRule> dataFlowRules;
    private Collection<ProcessVariable> processVariables;

    public DataFlowChecker(Rule rule, BpmnScanner bpmnScanner, File processDefinition, FileScanner fileScanner,
                           ProcessVariablesScanner variablesScanner) {
        super(rule, bpmnScanner, processDefinition, fileScanner, variablesScanner);
        this.setupChecker();
    }

    private void setupChecker() {
        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        // hold bpmn elements
        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);
        ProcessVariableReader variableReader = new ProcessVariableReader(rule, bpmnScanner, fileScanner);
        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(
                variablesScanner.getMessageIdToVariableMap(), variablesScanner.getProcessIdToVariableMap(), bpmnScanner, variableReader, fileScanner);
        // Data flow checker
        final Collection<BpmnElement> bpmnElements = BpmnModelDispatcher.getBpmnElements(processDefinition, baseElements, graphBuilder);
        processVariables = BpmnModelDispatcher.getProcessVariables(bpmnElements);
    }

    @Override
    public Collection<CheckerIssue> check() {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        for (DataFlowRule dataFlowRule : dataFlowRules) {
            dataFlowRule.evaluate(processVariables).stream()
                    .filter(r-> !r.isFulfilled())
                    .map(r -> IssueWriter.createIssue(rule, dataFlowRule.getRuleDescription(), dataFlowRule.getCriticality(),
                            r.getEvaluatedVariable(), dataFlowRule.getViolationMessageFor(r)))
                    .forEach(issues::addAll);
        }
        return issues;
    }

    public boolean isSingletonChecker() {
        return true;
    }
}
