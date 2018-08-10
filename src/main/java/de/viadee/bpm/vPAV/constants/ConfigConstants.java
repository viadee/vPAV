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
package de.viadee.bpm.vPAV.constants;

/**
 * Class to hold global constants
 */

public final class ConfigConstants {

    public static final String RULESET = "ruleSet.xml";

    public static final String RULESETDEFAULT = "ruleSetDefault.xml";

    public static final String RULESETPARENT = "parentRuleSet.xml";

    public static final String HASPARENTRULESET = "HasParentRuleSet";

    public static final String IGNORE_FILE = "src/test/resources/ignoreIssues.txt";

    public static final String IGNORE_FILE_OLD = "src/test/resources/.ignoreIssues";

    public static final String BPMN_FILE_PATTERN = "**/*.bpmn";

    public static final String DMN_FILE_PATTERN = "**/*.dmn";

    public static final String SCRIPT_FILE_PATTERN = "**/*.groovy";

    public static final String JAVA_FILE_PATTERN = "**/*.java";

    public static final String EFFECTIVE_RULESET = "target/vPAV/effectiveRuleSet.xml";

    public static final String VALIDATION_XML_OUTPUT = "target/vPAV/bpmn_validation.xml";

    public static final String VALIDATION_JS_MODEL_OUTPUT = "target/vPAV/js/bpmn_model.js";

    public static final String VALIDATION_JS_OUTPUT = "target/vPAV/js/bpmn_validation.js";

    public static final String VALIDATION_CHECKERS = "target/vPAV/js/checkers.js";

    public static final String VALIDATION_JS_SUCCESS_OUTPUT = "target/vPAV/js/bpmn_validation_success.js";

    public static final String VALIDATION_ISSUE_SEVERITY = "target/vPAV/js/issue_severity.js";

    public static final String VALIDATION_JS_TMP = "target/vPAV/js/tmp.js";

    public static final String VALIDATION_JS_PROCESSVARIABLES = "target/vPAV/js/processVariables.js";

    public static final String VALIDATION_JSON_OUTPUT = "target/vPAV/bpmn_validation.json";

    public static final String VALIDATION_IGNORED_ISSUES_OUTPUT = "target/vPAV/js/ignoredIssues.js";

    public static final String VALIDATION_FOLDER = "target/vPAV/";

    public static final String JS_FOLDER = "target/vPAV/js/";

    public static final String CSS_FOLDER = "target/vPAV/css/";

    public static final String IMG_FOLDER = "target/vPAV/img/";

    public static final String BASEPATH = "src/main/resources/";

    public static final String JAVAPATH = "src/main/java/";

    public static final String TEST_JAVAPATH = "src/test/java/";

    public static final String TEST_BASEPATH = "src/test/resources/";

    public static final String JS_BASEPATH = "src\\main\\resources\\";

    public static final String TARGET_CLASS_FOLDER = "target/classes";

    public static final String LOCATION = "location";

    public static final String VERSIONINGSCHEMEPACKAGE = "versioningSchemePackage";

    public static final String VERSIONINGSCHEMECLASS = "versioningSchemeClass";

    public static final String GROOVY = "groovy";

    public static final String RULENAME = "rulename";

    public static final String MESSAGE = "message";

    public static final String CRITICALITY = "Criticality";
    
    public static final String USE_STATIC_ANALYSIS_BOOLEAN = "UseStaticAnalysisBoolean";

    public static final String CREATE_OUTPUT_RULE = "CreateOutputHTML";

    private ConfigConstants() {
    }

}
