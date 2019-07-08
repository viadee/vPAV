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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.config.reader.ConfigReaderException;
import de.viadee.bpm.vPAV.config.reader.XmlConfigReader;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.output.*;
import de.viadee.bpm.vPAV.processing.BpmnModelDispatcher;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.ModelDispatchResult;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Runner {

    private static Logger logger = Logger.getLogger(Runner.class.getName());

    private FileScanner fileScanner;

    private ProcessVariablesScanner variableScanner;

    private Collection<CheckerIssue> issues;

    private Collection<CheckerIssue> filteredIssues;

    private RuleSet rules = new RuleSet();

    private Map<String, String> ignoredIssuesMap = new HashMap<>();

    private Map<String, String> fileMapping = createFileFolderMapping();

    private Map<String, String> wrongCheckersMap = new HashMap<>();

    private ArrayList<String> allOutputFilesArray = createAllOutputFilesArray();

    private Collection<BpmnElement> elements = new ArrayList<>();

    private Collection<ProcessVariable> processVariables = new ArrayList<>();

    private Collection<DataFlowRule> dataFlowRules = new ArrayList<>();

    private boolean checkProcessVariables = false;

    /**
     * Main method which represents lifecycle of the validation process. Calls main
     * functions
     *
     */
    public void viadeeProcessApplicationValidator() {

        // 1
        rules = readConfig();

        // 2
        setFileScanner(new FileScanner(rules));

        // 3
        getProcessVariables(rules);

        // 4
        createIssues(rules, dataFlowRules);

        // 5
        removeIgnoredIssues();

        // 6
        writeOutput(filteredIssues, elements, processVariables);

        // 7
        copyFiles();

        logger.info("BPMN validation successfully completed");
    }

    /**
     * 1) If local_ruleSet doesn't exist, then load default_RuleSet 2) If
     * local_ruleSet exist and parent is deactivated then override deactivatedRules
     * with local_ruleSet 3) If local_ruleSet exist and parent is activated then
     * override deactivatedRules with parent_ruleSet and then override with
     * local_ruleSet
     * <p>
     * write effectiveRuleSet to vPAV folder
     *
     * @return Map(String, Map ( String, Rule)) ruleSet
     */
    private RuleSet readConfig() {

        prepareOutputFolder();

        rules = new XmlConfigReader().getDeactivatedRuleSet();

        final RuleSetOutputWriter ruleSetOutputWriter = new RuleSetOutputWriter();
        try {
            if (new File(ConfigConstants.TEST_BASEPATH + ConfigConstants.RULESET).exists()) {
                RuleSet localRule = new XmlConfigReader().read(ConfigConstants.RULESET);

                if (localRule.getElementRules().containsKey(ConfigConstants.HASPARENTRULESET)
                        && localRule.getElementRules().get(ConfigConstants.HASPARENTRULESET).get(ConfigConstants.HASPARENTRULESET).isActive()) {
                    rules = mergeRuleSet(rules, new XmlConfigReader().read(ConfigConstants.RULESETPARENT));
                    rules = mergeRuleSet(rules, localRule);
                } else {
                    rules = mergeRuleSet(rules, localRule);
                }
            } else {
                rules = new XmlConfigReader().read(ConfigConstants.RULESETDEFAULT);
            }

            ruleSetOutputWriter.write(rules);
            RuntimeConfig.getInstance().setRuleSet(rules);
        } catch (final ConfigReaderException | OutputWriterException e) {
            throw new RuntimeException(e);
        }

        RuntimeConfig.getInstance().retrieveLocale(rules);

        return rules;
    }

    /**
     * Delete old output and create new output folder
     */
    private void prepareOutputFolder() {

        deleteFiles();
        createvPAVFolder();
        try {
            Files.createDirectory(Paths.get(ConfigConstants.JS_FOLDER));
            Files.createDirectory(Paths.get(ConfigConstants.CSS_FOLDER));
            Files.createDirectory(Paths.get(ConfigConstants.IMG_FOLDER));
        } catch (IOException e) {
            logger.warning("Could not create either output folder for JS, CSS or IMG");
        }

    }

    /**
     * merges ruleSets according to inheritance hierarchy (Deactivated &lt; global &lt;
     * default &lt; local)
     *
     * @param parentRules Basis RuleSet which will be overwritten
     * @param childRules  New RuleSet
     * @return Map(String, Rule) finalRules merged ruleSet
     */
    protected RuleSet mergeRuleSet(final RuleSet parentRules,
                                   final RuleSet childRules) {
        final Map<String, Map<String, Rule>> finalModelRules = new HashMap<>();

        final Map<String, Map<String, Rule>> finalElementRules = new HashMap<>(parentRules.getElementRules());
        finalModelRules.putAll(parentRules.getModelRules());

        // Merge element rules.
        for (Map.Entry<String, Map<String, Rule>> entry : childRules.getElementRules().entrySet()) {
            if (finalElementRules.containsKey(entry.getKey())) {
                finalElementRules.get(entry.getKey()).putAll(entry.getValue());
            } else {
                finalElementRules.put(entry.getKey(), entry.getValue());
            }
        }

        // Merge model rules.
        for (Map.Entry<String, Map<String, Rule>> entry : childRules.getModelRules().entrySet()) {
            if (finalModelRules.containsKey(entry.getKey())) {
                finalModelRules.get(entry.getKey()).putAll(entry.getValue());
            } else {
                finalModelRules.put(entry.getKey(), entry.getValue());
            }
        }

        return new RuleSet(finalElementRules, finalModelRules, false);
    }

    /**
     * Initializes the variableScanner to scan and read outer process variables with
     * the current javaResources
     *
     * @param rules Rules defined in ruleSet
     */
    protected void getProcessVariables(final RuleSet rules) {
        if (oneCheckerIsActive(rules.getModelRules(), "ProcessVariablesModelChecker")
                || oneCheckerIsActive(rules.getElementRules(), "ProcessVariablesNameConventionChecker")
                || oneCheckerIsActive(rules.getModelRules(), "DataFlowChecker")) {
            variableScanner = new ProcessVariablesScanner(getFileScanner().getJavaResourcesFileInputStream());
            readOuterProcessVariables(variableScanner);
            setCheckProcessVariables(true);
        } else {
            setCheckProcessVariables(false);
        }
    }

    private boolean oneCheckerIsActive(final Map<String, Map<String, Rule>> rules, String name) {
        for (Rule r : rules.get(name).values()) {
            if (r.isActive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the list of issues found for a given model and ruleSet Throws a
     * RuntimeException if errors are found, so automated builds in a CI/CD pipeline
     * will fail
     *
     * @param rules Map of rules
     */
    private void createIssues(RuleSet rules, Collection<DataFlowRule> dataFlowRules) {
        issues = checkModels(rules, getFileScanner(), variableScanner, dataFlowRules);
    }

    /**
     * Removes whitelisted issues from the list of issues found
     */
    private void removeIgnoredIssues() {
        filteredIssues = filterIssues(issues);
    }

    /**
     * Write output files (xml / json / js)
     *
     * @param filteredIssues   List of filteredIssues
     * @param elements         List of BPMN element across all models
     * @param processVariables List of process variables across all models
     */
    private void writeOutput(final Collection<CheckerIssue> filteredIssues, final Collection<BpmnElement> elements,
                             final Collection<ProcessVariable> processVariables) {

        if (filteredIssues.size() > 0) {
            final IssueOutputWriter xmlOutputWriter = new XmlOutputWriter();
            final IssueOutputWriter jsonOutputWriter = new JsonOutputWriter();
            final JsOutputWriter jsOutputWriter = new JsOutputWriter();
            try {
                jsOutputWriter.prepareMaps(this.getWrongCheckersMap(), this.getIgnoredIssuesMap(), this.getModelPath());

                xmlOutputWriter.write(filteredIssues);
                jsonOutputWriter.write(filteredIssues);
                jsOutputWriter.write(filteredIssues);
                jsOutputWriter.writeVars(elements, processVariables);

            } catch (final OutputWriterException e) {
                throw new RuntimeException("Output couldn't be written", e);
            }
        } else {
            try {
                final JsOutputWriter jsOutputWriter = new JsOutputWriter();
                jsOutputWriter.prepareMaps(this.getWrongCheckersMap(), this.getIgnoredIssuesMap(), this.getModelPath());
                jsOutputWriter.write(filteredIssues);
                jsOutputWriter.writeVars(elements, processVariables);
            } catch (OutputWriterException e) {
                throw new RuntimeException("JavaScript File couldn't be written", e);
            }
        }
    }

    /**
     * Create vPAV folder
     */
    private void createvPAVFolder() {
        File vPavDir = new File(ConfigConstants.VALIDATION_FOLDER);

        if (!vPavDir.exists()) {
            boolean success = vPavDir.mkdirs();
            if (!success) {
                throw new RuntimeException("vPAV directory does not exist and could not be created");
            }
        }
    }

    /**
     * Delete files from validation folder
     */
    private void deleteFiles() {
        File index = new File(ConfigConstants.VALIDATION_FOLDER);
        if (index.exists()) {
            String[] entries = index.list();
            for (String entry : entries) {
                File currentFile = new File(index.getPath(), entry);
                if (currentFile.isDirectory()) {
                    String[] subEntries = currentFile.list();
                    for (String subentry : subEntries) {
                        File file = new File(currentFile.getPath(), subentry);
                        file.delete();
                    }
                }
                currentFile.delete();
            }
        }
    }

    /**
     * Copies all necessary files and deletes outputFiles
     */
    private void copyFiles() {
        ArrayList<Path> outputFiles = new ArrayList<>();
        for (String file : allOutputFilesArray)
            outputFiles.add(Paths.get(fileMapping.get(file), file));

        if (ConfigConstants.getInstance().isHtmlOutputEnabled()) {
            for (String file : allOutputFilesArray)
                copyFileToVPAVFolder(file);
        }
    }

    /**
     * Creates ArrayList to hold output files
     *
     * @return ArrayList<String> allFiles
     */
    private ArrayList<String> createAllOutputFilesArray() {
        ArrayList<String> allFiles = new ArrayList<>();

        allFiles.add("bootstrap.min.js");
        allFiles.add("bpmn-navigated-viewer.js");
        allFiles.add("bpmn.io.viewer.app.js");
        allFiles.add("jquery-3.2.1.min.js");
        allFiles.add("popper.min.js");
        allFiles.add("infoPOM.js");
        allFiles.add("download.js");

        allFiles.add("bootstrap.min.css");
        allFiles.add("viadee.css");
        allFiles.add("MarkerStyle.css");

        allFiles.add("vPAV.png");
        allFiles.add("viadee_weiss.png");
        allFiles.add("GitHub.png");
        allFiles.add("error.png");
        allFiles.add("warning.png");
        allFiles.add("info.png");
        allFiles.add("success.png");
        allFiles.add("dl_button.png");

        allFiles.add("validationResult.html");

        return allFiles;
    }

    /**
     * Creates Map for files and corresponding folders
     *
     * @return Map<String                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               ,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               String> fMap
     */
    private Map<String, String> createFileFolderMapping() {
        Map<String, String> fMap = new HashMap<>();
        fMap.put("bootstrap.min.js", ConfigConstants.JS_FOLDER);
        fMap.put("bpmn-navigated-viewer.js", ConfigConstants.JS_FOLDER);
        fMap.put("bpmn.io.viewer.app.js", ConfigConstants.JS_FOLDER);
        fMap.put("jquery-3.2.1.min.js", ConfigConstants.JS_FOLDER);
        fMap.put("popper.min.js", ConfigConstants.JS_FOLDER);
        fMap.put("infoPOM.js", ConfigConstants.JS_FOLDER);
        fMap.put("download.js", ConfigConstants.JS_FOLDER);

        fMap.put("bootstrap.min.css", ConfigConstants.CSS_FOLDER);
        fMap.put("viadee.css", ConfigConstants.CSS_FOLDER);
        fMap.put("MarkerStyle.css", ConfigConstants.CSS_FOLDER);

        fMap.put("vPAV.png", ConfigConstants.IMG_FOLDER);
        fMap.put("viadee_weiss.png", ConfigConstants.IMG_FOLDER);
        fMap.put("GitHub.png", ConfigConstants.IMG_FOLDER);
        fMap.put("error.png", ConfigConstants.IMG_FOLDER);
        fMap.put("warning.png", ConfigConstants.IMG_FOLDER);
        fMap.put("info.png", ConfigConstants.IMG_FOLDER);
        fMap.put("success.png", ConfigConstants.IMG_FOLDER);
        fMap.put("dl_button.png", ConfigConstants.IMG_FOLDER);

        fMap.put("validationResult.html", ConfigConstants.VALIDATION_FOLDER);

        return fMap;
    }

    /**
     * Copies files to vPAV folder
     *
     * @param file File who will be copied to vPAV folder
     */
    private void copyFileToVPAVFolder(String file) {
        InputStream source = Runner.class.getClassLoader().getResourceAsStream(file);
        Path destination = Paths.get(fileMapping.get(file) + file);
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Files couldn't be written");
        }
    }

    /**
     * filter issues based on black list
     *
     * @param issues all found issues
     * @return filtered issues
     */
    private Collection<CheckerIssue> filterIssues(final Collection<CheckerIssue> issues) {
        Collection<CheckerIssue> filteredIssues;
        filteredIssues = getFilteredIssues(issues);
        Collections.sort((List<CheckerIssue>) filteredIssues);
        return filteredIssues;
    }

    /**
     * remove false positives from issue collection
     *
     * @param issues collection of issues
     * @return filteredIssues
     */
    private Collection<CheckerIssue> getFilteredIssues(Collection<CheckerIssue> issues) {

        // all issues
        final HashMap<String, CheckerIssue> issuesMap = new HashMap<>();

        // transform Collection into a HashMap
        for (final CheckerIssue issue : issues) {
            if (!issuesMap.containsKey(issue.getId())) {
                issuesMap.put(issue.getId(), issue);
            }
        }
        // all issues to be ignored
        final Collection<String> ignoredIssues = collectIgnoredIssues(ConfigConstants.IGNORE_FILE);

        final HashMap<String, CheckerIssue> filteredIssues = new HashMap<>(issuesMap);

        // remove issues that are listed in ignore file
        for (Map.Entry<String, CheckerIssue> entry : issuesMap.entrySet()) {
            if (ignoredIssues.contains(entry.getKey())) {
                filteredIssues.remove(entry.getKey());
            }
        }

        // transform back into collection
        final Collection<CheckerIssue> finalFilteredIssues = new ArrayList<>();
        for (Map.Entry<String, CheckerIssue> entry : filteredIssues.entrySet()) {
            finalFilteredIssues.add(entry.getValue());
        }

        return finalFilteredIssues;
    }

    /**
     * Read issue ids, that should be ignored
     * <p>
     * Assumption: Each row is an issue id
     *
     * @param filePath Path of ignoredIssues-file
     * @return issue ids
     */
    private Collection<String> collectIgnoredIssues(final String filePath) {

        final Map<String, String> ignoredIssuesMap = getIgnoredIssuesMap();
        final Collection<String> ignoredIssues = new ArrayList<>();

        try (FileReader fileReader = new FileReader(ConfigConstants.IGNORE_FILE_OLD)) {
            readIssues(ignoredIssuesMap, ignoredIssues, fileReader);
            logger.warning("Usage of .ignoreIssues is deprecated. Please use ignoreIssues.txt to whitelist issues.");
        } catch (IOException ex) {
            logger.info(ex.getMessage());
        }

        try (FileReader fileReader = new FileReader(filePath)) {
            readIssues(ignoredIssuesMap, ignoredIssues, fileReader);
        } catch (IOException ex) {
            logger.info("Ignored issues couldn't be read successfully");
        }

        return ignoredIssues;
    }

    /**
     * Reads the file and appends issues to the map of ignored issues
     *
     * @param ignoredIssuesMap Map of issues to be ignored
     * @param ignoredIssues    Collection of ignored issues
     * @param fileReader       FileReader
     */
    private void readIssues(final Map<String, String> ignoredIssuesMap, final Collection<String> ignoredIssues,
                            final FileReader fileReader) {
        try (final BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String zeile = bufferedReader.readLine();
            String prevLine = zeile;
            while (zeile != null) {
                addIgnoredIssue(ignoredIssuesMap, ignoredIssues, zeile, prevLine);
                prevLine = zeile;
                zeile = bufferedReader.readLine();
            }
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
    }

    /**
     * Check consistency of all models
     *
     * @param rules           all rules of ruleSet.xml
     * @param fileScanner     fileScanner
     * @param variableScanner variableScanner
     * @param dataFlowRules   dataFlowRules
     * @return foundIssues ConfigItem not found
     */
    private Collection<CheckerIssue> checkModels(final RuleSet rules, final FileScanner fileScanner,
                                                 final ProcessVariablesScanner variableScanner, Collection<DataFlowRule> dataFlowRules) {
        final Collection<CheckerIssue> issues = new ArrayList<>();

        for (final String pathToModel : fileScanner.getProcessDefinitions()) {
            issues.addAll(checkModel(rules, pathToModel, fileScanner, variableScanner, dataFlowRules));
        }
        return issues;
    }

    /**
     * Check consistency of a model
     *
     * @param rules
     *            all rules of ruleSet.xml
     * @param processDefinition
     *            processDefinition
     * @param fileScanner
     *            fileScanner
     * @param variableScanner
     *            variableScanner
     * @return modelIssues
     */
    private Collection<CheckerIssue> checkModel(final RuleSet rules, final String processDefinition,
                                                final FileScanner fileScanner, final ProcessVariablesScanner variableScanner,
                                                Collection<DataFlowRule> dataFlowRules) {
        BpmnModelDispatcher bpmnModelDispatcher = new BpmnModelDispatcher();
        ModelDispatchResult dispatchResult;
        File bpmnfile = null;
        String basepath = ConfigConstants.getInstance().getBasepath();

        if (basepath.startsWith("file:/")) {
            // Convert URI
            try {
                bpmnfile = new File(new URI(ConfigConstants.getInstance().getBasepath() + processDefinition));
            } catch (URISyntaxException e) {
                logger.log(Level.SEVERE, "URI of basedirectory seems to be malformed.", e);
            }
        } else {
            bpmnfile = new File(basepath + processDefinition);
        }

        if (variableScanner != null) {
            dispatchResult = bpmnModelDispatcher.dispatchWithVariables(fileScanner, bpmnfile, fileScanner.getDecisionRefToPathMap(),
                    fileScanner.getProcessIdToPathMap(), variableScanner, dataFlowRules,
                    fileScanner.getResourcesNewestVersions(), rules);
        } else {
            dispatchResult = bpmnModelDispatcher.dispatchWithoutVariables(bpmnfile, fileScanner.getDecisionRefToPathMap(),
                    fileScanner.getProcessIdToPathMap(), fileScanner.getResourcesNewestVersions(), rules);
        }
        elements.addAll(dispatchResult.getBpmnElements());
        processVariables.addAll(dispatchResult.getProcessVariables());
        setWrongCheckersMap(bpmnModelDispatcher.getIncorrectCheckers());

        return dispatchResult.getIssues();
    }

    /**
     * @param ignoredIssuesMap Map of ignored issues
     * @param issues           Collection of issues
     * @param row              row of file
     * @param prevLine         Previous line
     */
    private void addIgnoredIssue(final Map<String, String> ignoredIssuesMap, final Collection<String> issues,
                                 final String row, final String prevLine) {
        if (row != null && !row.isEmpty()) {
            if (!row.trim().startsWith("#")) {
                ignoredIssuesMap.put(row, prevLine);
                issues.add(row);
            }

        }
    }

    /**
     * Scan process variables in external classes, which are not referenced from
     * model
     *
     * @param scanner OuterProcessVariablesScanner
     */
    private void readOuterProcessVariables(final ProcessVariablesScanner scanner) {
        scanner.scanProcessVariables();
    }

    public Set<String> getModelPath() {
        return getFileScanner().getProcessDefinitions();
    }

    public Collection<CheckerIssue> getFilteredIssues() {
        return filteredIssues;
    }

    public Map<String, String> getIgnoredIssuesMap() {
        return ignoredIssuesMap;
    }

    public void setIgnoredIssuesMap(Map<String, String> ignoredIssuesMap) {
        this.ignoredIssuesMap = ignoredIssuesMap;
    }

    public boolean isCheckProcessVariables() {
        return checkProcessVariables;
    }

    public void setCheckProcessVariables(boolean checkProcessVariables) {
        this.checkProcessVariables = checkProcessVariables;
    }

    public void setDataFlowRules(Collection<DataFlowRule> dataFlowRules) {
        this.dataFlowRules = dataFlowRules;
    }

    public FileScanner getFileScanner() {
        return fileScanner;
    }

    public void setFileScanner(FileScanner fileScanner) {
        this.fileScanner = fileScanner;
    }

    public Map<String, String> getWrongCheckersMap() {
        return wrongCheckersMap;
    }

    public void setWrongCheckersMap(Map<String, String> wrongCheckersMap) {
        this.wrongCheckersMap = wrongCheckersMap;
    }

}
