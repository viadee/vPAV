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
package de.viadee.bpm.vPAV.output;

import com.cronutils.utils.StringUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.exceptions.OutputWriterException;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.*;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Create the JavaScript file for HTML-output; Needs: issues and bpmnFile names
 */
public class JsOutputWriter implements IssueOutputWriter {

	private static Logger logger = Logger.getLogger(JsOutputWriter.class.getName());

	private Map<String, String> ignoredIssuesMap = new HashMap<>();

	private Map<String, String> wrongCheckersMap = new HashMap<>();

	private Set<String> modelPaths = new HashSet<>();

	/**
	 * Writes the output as JavaScript to the vPAV output folder
	 */
	@Override
	public void write(final Collection<CheckerIssue> issues) throws OutputWriterException {

		final String json = transformToJsonDatastructure(issues, BpmnConstants.VPAV_ELEMENTS_TO_MARK);
		final String json_noIssues = transformToJsonDatastructure(getNoIssues(issues),
				BpmnConstants.VPAV_NO_ISSUES_ELEMENTS);
		final String bpmn = transformToXMLDatastructure();
		final String wrongCheckers = transformToJsDatastructure(getWrongCheckersMap()) + "\n";
		final String defaultCheckers = transformDefaultRulesToJsDatastructure(
				extractExternalCheckers(RuntimeConfig.getInstance().getActiveRules()));
		final String issueSeverity = transformSeverityToJsDatastructure(createIssueSeverity(issues));
		final String ignoredIssues = transformIgnoredIssuesToJsDatastructure(getIgnoredIssuesMap());
		final String properties = transformPropertiesToJsonDatastructure();
		final String summary = transformSummaryToJson(issues);

		final Map<String, String> fileMap = new HashMap<>();
		fileMap.put(RuntimeConfig.getInstance().getValidationJsOutput(), json);
		fileMap.put(RuntimeConfig.getInstance().getValidationJsSuccessOutput(), json_noIssues);
		fileMap.put(RuntimeConfig.getInstance().getValidationJsModelOutput(), bpmn);
		fileMap.put(RuntimeConfig.getInstance().getValidationJsCheckers(), wrongCheckers + defaultCheckers);
		fileMap.put(RuntimeConfig.getInstance().getValidationJsIssueSeverity(), issueSeverity);
		fileMap.put(RuntimeConfig.getInstance().getValidationIgnoredIssuesOutput(), ignoredIssues);
		fileMap.put(RuntimeConfig.getInstance().getPropertiesJsOutput(), properties);
		fileMap.put(RuntimeConfig.getInstance().getProjectSummaryJsOutput(), summary);

		writeJS(fileMap);
	}

	public void prepareMaps(final Map<String, String> wrongCheckers, final Map<String, String> ignoredIssues,
			final Set<String> modelPath) {
		this.setWrongCheckersMap(wrongCheckers);
		this.setIgnoredIssuesMap(ignoredIssues);
		this.setModelPaths(modelPath);
	}

	/**
	 * Creates list which contains elements with multiple issues and the marks it
	 * with highest severity
	 *
	 * @param issues Collected issues
	 */
	private Map<String, CriticalityEnum> createIssueSeverity(final Collection<CheckerIssue> issues) {
		Map<String, CriticalityEnum> issueSeverity = new HashMap<>();
		for (CheckerIssue issue : issues) {
			if (!issueSeverity.containsKey(issue.getElementId())) {
				issueSeverity.put(issue.getElementId(), issue.getClassification());
			} else if (issueSeverity.containsKey(issue.getElementId())
					&& issueSeverity.get(issue.getElementId()).equals(CriticalityEnum.WARNING)) {
				if (issue.getClassification().equals(CriticalityEnum.ERROR)) {
					issueSeverity.put(issue.getElementId(), issue.getClassification());
				}
			}
		}

		return issueSeverity;
	}

	/**
	 * Extract external rules from active ruleset
	 *
	 * @param activeRules Active RuleSet
	 * @return List of external configured checkers
	 */
	private ArrayList<String> extractExternalCheckers(final ArrayList<String> activeRules) {
		final ArrayList<String> defaultRules = new ArrayList<>();
		for (String entry : RuntimeConfig.getInstance().getViadeeRules()) {
			if (activeRules.contains(entry)) {
				defaultRules.add(entry);
			}
		}
		return defaultRules;
	}

	/**
	 * @param files A map with filenames as key and content as value
	 * @throws OutputWriterException If JS could not be written
	 */
	private void writeJS(Map<String, String> files) throws OutputWriterException {
		final String errorMessage = "JS output couldn't be written";
		for (Map.Entry<String, String> entry : files.entrySet()) {
			if (!StringUtils.isEmpty(entry.getValue())) {
				try (OutputStreamWriter streamWriter = new OutputStreamWriter(
						new FileOutputStream(entry.getKey()),
						StandardCharsets.UTF_8)) {
					streamWriter.write(entry.getValue());
				} catch (IOException e) {
					throw new OutputWriterException(errorMessage, e);
				}
			}
		}
	}

	/**
	 * Write javascript file with elements which have variables
	 *
	 * @param elements
	 *            Collection of BPMN elements across all models
	 * @param processVariables
	 *            Collection of process variables
	 */
	public void writeVars(final Collection<BpmnElement> elements, final Collection<ProcessVariable> processVariables) {

		try (FileWriter writer = new FileWriter(RuntimeConfig.getInstance().getValidationJsProcessVariables(), true)) {

			// write elements containing operations
			JsonArray jsonElements = elements.stream()
					.map(JsOutputWriter::transformElementToJsonIncludingProcessVariables)
					.filter(o -> o.has("elementId")).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
			StringBuilder jsFile = new StringBuilder();
			jsFile.append(transformJsonToJs("proz_vars", jsonElements)).append(";\n\n");
			JsonArray jsonVariables = processVariables.stream().map(JsOutputWriter::transformProcessVariablesToJson)
					.collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
			jsFile.append(transformJsonToJs("processVariables", jsonVariables));
			writer.write(jsFile.toString());
		} catch (IOException e1) {
			logger.warning("Processvariables couldn't be written");
		}
	}

	/**
	 * Transforms a bpmn element to json object
	 *
	 * @param element
	 *            BpmnElement
	 * @return BpmnElement as JSON Object
	 */
	private static JsonObject transformElementToJsonIncludingProcessVariables(final BpmnElement element) {
		final JsonObject obj = new JsonObject();
		if (!element.getProcessVariables().isEmpty()) {
			// elementID
			obj.addProperty("elementId", element.getBaseElement().getId());
			// bpmnFile
			obj.addProperty(BpmnConstants.VPAV_BPMN_FILE,
					replace("\\", element.getProcessDefinition()));
			// element Name
			if (element.getBaseElement().getAttributeValue("name") != null)
				obj.addProperty("elementName", element.getBaseElement().getAttributeValue(BpmnConstants.ATTR_NAME));

			Function<ProcessVariableOperation, JsonObject> processVariableToJson = o -> {
				final JsonObject jsonOperation = new JsonObject();
				jsonOperation.addProperty("name", o.getName());
				jsonOperation.addProperty("fieldType", o.getFieldType().getDescription());
				jsonOperation.addProperty("elementChapter", o.getChapter().toString());
				return jsonOperation;
			};
			obj.add("read",
					element.getProcessVariables().values().stream()
							.filter(o -> o.getOperation() == VariableOperation.READ).map(processVariableToJson)
							.collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
			obj.add("write",
					element.getProcessVariables().values().stream()
							.filter(o -> o.getOperation() == VariableOperation.WRITE).map(processVariableToJson)
							.collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
			obj.add("delete",
					element.getProcessVariables().values().stream()
							.filter(o -> o.getOperation() == VariableOperation.DELETE).map(processVariableToJson)
							.collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
		}
		return obj;
	}

	/**
	 * Transforms a process variable to a json object
	 *
	 * @param processVariable
	 *            ProcessVariable
	 * @return ProcessVariable as JSON object
	 */
	private static JsonObject transformProcessVariablesToJson(final ProcessVariable processVariable) {
		final JsonObject obj = new JsonObject();
		obj.addProperty("name", processVariable.getName());
		if (processVariable.getOperations().size() > 0) {
			String bpmnFile = processVariable.getOperations().get(0).getElement().getProcessDefinition();
			obj.addProperty(BpmnConstants.VPAV_BPMN_FILE, replace("\\", bpmnFile));
		}
		Function<ProcessVariableOperation, JsonObject> processVariableToJson = o -> {
			final JsonObject jsonOperation = new JsonObject();
			jsonOperation.addProperty("elementId", o.getElement().getBaseElement().getId());
			jsonOperation.addProperty("elementName", o.getElement().getBaseElement().getAttributeValue("name"));
			jsonOperation.addProperty("fieldType", o.getFieldType().getDescription());
			jsonOperation.addProperty("elementChapter", o.getChapter().toString());
			return jsonOperation;
		};
		obj.add("read", processVariable.getReads().stream().map(processVariableToJson).collect(JsonArray::new,
				JsonArray::add, JsonArray::addAll));
		obj.add("write", processVariable.getWrites().stream().map(processVariableToJson).collect(JsonArray::new,
				JsonArray::add, JsonArray::addAll));
		obj.add("delete", processVariable.getDeletes().stream().map(processVariableToJson).collect(JsonArray::new,
				JsonArray::add, JsonArray::addAll));
		return obj;
	}

	/**
	 * Check all checkers for successful verification
	 *
	 * @param issues
	 *            list of all issues
	 * @return list with checkers without issues
	 */
	private Collection<CheckerIssue> getNoIssues(final Collection<CheckerIssue> issues) {
		Collection<CheckerIssue> newIssues = new ArrayList<>();

		for (final String bpmnFilename : getModelPaths()) {
			Collection<CheckerIssue> modelIssues = new ArrayList<>(issues);

			for (CheckerIssue issue : issues) {
				String prettyBpmnFilename = replace("\\", issue.getBpmnFile());
				if (!prettyBpmnFilename.equals(ConfigConstants.JAVA_BASE_PATH + bpmnFilename))
					modelIssues.remove(issue);
			}

			for (final String ruleName : RuntimeConfig.getInstance().getActiveRules()) {
				Collection<CheckerIssue> ruleIssues = new ArrayList<>(modelIssues);
				for (CheckerIssue issue : modelIssues) {
					if (!issue.getRuleName().equals(ruleName))
						ruleIssues.remove(issue);
				}
				if (ruleIssues.isEmpty())
					newIssues.add(new CheckerIssue(ruleName, null, CriticalityEnum.SUCCESS,
							(ConfigConstants.JAVA_BASE_PATH + bpmnFilename), null, "", "", null, null, null,
							"No issues found", null, null));
			}
		}

		return newIssues;
	}

	/**
	 * Transforms the path and filename to XML
	 *
	 * @return output
	 * @throws OutputWriterException
	 *             If content can not bet transformed to XML format
	 */
	private String transformToXMLDatastructure() throws OutputWriterException {
		StringBuilder output = new StringBuilder("var diagramXMLSource = [\n");

		try {
			for (final String bpmnFilename : getModelPaths()) {
				String prettyBpmnFileName = replace("\\\\", bpmnFilename);
				output.append("{\"name\":\"").append(prettyBpmnFileName).append("\",\n \"xml\": \"");
				output.append(convertBpmnFile(RuntimeConfig.getInstance().getBasepath() + bpmnFilename));
				output.append("\"},\n");
			}
		} catch (IOException e) {
			throw new OutputWriterException("bpmnFile not found");
		}
		return output + "];\n";
	}

	/**
	 * Replaces FileSeparator with given char sequence
	 *
	 * @param replace
	 *            Chars to replace searched string
	 * @param str
	 *            String to be cleaned
	 * @return str Cleaned String
	 */
	private static String replace(String replace, String str) {
		int start = str.indexOf(File.separator);

		while (start != -1) {
			str = str.substring(0, start) + replace + str.substring(start + File.separator.length());
			start = str.indexOf(File.separator, start + replace.length());
		}
		return (str);
	}

	/**
	 * Cleans bad unicode chars in a string
	 *
	 * @param path
	 *            Path to file
	 * @return s Cleaned string
	 * @throws IOException
	 *             File not found
	 */
	private String convertBpmnFile(String path) throws IOException {
		byte[] encoded = null;
		if (path.startsWith("file:/")) {
			try {
				// Convert URI
				encoded = Files.readAllBytes(Paths.get(new URI(path)));
			} catch (URISyntaxException e) {
				logger.log(Level.SEVERE, "URI of path seems to be malformed.", e);
			}
		} else {
			encoded = Files.readAllBytes(Paths.get(path));
		}

		String s = new String(encoded);
		s = s.replace("\"", "\\\""); // replace " with \"
		s = s.replace('\n', ' '); // delete all \n
		s = s.replace('\r', ' '); // delete all \r
		s = s.replaceAll(">\\u0020*<", "><");
		s = s.replaceAll(">\\u0027*<", "><");
		return s;
	}

	/**
	 * Transforms the collection of issues into JSON format
	 *
	 * @param issues
	 *            Collection of found issues
	 * @param varName
	 *            Variable Name
	 * @return Collection of issues in JSON format
	 */
	private String transformToJsonDatastructure(final Collection<CheckerIssue> issues, String varName) {
		final JsonArray jsonIssues = new JsonArray();
		if (issues != null && issues.size() > 0) {
			for (final CheckerIssue issue : issues) {
				final JsonObject obj = new JsonObject();
				obj.addProperty(BpmnConstants.VPAV_ID, issue.getId());
				obj.addProperty(BpmnConstants.VPAV_BPMN_FILE, replace("\\", issue.getBpmnFile()));
				obj.addProperty(BpmnConstants.VPAV_RULE_NAME, issue.getRuleName());
				obj.addProperty(BpmnConstants.VPAV_RULE_DESCRIPTION, issue.getRuleDescription());
				obj.addProperty(BpmnConstants.VPAV_ELEMENT_ID, issue.getElementId());
				obj.addProperty(BpmnConstants.VPAV_ELEMENT_NAME, issue.getElementName());
				obj.addProperty(BpmnConstants.VPAV_CLASSIFICATION, issue.getClassification().name());
				obj.addProperty(BpmnConstants.VPAV_RESOURCE_FILE, issue.getResourceFile());
				obj.addProperty(BpmnConstants.VPAV_VARIABLE, issue.getVariable());
				obj.addProperty(BpmnConstants.VPAV_ANOMALY,
						issue.getAnomaly() == null ? null : issue.getAnomaly().getDescription());
				final JsonArray jsonPaths = new JsonArray();
				final List<Path> paths = issue.getInvalidPaths();
				if (paths != null && paths.size() > 0) {
					for (final Path path : paths) {
						final JsonArray jsonPath = new JsonArray();
						final List<BpmnElement> elements = path.getElements();
						for (BpmnElement element : elements) {
							final JsonObject jsonElement = new JsonObject();
							final String id = element.getBaseElement().getId();
							final String name = element.getBaseElement().getAttributeValue(BpmnConstants.ATTR_NAME);
							jsonElement.addProperty(BpmnConstants.VPAV_ELEMENT_ID, id);
							jsonElement.addProperty(BpmnConstants.VPAV_ELEMENT_NAME,
									name == null ? null : name.replaceAll("\n", ""));
							jsonPath.add(jsonElement);
						}
						jsonPaths.add(jsonPath);
					}
				}

				// Add more information regarding the implementation if given
				if (issue.getImplementationDetails() != null) {
					obj.addProperty(BpmnConstants.VPAV_IMPLEMENTATION_DETAILS, issue.getImplementationDetails());
				}

				obj.add(BpmnConstants.VPAV_PATHS, jsonPaths);
				obj.addProperty(BpmnConstants.VPAV_MESSAGE, issue.getMessage());
				obj.addProperty(BpmnConstants.VPAV_ELEMENT_DESCRIPTION, issue.getElementDescription());
				jsonIssues.add(obj);
			}
		}
		return transformJsonToJs(varName, jsonIssues);
	}

	/**
	 * Transforms the properties into JSON format
	 * @return Properties in JSON format
	 */
	private String transformPropertiesToJsonDatastructure() {
		final JsonObject obj = new JsonObject();
		String basePath = RuntimeConfig.getInstance().getBasepath();
		String absolutePath = "";

		if (basePath.startsWith("file:/")) {
			try {
				// Convert URI
				absolutePath = basePath;
				basePath = Paths.get(new URI(basePath)).toAbsolutePath().toString() + "/";
			} catch (URISyntaxException e) {
				logger.log(Level.SEVERE, "URI of path seems to be malformed.", e);
			}
		} else {
			// Create download basepath
			absolutePath = "file:///" + new File(basePath).getAbsolutePath() + "/";
		}
		obj.addProperty("basepath", basePath.replaceAll("/", "\\\\"));

		obj.addProperty("downloadBasepath", absolutePath);

		obj.addProperty("projectName", RuntimeConfig.getInstance().getProjectName());
		return transformJsonToJs("properties", obj);
	}

	private String transformSummaryToJson(Collection<CheckerIssue> issues) {
		final String projectName = "projectName";
		final String warnings = "warnings";
		final String errors = "errors";
		final String ignoredIssues = "ignoredIssues";
		final String flawedElements = "flawedElements";
		final JsonObject projectSummary = new JsonObject();

		//Total statics for project
		projectSummary.addProperty(projectName, RuntimeConfig.getInstance().getProjectName());
		final Long warningsTotal = issues.stream()
				.filter(issue -> issue.getClassification().equals(CriticalityEnum.WARNING)).count();
		projectSummary.addProperty(warnings, warningsTotal);
		final Long errorsTotal = issues.stream()
				.filter(issue -> issue.getClassification().equals(CriticalityEnum.ERROR)).count();
		projectSummary.addProperty(errors, errorsTotal);
		final Integer ignoredIssuesTotal = getIgnoredIssuesMap().size();
		projectSummary.addProperty(ignoredIssues, ignoredIssuesTotal);
		final Long flawedElementsTotal = issues.stream()
				.map(CheckerIssue::getElementId).distinct().count();
		projectSummary.addProperty(flawedElements, flawedElementsTotal);

		//Statistics per BPMN model
		final JsonArray modelsStats = new JsonArray();
		final List<String> modelsList = issues.stream()
				.map(CheckerIssue::getBpmnFile).distinct()
				.collect(Collectors.toList());
		modelsList.forEach(model -> {
			final JsonObject modelObject = new JsonObject();
			modelObject.addProperty(projectName, RuntimeConfig.getInstance().getProjectName());
			modelObject.addProperty("modelName", model);
			final Long warningsModelCount = issues.stream()
					.filter(issue -> issue.getBpmnFile().equals(model) &&
							issue.getClassification().equals(CriticalityEnum.WARNING))
					.count();
			modelObject.addProperty(warnings, warningsModelCount);
			final Long errorsModelCount = issues.stream()
					.filter(issue -> issue.getBpmnFile().equals(model) &&
							issue.getClassification().equals(CriticalityEnum.ERROR))
					.count();
			modelObject.addProperty(errors, errorsModelCount);
			final Long issuesModelCount = IssueService.getInstance().getIssues()
					.stream() //Retrieve the unfiltered issue collection
					.filter(issue -> issue.getBpmnFile().equals(model))
					.filter(issue -> getIgnoredIssuesMap().containsKey(issue.getId()))
					.distinct() //Some types of issues can be multiple times in the collection
					.count();
			modelObject.addProperty(ignoredIssues, issuesModelCount);
			final Long flawedElementsModelCount = issues.stream()
					.filter(issue -> issue.getBpmnFile().equals(model))
					.map(CheckerIssue::getElementId)
					.distinct() //The same element can have multiple issues
					.count();
			modelObject.addProperty(flawedElements, flawedElementsModelCount);

			modelsStats.add(modelObject);
		});
		projectSummary.add("models", modelsStats);
		return transformJsonToJs("projectSummary", projectSummary);
	}

	/**
	 * Transforms the collection of wrong checkers into JSON format
	 *
	 * @param wrongCheckers Map of wrongly configured checkers
	 * @return JavaScript variables containing the wrong checkers
	 */
	private String transformToJsDatastructure(final Map<String, String> wrongCheckers) {
		final JsonArray jsonIssues = new JsonArray();
		if (wrongCheckers != null && wrongCheckers.size() > 0) {
			for (Map.Entry<String, String> entry : wrongCheckers.entrySet()) {
				final JsonObject obj = new JsonObject();
				obj.addProperty(ConfigConstants.RULE_NAME, entry.getKey());
				obj.addProperty(ConfigConstants.MESSAGE, entry.getValue());
				jsonIssues.add(obj);
			}
		}
		return transformJsonToJs("unlocatedCheckers", jsonIssues);
	}

	/**
	 * Transforms the collection of issue severities into JSON format
	 *
	 * @param issues
	 *            Map of collected issues
	 * @return JavaScript variables containing the issues' id and severity
	 */
	private String transformSeverityToJsDatastructure(final Map<String, CriticalityEnum> issues) {
		final JsonArray jsonIssues = new JsonArray();
		if (issues != null && issues.size() > 0) {
			for (Map.Entry<String, CriticalityEnum> entry : issues.entrySet()) {
				final JsonObject obj = new JsonObject();
				obj.addProperty(BpmnConstants.ATTR_ID, entry.getKey());
				obj.addProperty(ConfigConstants.CRITICALITY, entry.getValue().name());
				jsonIssues.add(obj);
			}
		}
		return transformJsonToJs("issueSeverity", jsonIssues);
	}

	/**
	 * Transforms the map of ignored issues into JSON format
	 *
	 * @param ignoredIssues Map of issues to be ignored
	 * @return JavaScript variables containing the issues to be ignored
	 */
	private String transformIgnoredIssuesToJsDatastructure(final Map<String, String> ignoredIssues) {
		final JsonArray ignoredIssesJson = new JsonArray();

		if (ignoredIssues != null && ignoredIssues.size() > 0) {
			for (Map.Entry<String, String> entry : ignoredIssues.entrySet()) {
				final JsonObject obj = new JsonObject();
				obj.addProperty("ID", entry.getKey());
				obj.addProperty("Comment", entry.getValue());
				ignoredIssesJson.add(obj);
			}
		}
		return transformJsonToJs("ignoredIssues", ignoredIssesJson);
	}

	/**
	 * @param defaultCheckers ArrayList of default vPAV checkers
	 * @return JavaScript variables containing the default checkers
	 */
	private String transformDefaultRulesToJsDatastructure(final ArrayList<String> defaultCheckers) {
		final JsonArray jsonIssues = new JsonArray();
		if (defaultCheckers != null && defaultCheckers.size() > 0) {
			for (String entry : defaultCheckers) {
				final JsonObject obj = new JsonObject();
				obj.addProperty(ConfigConstants.RULE_NAME, entry);
				jsonIssues.add(obj);
			}
		}
		return transformJsonToJs("defaultCheckers", jsonIssues);
	}

	private String transformJsonToJs(String jsIdentifier, JsonElement json) {
		return ("var " + jsIdentifier + " = " +
				JsonOutputWriter.getJsonString(json) + ";");
	}

	/**
	 * Generates the JS file containing the relative paths of the external vPAV reports
	 *
	 * @param externalReportsPaths List of the relative paths
	 */
	public void writeGeneratedReportsData(List<String> externalReportsPaths) {
		JsonObject obj = new JsonObject();
		obj.addProperty("isMultiProjectScan", RuntimeConfig.getInstance().isMultiProjectScan().toString());
		JsonArray array = new JsonArray();
		externalReportsPaths.forEach(array::add);
		obj.add("reportsPaths", array);
		File reportsPathsFile = new File(RuntimeConfig.getInstance().getExternalReportsFolder() +
				ConfigConstants.VALIDATION_OVERVIEW_REPORT_DATA_JS);
		String reportData = transformJsonToJs("reportData", obj);
		try {
			FileUtils.write(reportsPathsFile, reportData, (Charset) null);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't write external reports paths JS");
		}
	}

	public Map<String, String> getIgnoredIssuesMap() {
		return ignoredIssuesMap;
	}

	public void setIgnoredIssuesMap(Map<String, String> ignoredIssuesMap) {
		this.ignoredIssuesMap = ignoredIssuesMap;
	}

	public Map<String, String> getWrongCheckersMap() {
		return wrongCheckersMap;
	}

	public void setWrongCheckersMap(Map<String, String> wrongCheckersMap) {
		this.wrongCheckersMap = wrongCheckersMap;
	}

	public Set<String> getModelPaths() {
		return modelPaths;
	}

	public void setModelPaths(Set<String> modelPaths) {
		this.modelPaths = modelPaths;
	}

}
