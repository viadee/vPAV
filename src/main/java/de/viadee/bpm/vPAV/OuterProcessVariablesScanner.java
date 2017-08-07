/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. All advertising materials mentioning features or use of this software must display the following
 * acknowledgement: This product includes software developed by the viadee Unternehmensberatung GmbH. 4. Neither the
 * name of the viadee Unternehmensberatung GmbH nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;

import groovyjarjarasm.asm.ClassReader;
import groovyjarjarasm.asm.ClassVisitor;
import groovyjarjarasm.asm.FieldVisitor;
import groovyjarjarasm.asm.Opcodes;

/**
 * scan process variables, which are set in outer java classes
 *
 * important TODO: variables to bpmn element (message correlation)
 */
public class OuterProcessVariablesScanner {

    private Set<String> javaResources;

    private ClassLoader classLoader;

    private Map<String, Collection<String>> messageIdToVariableMap = new HashMap<String, Collection<String>>();

    private Map<String, Collection<String>> processIdToVariableMap = new HashMap<String, Collection<String>>();

    public OuterProcessVariablesScanner(final Set<String> javaResources,
            final ClassLoader classLoader) {
        this.javaResources = javaResources;
        this.classLoader = classLoader;
    }

    /**
     * scan variables
     *
     * @throws IOException
     */
    public void scanProcessVariables() throws IOException {
        for (final String filePath : javaResources) {
            if (!filePath.startsWith("javax")) {
                final String content = readResourceFile(filePath);
                if (content != null) {
                    final Collection<String> initialProcessVariablesInFilePath = readVariablesOfInnerClassInitialProcessVariables(
                            filePath);
                    if (!initialProcessVariablesInFilePath.isEmpty()) {
                        // if correlateMessage and startProcessInstanceByMessage called
                        // together in one class take the intersection to avoid duplicates
                        final Set<String> messageIds = new HashSet<String>();
                        messageIds.addAll(checkStartProcessByMessageIdPattern(content));
                        messageIds.addAll(checkCorrelateMessagePattern(content));
                        for (final String messageId : messageIds) {
                            if (messageIdToVariableMap.containsKey(messageId)) {
                                // if messageId is already set, create intersection of variables and overwrite map
                                // item
                                final Collection<String> existingProcessVariables = messageIdToVariableMap
                                        .get(messageId);
                                final List<String> intersectionProcessVariables = ListUtils.intersection(
                                        (List<String>) existingProcessVariables,
                                        (List<String>) initialProcessVariablesInFilePath);
                                messageIdToVariableMap.put(messageId, intersectionProcessVariables);
                            } else {
                                messageIdToVariableMap.put(messageId, initialProcessVariablesInFilePath);
                            }
                        }
                        final Collection<String> processIds = checkStartProcessByKeyPattern(content);
                        for (final String processId : processIds) {
                            processIdToVariableMap.put(processId, initialProcessVariablesInFilePath);
                        }
                    }
                }
            }
        }
    }

    /**
     * get mapping for message id
     *
     * @return
     */
    public Map<String, Collection<String>> getMessageIdToVariableMap() {
        return messageIdToVariableMap;
    }

    /**
     * get mapping for process id
     *
     * @return
     */
    public Map<String, Collection<String>> getProcessIdToVariableMap() {
        return processIdToVariableMap;
    }

    /**
     * read resource file
     *
     * @param fileName
     * @return
     */
    @SuppressWarnings("deprecation")
    private String readResourceFile(final String fileName) {
        String methodBody = "";
        if (fileName != null && fileName.trim().length() > 0) {
            final InputStream resource = classLoader.getResourceAsStream(fileName);
            if (resource != null) {
                try {
                    methodBody = IOUtils.toString(classLoader.getResourceAsStream(fileName));
                } catch (final IOException ex) {
                    throw new RuntimeException(
                            "resource '" + fileName + "' could not be read: " + ex.getMessage(), ex);
                }
            }
        }
        return methodBody;
    }

    /**
     * check pattern for startProcessInstanceByMessage
     *
     * @param code
     * @return message ids
     */
    private Collection<String> checkStartProcessByMessageIdPattern(final String code) {

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");

        // search locations where variables are read
        final Pattern pattern = Pattern.compile("\\.startProcessInstanceByMessage\\((\\w+),(.*)");
        final Matcher matcher = pattern.matcher(cleanedCode);

        final Collection<String> messageIds = new ArrayList<String>();
        while (matcher.find()) {
            final String match = matcher.group(1);
            messageIds.add(match);
        }

        return messageIds;
    }

    /**
     * check pattern for startProcessInstanceByKey
     *
     * @param code
     * @return process keys
     */
    private Collection<String> checkStartProcessByKeyPattern(final String code) {

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");

        // search locations where variables are read
        final Pattern pattern = Pattern.compile("\\.startProcessInstanceByKey\\((\\w+),(.*)");
        final Matcher matcher = pattern.matcher(cleanedCode);

        final Collection<String> processIds = new ArrayList<String>();
        while (matcher.find()) {
            final String match = matcher.group(1);
            processIds.add(match);
        }

        return processIds;
    }

    /**
     * check pattern for correlateMessage
     *
     * @param code
     * @return message ids
     */
    private Collection<String> checkCorrelateMessagePattern(final String code) {

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");

        // search locations where variables are read
        final Pattern pattern = Pattern.compile("\\.correlateMessage\\((\\w+),(.*)");
        final Matcher matcher = pattern.matcher(cleanedCode);

        final Collection<String> messageIds = new ArrayList<String>();
        while (matcher.find()) {
            final String match = matcher.group(1);
            messageIds.add(match);
        }

        return messageIds;
    }

    /**
     * For given filePath returns fields of inner class InitialProcessVariables. This class is used to initialize the
     * process
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    private Collection<String> readVariablesOfInnerClassInitialProcessVariables(final String filePath)
            throws IOException {

        final Collection<String> processVariables = new ArrayList<String>();

        if (filePath != null) {
            final String[] splittedFilePath = filePath.split("\\.");
            if (splittedFilePath.length > 0) {
                ClassVisitor cl = new ClassVisitor(Opcodes.ASM4) {

                    @Override
                    public FieldVisitor visitField(int access, String name, String desc, String signature,
                            Object value) {
                        if (!name.startsWith("this")) {
                            processVariables.add(name);
                        }
                        super.visitField(access, name, desc, signature, value);
                        return null;
                    }
                };
                InputStream in = classLoader
                        .getResourceAsStream(splittedFilePath[0] + "$InitialProcessVariables.class");
                if (in != null) {
                    ClassReader classReader = new ClassReader(in);
                    classReader.accept(cl, 0);
                }
            }
        }
        return processVariables;
    }
}
