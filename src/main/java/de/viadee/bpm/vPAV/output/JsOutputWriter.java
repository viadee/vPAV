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
package de.viadee.bpm.vPAV.output;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.*;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		final String wrongCheckers = transformToJsDatastructure(getWrongCheckersMap());
		final String defaultCheckers = transformDefaultRulesToJsDatastructure(
				extractExternalCheckers(RuntimeConfig.getInstance().getActiveRules()));
		final String issueSeverity = transformSeverityToJsDatastructure(createIssueSeverity(issues));
		final String ignoredIssues = transformIgnoredIssuesToJsDatastructure(getIgnoredIssuesMap());
		final String properties = transformPropertiesToJsonDatastructure();

		writeJS(json, json_noIssues, bpmn, wrongCheckers, defaultCheckers, issueSeverity, ignoredIssues, properties);
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
	 * @param issues
	 *            Collected issues
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
	 * @param activeRules
	 *            Active RuleSet
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
	 *
	 * @param json
	 *            Elements to be marked (jsonified)
	 * @param json_noIssues
	 *            No issues (jsonified)
	 * @param bpmn
	 *            BPMN Model
	 * @param wrongCheckers
	 *            Wrong Checkers
	 * @param defaultCheckers
	 *            Default Checkers
	 * @param issueSeverity
	 *            Issue severity
	 * @param ignoredIssues
	 *            Ignored issues
	 * @param properties
	 * 	          vPav properties
	 * @throws OutputWriterException
	 *             If JavaScript could not be written
	 */
	private void writeJS(final String json, final String json_noIssues, final String bpmn, final String wrongCheckers,
			final String defaultCheckers, final String issueSeverity, final String ignoredIssues, final String properties)
			throws OutputWriterException {
		if (json != null && !json.isEmpty() && properties != null && !properties.isEmpty()) {
			String errorMsg = "js output couldn't be written";

			try (FileWriter file = new FileWriter(ConfigConstants.VALIDATION_JS_MODEL_OUTPUT)) {
				file.write(bpmn);
			} catch (IOException e) {
				throw new OutputWriterException(errorMsg, e);
			}
			try (OutputStreamWriter osWriter = new OutputStreamWriter(
					new FileOutputStream(ConfigConstants.VALIDATION_JS_OUTPUT), StandardCharsets.UTF_8)) {
				osWriter.write(json);
			} catch (IOException e) {
				throw new OutputWriterException(errorMsg, e);
			}
			try (OutputStreamWriter osWriterSuccess = new OutputStreamWriter(
					new FileOutputStream(ConfigConstants.VALIDATION_JS_SUCCESS_OUTPUT), StandardCharsets.UTF_8)) {
				osWriterSuccess.write(json_noIssues);
			} catch (IOException e) {
				throw new OutputWriterException(errorMsg, e);
			}

			if ((wrongCheckers != null && !wrongCheckers.isEmpty())
					&& (defaultCheckers != null && !defaultCheckers.isEmpty())) {
				try (OutputStreamWriter wrongAndDefaultCheckers = new OutputStreamWriter(
						new FileOutputStream(ConfigConstants.VALIDATION_CHECKERS), StandardCharsets.UTF_8)) {
					wrongAndDefaultCheckers.write(wrongCheckers);
					wrongAndDefaultCheckers.write(defaultCheckers);
				} catch (IOException e) {
					throw new OutputWriterException(errorMsg, e);
				}
			} else if ((wrongCheckers == null || wrongCheckers.isEmpty())
					&& (defaultCheckers != null && !defaultCheckers.isEmpty())) {
				try (OutputStreamWriter defaultCheckerJS = new OutputStreamWriter(
						new FileOutputStream(ConfigConstants.VALIDATION_CHECKERS), StandardCharsets.UTF_8)) {
					defaultCheckerJS.write(defaultCheckers);
				} catch (IOException e) {
					throw new OutputWriterException(errorMsg, e);
				}
			}

			if (issueSeverity != null && !issueSeverity.isEmpty()) {
				try (OutputStreamWriter issueSeverityWriter = new OutputStreamWriter(
						new FileOutputStream(ConfigConstants.VALIDATION_ISSUE_SEVERITY), StandardCharsets.UTF_8)) {
					issueSeverityWriter.write(issueSeverity);
				} catch (IOException e) {
					throw new OutputWriterException(errorMsg, e);
				}
			}
			if (ignoredIssues != null && !ignoredIssues.isEmpty()) {
				try (OutputStreamWriter ignoredIssuesWriter = new OutputStreamWriter(
						new FileOutputStream(ConfigConstants.VALIDATION_IGNORED_ISSUES_OUTPUT),
						StandardCharsets.UTF_8)) {
					ignoredIssuesWriter.write(ignoredIssues);
				} catch (IOException e) {
					throw new OutputWriterException(errorMsg, e);
				}
			}
			try (OutputStreamWriter osWriter = new OutputStreamWriter(
					new FileOutputStream(ConfigConstants.PROPERTIES_JS_OUTPUT), StandardCharsets.UTF_8)) {
				osWriter.write(properties);
			} catch (IOException e) {
				throw new OutputWriterException(errorMsg, e);
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

		try (FileWriter writer = new FileWriter(ConfigConstants.VALIDATION_JS_PROCESSVARIABLES, true)) {

			// write elements containing operations
			JsonArray jsonElements = elements.stream()
					.map(JsOutputWriter::transformElementToJsonIncludingProcessVariables)
					.filter(o -> o.has("elementId")).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
			StringBuilder jsFile = new StringBuilder();
			jsFile.append("var proz_vars = ")
					.append(new GsonBuilder().setPrettyPrinting().create().toJson(jsonElements)).append(";\n\n");

			JsonArray jsonVariables = processVariables.stream().map(JsOutputWriter::transformProcessVariablesToJson)
					.collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
			jsFile.append("var processVariables = ")
					.append(new GsonBuilder().setPrettyPrinting().create().toJson(jsonVariables)).append(";");

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
					replace(File.separator, "\\", element.getProcessDefinition()));
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
			obj.addProperty(BpmnConstants.VPAV_BPMN_FILE, replace(File.separator, "\\", bpmnFile));
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
				String prettyBpmnFilename = replace(File.separator, "\\", issue.getBpmnFile());
				if (!prettyBpmnFilename.equals(ConfigConstants.JS_BASEPATH + bpmnFilename))
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
							(ConfigConstants.JS_BASEPATH + bpmnFilename), null, "", "", null, null, null,
							"No issues found", null));
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
				String prettyBpmnFileName = replace(File.separator, "\\\\", bpmnFilename);
				output.append("{\"name\":\"").append(prettyBpmnFileName).append("\",\n \"xml\": \"");
				output.append(convertBpmnFile(ConfigConstants.getInstance().getBasepath() + bpmnFilename));
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
	 * @param search
	 *            FileSeparator
	 * @param replace
	 *            Chars to replace searched string
	 * @param str
	 *            String to be cleaned
	 * @return str Cleaned String
	 */
	private static String replace(String search, String replace, String str) {
		int start = str.indexOf(search);

		while (start != -1) {
			str = str.substring(0, start) + replace + str.substring(start + search.length(), str.length());
			start = str.indexOf(search, start + replace.length());
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
				obj.addProperty(BpmnConstants.VPAV_BPMN_FILE, replace(File.separator, "\\", issue.getBpmnFile()));
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
				obj.add(BpmnConstants.VPAV_PATHS, jsonPaths);
				obj.addProperty(BpmnConstants.VPAV_MESSAGE, issue.getMessage());
				obj.addProperty(BpmnConstants.VPAV_ELEMENT_DESCRIPTION, issue.getElementDescription());
				jsonIssues.add(obj);
			}
		}
		return ("var " + varName + " = " + new GsonBuilder().setPrettyPrinting().create().toJson(jsonIssues) + ";");
	}

	/**
	 * Transforms the properties into JSON format
	 * @return Properties in JSON format
	 */
	private String transformPropertiesToJsonDatastructure() {
		final JsonObject obj = new JsonObject();
		String basePath = ConfigConstants.getInstance().getBasepath();
		String absolutePath = "";

		if (basePath.startsWith("file:/")) {
			try {
				// Convert URI
				absolutePath = basePath;
				basePath = Paths.get(new URI(basePath)).toAbsolutePath().toString() + "/";
			} catch (URISyntaxException e) {
				logger.log(Level.SEVERE, "URI of path seems to be malformed.", e);
			}
		}
		else {
			// Create download basepath
			absolutePath = "file:///" + new File(basePath).getAbsolutePath() + "/";
		}
		obj.addProperty("basepath", basePath.replaceAll("/", "\\\\"));

		obj.addProperty("downloadBasepath", absolutePath);

		return ("var properties = " + new GsonBuilder().setPrettyPrinting().create().toJson(obj) + ";");
	}

	/**
	 * Transforms the collection of wrong checkers into JSON format
	 *
	 * @param wrongCheckers
	 *            Map of wrongly configured checkers
	 * @return JavaScript variables containing the wrong checkers
	 */
	private String transformToJsDatastructure(final Map<String, String> wrongCheckers) {
		final String varName = "unlocatedCheckers";
		final JsonArray jsonIssues = new JsonArray();
		if (wrongCheckers != null && wrongCheckers.size() > 0) {
			for (Map.Entry<String, String> entry : wrongCheckers.entrySet()) {
				final JsonObject obj = new JsonObject();
				obj.addProperty(ConfigConstants.RULENAME, entry.getKey());
				obj.addProperty(ConfigConstants.MESSAGE, entry.getValue());
				jsonIssues.add(obj);
			}
		}
		return ("var " + varName + " = " + new GsonBuilder().setPrettyPrinting().create().toJson(jsonIssues) + ";");
	}

	/**
	 * Transforms the collection of issue severities into JSON format
	 *
	 * @param issues
	 *            Map of collected issues
	 * @return JavaScript variables containing the issues' id and severity
	 */
	private String transformSeverityToJsDatastructure(final Map<String, CriticalityEnum> issues) {
		final String varName = "issueSeverity";
		final JsonArray jsonIssues = new JsonArray();
		if (issues != null && issues.size() > 0) {
			for (Map.Entry<String, CriticalityEnum> entry : issues.entrySet()) {
				final JsonObject obj = new JsonObject();
				obj.addProperty(BpmnConstants.ATTR_ID, entry.getKey());
				obj.addProperty(ConfigConstants.CRITICALITY, entry.getValue().name());
				jsonIssues.add(obj);
			}
		}
		return ("var " + varName + " = " + new GsonBuilder().setPrettyPrinting().create().toJson(jsonIssues) + ";");
	}

	/**
	 * Transforms the map of ignored issues into JSON format
	 * 
	 * @param ignoredIssues
	 *            Map of issues to be ignored
	 * @return JavaScript variables containing the issues to be ignored
	 */
	private String transformIgnoredIssuesToJsDatastructure(final Map<String, String> ignoredIssues) {
		final String ignoredIssuesList = "ignoredIssues";
		final JsonArray ignoredIssesJson = new JsonArray();

		if (ignoredIssues != null && ignoredIssues.size() > 0) {
			for (Map.Entry<String, String> entry : ignoredIssues.entrySet()) {
				final JsonObject obj = new JsonObject();
				obj.addProperty("ID", entry.getKey());
				obj.addProperty("Comment", entry.getValue());
				ignoredIssesJson.add(obj);
			}
		}
		return ("var " + ignoredIssuesList + " = "
				+ new GsonBuilder().setPrettyPrinting().create().toJson(ignoredIssues) + ";");
	}

	/**
	 * 
	 * @param defaultCheckers
	 *            ArrayList of default vPAV checkers
	 * @return JavaScript variables containing the default checkers
	 */
	private String transformDefaultRulesToJsDatastructure(final ArrayList<String> defaultCheckers) {
		final String varName = "defaultCheckers";
		final JsonArray jsonIssues = new JsonArray();
		if (defaultCheckers != null && defaultCheckers.size() > 0) {
			for (String entry : defaultCheckers) {
				final JsonObject obj = new JsonObject();
				obj.addProperty(ConfigConstants.RULENAME, entry);
				jsonIssues.add(obj);
			}
		}
		return ("\n var " + varName + " = " + new GsonBuilder().setPrettyPrinting().create().toJson(jsonIssues) + ";");
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
