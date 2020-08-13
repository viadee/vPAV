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

import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.config.reader.PropertiesReader;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class RuntimeConfig {

    private static Logger logger = Logger.getLogger(RuntimeConfig.class.getName());

    private static RuntimeConfig instance;

    private ApplicationContext ctx;

    private Map<String, String> beanMap;

    private RuleSet ruleSet;

    private ClassLoader classLoader;

    private ResourceBundle resourceBundle;

    private FileScanner fileScanner;

    private boolean test = false;

    private static Logger LOGGER = Logger.getLogger(RuntimeConfig.class.getName());

    private final String[] viadeeConfigRules = { "CreateOutputHTML" };

    private final String[] viadeeElementRules = { "XorConventionChecker", "TimerExpressionChecker",
            "JavaDelegateChecker", "NoScriptChecker", "NoExpressionChecker", "EmbeddedGroovyScriptChecker",
            "VersioningChecker", "DmnTaskChecker", "ProcessVariablesNameConventionChecker",
            "TaskNamingConventionChecker", "ElementIdConventionChecker", "MessageEventChecker", "FieldInjectionChecker",
            "BoundaryErrorChecker", "ExtensionChecker", "OverlapChecker", "SignalEventChecker",
            "MessageCorrelationChecker" };

    private final String[] viadeeModelRules = { "ProcessVariablesModelChecker", "DataFlowChecker" };

    private final URL[] urls = setURLs();

    private Properties properties;

    private RuntimeConfig() {
        properties = new PropertiesReader().initProperties();
    }

    public static RuntimeConfig getInstance() {
        if (RuntimeConfig.instance == null) {
            RuntimeConfig.instance = new RuntimeConfig();
        }
        return RuntimeConfig.instance;
    }

    String findBeanByName(String string) {
        if (string != null && !string.isEmpty() && beanMap != null && !beanMap.isEmpty()) {
            return beanMap.get(string);
        } else
            return null;
    }

    public void setBeanMapping(Map<String, String> beanMap) {
        this.beanMap = beanMap;
    }

    public Map<String, String> getBeanMapping() {
        return beanMap;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    void setApplicationContext(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public String[] getViadeeRules() {
        return Stream.of(viadeeConfigRules, viadeeElementRules, viadeeModelRules).flatMap(Stream::of)
                .toArray(String[]::new);
    }

    public String[] getViadeeElementRules() {
        return viadeeElementRules;
    }

    public ArrayList<String> getActiveRules() {
        return new ArrayList<>(ruleSet.getAllActiveRules().keySet());
    }

    public void setRuleSet(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public ApplicationContext getApplicationContext() {
        return ctx;
    }

    /**
     * Retrieve locale. If locale can not be retrieved, use system locale
     */
    public void retrieveLocale() {
        if (getLanguage().equals("de_DE")) {
            setResource("de_DE");
        } else {
            setResource("en_US");
        }
    }

    /**
     * Set base directory and set ResourceBundle
     *
     * @param locale Locale extracted from ruleSet or either default system locale
     *               Localization
     */
    public void setResource(final String locale) {
        setResourceBundle(fromClassLoader("messages_" + locale));
    }

    /**
     * Retrieves ResourceBundle from base directy and returns it to RuntimeConfig
     *
     * @param bundleName Bundle name for localization
     * @return ResourceBundle
     */
    private ResourceBundle fromClassLoader(final String bundleName) {
        URL[] urls = getURLs();
        ClassLoader loader = new URLClassLoader(urls);
        return ResourceBundle.getBundle(bundleName, Locale.getDefault(), loader);
    }

    /**
     * Retrieves URLs from java classpath
     *
     * @return Array of URLs
     */
    private URL[] setURLs() {
        String pathSeparator = System.getProperty("path.separator");

        String[] classPathEntries = System.getProperty("java.class.path").split(pathSeparator);

        ArrayList<URL> urlArrayList = new ArrayList<>();
        Arrays.asList(classPathEntries).forEach(entry -> {
            try {
                urlArrayList.add((new File(entry)).toURI().toURL());
            } catch (MalformedURLException ignored) {
                LOGGER.warning("Resource " + entry + " could not be loaded");
            }
        });
        return urlArrayList.toArray(new URL[0]);
    }

    private URL[] getURLs() {
        return urls;
    }

    ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    private void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public FileScanner getFileScanner() {
        return fileScanner;
    }

    public void setFileScanner(FileScanner fileScanner) {
        this.fileScanner = fileScanner;
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
            return properties.getProperty("whitelist") + "," + ConfigConstants.WHITELIST_SOOT_DEPENDENCIES;
        } else {
            return ConfigConstants.WHITELIST_SOOT_DEPENDENCIES;
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
            String warningMessage = "Could not retrieve localization from vpav.properties. Default localization: %s.";
            if (Locale.getDefault().toString().equals("de_DE")) {
                logger.warning(String.format(warningMessage, "de_DE"));
                return "de";
            } else {
                logger.warning(String.format(warningMessage, "en_US"));
                return "en";
            }
        }
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

    public String getJsFolder() {
        return getValidationFolder() + "js/";
    }

    public String getDataFolder() {
        return getValidationFolder() + ConfigConstants.DATA_FOLDER;
    }

    public String getValidationIgnoredIssuesOutput() {
        return getDataFolder() + "ignoredIssues.js";
    }

    public String getValidationJsProcessVariables() {
        return getDataFolder() + "processVariables.js";
    }

    public String getValidationJsIssueSeverity() {
        return getDataFolder() + "issue_severity.js";
    }

    public String getValidationJsSuccessOutput() {
        return getDataFolder() + "bpmn_validation_success.js";
    }

    public String getValidationJsCheckers() {
        return getDataFolder() + "checkers.js";
    }

    public String getPropertiesJsOutput() {
        return getDataFolder() + "properties.js";
    }

    public String getValidationJsOutput() {
        return getDataFolder() + "bpmn_validation.js";
    }

    public String getValidationJsModelOutput() {
        return getDataFolder() + "bpmn_model.js";
    }

    public String getCssFolder() {
        return getValidationFolder() + "css/";
    }

    public String getImgFolder() {
        return getValidationFolder() + "img/";
    }

    public String getExternalReportsFolder() {
        return getValidationFolder() + ConfigConstants.VALIDATION_OVERVIEW_REPORTS_FOLDER;
    }

    public String getEffectiveRuleset() {
        return getDataFolder() + "effectiveRuleSet.xml";
    }

    public String getValidationXmlOutput() {
        return getDataFolder() + "bpmn_validation.xml";
    }

    public String getValidationJsonOutput() {
        return getDataFolder() + "bpmn_validation.json";
    }

    public Boolean isMultiProjectScan() {
        return Boolean.parseBoolean(properties.getProperty("multiProjectReport", "false"));
    }

    public String[] getGeneratedReports() {
        return properties.getProperty("generatedReports", "").split(",");
    }

}