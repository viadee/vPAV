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
package de.viadee.bpm.vPAV.constants;

/**
 * Class to hold global constants
 */

public class ConfigConstants {

    public static final String MAIN_FOLDER = "src/main/";

    public static final String RESOURCES = "resources/";

    public static final String BASE_PATH = MAIN_FOLDER + RESOURCES;

    public static final String JAVA_PATH = MAIN_FOLDER + "java/";

    public static final String TARGET_CLASS_FOLDER = "target/classes/";

    public static final String TARGET_TEST_PATH = "target/test-classes/";

    public static final String CLASS_FILE_PATTERN = "**/*.class";

    public static final String TEST_FOLDER = "src/test/";

    public static final String JAVA_PATH_TEST = TEST_FOLDER + "java/";

    public static final String BASE_PATH_TEST = TEST_FOLDER + RESOURCES;

    public static final String DATA_FOLDER = "data/";

    public static final String JAVA_FILE_ENDING = ".java";

    public static final String RULESET = "ruleSet.xml";

    public static final String RULESET_DEFAULT = "ruleSetDefault.xml";

    public static final String RULESET_PARENT = "parentRuleSet.xml";

    public static final String HAS_PARENT_RULESET = "HasParentRuleSet";

    public static final String USER_VARIABLES_FILE = "variables.xml";

    public static final String IGNORE_FILE = BASE_PATH_TEST + "ignoreIssues.txt";

    public static final String IGNORE_FILE_OLD = BASE_PATH_TEST + ".ignoreIssues";

    public static final String BPMN_FILE_PATTERN = "**/*.bpmn";

    public static final String DMN_FILE_PATTERN = "**/*.dmn";

    public static final String SCRIPT_FILE_PATTERN = "**/*.groovy";

    public static final String HTML_INPUT_FOLDER = "html/";

    public static final String HTML_FILE = "validationResult.html";

    public static final String JS_INPUT_FOLDER = HTML_INPUT_FOLDER + "js/";

    public static final String CSS_INPUT_FOLDER = HTML_INPUT_FOLDER + "css/";

    public static final String IMG_INPUT_FOLDER = HTML_INPUT_FOLDER + "img/";

    public static final String FONT_INPUT_FOLDER = HTML_INPUT_FOLDER + "webfonts/";

    public static final String VERSIONING_SCHEME_PACKAGE = "versioningSchemePackage";

    public static final String VERSIONING_SCHEME_CLASS = "versioningSchemeClass";

    public static final String GROOVY = "groovy";

    public static final String RULE_NAME = "rulename";

    public static final String MESSAGE = "message";

    public static final String WHITELIST_SOOT_DEPENDENCIES = "org/camunda/bpm/camunda-engine";

    private ConfigConstants() {
    }
}
