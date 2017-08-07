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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DirectoryScanner;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelException;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelException;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Decision;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.checker.VersioningChecker;

/**
 * scans maven project for files, which are necessary for the later analysis
 *
 */
public class FileScanner {

    private final Set<String> processdefinitions;

    private Set<String> javaResources = new HashSet<String>();

    private Map<String, String> decisionRefToPathMap;

    private Collection<String> resourcesNewestVersions = new ArrayList<String>();

    private Map<String, String> processIdToPathMap;

    public static Logger logger = Logger.getLogger(FileScanner.class.getName());

    public FileScanner(final Map<String, Rule> rules, final String classPathScanLocation)
            throws DependencyResolutionRequiredException {

        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(ConstantsConfig.BASEPATH);

        // get file paths of process definitions
        scanner.setIncludes(new String[] { ConstantsConfig.BPMN_FILE_PATTERN });
        scanner.scan();
        processdefinitions = new HashSet<String>(Arrays.asList(scanner.getIncludedFiles()));

        // get mapping from process id to file path
        processIdToPathMap = createProcessIdToPathMap(processdefinitions);

        // get file paths of java files
        if (classPathScanLocation != null && !classPathScanLocation.isEmpty()) {
            URL[] urls;
            LinkedList<File> files = new LinkedList<File>();

            URLClassLoader ucl;
            if (RuntimeConfig.getInstance().getClassLoader() instanceof URLClassLoader) {
                ucl = ((URLClassLoader) RuntimeConfig.getInstance().getClassLoader());
            } else {
                ucl = ((URLClassLoader) RuntimeConfig.getInstance().getClassLoader().getParent());
            }
            urls = ucl.getURLs();

            // retrieve all jars during runtime and pass them to get class files
            for (URL url : urls) {
                if (url.getFile().contains("target/classes")) {
                    File f = new File(url.getFile().substring(1) + classPathScanLocation);
                    if (f.exists()) {
                        files = (LinkedList<File>) FileUtils.listFiles(f,
                                TrueFileFilter.INSTANCE,
                                TrueFileFilter.INSTANCE);
                        addResources(files);
                    }
                }
            }
        }

        // get mapping from decision reference to file path
        scanner.setIncludes(new String[] { ConstantsConfig.DMN_FILE_PATTERN });
        scanner.scan();
        decisionRefToPathMap = createDmnKeyToPathMap(
                new HashSet<String>(Arrays.asList(scanner.getIncludedFiles())));

        // determine version name schema for resources
        final String versioningSchema = loadVersioningSchemaClass(rules);
        if (versioningSchema != null) {
            scanner.setIncludes(new String[] { versioningSchema });
            scanner.scan();

            // get current versions for resources, that match the name schema
            resourcesNewestVersions = createResourcesToNewestVersions(
                    new HashSet<String>(Arrays.asList(scanner.getIncludedFiles())), versioningSchema);

        }
    }

    /**
     * process classes and add resource
     *
     */
    private void addResources(LinkedList<File> classes) {

        for (File file : classes) {
            if (file.getName().endsWith(".class")) {
                javaResources.add(file.getName());
            }
        }
    }

    /**
     * get file paths for process definitions
     *
     * @return
     */
    public Set<String> getProcessdefinitions() {
        return processdefinitions;
    }

    /**
     * get file paths of java resources
     *
     * @return
     */
    public Set<String> getJavaResources() {
        return javaResources;
    }

    /**
     * get mapping from process id to file path of bpmn models
     *
     * @return
     */
    public Map<String, String> getProcessIdToPathMap() {
        return processIdToPathMap;
    }

    /**
     * get mapping from decisionRef to file path of dmn models
     *
     * @return
     */
    public Map<String, String> getDecisionRefToPathMap() {
        return decisionRefToPathMap;
    }

    /**
     * get a list of versioned resources (only with current versions)
     *
     * @return
     */
    public Collection<String> getResourcesNewestVersions() {
        return resourcesNewestVersions;
    }

    /**
     * Get class loader for the maven project, which uses this plugin
     *
     * @param project
     * @return
     * @throws DependencyResolutionRequiredException
     * @throws MalformedURLException
     */
    static ClassLoader getClassLoader(final MavenProject project)
            throws DependencyResolutionRequiredException, MalformedURLException {
        final List<String> classPathElements = project.getRuntimeClasspathElements();
        final List<URL> classpathElementUrls = new ArrayList<URL>(classPathElements.size());
        for (final String classPathElement : classPathElements) {
            classpathElementUrls.add(new File(classPathElement).toURI().toURL());
        }
        classpathElementUrls.add(new File("src/main/java").toURI().toURL());
        return new URLClassLoader(classpathElementUrls.toArray(new URL[classpathElementUrls.size()]),
                Thread.currentThread().getContextClassLoader());
    }

    /**
     * Map for getting bpmn reference by process id
     *
     * @param paths
     * @return
     */
    private static Map<String, String> createProcessIdToPathMap(final Set<String> paths) {

        final Map<String, String> keyToPathMap = new HashMap<String, String>();

        for (final String path : paths) {
            // read bpmn file
            BpmnModelInstance modelInstance = null;
            try {
                modelInstance = Bpmn.readModelFromFile(new File(ConstantsConfig.BASEPATH + path));
            } catch (final BpmnModelException ex) {
                throw new RuntimeException("bpmn model couldn't be read", ex);
            }
            // if bpmn file could read
            if (modelInstance != null) {
                // find process
                final Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
                if (processes != null) {
                    for (final Process process : processes) {
                        // save path for each process
                        keyToPathMap.put(process.getId(), path);
                    }
                }
            }
        }
        return keyToPathMap;
    }

    /**
     * Map for getting dmn reference by key
     *
     * @param paths
     * @return
     */
    private static Map<String, String> createDmnKeyToPathMap(final Set<String> paths) {

        final Map<String, String> keyToPathMap = new HashMap<String, String>();

        for (final String path : paths) {
            // read dmn file
            DmnModelInstance modelInstance = null;
            try {
                modelInstance = Dmn.readModelFromFile(new File(ConstantsConfig.BASEPATH + path));
            } catch (final DmnModelException ex) {
                throw new RuntimeException("dmn model couldn't be read", ex);
            }
            // if dmn could read
            if (modelInstance != null) {
                // find decisions
                final Collection<Decision> decisions = modelInstance.getModelElementsByType(Decision.class);
                if (decisions != null) {
                    for (final Decision decision : decisions) {
                        // save path for each decision
                        keyToPathMap.put(decision.getId(), path);
                    }
                }
            }
        }

        return keyToPathMap;
    }

    /**
     * reads versioned classes and scripts and generates a map with newest versions
     *
     * @return Map
     */
    private static Collection<String> createResourcesToNewestVersions(
            final Set<String> versionedFiles, final String versioningSchema) {
        final Map<String, String> newestVersionsMap = new HashMap<String, String>();

        if (versionedFiles != null) {
            for (final String versionedFile : versionedFiles) {
                final Pattern pattern = Pattern.compile(versioningSchema);
                final Matcher matcher = pattern.matcher(versionedFile);
                while (matcher.find()) {
                    final String resource = matcher.group(1);
                    final String oldVersion = newestVersionsMap.get(resource);
                    if (oldVersion != null) {
                        // If smaller than 0 this version is newer
                        if (oldVersion.compareTo(versionedFile) < 0) {
                            newestVersionsMap.put(resource, versionedFile);
                        }
                    } else {
                        newestVersionsMap.put(resource, versionedFile);
                    }
                }
            }
        }
        return newestVersionsMap.values();
    }

    /**
     * determine versioning schema for an active versioning checker
     *
     * @param rules
     * @return schema (regex), if null the checker is inactive
     */
    private static String loadVersioningSchemaClass(final Map<String, Rule> rules) {
        final String SETTING_NAME = "versioningSchemaClass";
        String schema = null;
        final Rule rule = rules.get(VersioningChecker.class.getSimpleName());
        if (rule != null && rule.isActive()) {
            final Map<String, Setting> settings = rule.getSettings();
            final Setting setting = settings.get(SETTING_NAME);
            if (setting == null) {
                schema = ConstantsConfig.DEFAULT_VERSIONED_FILE_PATTERN;
                final Setting newSetting = new Setting(SETTING_NAME,
                        ConstantsConfig.DEFAULT_VERSIONED_FILE_PATTERN);
                settings.put(SETTING_NAME, newSetting);
            } else {
                schema = setting.getValue();
            }
        }
        return schema;
    }
}
