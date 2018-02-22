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
package de.viadee.bpm.vPAV;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.camunda.bpm.model.bpmn.instance.BaseElement;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.reader.ConfigReaderException;
import de.viadee.bpm.vPAV.config.reader.XmlConfigReader;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.output.IssueOutputWriter;
import de.viadee.bpm.vPAV.output.JsOutputWriter;
import de.viadee.bpm.vPAV.output.JsonOutputWriter;
import de.viadee.bpm.vPAV.output.OutputWriterException;
import de.viadee.bpm.vPAV.output.RuleSetOutputWriter;
import de.viadee.bpm.vPAV.output.XmlOutputWriter;
import de.viadee.bpm.vPAV.processing.BpmnModelDispatcher;
import de.viadee.bpm.vPAV.processing.ConfigItemNotFoundException;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;

public abstract class AbstractRunner {

    private static Logger logger = Logger.getLogger(AbstractRunner.class.getName());

    private static FileScanner fileScanner;

    private static OuterProcessVariablesScanner variableScanner;

    private static Collection<CheckerIssue> issues;

    private static Collection<CheckerIssue> filteredIssues;

    private static Map<String, String> fileMapping = createFileFolderMapping();

    private static ArrayList<String> allOutputFilesArray = createAllOutputFilesArray();

    private static Map<String, ArrayList<String>> sequenceFlowList = new HashMap<>();

    private static Map<String, BaseElement> signalNames = new HashMap<>();

    private static boolean isMisconfigured = false;

    /**
     * Main method which represents lifecycle of the validation process Calls main functions
     */
    public static void viadeeProcessApplicationValidator() {

        // 1
        final Map<String, Rule> rules = readConfig();

        // 2
        scanClassPath(rules);

        // 3
        getProcessVariables();

        // 4
        createIssues(rules);

        // 5
        removeIgnoredIssues();

        // 6
        writeOutput(filteredIssues);

        // 7
        copyFiles();

        logger.info("BPMN validation successful completed");

    }

    /**
     * 1) If local_ruleSet doesn't exist, then load default_RuleSet 2) If local_ruleSet exist and parent is deactivated
     * then override deactivatedRules with local_ruleSet 3) If local_ruleSet exist and parent is activated then override
     * deactivatedRules with parent_ruleSet and then override with local_ruleSet
     *
     * write effectiveRuleSet to vPAV folder
     *
     * @return Map(String, Rule) ruleSet
     */
    private static Map<String, Rule> readConfig() {
        createBaseFolder();
        Map<String, Rule> rules = new XmlConfigReader().getDeactivatedRuleSet();
        final RuleSetOutputWriter ruleSetOutputWriter = new RuleSetOutputWriter();
        try {
            if (new File(ConfigConstants.TEST_BASEPATH + ConfigConstants.RULESET).exists()) {
                Map<String, Rule> localRule = new XmlConfigReader().read(ConfigConstants.RULESET);

                if (localRule.containsKey(ConfigConstants.HASPARENTRULESET)
                        && localRule.get(ConfigConstants.HASPARENTRULESET).isActive()) {
                    rules = mergeRuleSet(rules, new XmlConfigReader().read(ConfigConstants.RULESETPARENT));
                    rules = mergeRuleSet(rules, localRule);
                } else {
                    rules = mergeRuleSet(rules, localRule);
                }
            } else {
                rules = new XmlConfigReader().read(ConfigConstants.RULESETDEFAULT);
            }
            ruleSetOutputWriter.write(rules);
            RuntimeConfig.getInstance().addActiveRules(rules);

        } catch (final ConfigReaderException | OutputWriterException e) {
            throw new RuntimeException(e);
        }

        rules.remove(ConfigConstants.HASPARENTRULESET);
        return rules;
    }

    /**
     * merges ruleSets according to inheritance hierarchy (Deactivated < global < default < local)
     *
     * @param parentRules
     *            Basis RuleSet which will be overwritten
     * @param childRules
     *            New RuleSet
     * @return Map(String, Rule) finalRules merged ruleSet
     */
    private static Map<String, Rule> mergeRuleSet(final Map<String, Rule> parentRules,
            final Map<String, Rule> childRules) {
        final Map<String, Rule> finalRules = new HashMap<>();

        finalRules.putAll(parentRules);
        finalRules.putAll(childRules);

        return finalRules;
    }

    /**
     * Initializes the fileScanner with the current set of rules
     *
     * @param rules
     *            Map of rules
     */
    private static void scanClassPath(Map<String, Rule> rules) {
        fileScanner = new FileScanner(rules);
    }

    /**
     * Initializes the variableScanner to scan and read outer process variables with the current javaResources
     */
    private static void getProcessVariables() {
        variableScanner = new OuterProcessVariablesScanner(fileScanner.getJavaResourcesFileInputStream());
        readOuterProcessVariables(variableScanner);
    }

    /**
     * Creates the list of issues found for a given model and ruleSet Throws a RuntimeException if errors are found, so
     * automated builds in a CI/CD pipeline will fail
     *
     * @param rules
     *            Map of rules
     * @throws RuntimeException
     *             Config item couldn't be read
     */
    private static void createIssues(Map<String, Rule> rules) throws RuntimeException {
        issues = checkModels(rules, fileScanner, variableScanner);
    }

    /**
     * Removes whitelisted issues from the list of issues found
     *
     * @throws RuntimeException
     *             Ignored issues couldn't be read successfully
     */
    private static void removeIgnoredIssues() throws RuntimeException {
        filteredIssues = filterIssues(issues);
    }

    /**
     * Write output files (xml / json / js)
     *
     * @param filteredIssues
     *            List of filteredIssues
     * @throws RuntimeException
     *             Abort if writer can not be instantiated
     */
    private static void writeOutput(final Collection<CheckerIssue> filteredIssues) throws RuntimeException {
        if (filteredIssues.size() > 0) {
            final IssueOutputWriter xmlOutputWriter = new XmlOutputWriter();
            final IssueOutputWriter jsonOutputWriter = new JsonOutputWriter();
            final IssueOutputWriter jsOutputWriter = new JsOutputWriter();
            try {
                xmlOutputWriter.write(filteredIssues);
                jsonOutputWriter.write(filteredIssues);
                jsOutputWriter.write(filteredIssues);

            } catch (final OutputWriterException e) {
                throw new RuntimeException("Output couldn't be written");
            }
        } else {
            // 6a if no issues, then delete files if exists
            ArrayList<Path> validationFiles = new ArrayList<Path>();
            validationFiles.add(Paths.get(ConfigConstants.VALIDATION_JS_OUTPUT));
            validationFiles.add(Paths.get(ConfigConstants.VALIDATION_JSON_OUTPUT));
            validationFiles.add(Paths.get(ConfigConstants.VALIDATION_XML_OUTPUT));
            deleteFiles(validationFiles);
            final IssueOutputWriter jsOutputWriter = new JsOutputWriter();
            try {
                jsOutputWriter.write(filteredIssues);
            } catch (OutputWriterException e) {
                throw new RuntimeException("JavaScript File couldn't be written");
            }
        }
    }

    /**
     * Create Base folders
     *
     * @throws RuntimeException
     *             Folders couldn't be created
     */
    private static void createBaseFolder() throws RuntimeException {
        createvPAVFolder();
        createImgFolder();
        createCssFolder();
        createJsFolder();
    }

    /**
     * Create vPAV folder
     *
     */
    private static void createvPAVFolder() {
        File vPavDir = new File(ConfigConstants.VALIDATION_FOLDER);

        if (!vPavDir.exists()) {
            boolean success = vPavDir.mkdirs();
            if (!success) {
                throw new RuntimeException("vPAV directory does not exist and could not be created");
            }
        }
    }

    /**
     * Make img folder
     */
    private static void createImgFolder() {

        File imgDir = new File(ConfigConstants.IMG_FOLDER);

        if (!imgDir.exists()) {
            boolean success = imgDir.mkdirs();
            if (!success) {
                throw new RuntimeException("vPAV/img directory does not exist and could not be created");
            }
        }
    }

    /**
     * Make css folder
     */
    private static void createJsFolder() {
        File jsDir = new File(ConfigConstants.JS_FOLDER);
        if (!jsDir.exists()) {
            boolean success = jsDir.mkdirs();
            if (!success)
                throw new RuntimeException("vPAV/js directory does not exist and could not be created");
        }
    }

    /**
     * Make css folder
     */
    private static void createCssFolder() {
        File cssDir = new File(ConfigConstants.CSS_FOLDER);
        if (!cssDir.exists()) {
            boolean success = cssDir.mkdirs();
            if (!success)
                throw new RuntimeException("vPAV/css directory does not exist and could not be created");
        }

    }

    /**
     * Delete files from destinations
     *
     * @param destinations
     *            List of destinations who will be deleted
     */
    private static void deleteFiles(ArrayList<Path> destinations) {
        for (Path destination : destinations) {
            if (destination.toFile().exists())
                destination.toFile().delete();
        }
    }

    /**
     * Copies all necessary files and deletes outputFiles
     *
     * @throws RuntimeException
     *             Files couldn't be copied
     */
    private static void copyFiles() throws RuntimeException {
        // 7a delete files before copy
        ArrayList<Path> outputFiles = new ArrayList<Path>();
        for (String file : allOutputFilesArray)
            outputFiles.add(Paths.get(fileMapping.get(file), file));
        deleteFiles(outputFiles);

        for (String file : allOutputFilesArray)
            copyFileToVPAVFolder(file);
    }

    /**
     * Creates ArrayList to hold output files
     *
     * @return ArrayList<String> allFiles
     */
    private static ArrayList<String> createAllOutputFilesArray() {
        ArrayList<String> allFiles = new ArrayList<String>();
        allFiles.add("bootstrap.min.js");
        allFiles.add("bpmn-navigated-viewer.js");
        allFiles.add("bpmn.io.viewer.app.js");
        allFiles.add("jquery-3.2.1.min.js");
        allFiles.add("popper.min.js");
        allFiles.add("infoPOM.js");

        allFiles.add("bootstrap.min.css");
        allFiles.add("viadee.css");
        allFiles.add("MarkerStyle.css");

        allFiles.add("vPAV.png");
        allFiles.add("viadee_Logo.png");
        allFiles.add("GitHub.png");
        allFiles.add("error.png");
        allFiles.add("warning.png");
        allFiles.add("info.png");
        allFiles.add("success.png");

        allFiles.add("validationResult.html");

        return allFiles;
    }

    /**
     * Creates Map for files and corresponding folders
     *
     * @return Map<String, String> fMap
     */
    private static Map<String, String> createFileFolderMapping() {
        Map<String, String> fMap = new HashMap<String, String>();
        fMap.put("bootstrap.min.js", ConfigConstants.JS_FOLDER);
        fMap.put("bpmn-navigated-viewer.js", ConfigConstants.JS_FOLDER);
        fMap.put("bpmn.io.viewer.app.js", ConfigConstants.JS_FOLDER);
        fMap.put("jquery-3.2.1.min.js", ConfigConstants.JS_FOLDER);
        fMap.put("popper.min.js", ConfigConstants.JS_FOLDER);
        fMap.put("infoPOM.js", ConfigConstants.JS_FOLDER);

        fMap.put("bootstrap.min.css", ConfigConstants.CSS_FOLDER);
        fMap.put("viadee.css", ConfigConstants.CSS_FOLDER);
        fMap.put("MarkerStyle.css", ConfigConstants.CSS_FOLDER);

        fMap.put("vPAV.png", ConfigConstants.IMG_FOLDER);
        fMap.put("viadee_Logo.png", ConfigConstants.IMG_FOLDER);
        fMap.put("GitHub.png", ConfigConstants.IMG_FOLDER);
        fMap.put("error.png", ConfigConstants.IMG_FOLDER);
        fMap.put("warning.png", ConfigConstants.IMG_FOLDER);
        fMap.put("info.png", ConfigConstants.IMG_FOLDER);
        fMap.put("success.png", ConfigConstants.IMG_FOLDER);

        fMap.put("validationResult.html", ConfigConstants.VALIDATION_FOLDER);

        return fMap;
    }

    /**
     * Copies files to vPAV folder
     *
     * @param file
     *            File who will be copied to vPAV folder
     * @throws RuntimeException
     *             Files couldn't be written
     */
    private static void copyFileToVPAVFolder(String file) throws RuntimeException {
        InputStream source = AbstractRunner.class.getClassLoader().getResourceAsStream(file);
        Path destination = Paths.get(fileMapping.get(file) + file);
        try {
            Files.copy(source, destination);
        } catch (IOException e) {
            throw new RuntimeException("Files couldn't be written");
        }
    }

    /**
     * filter issues based on black list
     *
     * @param issues
     *            all found issues
     * @return filtered issues
     * @throws IOException
     *             Ignored issues couldn't be read successfully
     */
    private static Collection<CheckerIssue> filterIssues(final Collection<CheckerIssue> issues)
            throws RuntimeException {
        Collection<CheckerIssue> filteredIssues;
        try {
            filteredIssues = getFilteredIssues(issues);
        } catch (final IOException e) {
            throw new RuntimeException("Ignored issues couldn't be read successfully", e);
        }
        Collections.sort((List<CheckerIssue>) filteredIssues);
        return filteredIssues;
    }

    /**
     * remove false positives from issue collection
     *
     * @param issues
     *            collection of issues
     * @return filteredIssues
     * @throws IOException
     *             ignoreIssues file doesn't exist
     */
    private static Collection<CheckerIssue> getFilteredIssues(Collection<CheckerIssue> issues)
            throws IOException {
        final Collection<CheckerIssue> filteredIssues = new ArrayList<CheckerIssue>();
        filteredIssues.addAll(issues);

        final Collection<String> ignoredIssues = collectIgnoredIssues(ConfigConstants.IGNORE_FILE);
        for (final CheckerIssue issue : issues) {
            if (ignoredIssues.contains(issue.getId())) {
                filteredIssues.remove(issue);
            }
        }
        return filteredIssues;
    }

    /**
     * Read issue ids, that should be ignored
     *
     * Assumption: Each row is an issue id
     *
     * @param filePath
     *            Path of ignoredIssues-file
     * @return issue ids
     * @throws IOException
     *             ignoreIssues file doesn't exist
     */
    private static Collection<String> collectIgnoredIssues(final String filePath) throws IOException {

        final Collection<String> ignoredIssues = new ArrayList<String>();

        FileReader fileReader = null;
        try {
            fileReader = new FileReader(filePath);
        } catch (final FileNotFoundException ex) {
            logger.info(".ignoreIssues file doesn't exist");
        }
        if (fileReader != null) {
            final BufferedReader bufferedReader = new BufferedReader(fileReader);
            String zeile = bufferedReader.readLine();
            addIgnoredIssue(ignoredIssues, zeile);
            while (zeile != null) {
                zeile = bufferedReader.readLine();
                addIgnoredIssue(ignoredIssues, zeile);
            }
            bufferedReader.close();
        }

        return ignoredIssues;
    }

    /**
     * Check consistency of all models
     *
     * @param rules
     *            all rules of ruleSet.xml
     * @param beanMapping
     *            beanMapping if spring context is available
     * @param fileScanner
     *            fileScanner
     * @param variableScanner
     *            variablenScanner
     * @return foundIssues
     * @throws ConfigItemNotFoundException
     *             ConfigItem not found
     */
    private static Collection<CheckerIssue> checkModels(final Map<String, Rule> rules, final FileScanner fileScanner,
            final OuterProcessVariablesScanner variableScanner) throws RuntimeException {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        for (final String pathToModel : fileScanner.getProcessdefinitions()) {
            issues.addAll(checkModel(rules, pathToModel, fileScanner,
                    variableScanner));
            AbstractRunner.resetSequenceFlowList();
        }
        checkMisconfiguration();
        JsOutputWriter.finish();
        return issues;
    }

    /**
     * Check consistency of a model
     *
     * @param rules
     *            all rules of ruleSet.xml
     * @param beanMapping
     *            beanMapping if spring context is available
     * @param processdef
     *            processdefintion
     * @param fileScanner
     *            fileScanner
     * @param variableScanner
     *            variableScanner
     * @return modelIssues
     * @throws ConfigItemNotFoundException
     */
    private static Collection<CheckerIssue> checkModel(final Map<String, Rule> rules, final String processdef,
            final FileScanner fileScanner,
            final OuterProcessVariablesScanner variableScanner) throws RuntimeException {
        Collection<CheckerIssue> modelIssues;
        try {
            modelIssues = BpmnModelDispatcher.dispatch(new File(ConfigConstants.BASEPATH + processdef),
                    fileScanner.getDecisionRefToPathMap(), fileScanner.getProcessIdToPathMap(),
                    variableScanner.getMessageIdToVariableMap(), variableScanner.getProcessIdToVariableMap(),
                    fileScanner.getResourcesNewestVersions(), rules);

        } catch (final ConfigItemNotFoundException e) {
            throw new RuntimeException("Config item couldn't be read");
        }
        return modelIssues;
    }

    /**
     * Scan process variables in external classes, which are not referenced from model
     *
     * @param scanner
     *            OuterProcessVariablesScanner
     * @throws IOException
     *             Outer process variables couldn't be read
     */
    private static void readOuterProcessVariables(final OuterProcessVariablesScanner scanner)
            throws RuntimeException {
        try {
            scanner.scanProcessVariables();
        } catch (final IOException e) {
            throw new RuntimeException("Outer process variables couldn't be read: " + e.getMessage());
        }
    }

    /**
     * Checks whether a misconfiguration of the ruleSet.xml occured
     *
     */
    private static void checkMisconfiguration() {
        if (getIsMisconfigured())
            logger.warning(
                    "Misconfiguration of rule for ExtensionChecker. Please provide either tasktype or a specific ID of an element.");
    }

    /**
     * Add ignored issue
     *
     * @param issues
     *            Collection of issues
     * @param row
     *            row of file
     */
    private static void addIgnoredIssue(final Collection<String> issues, final String row) {
        if (row != null && !row.isEmpty() && !row.trim().startsWith("#"))
            issues.add(row);
    }

    public static Set<String> getModelPath() {
        return fileScanner.getProcessdefinitions();
    }

    public static Collection<CheckerIssue> getfilteredIssues() {
        return filteredIssues;
    }

    public static boolean getIsMisconfigured() {
        return isMisconfigured;
    }

    public static void setIsMisconfigured(boolean isMisconfigured) {
        AbstractRunner.isMisconfigured = isMisconfigured;
    }

    public static Map<String, ArrayList<String>> getSequenceFlowList() {
        return sequenceFlowList;
    }

    public static void addToSequenceFlowList(String id, ArrayList<String> sequenceFlowList) {
        AbstractRunner.sequenceFlowList.put(id, sequenceFlowList);
    }

    public static void resetSequenceFlowList() {
        AbstractRunner.sequenceFlowList.clear();
    }

    public static boolean addSignal(final BaseElement baseElement, final String name) {
        if (!AbstractRunner.signalNames.containsKey(name)) {
            AbstractRunner.signalNames.put(name, baseElement);
            return true;
        } else {
            return false;
        }
    }

    public static BaseElement getSignal(final String name) {
        return AbstractRunner.signalNames.get(name);
    }

    public static void removeElement(final String name) {
        AbstractRunner.signalNames.remove(name);
    }

}
