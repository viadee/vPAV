/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the viadee Unternehmensberatung GmbH.
 * 4. Neither the name of the viadee Unternehmensberatung GmbH nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <viadee Unternehmensberatung GmbH> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.reader.ConfigReaderException;
import de.viadee.bpm.vPAV.config.reader.XmlConfigReader;
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

    private static boolean isExecuted = false;

    public static void run_vPAV() {

        // 1
        final Map<String, Rule> rules = readConfig();

        // 2
        scanClassPath(rules);

        // 3
        getProcessVariables(rules);

        // 4
        createIssues(rules);

        // 5
        filteredIssues = filterIssues(issues);

        // 6
        writeOutput(filteredIssues);

        // 7
        copyFiles();

        logger.info("BPMN validation successful completed");

    }

    // 1
    public static Map<String, Rule> readConfig() {
    		createBaseFolder();
        Map<String, Rule> rules;
        final RuleSetOutputWriter ruleSetOutputWriter = new RuleSetOutputWriter();
        try {
            rules = new XmlConfigReader().read(new File(ConstantsConfig.RULESET));

            if ((!rules.containsKey(ConstantsConfig.HASPARENTRULESET))
                    || rules.containsKey(ConstantsConfig.HASPARENTRULESET)
                            && !rules.get(ConstantsConfig.HASPARENTRULESET).isActive()) {
                try {
					ruleSetOutputWriter.write(rules);
				} catch (OutputWriterException e) {
					e.printStackTrace();
				}
                return rules;
            } else if (rules.containsKey(ConstantsConfig.HASPARENTRULESET)
                    && rules.get(ConstantsConfig.HASPARENTRULESET).isActive()) {
                rules = readParentRule(rules);
            } else {
                rules = new XmlConfigReader().read(new File(ConstantsConfig.RULESETDEFAULT));
            }
        } catch (final ConfigReaderException e) {
            throw new RuntimeException("Config file could not be read");
        }
        try {
			ruleSetOutputWriter.write(rules);
		} catch (OutputWriterException e) {
			e.printStackTrace();
		}
        return rules;
    }

    // 1b - Read parent config file
    public static Map<String, Rule> readParentRule(final Map<String, Rule> childRules) {
        final Map<String, Rule> parentRules;
        final Map<String, Rule> finalRules = new HashMap<>();
        try {
            parentRules = new XmlConfigReader().read(new File(getParentConfig()));
        } catch (final ConfigReaderException e) {
            throw new RuntimeException("Parent config file could not be read");
        }

        finalRules.putAll(parentRules);
        finalRules.putAll(childRules);
        finalRules.remove(ConstantsConfig.HASPARENTRULESET);

        return finalRules;
    }

    // 2b - Scan classpath for models
    public static void scanClassPath(Map<String, Rule> rules) {

        final Rule processVariablesLocationRule = rules.get(ConstantsConfig.PROCESS_VARIABLES_LOCATION);

        // logger.warning(processVariablesLocationRule.getSettings().get(ConstantsConfig.LOCATION).getValue());

        if (processVariablesLocationRule == null
                || processVariablesLocationRule.getSettings().get(ConstantsConfig.LOCATION).getValue() == null
                || processVariablesLocationRule.getSettings().get(ConstantsConfig.LOCATION).getValue().isEmpty()) {
            logger.warning("Rule for ProcessVariablesLocation is not working correctly. Please verify the ruleSet.xml");
            fileScanner = new FileScanner(rules);
        } else {
            final String location = processVariablesLocationRule.getSettings().get("location").getValue();
            fileScanner = new FileScanner(rules, location);
        }

    }

    // 3 - Get process variables
    public static void getProcessVariables(final Map<String, Rule> rules) {

        final Rule processVariablesLocationRule = rules.get(ConstantsConfig.PROCESS_VARIABLES_LOCATION);

        variableScanner = new OuterProcessVariablesScanner(fileScanner.getJavaResources());

        if (processVariablesLocationRule == null
                || processVariablesLocationRule.getSettings().get(ConstantsConfig.LOCATION).getValue() == null
                || processVariablesLocationRule.getSettings().get(ConstantsConfig.LOCATION).getValue().isEmpty()) {
            logger.warning("Rule for ProcessVariablesLocation is not working correctly. Please verify the ruleSet.xml");
        } else if (rules.containsKey(ConstantsConfig.PROCESS_VARIABLES_LOCATION)
                && processVariablesLocationRule.isActive()) {
            readOuterProcessVariables(variableScanner);
        }

    }

    // 4 - Check each model
    public static void createIssues(Map<String, Rule> rules) throws RuntimeException {
        issues = checkModels(rules, fileScanner, variableScanner);
    }

    // 5 remove ignored issues
    public static void removeIgnoredIssues() throws RuntimeException {
        filteredIssues = filterIssues(issues);
    }

    /**
     * write output files (xml / json/ js)
     *
     * @param filteredIssues
     *            List of filteredIssues
     * @throws RuntimeException
     *             Abort if writer can not be instantiated
     */
    public static void writeOutput(final Collection<CheckerIssue> filteredIssues) throws RuntimeException {
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
            validationFiles.add(Paths.get(ConstantsConfig.VALIDATION_JS_OUTPUT));
            validationFiles.add(Paths.get(ConstantsConfig.VALIDATION_JSON_OUTPUT));
            validationFiles.add(Paths.get(ConstantsConfig.VALIDATION_XML_OUTPUT));
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
     * create Base folders
     *
     * @throws RuntimeException
     */
    private static void createBaseFolder() throws RuntimeException {
        createvPAVFolder();
        createImgFolder();
        createCssFolder();
        createJsFolder();
    }

    /**
     * make vPAV folder
     */
    private static void createvPAVFolder() {
        File vPavDir = new File(ConstantsConfig.VALIDATION_FOLDER);

        if (!vPavDir.exists()) {
            boolean success = vPavDir.mkdirs();
            if (!success) {
                throw new RuntimeException("vPav directory does not exist and could not be created");
            }
        }
    }

    /**
     * make img folder
     */
    private static void createImgFolder() {

        File imgDir = new File(ConstantsConfig.IMG_FOLDER);

        if (!imgDir.exists()) {
            boolean success = imgDir.mkdirs();
            if (!success) {
                throw new RuntimeException("vPav/img directory does not exist and could not be created");
            }
        }
    }

    /**
     * make css folder
     */
    private static void createJsFolder() {
        File jsDir = new File(ConstantsConfig.JS_FOLDER);
        if (!jsDir.exists()) {
            boolean success = jsDir.mkdirs();
            if (!success)
                throw new RuntimeException("vPav/js directory does not exist and could not be created");
        }
    }

    /**
     * make css folder
     */
    private static void createCssFolder() {
        File cssDir = new File(ConstantsConfig.CSS_FOLDER);
        if (!cssDir.exists()) {
            boolean success = cssDir.mkdirs();
            if (!success)
                throw new RuntimeException("vPav/css directory does not exist and could not be created");
        }

    }

    /**
     * delete files from destinations
     *
     * @param destinations
     */
    private static void deleteFiles(ArrayList<Path> destinations) {
        for (Path destination : destinations) {
            if (destination.toFile().exists())
                destination.toFile().delete();
        }
    }

    // 7 copy html-files to target
    // 7a delete files before
    private static void copyFiles() throws RuntimeException {
        // 7a delete files before copy
        ArrayList<Path> outputFiles = new ArrayList<Path>();
        for (String file : allOutputFilesArray)
            outputFiles.add(Paths.get(fileMapping.get(file), file));
        deleteFiles(outputFiles);

        for (String file : allOutputFilesArray)
            copyFileToVPAVFolder(file);
    }

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

        allFiles.add("validationResult.html");

        return allFiles;
    }

    private static Map<String, String> createFileFolderMapping() {
        Map<String, String> fMap = new HashMap<String, String>();
        fMap.put("bootstrap.min.js", ConstantsConfig.JS_FOLDER);
        fMap.put("bpmn-navigated-viewer.js", ConstantsConfig.JS_FOLDER);
        fMap.put("bpmn.io.viewer.app.js", ConstantsConfig.JS_FOLDER);
        fMap.put("jquery-3.2.1.min.js", ConstantsConfig.JS_FOLDER);
        fMap.put("popper.min.js", ConstantsConfig.JS_FOLDER);
        fMap.put("infoPOM.js", ConstantsConfig.JS_FOLDER);

        fMap.put("bootstrap.min.css", ConstantsConfig.CSS_FOLDER);
        fMap.put("viadee.css", ConstantsConfig.CSS_FOLDER);
        fMap.put("MarkerStyle.css", ConstantsConfig.CSS_FOLDER);

        fMap.put("vPAV.png", ConstantsConfig.IMG_FOLDER);
        fMap.put("viadee_Logo.png", ConstantsConfig.IMG_FOLDER);
        fMap.put("GitHub.png", ConstantsConfig.IMG_FOLDER);
        fMap.put("error.png", ConstantsConfig.IMG_FOLDER);
        fMap.put("warning.png", ConstantsConfig.IMG_FOLDER);
        fMap.put("info.png", ConstantsConfig.IMG_FOLDER);

        fMap.put("validationResult.html", ConstantsConfig.VALIDATION_FOLDER);

        return fMap;
    }

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
     * @return
     * @throws IOException
     */
    private static Collection<CheckerIssue> filterIssues(final Collection<CheckerIssue> issues)
            throws RuntimeException {
        Collection<CheckerIssue> filteredIssues;
        try {
            filteredIssues = getFilteredIssues(issues);
        } catch (final IOException e) {
            throw new RuntimeException("Ignored issues couldn't be read successfully", e);
        }
        return filteredIssues;
    }

    /**
     * remove false positives from issue collection
     *
     * @param issues
     * @return filteredIssues
     * @throws IOException
     */
    private static Collection<CheckerIssue> getFilteredIssues(Collection<CheckerIssue> issues)
            throws IOException {
        final Collection<CheckerIssue> filteredIssues = new ArrayList<CheckerIssue>();
        filteredIssues.addAll(issues);

        final Collection<String> ignoredIssues = collectIgnoredIssues(ConstantsConfig.IGNORE_FILE);
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
     * @return issue ids
     * @throws IOException
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
     * check consistency of all models
     *
     * @param rules
     * @param beanMapping
     * @param fileScanner
     * @param variableScanner
     * @return
     * @throws ConfigItemNotFoundException
     */
    private static Collection<CheckerIssue> checkModels(final Map<String, Rule> rules, final FileScanner fileScanner,
            final OuterProcessVariablesScanner variableScanner) throws RuntimeException {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        for (final String pathToModel : fileScanner.getProcessdefinitions()) {
            issues.addAll(checkModel(rules, pathToModel, fileScanner,
                    variableScanner));
        }
        return issues;
    }

    /**
     * check consistency of a model
     *
     * @param rules
     * @param beanMapping
     * @param processdef
     * @param fileScanner
     * @param variableScanner
     * @return
     * @throws ConfigItemNotFoundException
     */
    private static Collection<CheckerIssue> checkModel(final Map<String, Rule> rules, final String processdef,
            final FileScanner fileScanner,
            final OuterProcessVariablesScanner variableScanner) throws RuntimeException {
        Collection<CheckerIssue> modelIssues;
        try {
            modelIssues = BpmnModelDispatcher.dispatch(new File(ConstantsConfig.BASEPATH + processdef),
                    fileScanner.getDecisionRefToPathMap(), fileScanner.getProcessIdToPathMap(),
                    variableScanner.getMessageIdToVariableMap(), variableScanner.getProcessIdToVariableMap(),
                    fileScanner.getResourcesNewestVersions(), rules);

        } catch (final ConfigItemNotFoundException e) {
            throw new RuntimeException("Config item couldn't be read");
        }
        return modelIssues;
    }

    /**
     * scan process variables in external classes, which are not referenced from model
     *
     * @param scanner
     * @throws IOException
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
     * Add ignored issue
     *
     * @param issues
     * @param zeile
     */
    private static void addIgnoredIssue(final Collection<String> issues, final String zeile) {
        if (zeile != null && !zeile.isEmpty() && !zeile.trim().startsWith("#"))
            issues.add(zeile);
    }

    public static Set<String> getModelPath() {
        return fileScanner.getProcessdefinitions();
    }

    public static Collection<CheckerIssue> getfilteredIssues() {
        return filteredIssues;
    }

    /**
     * get path to parent ruleset
     *
     * @return String path to ruleset at runtime
     */
    public static String getParentConfig() {
        URLClassLoader ucl;
        if (RuntimeConfig.getInstance().getClassLoader() instanceof URLClassLoader) {
            ucl = ((URLClassLoader) RuntimeConfig.getInstance().getClassLoader());
        } else {
            ucl = ((URLClassLoader) RuntimeConfig.getInstance().getClassLoader().getParent());
        }

        final URL path = ucl.getResource(ConstantsConfig.RULESETPARENT);

        return path.toString().substring(6);
    }

    public static boolean isExecuted() {
        return isExecuted;
    }

    public static void setExecuted(boolean isExecuted) {
        AbstractRunner.isExecuted = isExecuted;
    }

}
