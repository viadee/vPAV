/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. All advertising materials mentioning features or use of this software must display the following
 * acknowledgement: This product includes software developed by the viadee Unternehmensberatung GmbH. 4. Neither the
 * name of the viadee Unternehmensberatung GmbH nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV;

public final class ConstantsConfig {

    public static final String RULESET = "src/main/resources/ruleSet.xml";

    public static final String RULESETDEFAULT = "ruleSetDefault.xml";

    public static final String BEAN_MAPPING = "target/beanMapping.xml";

    public static final String IGNORE_FILE = "src/main/resources/.ignoreIssues";

    public static final String BPMN_FILE_PATTERN = "**/*.bpmn";

    public static final String DMN_FILE_PATTERN = "**/*.dmn";

    public static final String SCRIPT_FILE_PATTERN = ".*\\.groovy";

    public static final String JAVA_FILE_PATTERN = ".*\\.java";

    public static final String JAR_FILE_PATTERN = ".jar";

    public static final String DEFAULT_VERSIONED_FILE_PATTERN = "([^_]*)_{1}([0-9][_][0-9]{1})\\.(java|groovy)";

    public static final String VALIDATION_XML_OUTPUT = "target/bpmn_validation.xml";

    public static final String VALIDATION_JS_OUTPUT = "target/bpmn_validation.js";

    public static final String VALIDATION_JSON_OUTPUT = "target/bpmn_validation.json";

    public static final String BASEPATH = "src/main/resources/";

    public static final String TEST_BASEPATH = "src/test/resources/";

    public static final String PROCESS_VARIABLES_LOCATION = "ProcessVariablesLocation";

}
