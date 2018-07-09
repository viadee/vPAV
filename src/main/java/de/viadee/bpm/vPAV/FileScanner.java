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
package de.viadee.bpm.vPAV;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
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
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ConfigItemNotFoundException;
import de.viadee.bpm.vPAV.processing.checker.VersioningChecker;

/**
 * scans maven project for files, which are necessary for the later analysis
 *
 */
public class FileScanner {

    private final Set<String> processdefinitions;

    private static Set<String> javaResourcesFileInputStream = new HashSet<String>();

    private Set<String> includedFiles = new HashSet<String>();

    private Map<String, String> decisionRefToPathMap;

    private Collection<String> resourcesNewestVersions = new ArrayList<String>();

    private Map<String, String> processIdToPathMap;

    private static String scheme = null;

    private static String sootPath = "";

    private static boolean isDirectory = false;

    private static final Logger LOGGER = Logger.getLogger(FileScanner.class.getName());

    public FileScanner(final Map<String, Rule> rules) {

        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(ConfigConstants.BASEPATH);

        // get file paths of process definitions
        scanner.setIncludes(new String[] { ConfigConstants.BPMN_FILE_PATTERN });
        scanner.scan();
        processdefinitions = new HashSet<String>(Arrays.asList(scanner.getIncludedFiles()));

        scanner.setBasedir(ConfigConstants.JAVAPATH);
        // get file paths of process definitions
        scanner.setIncludes(new String[] { ConfigConstants.JAVA_FILE_PATTERN });
        scanner.scan();
        javaResourcesFileInputStream = new HashSet<String>(Arrays.asList(scanner.getIncludedFiles()));

        // get mapping from process id to file path
        processIdToPathMap = createProcessIdToPathMap(processdefinitions);

        // determine version name schema for resources
        String versioningScheme = null;

        try {
            versioningScheme = loadVersioningScheme(rules);
        } catch (ConfigItemNotFoundException e) {
            LOGGER.log(Level.WARNING, "Versioning Scheme could not be loaded.", e);
        }

        // get file paths of java files
        URL[] urls;
        LinkedList<File> files = new LinkedList<File>();
        LinkedList<File> dirs = new LinkedList<File>();
        URLClassLoader ucl;
        if (RuntimeConfig.getInstance().getClassLoader() instanceof URLClassLoader) {
            ucl = ((URLClassLoader) RuntimeConfig.getInstance().getClassLoader());
        } else {
            ucl = ((URLClassLoader) RuntimeConfig.getInstance().getClassLoader().getParent());
        }

        urls = ucl.getURLs();

        // retrieve all jars and create one String for Soot path
        for (URL url : urls) {

            String sootPathCurrent = url.toString();
            AddStringToSootPath(sootPathCurrent);

        }

        URL urlTargetClass = this.getClass().getResource("/");
        if (urlTargetClass != null) {
            String path = urlTargetClass.toString();
            AddStringToSootPath(path);
        }

        sootPath = sootPath.replace("\\\\;", ";");

        // retrieve all jars during runtime and pass them to get class files

        for (URL url : urls) {
            if (url.getFile().contains(ConfigConstants.TARGET_CLASS_FOLDER)) {
                File f = new File(url.getFile());
                if (!isDirectory && f.exists()) {
                    files = (LinkedList<File>) FileUtils.listFiles(f,
                            TrueFileFilter.INSTANCE,
                            TrueFileFilter.INSTANCE);
                    addResources(files);
                } else {
                    files = (LinkedList<File>) FileUtils.listFilesAndDirs(f,
                            DirectoryFileFilter.INSTANCE,
                            TrueFileFilter.INSTANCE);
                    dirs.addAll(findLastDir(files));
                }
            }
        }

        // get mapping from decision reference to file path
        scanner.setBasedir(ConfigConstants.BASEPATH);
        scanner.setIncludes(new String[] { ConfigConstants.DMN_FILE_PATTERN });
        scanner.scan();
        decisionRefToPathMap = createDmnKeyToPathMap(
                new HashSet<String>(Arrays.asList(scanner.getIncludedFiles())));

        final Rule rule = rules.get(VersioningChecker.class.getSimpleName());
        if (rule != null && rule.isActive()) {
            if (versioningScheme != null && !isDirectory) {
                // also add groovy files to included files
                scanner.setIncludes(new String[] { ConfigConstants.SCRIPT_FILE_PATTERN });
                scanner.scan();
                includedFiles.addAll(Arrays.asList(scanner.getIncludedFiles()));

                // filter files by versioningSchema
                resourcesNewestVersions = createResourcesToNewestVersions(includedFiles, versioningScheme);
            } else {

                for (File file : dirs) {
                    includedFiles.add(file.getAbsolutePath());
                }
                resourcesNewestVersions = createDirectoriesToNewestVersions(includedFiles, versioningScheme);
            }
        }
    }

    /**
     * Take one jar`s path, modify it from ClassLoader format to Soot`s format and add it to the previous paths.
     *
     * @param sootPathCurrent
     *            - one jar's local path
     */
    private void AddStringToSootPath(String sootPathCurrent) {

        // Create a long String with every file and jar path for Soot.

        if (sootPathCurrent != null) {
            sootPathCurrent = sootPathCurrent.replace("file:/", "");
            sootPathCurrent = sootPathCurrent.replace("/./", "/");

        }

        sootPath = sootPath + sootPathCurrent + ";";

    }

    /**
     * Find the bottom folder of a given list of starting folders to check a package versioning scheme
     *
     * @param list
     * @return
     */
    private LinkedList<File> findLastDir(LinkedList<File> list) {

        LinkedList<File> returnList = new LinkedList<File>();
        returnList.addAll(list);

        for (File f : list) {
            if (f.isFile())
                returnList.remove(f);
            File[] fileArr = f.listFiles();
            for (File uF : fileArr) {
                if (uF.isDirectory())
                    returnList.remove(f);
            }
        }

        return returnList;
    }

    /**
     * Process classes and add compiled classes to javaResources Also adss all filenames to includedFiles
     *
     * @param classes
     */
    private void addResources(LinkedList<File> classes) {

        for (File file : classes) {
            includedFiles.add(file.getName());
        }
    }

    /**
     * get file paths for process definitions
     *
     * @return processdefinitions Process definitions
     */
    public Set<String> getProcessdefinitions() {
        return processdefinitions;
    }

    /**
     * get mapping from process id to file path of bpmn models
     *
     * @return processIdToPathMap returns processIdToPathMap
     */
    public Map<String, String> getProcessIdToPathMap() {
        return processIdToPathMap;
    }

    /**
     * get mapping from decisionRef to file path of dmn models
     *
     * @return decisionRefToPathMap returns decisionRefToPathMap
     */
    public Map<String, String> getDecisionRefToPathMap() {
        return decisionRefToPathMap;
    }

    /**
     * get a list of versioned resources (only with current versions)
     *
     * @return resourcesNewestVersions returns resourcesNewestVersions
     */
    public Collection<String> getResourcesNewestVersions() {
        return resourcesNewestVersions;
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
                modelInstance = Bpmn.readModelFromFile(new File(ConfigConstants.BASEPATH + path));
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
                modelInstance = Dmn.readModelFromFile(new File(ConfigConstants.BASEPATH + path));
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
     * reads versioned directories and generates a map with newest versions
     *
     * @return Map
     */
    private static Collection<String> createDirectoriesToNewestVersions(
            final Set<String> versionedFiles, final String versioningSchema) {
        final Map<String, String> newestVersionsPathMap = new HashMap<String, String>();
        final Map<String, String> newestVersionsMap = new HashMap<String, String>();

        if (versionedFiles != null && versioningSchema != null) {
            for (final String versionedFile : versionedFiles) {
                final Pattern pattern = Pattern.compile(versioningSchema);
                final Matcher matcher = pattern.matcher(versionedFile);
                while (matcher.find()) {
                    String temp;
                    if (matcher.group(0).contains(File.separator)) {
                        temp = versionedFile
                                .replace(matcher.group(0).substring(matcher.group(0).lastIndexOf(File.separator)), "");
                    } else {
                        temp = versionedFile.replace(matcher.group(0), "");
                    }
                    final String value = versionedFile.substring(versionedFile.lastIndexOf("classes") + 8);

                    final String resource = temp.substring(temp.lastIndexOf("classes") + 8);
                    final String oldVersion = newestVersionsMap.get(resource);

                    if (oldVersion != null) {
                        if (oldVersion.compareTo(matcher.group(0)) < 0) {
                            newestVersionsMap.put(resource, matcher.group(0));
                            newestVersionsPathMap.put(resource, value);
                        }
                    } else {
                        newestVersionsMap.put(resource, matcher.group(0));
                        newestVersionsPathMap.put(resource, value);
                    }
                }
            }
        }
        return newestVersionsPathMap.values();
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
     * @throws ConfigItemNotFoundException
     */
    private static String loadVersioningScheme(final Map<String, Rule> rules)
            throws ConfigItemNotFoundException {

        final Rule rule = rules.get(VersioningChecker.class.getSimpleName());
        if (rule != null && rule.isActive()) {
            Setting setting = null;
            final Map<String, Setting> settings = rule.getSettings();
            if (settings.containsKey(ConfigConstants.VERSIONINGSCHEMECLASS)
                    && !settings.containsKey(ConfigConstants.VERSIONINGSCHEMEPACKAGE)) {
                setting = settings.get(ConfigConstants.VERSIONINGSCHEMECLASS);
                isDirectory = false;
            } else if (!settings.containsKey(ConfigConstants.VERSIONINGSCHEMECLASS)
                    && settings.containsKey(ConfigConstants.VERSIONINGSCHEMEPACKAGE)) {
                setting = settings.get(ConfigConstants.VERSIONINGSCHEMEPACKAGE);
                isDirectory = true;
            }
            if (setting == null) {
                throw new ConfigItemNotFoundException("VersioningChecker: Versioning Scheme could not be read. "
                        + "Possible options: " + ConfigConstants.VERSIONINGSCHEMECLASS + " or "
                        + ConfigConstants.VERSIONINGSCHEMEPACKAGE);
            } else {
                scheme = setting.getValue().trim();
            }
        }
        return scheme;
    }

    public static String getVersioningScheme() {
        return scheme;
    }

    public static Set<String> getJavaResourcesFileInputStream() {
        return javaResourcesFileInputStream;
    }

    public static boolean getIsDirectory() {
        return isDirectory;
    }

    public static void setIsDirectory(boolean isDirectory) {
        FileScanner.isDirectory = isDirectory;
    }

    /**
     *
     * @return - Concatenated String of jars' local paths
     */
    public static String getSootPath() {
        return sootPath;
    }
}
