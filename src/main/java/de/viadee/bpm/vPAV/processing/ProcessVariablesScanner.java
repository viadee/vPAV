/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung AG
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.Resource;

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

/**
 * scan process variables, which are set in outer java classes
 *
 */
public class ProcessVariablesScanner {

    private Set<String> javaResources;

    private Map<String, Collection<String>> messageIdToVariableMap = new HashMap<String, Collection<String>>();

    private Map<String, Collection<String>> processIdToVariableMap = new HashMap<String, Collection<String>>();
    
    private Set<String> initialProcessVariablesLocation = new HashSet<String>();
    
    private Set<String> processEntryPoints = new HashSet<String>();
    
    private Set<String> methodEntryPoints = new HashSet<String>();

	public ProcessVariablesScanner(final Set<String> javaResources) {
        this.javaResources = javaResources;
    }

    /**
     * scan variables
     *
     * @throws IOException
     *             possible exception if filepath can not be resolved
     */
    public void scanProcessVariables() throws IOException {
        for (final String filePath : javaResources) {
            if (!filePath.startsWith("javax")) { 
                final String content = readResourceFile(filePath);
                if (content != null) {
                    // if correlateMessage and startProcessInstanceByMessage called
                    // together in one class take the intersection to avoid duplicates
                    final Set<String> messageIds = new HashSet<String>();
                    messageIds.addAll(checkStartProcessByMessageIdPattern(content, filePath));
                    messageIds.addAll(checkStartProcessByMessageAndProcessDefinitionId(content, filePath));
                    messageIds.addAll(checkCorrelateMessagePattern(content, filePath));
                    for (final String messageId : messageIds) {
                        if (messageIdToVariableMap.containsKey(messageId)) {
                            // if messageId is already set, create intersection of variables and overwrite map
                            // item
                            final Collection<String> existingProcessVariables = messageIdToVariableMap
                                    .get(messageId);                                
//                            final List<String> intersectionProcessVariables = existingProcessVariables.stream()
//                                    .filter(initialProcessVariablesInFilePath::contains)
//                                    .collect(Collectors.toList());
                            
//                            messageIdToVariableMap.put(messageId, intersectionProcessVariables);
                        } else {
//                            messageIdToVariableMap.put(messageId, initialProcessVariablesInFilePath);
                        }
                    }
        
                    final Set<String> processIds = new HashSet<String>();
                    processIds.addAll(checkStartProcessByKeyPattern(content, filePath));
                    processIds.addAll(checkStartProcessByIdPattern(content, filePath));                      
                    
                    for (final String processId : processIds) {
//                        processIdToVariableMap.put(processId, initialProcessVariablesInFilePath);
                    }
                    
                    if (!messageIds.isEmpty() || !processIds.isEmpty()) {
                    	retrieveMethod(filePath);
                    }
                }
            }
        }
    }

    /**
     * 
     * @param filePath
     */
	private void retrieveMethod(final String filePath) {
		final String sootPath = FileScanner.getSootPath();
		System.setProperty("soot.class.path", sootPath);

		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);

		SootClass sootClass = Scene.v().forceResolve(cleanString(filePath, true), SootClass.SIGNATURES);

		if (sootClass != null) {
			sootClass.setApplicationClass();
			Scene.v().loadNecessaryClasses();
			for (SootMethod method : sootClass.getMethods()) {
				final Body body = method.retrieveActiveBody();
				for (String entryPoint : processEntryPoints) {
					if (body.toString().contains(entryPoint)) {
						methodEntryPoints.add(method.getName());
					}
				}
			}
		}
	}

	
	/**
	 * Strips unnecessary characters and returns cleaned name
	 * 
	 * @param className
	 *            Classname to be stripped of unused chars
	 * @return cleaned String
	 */
	private String cleanString(String className, boolean dot) {
		final String replaceDot = ".";
		final String replaceEmpty = "";
		final String replaceSingleBackSlash = "\\";
		final String replaceSingleForwardSlash = "/";
		final String replaceDotJava = ".java";

		if (dot) {
			if (System.getProperty("os.name").startsWith("Windows")) {
				className = className.replace(replaceSingleBackSlash, replaceDot).replace(replaceDotJava, replaceEmpty);
			} else {
				className = className.replace(replaceSingleForwardSlash, replaceDot).replace(replaceDotJava,
						replaceEmpty);
			}
		} else {
			if (System.getProperty("os.name").startsWith("Windows")) {
				className = className.replace(replaceDot, replaceSingleBackSlash);
				className = className.concat(replaceDotJava);
			} else {
				className = className.replace(replaceDot, replaceSingleForwardSlash);
				className = className.concat(replaceDotJava);
			}
		}
		return className;
	}
	
    /**
     * get list of processEntryPoints where initial process variables have been injected 
     * 
     * @return returns list of processEntryPoints
     */
    public Set<String> getProcessEntryPoints() {
		return processEntryPoints;
	}
    
    /**
     * get list of methodEntryPoints where initial process variables have been injected 
     * 
     * @return returns list of entryPoints
     */
    public Set<String> getMethodEntryPoints() {
		return methodEntryPoints;
	}
    
    /**
     * get list of locations where initial process variables have been found 
     * 
     * @return returns list of locations
     */
    public Collection<String> getInitialProcessVariablesLocation() {
		return initialProcessVariablesLocation;
	}


	/**
     * get mapping for message id
     *
     * @return messageIdToVariableMap returns messageIdToVariableMap
     */
    public Map<String, Collection<String>> getMessageIdToVariableMap() {
        return messageIdToVariableMap;
    }

    /**
     * get mapping for process id
     *
     * @return processIdToVariableMap returns processIdToVariableMap
     */
    public Map<String, Collection<String>> getProcessIdToVariableMap() {
        return processIdToVariableMap;
    }

    /**
     * read resource file
     *
     * @param filePath
     *            path of the file
     * @return methodBody returns methodBody
     */
    private String readResourceFile(final String filePath) {
        String methodBody = "";

        if (filePath != null && filePath.trim().length() > 0) {
            try {
                final DirectoryScanner scanner = new DirectoryScanner();

                if (RuntimeConfig.getInstance().isTest()) {
                    scanner.setBasedir(ConfigConstants.TEST_JAVAPATH);
                } else {
                    scanner.setBasedir(ConfigConstants.JAVAPATH);
                }

                Resource s = scanner.getResource(filePath);

                if (s.isExists()) {
                    InputStreamReader resource = new InputStreamReader(new FileInputStream(s.toString()));
                    methodBody = IOUtils.toString(resource);
                } else {
                	
                }
            } catch (final IOException ex) {
                throw new RuntimeException(
                        "resource '" + filePath + "' could not be read: " + ex.getMessage());
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
    private Collection<String> checkStartProcessByMessageIdPattern(final String code, final String filePath) {

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");

        // search locations where variables are read
        final Pattern pattern = Pattern.compile("\\.(startProcessInstanceByMessage)(\\()([a-z0-9_.]+)(,.?)*(\\n?)([a-z0-9_.()])*(,?)([a-z0-9_.()])*\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(cleanedCode);

        final Collection<String> messageIds = new ArrayList<String>();
        while (matcher.find()) {
            final String match = matcher.group(3);
            messageIds.add(match);
            processEntryPoints.add("startProcessInstanceByMessage");
            initialProcessVariablesLocation.add(filePath);
        }

        return messageIds;
    }
    
    /**
     * check pattern for startProcessInstanceById
     *
     * @param code
     * @return message ids
     */
    private Collection<String> checkStartProcessByIdPattern(final String code, final String filePath) {

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");
        
        // search locations where variables are read
        final Pattern pattern = Pattern.compile("\\.(startProcessInstanceById)(\\()([a-z0-9_.]+)(,.?)*(\\n?)([a-z0-9_.()])*(,?)([a-z0-9_.()])*\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(cleanedCode);

        final Collection<String> processIds = new ArrayList<String>();
        while (matcher.find()) {
            final String match = matcher.group(3);
            processIds.add(match);
            processEntryPoints.add("startProcessInstanceById");
            initialProcessVariablesLocation.add(filePath);
        }

        return processIds;
    }
    
    
    /**
     * check pattern for startProcessInstanceByMessageAndProcessDefinitionId
     *
     * @param code
     * @return message ids
     */
    private Collection<String> checkStartProcessByMessageAndProcessDefinitionId(final String code, final String filePath) {

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");
        
        // search locations where variables are read
        final Pattern pattern = Pattern.compile("\\.(startProcessInstanceByMessageAndProcessDefinitionId)(\\()([a-z0-9_.]+)(,.?)*(\\n?)([a-z0-9_.()])*(,?)([a-z0-9_.()])*\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(cleanedCode);

        final Collection<String> messageAndProcessIds = new ArrayList<String>();
        while (matcher.find()) {
            final String match = matcher.group(3);
            messageAndProcessIds.add(match);
            processEntryPoints.add("startProcessInstanceByMessageAndProcessDefinitionId");
            initialProcessVariablesLocation.add(filePath);
        }

        return messageAndProcessIds;
    }

    /**
     * check pattern for startProcessInstanceByKey
     *
     * @param code
     * @return process keys
     */
    private Collection<String> checkStartProcessByKeyPattern(final String code, final String filePath) {

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");

        // search locations where variables are read
        final Pattern pattern = Pattern.compile("\\.(startProcessInstanceByKey)(\\()([a-z0-9_.]+)(,.?)*(\\n?)([a-z0-9_.()])*(,?)([a-z0-9_.()])*\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(cleanedCode);

        final Collection<String> processIds = new ArrayList<String>();
        while (matcher.find()) {
            final String match = matcher.group(3);
            processIds.add(match);
            processEntryPoints.add("startProcessInstanceByKey");
            initialProcessVariablesLocation.add(filePath);
        }

        return processIds;
    }

    /**
     * check pattern for correlateMessage
     *
     * @param code
     * @return message ids
     */
    private Collection<String> checkCorrelateMessagePattern(final String code, final String filePath) {

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
            initialProcessVariablesLocation.add(filePath);
        }

        return messageIds;
    }

}
