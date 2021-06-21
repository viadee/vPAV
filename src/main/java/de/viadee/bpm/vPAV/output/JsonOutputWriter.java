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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.exceptions.OutputWriterException;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public class JsonOutputWriter implements IssueOutputWriter {

	/**
	 * Writes all collected issues to the vPAV output
	 *
	 * @param issues
	 *            Collection of CheckerIssues
	 */
	@Override
	public void write(final Collection<CheckerIssue> issues) throws OutputWriterException {
		final String json = transformToJsonDatastructure(issues);
		if (json != null && !json.isEmpty()) {

			try (OutputStreamWriter osWriter = new OutputStreamWriter(
					new FileOutputStream(RuntimeConfig.getInstance().getValidationJsonOutput()),
					StandardCharsets.UTF_8)) {
				osWriter.write(json);
			} catch (final IOException ex) {
				throw new OutputWriterException("json output couldn't be written");
			}
		}
	}

	/**
	 * Transforms the collected issues to a JSON-String
	 *
	 * @param issues
	 *            Collection of found issues
	 * @return jsonified string
	 */
	private static String transformToJsonDatastructure(final Collection<CheckerIssue> issues) {
		final JsonArray jsonIssues = new JsonArray();
		if (issues != null && !issues.isEmpty()) {
			for (final CheckerIssue issue : issues) {
				final JsonObject obj = new JsonObject();
				obj.addProperty(BpmnConstants.VPAV_ID, issue.getId());
				obj.addProperty(BpmnConstants.VPAV_BPMN_FILE, issue.getBpmnFile());
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
				if (paths != null && !paths.isEmpty()) {
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
				if(issue.getImplementationDetails() != null) {
					obj.addProperty(BpmnConstants.VPAV_IMPLEMENTATION_DETAILS, issue.getImplementationDetails());
				}

				obj.add(BpmnConstants.VPAV_PATHS, jsonPaths);
				obj.addProperty(BpmnConstants.VPAV_MESSAGE, issue.getMessage());
				obj.addProperty(BpmnConstants.VPAV_ELEMENT_DESCRIPTION, issue.getElementDescription());
				jsonIssues.add(obj);
			}
		}

		return getJsonString(jsonIssues);
	}

	public static String getJsonString(JsonElement json) {
		return new GsonBuilder().setPrettyPrinting().create().toJson(json);
	}

}
