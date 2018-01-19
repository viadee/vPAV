/**
 * Copyright © 2017, viadee Unternehmensberatung GmbH
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
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.viadee.bpm.vPAV.AbstractRunner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

/**
 * Create the JavaScript file for HTML-output; Needs: issues and bpmnFile names
 */
public class JsOutputWriter implements IssueOutputWriter {

    /**
     * Writes the output as JavaScript to the vPAV output folder
     */
    @Override
    public void write(final Collection<CheckerIssue> issues) throws OutputWriterException {
        final String json = transformToJsonDatastructure(issues, BpmnConstants.VPAV_ELEMENTS_TO_MARK);
        final String json_noIssues = transformToJsonDatastructure(getNoIssues(issues),
                BpmnConstants.VPAV_NO_ISSUES_ELEMENTS);
        final String bpmn = transformToXMLDatastructure();

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

            } catch (final IOException ex) {
                throw new OutputWriterException("js output couldn't be written");
            }
        }
    }

    /**
     * write javascript file with elements which have variables
     *
     * @param baseElements
     *            Collection of baseelements
     * @param graphBuilder
     *            graphBuilder
     * @param processdefinition
     *            bpmn file
     * @throws OutputWriterException
     *             javascript couldnt be written
     */
    public void writeVars(Collection<BaseElement> baseElements, ElementGraphBuilder graphBuilder,
            File processdefinition) throws OutputWriterException {
        String modelVariables = "";

        // add infos for specific processdefinition
        try {
            FileWriter writer = new FileWriter(ConfigConstants.VALIDATION_JS_TMP, true);
            for (final BaseElement baseElement : baseElements) {
                BpmnElement element = graphBuilder.getElement(baseElement.getId());
                if (element == null) {
                    // if element is not in the data flow graph, create it.
                    element = new BpmnElement(processdefinition.getPath(), baseElement);
                }
                modelVariables += (transformToString(element));
            }
            writer.write(modelVariables);
            writer.close();
        } catch (IOException e) {
            throw new OutputWriterException("js variables output couldn't be written");
        }
    }

    /**
     * Finish the javascript file for processvariables
     */
    public static void finish() {
        String jsFile = "var proz_vars = [\n";
        if (new File(ConfigConstants.VALIDATION_JS_TMP).exists()) {
            try {
                // add file content
                byte[] encoded = Files.readAllBytes(Paths.get(ConfigConstants.VALIDATION_JS_TMP));
                jsFile += new String(encoded, "UTF-8");

                // remove last ','
                jsFile = (jsFile.length() > 1 ? jsFile.substring(0, jsFile.lastIndexOf(','))
                        : jsFile);

                // add end '];'
                jsFile += "];";

                // delete files
                new File(ConfigConstants.VALIDATION_JS_TMP).delete();
                if (new File(ConfigConstants.VALIDATION_JS_PROCESSVARIABLES).exists())
                    new File(ConfigConstants.VALIDATION_JS_PROCESSVARIABLES).delete();

                FileWriter writer = new FileWriter(ConfigConstants.VALIDATION_JS_PROCESSVARIABLES, false);
                // write file to target
                writer.write(jsFile);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String transformToString(BpmnElement element) {
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
                        + element.getBaseElement().getAttributeValue("name").trim().replace('\n', ' ')
                        + "\",\n";

            for (Map.Entry<String, ProcessVariable> entry : element.getProcessVariables().entrySet()) {
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
            elementString += "},\n\n";
        }
        return elementString;
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

        for (final String bpmnFilename : AbstractRunner.getModelPath()) {
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
                            (ConfigConstants.JS_BASEPATH + bpmnFilename), null,
                            "", "", null, null, null, "No issues found", null));
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
            for (final String bpmnFilename : AbstractRunner.getModelPath()) {
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
    private static String transformToJsonDatastructure(final Collection<CheckerIssue> issues, String varName) {
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
}
