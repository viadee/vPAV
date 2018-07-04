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
package de.viadee.bpm.vPAV.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.*;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

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
                extractExternalCheckers(
                        RuntimeConfig.getInstance().getActiveRules()));
        final String issueSeverity = transformSeverityToJsDatastructure(createIssueSeverity(issues));
        final String ignoredIssues = transformIgnoredIssuesToJsDatastructure(getIgnoredIssuesMap());

        writeJS(json, json_noIssues, bpmn, wrongCheckers, defaultCheckers, issueSeverity, ignoredIssues);
    }

    public void prepareMaps(final Map<String, String> wrongCheckers, final Map<String, String> ignoredIssues, final Set<String> modelPath) {
    	this.setWrongCheckersMap(wrongCheckers);
    	this.setIgnoredIssuesMap(ignoredIssues);
    	this.setModelPaths(modelPath);
    }


	/**
     * Creates list which contains elements with multiple issues and the marks it with highest severity
     * 
     * @param issues
     */
    private Map<String, CriticalityEnum> createIssueSeverity(final Collection<CheckerIssue> issues) {
        Map<String, CriticalityEnum> issueSeverity = new HashMap<String, CriticalityEnum>();
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
     * @return
     */
    private ArrayList<String> extractExternalCheckers(final ArrayList<String> activeRules) {
        final ArrayList<String> defaultRules = new ArrayList<String>();
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
     * @param json_noIssues
     * @param bpmn
     * @throws OutputWriterException
     */
    private void writeJS(final String json, final String json_noIssues, final String bpmn, final String wrongCheckers,
            final String defaultCheckers, final String issueSeverity, final String ignoredIssues)
            throws OutputWriterException {
        if (json != null && !json.isEmpty()) {
            try {
                final FileWriter file = new FileWriter(ConfigConstants.VALIDATION_JS_MODEL_OUTPUT);
                file.write(bpmn);
                file.close();

                final OutputStreamWriter osWriter = new OutputStreamWriter(
                        new FileOutputStream(ConfigConstants.VALIDATION_JS_OUTPUT), StandardCharsets.UTF_8);
                osWriter.write(json);
                osWriter.close();

                final OutputStreamWriter osWriterSuccess = new OutputStreamWriter(
                        new FileOutputStream(ConfigConstants.VALIDATION_JS_SUCCESS_OUTPUT), StandardCharsets.UTF_8);
                osWriterSuccess.write(json_noIssues);
                osWriterSuccess.close();

                if ((wrongCheckers != null && !wrongCheckers.isEmpty())
                        && (defaultCheckers != null && !defaultCheckers.isEmpty())) {
                    final OutputStreamWriter wrongAndDefaultCheckers = new OutputStreamWriter(
                            new FileOutputStream(ConfigConstants.VALIDATION_CHECKERS), StandardCharsets.UTF_8);
                    wrongAndDefaultCheckers.write(wrongCheckers);
                    wrongAndDefaultCheckers.write(defaultCheckers);
                    wrongAndDefaultCheckers.close();
                } else if ((wrongCheckers == null || wrongCheckers.isEmpty())
                        && (defaultCheckers != null && !defaultCheckers.isEmpty())) {
                    final OutputStreamWriter defaultCheckerJS = new OutputStreamWriter(
                            new FileOutputStream(ConfigConstants.VALIDATION_CHECKERS), StandardCharsets.UTF_8);
                    defaultCheckerJS.write(defaultCheckers);
                    defaultCheckerJS.close();
                }

                if (issueSeverity != null && !issueSeverity.isEmpty()) {
                    final OutputStreamWriter issueSeverityWriter = new OutputStreamWriter(
                            new FileOutputStream(ConfigConstants.VALIDATION_ISSUE_SEVERITY), StandardCharsets.UTF_8);
                    issueSeverityWriter.write(issueSeverity);
                    issueSeverityWriter.close();
                }
                if (ignoredIssues != null && !ignoredIssues.isEmpty()) {
                    final OutputStreamWriter ignoredIssuesWriter = new OutputStreamWriter(
                            new FileOutputStream(ConfigConstants.VALIDATION_IGNORED_ISSUES_OUTPUT),
                            StandardCharsets.UTF_8);
                    ignoredIssuesWriter.write(ignoredIssues);
                    ignoredIssuesWriter.close();
                }

            } catch (final IOException ex) {
                throw new OutputWriterException("js output couldn't be written", ex);
            }
        }
    }

    /**
     * write javascript file with elements which have variables
     *
     * @param elements
     *            Collection of BPMN elements across all models
     * @throws OutputWriterException
     *             javascript couldnt be written
     */
    public void writeVars(Collection<BpmnElement> elements, Collection<ProcessVariable> processVariables) throws OutputWriterException {
        try {
            FileWriter writer = new FileWriter(ConfigConstants.VALIDATION_JS_PROCESSVARIABLES, true);

            // write elements containing operations
            StringBuilder jsFile = new StringBuilder();
            jsFile.append("var proz_vars = [\n")
                    .append(elements.stream()
                            .filter(e -> !e.getProcessVariables().isEmpty())
                            .map(JsOutputWriter::transformElementToJsonIncludingProcessVariables)
                            .collect(Collectors.joining(",\n\n")))
                    .append("];\n\n");



            JsonArray jsonIssues = processVariables.stream()
                    .map(JsOutputWriter::transformProcessVariablesToJson)
                    .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
            jsFile.append("var processVariables = ")
                    .append(new GsonBuilder().setPrettyPrinting().create().toJson(jsonIssues))
                    .append(";");

            writer.write(jsFile.toString());
            writer.close();
        } catch (IOException e) {
            logger.warning("Processvariables couldn't be written");
        }
    }

    private static String transformElementToJsonIncludingProcessVariables(BpmnElement element) {
        String elementString = "";
        String read = "";
        String write = "";
        String delete = "";
        if (!element.getProcessVariables().isEmpty()) {
            // elementID
            elementString += "{\n\"elementId\" : \"" + element.getBaseElement().getId() + "\",\n";
            // bpmnFile
            elementString += "\"bpmnFile\" : \"" + replace(File.separator, "\\\\", element.getProcessdefinition())
                    + "\",\n";
            // element Name
            if (element.getBaseElement().getAttributeValue("name") != null)
                elementString += "\"elementName\" : \""
                        + element.getBaseElement().getAttributeValue("name").trim().replace('\n', ' ') + "\",\n";

            for (Map.Entry<String, ProcessVariableOperation> entry : element.getProcessVariables().entrySet()) {
                entry.getValue().getOperation();
                if (entry.getValue().getOperation().equals(VariableOperation.READ))
                    read += "\"" + entry.getValue().getName() + "\",";
                if (entry.getValue().getOperation().equals(VariableOperation.WRITE))
                    write += "\"" + entry.getValue().getName() + "\",";
                if (entry.getValue().getOperation().equals(VariableOperation.DELETE))
                    delete += "\"" + entry.getValue().getName() + "\",";
            }
            // red
            elementString += "\"read\" : [" + (read.length() > 1 ? read.substring(0, read.length() - 1) : read)
                    + "],\n";
            // write
            elementString += "\"write\" : [" + (write.length() > 1 ? write.substring(0, write.length() - 1) : write)
                    + "],\n";
            // delete
            elementString += "\"delete\" : ["
                    + (delete.length() > 1 ? delete.substring(0, delete.length() - 1) : delete) + "]\n";
            // end
            elementString += "}";
        }
        return elementString;
    }

    /**
     * Transforms a process variable to a json object
     *
     * @param processVariable
     * @return
     */
    private static JsonObject transformProcessVariablesToJson(final ProcessVariable processVariable) {
        final JsonObject obj = new JsonObject();
        obj.addProperty("name", processVariable.getName());
        if (processVariable.getOperations().size() > 0) {
            String bpmnFile = processVariable.getOperations().get(0).getElement().getProcessdefinition();
            obj.addProperty(BpmnConstants.VPAV_BPMN_FILE, replace(File.separator, "\\", bpmnFile));
        }
        obj.add("read", processVariable.getReads().stream().map(o -> {
            final JsonObject jsonOperation = new JsonObject();
            jsonOperation.addProperty("elementId", o.getElement().getBaseElement().getId());
            jsonOperation.addProperty("elementName", o.getElement().getBaseElement().getAttributeValue("name"));
            return jsonOperation;
        }).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        obj.add("write", processVariable.getWrites().stream().map(o -> {
            final JsonObject jsonOperation = new JsonObject();
            jsonOperation.addProperty("elementId", o.getElement().getBaseElement().getId());
            jsonOperation.addProperty("elementName", o.getElement().getBaseElement().getAttributeValue("name"));
            return jsonOperation;
        }).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        obj.add("delete", processVariable.getDeletes().stream().map(o -> {
            final JsonObject jsonOperation = new JsonObject();
            jsonOperation.addProperty("elementId", o.getElement().getBaseElement().getId());
            jsonOperation.addProperty("elementName", o.getElement().getBaseElement().getAttributeValue("name"));
            return jsonOperation;
        }).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
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
        Collection<CheckerIssue> newIssues = new ArrayList<CheckerIssue>();

        for (final String bpmnFilename : getModelPaths()) {
            Collection<CheckerIssue> modelIssues = new ArrayList<CheckerIssue>();
            modelIssues.addAll(issues);

            for (CheckerIssue issue : issues) {
                String prettyBpmnFilename = replace(File.separator, "\\", issue.getBpmnFile());
                if (!prettyBpmnFilename.equals(ConfigConstants.JS_BASEPATH + bpmnFilename))
                    modelIssues.remove(issue);
            }

            for (final String ruleName : RuntimeConfig.getInstance().getActiveRules()) {
                Collection<CheckerIssue> ruleIssues = new ArrayList<CheckerIssue>();
                ruleIssues.addAll(modelIssues);
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
     */
    private String transformToXMLDatastructure() throws OutputWriterException {
        String output = "var diagramXMLSource = [\n";

        try {
            for (final String bpmnFilename : getModelPaths()) {
                String prettyBpmnFileName = replace(File.separator, "\\\\", bpmnFilename);
                output += "{\"name\":\"" + prettyBpmnFileName + "\",\n \"xml\": \"";
                output += convertBpmnFile(ConfigConstants.BASEPATH + bpmnFilename);
                output += "\"},\n";
            }
        } catch (IOException e) {
            throw new OutputWriterException("bpmnFile not found");
        }
        return output + "];\n";
    }

    /**
     *
     * @param search
     * @param replace
     * @param str
     * @return str
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
     * @return s
     * @throws IOException
     */
    private String convertBpmnFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
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
     * @return
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
     * Transforms the collection of wrong checkers into JSON format
     *
     * @param issues
     * @return
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
     * @return
     */
    private String transformSeverityToJsDatastructure(final Map<String, CriticalityEnum> issues) {
        final String varName = "issueSeverity";
        final JsonArray jsonIssues = new JsonArray();
        if (issues != null && issues.size() > 0) {
            for (Map.Entry<String, CriticalityEnum> entry : issues.entrySet()) {
                final JsonObject obj = new JsonObject();
                obj.addProperty(BpmnConstants.ATTR_ID, entry.getKey());
                obj.addProperty(ConfigConstants.CRITICALITY, entry.getValue().name().toString());
                jsonIssues.add(obj);
            }
        }
        return ("var " + varName + " = " + new GsonBuilder().setPrettyPrinting().create().toJson(jsonIssues) + ";");
    }

    /**
     * Transforms the map of ignored issues into JSON format
     * 
     * @param ignoredIssues
     * @return
     */
    private String transformIgnoredIssuesToJsDatastructure(Map<String, String> ignoredIssues) {
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
     * @param wrongCheckers
     * @return
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
