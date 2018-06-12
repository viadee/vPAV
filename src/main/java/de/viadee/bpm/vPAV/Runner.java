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
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

public class Runner {

	private static Logger logger = Logger.getLogger(Runner.class.getName());

	private static FileScanner fileScanner;

	private static OuterProcessVariablesScanner variableScanner;

	private static Collection<CheckerIssue> issues;

	private static Collection<CheckerIssue> filteredIssues;

	private static Map<String, Rule> rules = new HashMap<String, Rule>(); 	

	private static Map<String, String> ignoredIssuesMap = new HashMap<String, String>();

	private static Map<String, String> incorrectCheckers = new HashMap<>();

	private static Map<String, String> fileMapping = createFileFolderMapping();

	private static ArrayList<String> allOutputFilesArray = createAllOutputFilesArray();

	private static Map<String, ArrayList<String>> sequenceFlowList = new HashMap<>();

	private static Map<String, BaseElement> signalNames = new HashMap<>();

	private static boolean isMisconfigured = false;

	private static boolean checkProcessVariables = false;

	/**
	 * Main method which represents lifecycle of the validation process Calls main
	 * functions
	 */
	public static void viadeeProcessApplicationValidator() {
		
		// 1
		rules = readConfig();

		// 2
		scanClassPath(rules);

		// 3
		getProcessVariables(rules);

		// 4
		createIssues(rules);

		// 5
		removeIgnoredIssues();

		// 6
		writeOutput(filteredIssues);

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
	 *
	 * write effectiveRuleSet to vPAV folder
	 *
	 * @return Map(String, Rule) ruleSet
	 */
	private static Map<String, Rule> readConfig() {		
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

		try {
			RuntimeConfig.getInstance().retrieveLocale(rules);
		} catch (MalformedURLException e) {
			logger.warning("Could not read language files. No localization available");
		}

		return rules;
	}

	/**
	 * merges ruleSets according to inheritance hierarchy (Deactivated < global <
	 * default < local)
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
	 * Initializes the variableScanner to scan and read outer process variables with
	 * the current javaResources
	 */
	private static void getProcessVariables(Map<String, Rule> rules) {
		if (rules.get("ProcessVariablesModelChecker").isActive()
				|| rules.get("ProcessVariablesNameConventionChecker").isActive()) {
			variableScanner = new OuterProcessVariablesScanner(fileScanner.getJavaResourcesFileInputStream());
			readOuterProcessVariables(variableScanner);
			setCheckProcessVariables(true);
		} else {
			setCheckProcessVariables(false);
		}
	}

	/**
	 * Creates the list of issues found for a given model and ruleSet Throws a
	 * RuntimeException if errors are found, so automated builds in a CI/CD pipeline
	 * will fail
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
		deleteFiles();
		createvPAVFolder();
		
		try {
			Files.createDirectory(Paths.get(ConfigConstants.JS_FOLDER));
			Files.createDirectory(Paths.get(ConfigConstants.CSS_FOLDER));
			Files.createDirectory(Paths.get(ConfigConstants.IMG_FOLDER));
		} catch (IOException e) {
			logger.warning("Could not create either output folder for JS, CSS or IMG");
		}
		
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
			deleteFiles();
			final IssueOutputWriter jsOutputWriter = new JsOutputWriter();
			try {
				jsOutputWriter.write(filteredIssues);
			} catch (OutputWriterException e) {
				throw new RuntimeException("JavaScript File couldn't be written");
			}
		}
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
	 * Delete files from destinations
	 *
	 * @param destinations
	 *            List of destinations who will be deleted
	 */
	private static void deleteFiles() {
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
	 *
	 * @throws RuntimeException
	 *             Files couldn't be copied
	 */
	private static void copyFiles() throws RuntimeException {

		ArrayList<Path> outputFiles = new ArrayList<Path>();
		for (String file : allOutputFilesArray)
			outputFiles.add(Paths.get(fileMapping.get(file), file));

		if (rules.get("CreateOutputHTML").isActive()) {			
			for (String file : allOutputFilesArray)
				copyFileToVPAVFolder(file);
		}

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
	 * @param file
	 *            File who will be copied to vPAV folder
	 * @throws RuntimeException
	 *             Files couldn't be written
	 */
	private static void copyFileToVPAVFolder(String file) throws RuntimeException {
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
	private static Collection<CheckerIssue> getFilteredIssues(Collection<CheckerIssue> issues) throws IOException {

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

		final HashMap<String, CheckerIssue> filteredIssues = new HashMap<>();
		filteredIssues.putAll(issuesMap);

		// remove issues that are listed in ignore file
		for (Map.Entry<String, CheckerIssue> entry : issuesMap.entrySet()) {
			if (ignoredIssues.contains(entry.getKey())) {
				filteredIssues.remove(entry.getKey());
			}
		}

		// transform back into collection
		final Collection<CheckerIssue> finalFilteredIssues = new ArrayList<CheckerIssue>();
		for (Map.Entry<String, CheckerIssue> entry : filteredIssues.entrySet()) {
			finalFilteredIssues.add(entry.getValue());
		}

		return finalFilteredIssues;
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

		final Map<String, String> ignoredIssuesMap = getIgnoredIssuesMap();
		final Collection<String> ignoredIssues = new ArrayList<String>();

		FileReader fileReader = createFileReader(filePath);

		if (fileReader != null) {
			final BufferedReader bufferedReader = new BufferedReader(fileReader);
			String zeile = bufferedReader.readLine();
			String prevLine = zeile;
			while (zeile != null) {
				addIgnoredIssue(ignoredIssuesMap, ignoredIssues, zeile, prevLine);
				prevLine = zeile;
				zeile = bufferedReader.readLine();
			}
			bufferedReader.close();
			fileReader.close();
		}
		return ignoredIssues;
	}

	/**
	 * 
	 * @param filePath
	 *            Path to ignoreIssues file
	 * @return FileReader ignoreIssue file
	 */
	private static FileReader createFileReader(final String filePath) {

		FileReader fileReader = null;
		try {
			fileReader = new FileReader(ConfigConstants.IGNORE_FILE_OLD);

			if (fileReader != null) {
				logger.warning(
						"Usage of .ignoreIssues is deprecated. Please use ignoreIssues.txt to whitelist issues.");
			}
		} catch (final FileNotFoundException ex) {
		}

		try {
			fileReader = new FileReader(filePath);
		} catch (final FileNotFoundException ex) {
			logger.info(ex.getMessage());
		}

		return fileReader;
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
			issues.addAll(checkModel(rules, pathToModel, fileScanner, variableScanner));
			Runner.resetSequenceFlowList();
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
			final FileScanner fileScanner, final OuterProcessVariablesScanner variableScanner) throws RuntimeException {
		Collection<CheckerIssue> modelIssues = new ArrayList<CheckerIssue>();
		try {
			if (variableScanner != null) {
				modelIssues = BpmnModelDispatcher.dispatchWithVariables(new File(ConfigConstants.BASEPATH + processdef),
						fileScanner.getDecisionRefToPathMap(), fileScanner.getProcessIdToPathMap(),
						variableScanner.getMessageIdToVariableMap(), variableScanner.getProcessIdToVariableMap(),
						fileScanner.getResourcesNewestVersions(), rules);
			} else {
				modelIssues = BpmnModelDispatcher.dispatchWithoutVariables(
						new File(ConfigConstants.BASEPATH + processdef), fileScanner.getDecisionRefToPathMap(),
						fileScanner.getProcessIdToPathMap(), fileScanner.getResourcesNewestVersions(), rules);
			}
		} catch (final ConfigItemNotFoundException e) {
			throw new RuntimeException("Config item couldn't be read");
		}

		return modelIssues;
	}

	/**
	 * Scan process variables in external classes, which are not referenced from
	 * model
	 *
	 * @param scanner
	 *            OuterProcessVariablesScanner
	 * @throws IOException
	 *             Outer process variables couldn't be read
	 */
	private static void readOuterProcessVariables(final OuterProcessVariablesScanner scanner) throws RuntimeException {
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
	private static Map<String, String> addIgnoredIssue(final Map<String, String> ignoredIssuesMap,
			final Collection<String> issues, final String row, final String prevLine) {
		if (row != null && !row.isEmpty()) {
			if (!row.trim().startsWith("#")) {
				ignoredIssuesMap.put(row, prevLine);
				issues.add(row);
			}

		}
		return ignoredIssuesMap;
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
		Runner.isMisconfigured = isMisconfigured;
	}

	public static Map<String, ArrayList<String>> getSequenceFlowList() {
		return sequenceFlowList;
	}

	public static void addToSequenceFlowList(String id, ArrayList<String> sequenceFlowList) {
		Runner.sequenceFlowList.put(id, sequenceFlowList);
	}

	public static void resetSequenceFlowList() {
		Runner.sequenceFlowList.clear();
	}

	public static boolean addSignal(final BaseElement baseElement, final String name) {
		if (!Runner.signalNames.containsKey(name)) {
			Runner.signalNames.put(name, baseElement);
			return true;
		} else {
			return false;
		}
	}

	public static BaseElement getSignal(final String name) {
		return Runner.signalNames.get(name);
	}

	public static void removeElement(final String name) {
		Runner.signalNames.remove(name);
	}

	public static Map<String, String> getIncorrectCheckers() {
		return incorrectCheckers;
	}

	public static void setIncorrectCheckers(final Map.Entry<String, Rule> rule, final String message) {
		if (!getIncorrectCheckers().containsKey(rule.getValue().getName())) {
			Runner.incorrectCheckers.put(rule.getValue().getName(), message);
		}
	}

	public static Map<String, String> getIgnoredIssuesMap() {
		return ignoredIssuesMap;
	}

	public static void setIgnoredIssuesMap(Map<String, String> ignoredIssuesMap) {
		Runner.ignoredIssuesMap = ignoredIssuesMap;
	}

	public static boolean isCheckProcessVariables() {
		return checkProcessVariables;
	}

	public static void setCheckProcessVariables(boolean checkProcessVariables) {
		Runner.checkProcessVariables = checkProcessVariables;
	}
}
