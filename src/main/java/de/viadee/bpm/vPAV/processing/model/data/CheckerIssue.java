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
package de.viadee.bpm.vPAV.processing.model.data;

import de.viadee.bpm.vPAV.processing.checker.Checker;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.springframework.util.DigestUtils;

import java.util.List;

/**
 * Class for holding issues (errors, warnings, infos) from the checkers. Used to
 * create an issue which can be represented graphically in the HTML output
 */
public class CheckerIssue implements Comparable<CheckerIssue> {

    private final String ruleName;

    private String ruleDescription;

    private CriticalityEnum classification;

    private String bpmnFile;

    private String resourceFile;

    private String elementId;

    private String elementName;

    private String variable;

    private Anomaly anomaly;

    private List<Path> invalidPaths;

    private String message;

    private String elementDescription;

    private final String implementationDetails; // Contains expression or source code reference if applicable

    /**
     * CheckerIssue
     *
     * @param ruleName              Name of the Rule
     * @param ruleDescription       Issue ruleDescription
     * @param classification        Classification (Info, Warning or Error) of the rule
     * @param bpmnFile              Path to the BPMNFile
     * @param resourceFile          Path to resource file (e.g. dmn oder java)
     * @param elementId             Id of the Element with issue
     * @param elementName           Name of the Element with issue
     * @param variable              Name of variable
     * @param anomaly               Type of anomaly (DD, DU, UR)
     * @param invalidPaths          Invalid path
     * @param message               Issue message
     * @param elementDescription    Issue elementDescription
     * @param implementationDetails like Java class if issue is related to source code
     */
    public CheckerIssue(final String ruleName, final String ruleDescription, final CriticalityEnum classification,
            final String bpmnFile, final String resourceFile, final String elementId, final String elementName,
            final String variable, final Anomaly anomaly, final List<Path> invalidPaths, final String message,
            final String elementDescription, final String implementationDetails) {
        super();
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.variable = variable;
        this.anomaly = anomaly;
        this.invalidPaths = invalidPaths;
        this.classification = classification;
        this.bpmnFile = bpmnFile;
        this.resourceFile = resourceFile;
        this.elementId = elementId;
        this.elementName = elementName;
        this.message = message;
        this.elementDescription = elementDescription;
        this.implementationDetails = implementationDetails;
    }

    /**
     * CheckerIssue
     *
     * @param ruleName              Name of the Rule
     * @param ruleDescription       Issue ruleDescription
     * @param classification        Classification (Info, Warning or Error) of the rule
     * @param bpmnFile              Path to the BPMNFile
     * @param resourceFile          Path to resource file (e.g. dmn oder java)
     * @param elementId             Id of the Element with issue
     * @param elementName           Name of the Element with issue
     * @param variable              Variable
     * @param anomaly               Type of anomaly (DD, DU, UR)
     * @param invalidPaths          Invalid path
     * @param message               Issue message
     * @param implementationDetails like Java class if issue is related to source code
     */
    public CheckerIssue(final String ruleName, final String ruleDescription, final CriticalityEnum classification,
            final String bpmnFile, final String resourceFile, final String elementId, final String elementName,
            final String variable, final Anomaly anomaly, final List<Path> invalidPaths, final String message,
            final String implementationDetails) {
        super();
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.variable = variable;
        this.anomaly = anomaly;
        this.invalidPaths = invalidPaths;
        this.classification = classification;
        this.bpmnFile = bpmnFile;
        this.resourceFile = resourceFile;
        this.elementId = elementId;
        this.elementName = elementName;
        this.message = message;
        this.implementationDetails = implementationDetails;
    }

    /**
     * CheckerIssue
     *
     * @param ruleName              Name of the Rule
     * @param ruleDescription       Issue ruleDescription
     * @param classification        Classification (Info, Warning or Error) of the rule
     * @param bpmnFile              Path to the BPMNFile
     * @param resourceFile          Path to resource file (e.g. dmn oder java)
     * @param elementId             Id of the Element with issue
     * @param elementName           Name of the Element woth issue
     * @param message               Issue message
     * @param elementDescription    Issue elementDescription
     * @param implementationDetails like Java class if issue is related to source code
     */
    public CheckerIssue(final String ruleName, final String ruleDescription, final CriticalityEnum classification,
            final String bpmnFile, final String resourceFile, final String elementId, final String elementName,
            final String message, final String elementDescription, final String implementationDetails) {
        super();
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.classification = classification;
        this.bpmnFile = bpmnFile;
        this.resourceFile = resourceFile;
        this.elementId = elementId;
        this.elementName = elementName;
        this.message = message;
        this.elementDescription = elementDescription;
        this.implementationDetails = implementationDetails;
    }

    /**
     * CheckerIssue
     *
     * @param ruleName              Name of the Rule
     * @param ruleDescription       Issue ruleDescription
     * @param classification        Classification (Info, Warning or Error) of the rule
     * @param bpmnFile              Path to the BPMNFile
     * @param elementId             Id of the Element with issue
     * @param elementName           Name of the Element woth issue
     * @param message               Issue message
     * @param elementDescription    Issue elementDescription
     * @param implementationDetails like Java class if issue is related to source code
     */
    public CheckerIssue(final String ruleName, final String ruleDescription, final CriticalityEnum classification,
            final String bpmnFile, final String elementId, final String elementName, final String message,
            final String elementDescription, final String implementationDetails) {
        super();
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.classification = classification;
        this.bpmnFile = bpmnFile;
        this.elementId = elementId;
        this.elementName = elementName;
        this.message = message;
        this.elementDescription = elementDescription;
        this.implementationDetails = implementationDetails;
    }

    /**
     * CheckerIssue
     *
     * @param ruleName              Name of the Rule
     * @param ruleDescription       Issue ruleDescription
     * @param classification        Classification (Info, Warning or Error) of the rule
     * @param bpmnFile              Path to the BPMNFile
     * @param elementId             Id of the Element with issue
     * @param elementName           Name of the Element woth issue
     * @param message               Issue message
     * @param implementationDetails like Java class if issue is related to source code
     */
    public CheckerIssue(final String ruleName, final String ruleDescription, final CriticalityEnum classification,
            final String bpmnFile, final String elementId, final String elementName, final String message,
            final String implementationDetails) {
        super();
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.classification = classification;
        this.bpmnFile = bpmnFile;
        this.elementId = elementId;
        this.elementName = elementName;
        this.message = message;
        this.implementationDetails = implementationDetails;
    }

    /**
     * CheckerIssue
     *
     * @param ruleName              Name of the Rule
     * @param classification        Classification (Info, Warning or Error) of the rule
     * @param bpmnFile              BpmnFile
     * @param elementId             Id of the Element with issue
     * @param elementName           Name of the Element woth issue
     * @param message               Issue message
     * @param implementationDetails like Java class if issue is related to source code
     */
    public CheckerIssue(final String ruleName, final CriticalityEnum classification, final String bpmnFile,
            final String elementId, final String elementName, final String message,
            final String implementationDetails) {
        super();
        this.ruleName = ruleName;
        this.classification = classification;
        this.bpmnFile = bpmnFile;
        this.elementId = elementId;
        this.elementName = elementName;
        this.message = message;
        this.implementationDetails = implementationDetails;
    }

    /**
     * CheckerIssue
     *
     * @param ruleName              Name of the Rule
     * @param ruleDescription       Issue ruleDescription
     * @param classification        Classification (Info, Warning or Error) of the rule
     * @param bpmnFile              Path to the BPMNFile
     * @param resourceFile          Path to resource file (e.g. dmn oder java)
     * @param elementId             Id of the Element with issue
     * @param elementName           Name of the Element woth issue
     * @param variable              Name of variable
     * @param message               Issue message
     * @param description           Issue description
     * @param implementationDetails like Java class if issue is related to source code
     */
    public CheckerIssue(final String ruleName, final String ruleDescription, final CriticalityEnum classification,
            final String bpmnFile, final String resourceFile, final String elementId, final String elementName,
            final String variable, final String message, final String description, final String implementationDetails) {
        super();
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.classification = classification;
        this.bpmnFile = bpmnFile;
        this.resourceFile = resourceFile;
        this.elementId = elementId;
        this.elementName = elementName;
        this.variable = variable;
        this.message = message;
        elementDescription = description;
        this.implementationDetails = implementationDetails;
    }

    public String getId() {
        final String inputString = ruleName + "_" + cleanPath(bpmnFile) + "_" + cleanPath(resourceFile) + "_"
                + elementId + "_" + variable + "_" + message;
        return DigestUtils.md5DigestAsHex(inputString.getBytes());
    }

    // by replacing this way, ignored issues from a windows system remain ignored on
    // a linux based CI system (only relevant for old issues)
    private String cleanPath(final String path) {
        String cleanedPath = null;

        if (path != null) {
            cleanedPath = path.replace('/', '\\');
        }

        return cleanedPath;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }

    public String getVariable() {
        return variable;
    }

    public Anomaly getAnomaly() {
        return anomaly;
    }

    public List<Path> getInvalidPaths() {
        return invalidPaths;
    }

    public CriticalityEnum getClassification() {
        return classification;
    }

    public String getBpmnFile() {
        return bpmnFile;
    }

    public String getResourceFile() {
        return resourceFile;
    }

    public String getElementId() {
        return elementId;
    }

    public String getElementName() {
        return elementName;
    }

    public String getMessage() {
        return message;
    }

    public String getElementDescription() {
        return elementDescription;
    }

    public String getImplementationDetails() {
        return implementationDetails;
    }

    public void setClassification(final CriticalityEnum classification) {
        this.classification = classification;
    }

    public void setBpmnFile(final String bpmnFile) {
        this.bpmnFile = bpmnFile;
    }

    public void setElementId(final String elementId) {
        this.elementId = elementId;
    }

    public void setElementName(final String elementName) {
        this.elementName = elementName;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setElementDescription(final String elementDescription) {
        this.elementDescription = elementDescription;
    }

    public void setRuleDescription(final String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }

    @Override
    public int compareTo(CheckerIssue cI) {
        return classification.compareTo(cI.getClassification()) * (-1);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CheckerIssue && obj.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        return (elementId + elementName).hashCode();
    }
}
