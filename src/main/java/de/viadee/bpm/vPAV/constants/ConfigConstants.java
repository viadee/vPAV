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

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.reader.PropertiesReader;

import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Class to hold global constants
 */

public class ConfigConstants {

    public static final String JS_FOLDER_SINGLE_PROJECT = getInstance().getValidationFolder() + "js/";

    public static final String CSS_FOLDER = getInstance().getValidationFolder() + "css/";

    public static final String IMG_FOLDER = getInstance().getValidationFolder() + "img/";

    public static final String MAIN_FOLDER = "src/main/";

    private static final String BASE_PATH = MAIN_FOLDER + "resources/";

    public static final String JAVA_PATH = MAIN_FOLDER + "java/";

    public static final String TARGET_CLASS_FOLDER = "target/classes";

    public static final String TARGET_TEST_PATH = "target/test-classes/";

    public static final String CLASS_FILE_PATTERN = "**/*.class";

    public static final String TEST_FOLDER = "src/test/";

    public static final String JAVA_PATH_TEST = TEST_FOLDER + "/java/";

    public static final String BASE_PATH_TEST = TEST_FOLDER + "resources/";

    public static final String JS_BASE_PATH = MAIN_FOLDER + "resources/";

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

    public static final String EFFECTIVE_RULESET = getInstance().getValidationFolder() + "effectiveRuleSet.xml";

    public static final String VALIDATION_XML_OUTPUT = getInstance().getValidationFolder() + "bpmn_validation.xml";

    public static final String VALIDATION_JS_MODEL_OUTPUT = JS_FOLDER_SINGLE_PROJECT + "bpmn_model.js";

    public static final String VALIDATION_JS_OUTPUT = JS_FOLDER_SINGLE_PROJECT + "bpmn_validation.js";

    public static final String PROPERTIES_JS_OUTPUT = JS_FOLDER_SINGLE_PROJECT + "properties.js";

    public static final String VALIDATION_JS_CHECKERS = JS_FOLDER_SINGLE_PROJECT + "checkers.js";

    public static final String VALIDATION_JS_SUCCESS_OUTPUT = JS_FOLDER_SINGLE_PROJECT + "bpmn_validation_success.js";

    public static final String VALIDATION_JS_ISSUE_SEVERITY = JS_FOLDER_SINGLE_PROJECT + "issue_severity.js";

    public static final String VALIDATION_JS_PROCESS_VARIABLES = JS_FOLDER_SINGLE_PROJECT + "processVariables.js";

    public static final String VALIDATION_JSON_OUTPUT = getInstance().getValidationFolder() + "bpmn_validation.json";

    public static final String VALIDATION_IGNORED_ISSUES_OUTPUT = JS_FOLDER_SINGLE_PROJECT + "ignoredIssues.js";

    public static final String VERSIONING_SCHEME_PACKAGE = "versioningSchemePackage";

    public static final String VERSIONING_SCHEME_CLASS = "versioningSchemeClass";

    public static final String GROOVY = "groovy";

    public static final String RULE_NAME = "rulename";

    public static final String MESSAGE = "message";

    public static final String CRITICALITY = "Criticality";

    public static final String WHITELIST_SOOT_DEPENDENCIES = "org/camunda/bpm/camunda-engine";

    public static final String EXTERNAL_REPORTS_FOLDER = getInstance().getValidationFolder() + "externalReports/";

    public static final String JS_FOLDER_MULTI_PROJECT = EXTERNAL_REPORTS_FOLDER + "js/";

    public static final String VALIDATION_HTML_OUTPUT_FILE = "validationResult.html";

    public static final String VALIDATION_OVERVIEW_HTML_OUTPUT_FILE = "overview.html";

    public static final String VALIDATION_OVERVIEW_JS_OUTPUT_FILE = "overview.js";

    public static final String VALIDATION_OVERVIEW_REPORT_PATHS_JS = "reportPaths.js";

    private static Logger logger = Logger.getLogger(ConfigConstants.class.getName());

    private static ConfigConstants instance;

    private Properties properties;

    private ConfigConstants() {
        properties = (new PropertiesReader()).read();
    }

    public static ConfigConstants getInstance() {
        if (ConfigConstants.instance == null) {
            ConfigConstants.instance = new ConfigConstants();
        }
        return ConfigConstants.instance;
    }

    /**
     * Only used for tests in order to inject mocked properties.
     *
     * @param newProperties mocked properties
     */
    public void setProperties(Properties newProperties) {
        this.properties = newProperties;
    }

    public String getWhiteList() {
        if (properties.getProperty("whitelist") != null) {
            return properties.getProperty("whitelist") + "," + WHITELIST_SOOT_DEPENDENCIES;
        } else {
            return WHITELIST_SOOT_DEPENDENCIES;
        }
    }

    public String getValidationFolder() {
        return properties.getProperty("validationFolder", "target/vPAV") + '/';
    }

    public String getRuleSetPath() {
        return properties.getProperty("ruleSetPath", ConfigConstants.BASE_PATH_TEST);
    }

    public String getBasepath() {
        if (RuntimeConfig.getInstance().isTest()) {
            return properties.getProperty("basepath", ConfigConstants.BASE_PATH_TEST);
        }
        return properties.getProperty("basepath", ConfigConstants.BASE_PATH);
    }

    public String getScanPath() {
        if (RuntimeConfig.getInstance().isTest()) {
            return properties.getProperty("scanpath", ConfigConstants.TARGET_TEST_PATH);
        }
        return properties.getProperty("scanpath", ConfigConstants.TARGET_CLASS_FOLDER);
    }

    public String getUserVariablesFilePath() {
        return properties.getProperty("userVariablesFilePath", ConfigConstants.USER_VARIABLES_FILE);
    }

    public String getRuleSetFileName() {
        return properties.getProperty("ruleSet", ConfigConstants.RULESET);
    }

    public String getParentRuleSetFileName() {
        return properties.getProperty("parentRuleSet", ConfigConstants.RULESET_PARENT);
    }

    public String getFilePattern() {
        return ConfigConstants.CLASS_FILE_PATTERN;
    }

    /**
     * Checks whether the output of the result should be in html
     *
     * @return true (default) or false if false is defined in the properties file
     */
    public boolean isHtmlOutputEnabled() {
        return Boolean.parseBoolean(properties.getProperty("outputhtml", "true"));
    }

    /**
     * @param htmlOutput true if the results should be visualized as html page
     * @deprecated As of release 3.0.0, html output property should be set in
     * property file
     */
    @Deprecated
    public void setHtmlOutputEnabled(boolean htmlOutput) {
        properties.setProperty("outputhtml", String.valueOf(htmlOutput));
    }

    /**
     * Retrieves language property
     *
     * @return Language
     */
    public String getLanguage() {
        if (properties.containsKey("language")) {
            return properties.getProperty("language");
        } else {
            if (Locale.getDefault().toString().equals("de_DE")) {
                logger.warning("Could not retrieve localization from vpav.properties. Default localization: de_DE.");
                return "de";
            } else {
                logger.warning("Could not retrieve localization from vpav.properties. Default localization: en_US.");
                return "en";
            }
        }
    }

    public Boolean isMultiProjectScan() {
        return Boolean.parseBoolean(properties.getProperty("multiProjectReport", "false"));
    }

    public String[] getGeneratedReports() {
        return properties.getProperty("generatedReports", "").split(",");
    }

    /**
     * @param languageCode language code like de_DE
     * @deprecated As of release 3.0.0, language should be directly set in
     * properties file
     */
    @Deprecated
    public void setLanguage(String languageCode) {
        properties.setProperty("language", languageCode);
    }
}
