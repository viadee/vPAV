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

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.config.reader.ConfigReaderException;
import de.viadee.bpm.vPAV.config.reader.XmlConfigReader;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.output.*;
import de.viadee.bpm.vPAV.processing.BpmnModelDispatcher;
import de.viadee.bpm.vPAV.processing.EntryPointScanner;
import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.ModelDispatchResult;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.apache.commons.io.FileUtils;

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

    private EntryPointScanner variableScanner;

    private Collection<CheckerIssue> filteredIssues;

    private RuleSet rules = new RuleSet();

    private Map<String, String> ignoredIssuesMap = new HashMap<>();

    private Map<String, String> fileMapping = mapStaticFilesToTargetFolders();

    private Map<String, String> wrongCheckersMap = new HashMap<>();

    private Collection<BpmnElement> elements = new ArrayList<>();

    private Collection<ProcessVariable> processVariables = new ArrayList<>();

    private Collection<DataFlowRule> dataFlowRules = new ArrayList<>();

    /**
     * Main method which represents lifecycle of the validation process. Calls main
     * functions
     */
    public void viadeeProcessApplicationValidator() {
        // 1
        rules = readConfig();

        // 2
        setFileScanner(new FileScanner(rules));

        // 3
        JavaReaderStatic.setupSoot();

        // 4
        getProcessVariables(rules);

        // 5
        createIssues(rules, dataFlowRules);

        // 6
        removeIgnoredIssues();

        // 7
        writeOutput(filteredIssues, elements, processVariables);

        // 8
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
    public RuleSet readConfig() {

        prepareOutputFolder();

        rules = new XmlConfigReader().getDeactivatedRuleSet();

        final RuleSetOutputWriter ruleSetOutputWriter = new RuleSetOutputWriter();
        try {
            String ruleSetPath =
                    RuntimeConfig.getInstance().getRuleSetPath() + RuntimeConfig.getInstance().getRuleSetFileName();
            if (new File(ruleSetPath).exists()) {
                RuleSet localRules = new XmlConfigReader().read(RuntimeConfig.getInstance().getRuleSetFileName());

                if (localRules.hasParentRuleSet()) {
                    rules = mergeRuleSet(localRules,
                            new XmlConfigReader().read(RuntimeConfig.getInstance().getParentRuleSetFileName()));
                } else {
                    rules = localRules;
                }
            } else {
                rules = new XmlConfigReader().read(ConfigConstants.RULESET_DEFAULT);
            }

            ruleSetOutputWriter.write(rules);
            RuntimeConfig.getInstance().setRuleSet(rules);
        } catch (final ConfigReaderException | OutputWriterException e) {
            throw new RuntimeException(e);
        }

        RuntimeConfig.getInstance().retrieveLocale();

        return rules;
    }

    /**
     * Delete old output and create new output folder
     */
    private void prepareOutputFolder() {

        deleteFiles();
        createvPAVFolder();
        try {
            Files.createDirectory(Paths.get(RuntimeConfig.getInstance().getJsFolder()));
            Files.createDirectory(Paths.get(RuntimeConfig.getInstance().getCssFolder()));
            Files.createDirectory(Paths.get(RuntimeConfig.getInstance().getImgFolder()));
            Files.createDirectory(Paths.get(RuntimeConfig.getInstance().getFontFolder()));
            Files.createDirectory(Paths.get(RuntimeConfig.getInstance().getDataFolder()));
        } catch (IOException e) {
            logger.warning("Could not create one of the resources output folders:" + e.getMessage());
        }

    }

    /**
     * merges ruleSets according to inheritance hierarchy (Deactivated &lt; global
     * &lt; default &lt; local)
     *
     * @param parentRules Basis RuleSet which will be overwritten
     * @param childRules  New RuleSet
     * @return Map(String, Rule) finalRules merged ruleSet
     */
    protected RuleSet mergeRuleSet(final RuleSet parentRules, final RuleSet childRules) {
        final Map<String, Map<String, Rule>> finalElementRules = new HashMap<>(parentRules.getElementRules());
        final Map<String, Map<String, Rule>> finalModelRules = new HashMap<>(parentRules.getModelRules());

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
        return new RuleSet(finalElementRules, finalModelRules, childRules.hasParentRuleSet());
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
            variableScanner = new EntryPointScanner(getFileScanner().getJavaResourcesFileInputStream());
            readOuterProcessVariables(variableScanner);
        }
        setCheckProcessVariables();
    }

    private boolean oneCheckerIsActive(final Map<String, Map<String, Rule>> rules, String name) {
        if (!rules.isEmpty() && Objects.nonNull(rules.get(name))) {
            for (Rule r : rules.get(name).values()) {
                if (r.isActive()) {
                    return true;
                }
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
        checkModels(rules, getFileScanner(), variableScanner, dataFlowRules);
    }

    /**
     * Removes whitelisted issues from the list of issues found
     */
    private void removeIgnoredIssues() {
        filteredIssues = filterIssues(IssueService.getInstance().getIssues());
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
        File vPavDir = new File(RuntimeConfig.getInstance().getValidationFolder());

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
        File index = new File(RuntimeConfig.getInstance().getValidationFolder());
        if (index.exists()) {
            try {
                FileUtils.deleteDirectory(index);
            } catch (IOException e) {
                logger.warning("Couldn't delete directory: " + e.getMessage());
            }
        }
    }

    /**
     * Copies files to vPAV folder
     */
    private void copyFiles() {
        if (RuntimeConfig.getInstance().isHtmlOutputEnabled()) {
            fileMapping.keySet().forEach(file -> {
                InputStream source = Runner.class.getClassLoader().getResourceAsStream(file);
                Path destination = Paths.get(fileMapping.get(file) + file.substring(file.lastIndexOf('/')));
                try {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException | NullPointerException e) {
                    throw new RuntimeException("Files couldn't be written");
                }
            });
        }
    }

    /**
     * Creates a map for static files regarding the HTML output and their corresponding folders
     *
     * @return Map<String, String> fMap
     */
    private Map<String, String> mapStaticFilesToTargetFolders() {
        Map<String, String> fileToFolderMap = new HashMap<>();
        fileToFolderMap.put(ConfigConstants.JS_INPUT_FOLDER + "bootstrap.bundle.min.js",
                RuntimeConfig.getInstance().getJsFolder());
        fileToFolderMap.put(ConfigConstants.JS_INPUT_FOLDER + "bpmn-navigated-viewer.js",
                RuntimeConfig.getInstance().getJsFolder());
        fileToFolderMap.put(ConfigConstants.JS_INPUT_FOLDER + "bpmn.io.viewer.app.js",
                RuntimeConfig.getInstance().getJsFolder());
        fileToFolderMap.put(ConfigConstants.JS_INPUT_FOLDER + "jquery-3.5.1.min.js",
                RuntimeConfig.getInstance().getJsFolder());
        fileToFolderMap.put(ConfigConstants.JS_INPUT_FOLDER + "download.js",
                RuntimeConfig.getInstance().getJsFolder());
        fileToFolderMap.put(ConfigConstants.JS_INPUT_FOLDER + "script-loader.js",
                RuntimeConfig.getInstance().getJsFolder());
        //TODO put POM.js in Js writer
        fileToFolderMap.put("./infoPOM.js", RuntimeConfig.getInstance().getDataFolder());

        fileToFolderMap.put(ConfigConstants.CSS_INPUT_FOLDER + "bootstrap.min.css",
                RuntimeConfig.getInstance().getCssFolder());
        fileToFolderMap.put(ConfigConstants.CSS_INPUT_FOLDER + "viadee.css",
                RuntimeConfig.getInstance().getCssFolder());
        fileToFolderMap.put(ConfigConstants.CSS_INPUT_FOLDER + "MarkerStyle.css",
                RuntimeConfig.getInstance().getCssFolder());
        fileToFolderMap.put(ConfigConstants.CSS_INPUT_FOLDER + "all.css",
                RuntimeConfig.getInstance().getCssFolder());

        fileToFolderMap.put(ConfigConstants.IMG_INPUT_FOLDER + "vPAV.png",
                RuntimeConfig.getInstance().getImgFolder());
        fileToFolderMap.put(ConfigConstants.IMG_INPUT_FOLDER + "viadee_weiss.png",
                RuntimeConfig.getInstance().getImgFolder());

        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-brands-400.eot",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-brands-400.svg",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-brands-400.ttf",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-brands-400.woff",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-brands-400.woff2",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-regular-400.eot",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-regular-400.svg",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-regular-400.ttf",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-regular-400.woff",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-regular-400.woff2",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-solid-900.eot",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-solid-900.svg",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-solid-900.ttf",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-solid-900.woff",
                RuntimeConfig.getInstance().getFontFolder());
        fileToFolderMap.put(ConfigConstants.FONT_INPUT_FOLDER + "fa-solid-900.woff2",
                RuntimeConfig.getInstance().getFontFolder());

        fileToFolderMap
                .put(ConfigConstants.HTML_INPUT_FOLDER + ConfigConstants.HTML_FILE,
                        RuntimeConfig.getInstance().getValidationFolder());

        return fileToFolderMap;
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
        final Collection<String> ignoredIssues = collectIgnoredIssues();

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
     * @return issue ids
     */
    private Collection<String> collectIgnoredIssues() {

        final Map<String, String> ignoredIssuesMap = getIgnoredIssuesMap();
        final Collection<String> ignoredIssues = new ArrayList<>();

        try (FileReader fileReader = new FileReader(ConfigConstants.IGNORE_FILE_OLD)) {
            readIssues(ignoredIssuesMap, ignoredIssues, fileReader);
            logger.warning("Usage of .ignoreIssues is deprecated. Please use ignoreIssues.txt to whitelist issues.");
        } catch (IOException ex) {
            logger.info(ex.getMessage());
        }

        try (FileReader fileReader = new FileReader(ConfigConstants.IGNORE_FILE)) {
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
     */
    private void checkModels(final RuleSet rules, final FileScanner fileScanner,
            final EntryPointScanner variableScanner, Collection<DataFlowRule> dataFlowRules) {
        for (final String pathToModel : fileScanner.getProcessDefinitions()) {
            checkModel(rules, pathToModel, fileScanner, variableScanner, dataFlowRules);
        }
    }

    /**
     * Check consistency of a model
     *
     * @param rules             all rules of ruleSet.xml
     * @param processDefinition processDefinition
     * @param fileScanner       fileScanner
     * @param variableScanner   variableScanner
     */
    private void checkModel(final RuleSet rules, final String processDefinition,
            final FileScanner fileScanner, final EntryPointScanner variableScanner,
            Collection<DataFlowRule> dataFlowRules) {
        BpmnModelDispatcher bpmnModelDispatcher = new BpmnModelDispatcher();
        ModelDispatchResult dispatchResult;
        File bpmnfile = null;
        String basepath = RuntimeConfig.getInstance().getBasepath();

        if (basepath.startsWith("file:/")) {
            // Convert URI
            try {
                bpmnfile = new File(new URI(RuntimeConfig.getInstance().getBasepath() + processDefinition));
            } catch (URISyntaxException e) {
                logger.log(Level.SEVERE, "URI of basedirectory seems to be malformed.", e);
            }
        } else {
            bpmnfile = new File(basepath + processDefinition);
        }

        if (variableScanner != null) {
            dispatchResult = bpmnModelDispatcher.dispatchWithVariables(fileScanner, bpmnfile, variableScanner,
                    dataFlowRules, rules);
        } else {
            dispatchResult = bpmnModelDispatcher.dispatchWithoutVariables(bpmnfile,
                    fileScanner.getDecisionRefToPathMap(), fileScanner.getProcessIdToPathMap(),
                    fileScanner.getResourcesNewestVersions(), rules);
        }
        elements.addAll(dispatchResult.getBpmnElements());
        processVariables.addAll(dispatchResult.getProcessVariables());
        setWrongCheckersMap(bpmnModelDispatcher.getIncorrectCheckers());
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
    private void readOuterProcessVariables(final EntryPointScanner scanner) {
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

    public void setCheckProcessVariables() {
    }

    public void setDataFlowRules(Collection<DataFlowRule> dataFlowRules) {
        this.dataFlowRules = dataFlowRules;
    }

    public FileScanner getFileScanner() {
        return fileScanner;
    }

    public void setFileScanner(FileScanner fileScanner) {
        this.fileScanner = fileScanner;
        RuntimeConfig.getInstance().setFileScanner(fileScanner);
    }

    public Map<String, String> getWrongCheckersMap() {
        return wrongCheckersMap;
    }

    public void setWrongCheckersMap(Map<String, String> wrongCheckersMap) {
        this.wrongCheckersMap = wrongCheckersMap;
    }

}
