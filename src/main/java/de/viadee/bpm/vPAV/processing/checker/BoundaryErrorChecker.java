/*
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
package de.viadee.bpm.vPAV.processing.checker;

import de.odysseus.el.tree.IdentifierNode;
import de.odysseus.el.tree.Tree;
import de.odysseus.el.tree.TreeBuilder;
import de.odysseus.el.tree.impl.Builder;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.Resource;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Error;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoundaryErrorChecker extends AbstractElementChecker {

    private static Logger logger = Logger.getLogger(BoundaryErrorChecker.class.getName());

    public BoundaryErrorChecker(final Rule rule) {
        super(rule);
    }

    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement bpmnElement = element.getBaseElement();

        // Grab only boundaryEvents
        if (bpmnElement instanceof BoundaryEvent) {
            BoundaryEvent boundaryEvent = (BoundaryEvent) bpmnElement;

            // Map<String, String> errorEventDef -> "errorRef" , "camunda:errorMessageVariable"
            ErrorEventDefinition errorEventDefinition = BpmnScanner.getErrorEventDefinition(boundaryEvent);

            // Check if boundaryEvent consists of an errorEventDefinition
            if (errorEventDefinition != null) {
                Activity activity = boundaryEvent.getAttachedTo();
                Map.Entry<String, String> implementation = BpmnScanner.getImplementation(activity);

                // TODO add test case for this path
                // No error has been referenced
                if (errorEventDefinition.getError() == null) {
                    final String errorCode = errorEventDefinition.getCamundaErrorCodeVariable();
                    if (errorCode == null || errorCode.isEmpty()) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("BoundaryErrorChecker.0"), //$NON-NLS-1$
                                        CheckName.checkName(bpmnElement))));
                    } else {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("BoundaryErrorChecker.1"), //$NON-NLS-1$
                                        CheckName.checkName(bpmnElement))));
                    }
                } else {
                    // Error reference could be resolved, retrieve errorDefinition
                    Error errorDef = errorEventDefinition.getError();

                    // No errorCode has been specified
                    if (errorDef == null || errorDef.getErrorCode().isEmpty()) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element,
                                String.format(Messages.getString("BoundaryErrorChecker.2"), //$NON-NLS-1$
                                        CheckName.checkName(bpmnElement))));

                    } else {
                        // Check the BeanMapping to resolve delegate expression
                        if (implementation.getKey().equals(BpmnConstants.CAMUNDA_DEXPRESSION)) {
                            checkBeanMapping(element, issues, bpmnElement,
                                    errorDef.getErrorCode(), implementation.getValue());

                            // Check the directly referenced class
                        } else if (implementation.getKey().equals(BpmnConstants.CAMUNDA_CLASS)) {
                            if (!readResourceFile(implementation.getValue(),
                                    errorDef.getErrorCode())) {
                                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                        String.format(
                                                Messages.getString("BoundaryErrorChecker.3"), //$NON-NLS-1$
                                                CheckName.checkName(bpmnElement), implementation.getValue())));

                            }
                        }
                    }

                    // No errorName has been specified
                    if (errorDef == null || errorDef.getName() == null || errorDef.getName().isEmpty()) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element,
                                String.format(Messages.getString("BoundaryErrorChecker.4"), //$NON-NLS-1$
                                        CheckName.checkName(bpmnElement))));
                    }

                    // No ErrorMessageVariable has been specified
                    if (errorEventDefinition.getCamundaErrorMessageVariable() == null
                            || errorEventDefinition.getCamundaErrorMessageVariable().isEmpty()) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element,
                                String.format(Messages.getString("BoundaryErrorChecker.5"), //$NON-NLS-1$
                                        CheckName.checkName(bpmnElement))));
                    }
                }
            }
        }
        return issues;
    }

    /**
     * In case a bean mapping exists, we check for validity of a bean, so the event can be mapped against the respective
     * task If the class or bean can be resolved, the ErrorCode gets validated
     *
     * @param element
     * @param issues
     * @param bpmnElement
     * @param errorDefEntry
     * @param implementationRef
     */
    private void checkBeanMapping(BpmnElement element, final Collection<CheckerIssue> issues,
            final BaseElement bpmnElement, final String errorDefEntry, final String implementationRef) {
        if (RuntimeConfig.getInstance().getBeanMapping() != null) {
            final TreeBuilder treeBuilder = new Builder();
            final Tree tree = treeBuilder.build(implementationRef);
            final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
            // if beanMapping ${...} reference
            if (identifierNodes.iterator().hasNext()) {
                for (final IdentifierNode node : identifierNodes) {
                    final String classFile = RuntimeConfig.getInstance().getBeanMapping()
                            .get(node.getName());
                    // correct beanmapping was found -> check if class exists
                    if (classFile != null && classFile.trim().length() > 0) {
                        if (checkClassFile(classFile)) {
                            if (!readResourceFile(classFile, errorDefEntry)) {
                                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                        String.format(
                                                Messages.getString("BoundaryErrorChecker.6"), //$NON-NLS-1$
                                                CheckName.checkName(bpmnElement), node.getName())));
                            }
                        } else {
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                    Messages.getString("BoundaryErrorChecker.7"))); //$NON-NLS-1$
                        }
                    } else {
                        // incorrect beanmapping
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, String.format(
                                Messages.getString("BoundaryErrorChecker.8"), //$NON-NLS-1$
                                implementationRef)));
                    }
                }
            }
        } else {
            if (!checkClassFile(implementationRef)) {
                issues.addAll(
                        IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(
                                        Messages.getString("BoundaryErrorChecker.9"), //$NON-NLS-1$
                                        implementationRef, CheckName.checkName(bpmnElement))));
            }
        }
    }

    /**
     * Reads a resource and retrieves content as String
     *
     * @param className
     * @param errorCode
     * @return boolean
     */
    private boolean readResourceFile(final String className, final String errorCode) {

        final String fileName = className.replaceAll("\\.", "/") + ".java"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        boolean matchingErrorCode = false;

        if (fileName != null && fileName.trim().length() > 0) {
            try {
                final DirectoryScanner scanner = new DirectoryScanner();

                if (RuntimeConfig.getInstance().isTest()) {
                    if (fileName.endsWith(".java")) //$NON-NLS-1$
                        scanner.setBasedir(ConfigConstants.TEST_JAVAPATH);
                    else
                        scanner.setBasedir(ConfigConstants.TEST_BASEPATH);
                } else {
                    if (fileName.endsWith(".java")) //$NON-NLS-1$
                        scanner.setBasedir(ConfigConstants.JAVAPATH);
                    else
                        scanner.setBasedir(ConfigConstants.getInstance().getBasepath());
                }

                Resource s = scanner.getResource(fileName);

                if (s.isExists()) {
                    InputStreamReader resource = new InputStreamReader(new FileInputStream(s.toString()));
                    final String methodBody = IOUtils.toString(resource);
                    return validateContent(methodBody, errorCode);
                } else {
                    logger.warning(
                            "Class " + fileName + " could not be read or does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            } catch (final IOException ex) {
                logger.warning("Resource '" + fileName + "' could not be read: " + ex
                        .getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return matchingErrorCode;
    }

    /**
     * Check methodBody for content and return true if a "throw new BpmnError.." declaration is found
     *
     * @param errorCode
     * @return boolean
     */
    private boolean validateContent(final String classBody, final String errorCode) {

        if (classBody != null && !classBody.isEmpty()) {
            String remainingClassBody = classBody;
            while (remainingClassBody.contains("throw new BpmnError")) {
                String bpmnErrorArguments = StringUtils
                        .substringBetween(remainingClassBody, "throw new BpmnError(", ");");
                String delErrorCode = null;

                if (bpmnErrorArguments.startsWith("\"")) {
                    // Let's assume a string is directly passed

                    delErrorCode = StringUtils
                            .substringBetween(bpmnErrorArguments, "\"", "\""); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    // Let's assume a object is passed
                    String objErrorCode = StringUtils.substringBefore(bpmnErrorArguments, ",");
                    // Check if string object is somewhere created in the class
                    // Support only final string objects
                    if (classBody.contains("final String " + objErrorCode + " =")) {
                        // Check the only assignment
                        Matcher m = Pattern.compile("final String " + objErrorCode + " = \".*\";")
                                .matcher(classBody);
                        // Assume that there is only one match
                        if (m.find()) {
                            bpmnErrorArguments = m.group();
                            delErrorCode = StringUtils.substringBetween(bpmnErrorArguments, "\"", "\"");
                        } else {
                            logger.warning(
                                    "The error code could not be parsed because the string declaration was not found.");
                        }
                    } else {
                        logger.warning(
                                "The error code could not be read because only string literals and final string objects (defined in the class) or "
                                        + "local string objects (defined in the method) are supported.");
                    }
                }
                if (errorCode.equals(delErrorCode)) {
                    return true;
                } else {
                    remainingClassBody = StringUtils.substringAfter(remainingClassBody, "throw new BpmnError");
                }
            }
        }
        return false;
    }

    /**
     * Check if class reference for a given element exists
     *
     * @param className
     * @return boolean
     */
    private boolean checkClassFile(final String className) {

        try {
            RuntimeConfig.getInstance().getClassLoader().loadClass(className);
        } catch (final ClassNotFoundException e) {
            return false;
        }
        return true;
    }

}
