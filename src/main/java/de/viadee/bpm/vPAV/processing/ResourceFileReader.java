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
package de.viadee.bpm.vPAV.processing;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.Resource;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;

public class ResourceFileReader {

    public static final Logger LOGGER = Logger.getLogger(ResourceFileReader.class.getName());

    /**
     * Reads a resource file from class path
     *
     * @param fileName
     *            - Name of Java Delegate class
     * @param element
     *            - Bpmn element
     * @return variables - found Process Variables
     */
    public static Map<String, ProcessVariableOperation> readResourceFile(final String fileName,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String scopeId) {
        Map<String, ProcessVariableOperation> variables = new HashMap<String, ProcessVariableOperation>();
        if (fileName != null && fileName.trim().length() > 0) {
            try {
                final DirectoryScanner directoryScanner = new DirectoryScanner();

                if (RuntimeConfig.getInstance().isTest()) {
                    if (fileName.endsWith(".java"))
                        directoryScanner.setBasedir(ConfigConstants.TEST_JAVAPATH);
                    else
                        directoryScanner.setBasedir(ConfigConstants.TEST_BASEPATH);
                } else {
                    if (fileName.endsWith(".java"))
                        directoryScanner.setBasedir(ConfigConstants.JAVAPATH);
                    else
                        directoryScanner.setBasedir(ConfigConstants.BASEPATH);
                }

                Resource s = directoryScanner.getResource(fileName);

                if (s.isExists()) {

                    InputStreamReader resource = new InputStreamReader(new FileInputStream(s.toString()));

                    final String methodBody = IOUtils.toString(resource);
                    variables = searchProcessVariablesInCode(element, chapter, fieldType, fileName, scopeId,
                            methodBody);
                } else {
                    LOGGER.warning("Class " + fileName + " does not exist");
                }
            } catch (final IOException ex) {
                throw new RuntimeException("resource '" + fileName + "' could not be read: " + ex.getMessage());
            }

        }

        return variables;
    }

    /**
     * Examine java code for process variables
     *
     * @param element
     *            - Bpmn element
     * @param chapter
     *            - ElementChapter
     * @param fieldType
     *            - KnownElementFieldType
     * @param fileName
     *            - class name
     * @param scopeId
     *            - Scope
     * @param code
     *            - cleaned source code
     * @return - found Process Variables
     */
    public static Map<String, ProcessVariableOperation> searchProcessVariablesInCode(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final String fileName,
            final String scopeId, final String code) {

        final Map<String, ProcessVariableOperation> variables = new HashMap<String, ProcessVariableOperation>();
        variables.putAll(searchReadProcessVariablesInCode(element, chapter, fieldType, fileName, scopeId, code));
        variables.putAll(searchWrittenProcessVariablesInCode(element, chapter, fieldType, fileName, scopeId, code));
        variables.putAll(searchRemovedProcessVariablesInCode(element, chapter, fieldType, fileName, scopeId, code));

        return variables;
    }

    /**
     * Search read process variables
     *
     * @param element
     *            - Bpmn element
     * @param chapter
     *            - ElementChapter
     * @param fieldType
     *            - KnownElementFieldType
     * @param fileName
     *            - class name
     * @param scopeId
     *            - Scope
     * @param code
     *            - cleaned source code
     * @return - found Process Variables
     */
    public static Map<String, ProcessVariableOperation> searchReadProcessVariablesInCode(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final String fileName,
            final String scopeId, final String code) {

        final Map<String, ProcessVariableOperation> variables = new HashMap<String, ProcessVariableOperation>();

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String COMMENT_PATTERN = "//.*";
        final String IMPORT_PATTERN = "import .*";
        final String PACKAGE_PATTERN = "package .*";
        final String cleanedCode = code.replaceAll(COMMENT_PATTERN, "").replaceAll(IMPORT_PATTERN, "")
                .replaceAll(PACKAGE_PATTERN, "").replaceAll(FILTER_PATTERN, "");

        // search locations where variables are read
        final Pattern getVariablePatternRuntimeService = Pattern.compile("\\.getVariable\\((.*),(\\w+)\\)");
        final Matcher matcherRuntimeService = getVariablePatternRuntimeService.matcher(cleanedCode);

        while (matcherRuntimeService.find()) {
            final String match = matcherRuntimeService.group(2);
            variables.put(match, new ProcessVariableOperation(match, element, chapter, fieldType, fileName,
                    VariableOperation.READ, scopeId));
        }

        final Pattern getVariablePatternDelegateExecution = Pattern.compile("\\.getVariable\\((\\w+)\\)");
        final Matcher matcherDelegateExecution = getVariablePatternDelegateExecution.matcher(cleanedCode);

        while (matcherDelegateExecution.find()) {
            final String match = matcherDelegateExecution.group(1);
            variables.put(match, new ProcessVariableOperation(match, element, chapter, fieldType, fileName,
                    VariableOperation.READ, scopeId));
        }

        return variables;
    }

    /**
     * Search written process variables
     *
     * @param element
     *            - Bpmn element
     * @param chapter
     *            - ElementChapter
     * @param fieldType
     *            - KnownElementFieldType
     * @param fileName
     *            - Name of file
     * @param scopeId
     *            - Scope
     * @param code
     *            - cleaned code
     * @return
     */
    public static Map<String, ProcessVariableOperation> searchWrittenProcessVariablesInCode(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final String fileName,
            final String scopeId, final String code) {

        final Map<String, ProcessVariableOperation> variables = new HashMap<String, ProcessVariableOperation>();

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String COMMENT_PATTERN = "//.*";
        final String IMPORT_PATTERN = "import .*";
        final String PACKAGE_PATTERN = "package .*";
        final String cleanedCode = code.replaceAll(COMMENT_PATTERN, "").replaceAll(IMPORT_PATTERN, "")
                .replaceAll(PACKAGE_PATTERN, "").replaceAll(FILTER_PATTERN, "");

        // search locations where variables are written
        final Pattern setVariablePatternRuntimeService = Pattern.compile("\\.setVariable\\((.*),(\\w+),(.*)\\)");
        final Matcher matcherPatternRuntimeService = setVariablePatternRuntimeService.matcher(cleanedCode);
        while (matcherPatternRuntimeService.find()) {
            final String match = matcherPatternRuntimeService.group(2);
            variables.put(match, new ProcessVariableOperation(match, element, chapter, fieldType, fileName,
                    VariableOperation.WRITE, scopeId));
        }

        final Pattern setVariablePatternDelegateExecution = Pattern.compile("\\.setVariable\\((\\w+),(.*)\\)");
        final Matcher matcherPatternDelegateExecution = setVariablePatternDelegateExecution.matcher(cleanedCode);
        while (matcherPatternDelegateExecution.find()) {
            final String match = matcherPatternDelegateExecution.group(1);
            variables.put(match, new ProcessVariableOperation(match, element, chapter, fieldType, fileName,
                    VariableOperation.WRITE, scopeId));
        }

        return variables;
    }

    /**
     * Search removed process variables
     *
     * @param element
     *            - BpmnElement
     * @param chapter
     *            - ElementChapter
     * @param fieldType
     *            - KnownElementFieldType
     * @param fileName
     *            - Name of file
     * @param scopeId
     *            - Scope
     * @param code
     *            - cleaned source code
     * @return - found Process Variables
     */
    public static Map<String, ProcessVariableOperation> searchRemovedProcessVariablesInCode(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final String fileName,
            final String scopeId, final String code) {

        final Map<String, ProcessVariableOperation> variables = new HashMap<String, ProcessVariableOperation>();

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String COMMENT_PATTERN = "//.*";
        final String IMPORT_PATTERN = "import .*";
        final String PACKAGE_PATTERN = "package .*";
        final String cleanedCode = code.replaceAll(COMMENT_PATTERN, "").replaceAll(IMPORT_PATTERN, "")
                .replaceAll(PACKAGE_PATTERN, "").replaceAll(FILTER_PATTERN, "");

        // search locations where variables are removed
        final Pattern removeVariablePatternRuntimeService = Pattern.compile("\\.removeVariable\\((.*),(\\w+)\\)");
        final Matcher matcherRuntimeService = removeVariablePatternRuntimeService.matcher(cleanedCode);

        while (matcherRuntimeService.find()) {
            final String match = matcherRuntimeService.group(2);
            variables.put(match, new ProcessVariableOperation(match, element, chapter, fieldType, fileName,
                    VariableOperation.DELETE, scopeId));
        }

        final Pattern removeVariablePatternDelegateExecution = Pattern.compile("\\.removeVariable\\((\\w+)\\)");
        final Matcher matcherDelegateExecution = removeVariablePatternDelegateExecution.matcher(cleanedCode);

        while (matcherDelegateExecution.find()) {
            final String match = matcherDelegateExecution.group(1);
            variables.put(match, new ProcessVariableOperation(match, element, chapter, fieldType, fileName,
                    VariableOperation.DELETE, scopeId));
        }

        return variables;
    }

}
