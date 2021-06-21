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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.exceptions.ConfigItemNotFoundException;
import de.viadee.bpm.vPAV.processing.checker.VersioningChecker;
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * scans maven project for files, which are necessary for the later analysis
 */
public class FileScanner {

    private final Set<String> processDefinitions;

    private Set<String> resourcesFileInputStream = new HashSet<>();

    private static final Set<String> includedFiles = new HashSet<>();

    private final Map<String, String> decisionRefToPathMap;

    private Collection<String> resourcesNewestVersions = new ArrayList<>();

    private final Map<String, String> processIdToPathMap;

    private static String scheme = null;

    private static final StringBuilder sootPath = new StringBuilder();

    private static final Collection<String> sootPaths = new HashSet<>();

    private static boolean isDirectory = false;

    private static final Logger LOGGER = Logger.getLogger(FileScanner.class.getName());

    public FileScanner(final RuleSet rules) {

        final DirectoryScanner scanner = new DirectoryScanner();
        File basedir = null;
        String basepath = RuntimeConfig.getInstance().getBasepath();

        if (basepath.startsWith("file:/")) {
            // Convert URI
            try {
                basedir = new File(new URI(RuntimeConfig.getInstance().getBasepath()));
            } catch (URISyntaxException e) {
                LOGGER.log(Level.SEVERE, "URI of basedirectory seems to be malformed.", e);
            }
        } else {
            basedir = new File(basepath);
        }

        scanner.setBasedir(basedir);

        // get file paths of process definitions
        scanner.setIncludes(new String[] { ConfigConstants.BPMN_FILE_PATTERN });
        scanner.scan();
        processDefinitions = new HashSet<>(Arrays.asList(scanner.getIncludedFiles()));

        if (processDefinitions.isEmpty() && !RuntimeConfig.getInstance().isTest()) {
            LOGGER.log(Level.SEVERE, String.format("No model present in given location (%s)", basepath));
            System.exit(0);
        }

        String scanPath = RuntimeConfig.getInstance().getScanPath();
        String filePattern = RuntimeConfig.getInstance().getFilePattern();

        scanner.setBasedir(scanPath);
        // get file paths of process definitions
        scanner.setIncludes(new String[] { filePattern });

        if (scanner.getBasedir().exists()) {
            scanner.scan();
            resourcesFileInputStream = new HashSet<>(Arrays.asList(scanner.getIncludedFiles()));
        } else {
            LOGGER.log(Level.SEVERE, "No compiled files in target folder. Build project at least once.");
        }

        scanner.setBasedir("target/generated-sources/");
        // get file paths of process definitions
        scanner.setIncludes(new String[] { filePattern });
        if (scanner.getBasedir().exists()) {
            scanner.scan();
            resourcesFileInputStream.addAll(Arrays.asList(scanner.getIncludedFiles()));
        }

        // get mapping from process id to file path
        processIdToPathMap = createProcessIdToPathMap(processDefinitions);

        // determine version name schema for resources
        String versioningScheme = null;

        try {
            versioningScheme = loadVersioningScheme(rules.getElementRules());
        } catch (ConfigItemNotFoundException e) {
            LOGGER.log(Level.WARNING, "Versioning Scheme could not be loaded.", e);
        } catch (NullPointerException e) {
            LOGGER.log(Level.SEVERE, "RuleSet appears to be null", e);
        }

        LinkedList<File> dirs = new LinkedList<>();
        setupSootClassPaths(dirs);

        // get mapping from decision reference to file path
        scanner.setBasedir(basedir);
        scanner.setIncludes(new String[] { ConfigConstants.DMN_FILE_PATTERN });
        scanner.scan();
        decisionRefToPathMap = createDmnKeyToPathMap(new HashSet<>(Arrays.asList(scanner.getIncludedFiles())));

        if (rules.getElementRules().get(VersioningChecker.class.getSimpleName()) != null) {
            final Rule rule = rules.getElementRules().get(VersioningChecker.class.getSimpleName())
                    .get(VersioningChecker.class.getSimpleName());
            if (rule != null && rule.isActive()) {
                if (versioningScheme != null && !isDirectory) {
                    // also add groovy files to included files
                    scanner.setIncludes(new String[] { ConfigConstants.SCRIPT_FILE_PATTERN });
                    scanner.scan();

                    includedFiles.addAll(Arrays.asList(scanner.getIncludedFiles()));

                    // filter files by versioningSchema
                    resourcesNewestVersions = createResourcesToNewestVersions(versioningScheme);
                } else {

                    for (File file : dirs) {
                        includedFiles.add(file.getAbsolutePath());
                    }
                    resourcesNewestVersions = createDirectoriesToNewestVersions(versioningScheme);
                }
            }
        }
    }

    public static void setupSootClassPaths(List<File> dirs) {
        // get file paths of java files
        LinkedList<File> files;
        String pathSeparator = System.getProperty("path.separator");
        String[] classPathEntries = System.getProperty("java.class.path").split(pathSeparator);

        try {
            URL urlTargetClass;
            urlTargetClass = Paths.get(RuntimeConfig.getInstance().getScanPath()).toUri().toURL();
            String path = urlTargetClass.toString();
            addStringToSootPath(path);
        } catch (MalformedURLException e) {
            LOGGER.warning("Could not find target/classes folder");
        }

        final String whiteListProperty = RuntimeConfig.getInstance().getWhiteList();
        List<String> whitelist = Arrays.stream(whiteListProperty.split("\\s*,\\s*"))
                .map(entry -> entry.replace("/", "\\\\").trim())
                .collect(Collectors.toList());

        for (String entry : classPathEntries) {
            if (!whiteListProperty.isEmpty()) {
                for (String item : whitelist) {
                    Pattern pattern = Pattern.compile(item);
                    if (pattern.matcher(entry).find()) {
                        addStringToSootPath(entry);
                    }
                }
            }
            // retrieve all jars during runtime and pass them to get class files
            if (Pattern.compile(".*" + RuntimeConfig.getInstance().getScanPath() + ".*").matcher(entry).find()) {
                addStringToSootPath(entry);
            }

            if (entry.contains(ConfigConstants.TARGET_CLASS_FOLDER)) {
                File f = new File(entry);
                if (!isDirectory && f.exists()) {
                    files = (LinkedList<File>) FileUtils.listFiles(f, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                    addResources(files);
                } else {
                    files = (LinkedList<File>) FileUtils.listFilesAndDirs(f, DirectoryFileFilter.INSTANCE,
                            TrueFileFilter.INSTANCE);
                    dirs.addAll(findLastDir(files));
                }
            }
        }
    }

    /**
     * Take one jar`s path, modify it from ClassLoader format to Soot`s format and
     * add it to the previous paths.
     *
     * @param sootPathCurrent - one jar's local path
     */
    private static void addStringToSootPath(String sootPathCurrent) {
        // Create a long String with every file and jar path for Soot.
        if (sootPathCurrent != null) {
            sootPathCurrent = sootPathCurrent.replaceAll("%20", " ");
            sootPathCurrent = sootPathCurrent.replace("/./", "\\\\").replaceAll("/$", "");
            if (System.getProperty("os.name").startsWith("Windows")) {
                sootPathCurrent = sootPathCurrent.replace("file:/", "");
            } else {
                sootPathCurrent = sootPathCurrent.replace("file:", "");
            }
            sootPaths.add(sootPathCurrent);
        }
    }

    /**
     * Find the bottom folder of a given list of starting folders to check a package
     * versioning scheme
     *
     * @param list List of starting folders
     * @return List of bottom folders
     */
    public static List<File> findLastDir(List<File> list) {

        LinkedList<File> returnList = new LinkedList<>(list);

        for (File f : list) {
            if (f.isFile())
                returnList.remove(f);
            File[] fileArr = f.listFiles();
            assert fileArr != null;
            for (File uF : fileArr) {
                if (uF.isDirectory())
                    returnList.remove(f);
            }
        }

        return returnList;
    }

    /**
     * Process classes and add compiled classes to javaResources Also adss all
     * filenames to includedFiles
     *
     * @param classes Classes
     */
    public static void addResources(List<File> classes) {
        for (File file : classes) {
            includedFiles.add(file.getName());
        }
    }

    /**
     * get file paths for process definitions
     *
     * @return processDefinitions Process definitions
     */
    Set<String> getProcessDefinitions() {
        return processDefinitions;
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
     * @param paths file paths
     * @return Map of processIds and corresponding file paths of process definitions
     */
    private static Map<String, String> createProcessIdToPathMap(final Set<String> paths) {

        final Map<String, String> keyToPathMap = new HashMap<>();

        for (final String path : paths) {
            // read bpmn file
            BpmnModelInstance modelInstance;
            File bpmnfile = null;
            String basepath = RuntimeConfig.getInstance().getBasepath();

            if (basepath.startsWith("file:/")) {
                // Convert URI
                try {
                    bpmnfile = new File(new URI(RuntimeConfig.getInstance().getBasepath() + path));
                } catch (URISyntaxException e) {
                    LOGGER.log(Level.SEVERE, "URI of basedirectory seems to be malformed.", e);
                }
            } else {
                bpmnfile = new File(basepath + path);
            }

            try {
                modelInstance = Bpmn.readModelFromFile(bpmnfile);
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
     * @param paths file paths
     * @return Map of dmn keys and corresponding file paths of dmn definitions
     */
    private static Map<String, String> createDmnKeyToPathMap(final Set<String> paths) {

        final Map<String, String> keyToPathMap = new HashMap<>();

        for (final String path : paths) {
            // read dmn file
            DmnModelInstance modelInstance;
            try {
                modelInstance = Dmn.readModelFromFile(new File(RuntimeConfig.getInstance().getBasepath() + path));
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
    private static Collection<String> createDirectoriesToNewestVersions(final String versioningSchema) {
        final Map<String, String> newestVersionsPathMap = new HashMap<>();
        final Map<String, String> newestVersionsMap = new HashMap<>();

        if (versioningSchema != null) {
            for (final String versionedFile : includedFiles) {
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
            final String versioningSchema) {
        final Map<String, String> newestVersionsMap = new HashMap<>();

        for (final String versionedFile : includedFiles) {
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
        return newestVersionsMap.values();
    }

    /**
     * determine versioning schema for an active versioning checker
     *
     * @param rules RuleSet
     * @return schema (regex), if null the checker is inactive
     * @throws ConfigItemNotFoundException If versioning scheme setting could not be
     *                                     found
     */
    private static String loadVersioningScheme(final Map<String, Map<String, Rule>> rules)
            throws ConfigItemNotFoundException {

        if (rules.get(VersioningChecker.class.getSimpleName()) != null) {
            final Rule rule = rules.get(VersioningChecker.class.getSimpleName())
                    .get(VersioningChecker.class.getSimpleName());
            if (rule != null && rule.isActive()) {
                Setting setting = null;
                final Map<String, Setting> settings = rule.getSettings();
                if (settings.containsKey(ConfigConstants.VERSIONING_SCHEME_CLASS)
                        && !settings.containsKey(ConfigConstants.VERSIONING_SCHEME_PACKAGE)) {
                    setting = settings.get(ConfigConstants.VERSIONING_SCHEME_CLASS);
                    isDirectory = false;
                } else if (!settings.containsKey(ConfigConstants.VERSIONING_SCHEME_CLASS)
                        && settings.containsKey(ConfigConstants.VERSIONING_SCHEME_PACKAGE)) {
                    setting = settings.get(ConfigConstants.VERSIONING_SCHEME_PACKAGE);
                    isDirectory = true;
                }
                if (setting == null) {
                    throw new ConfigItemNotFoundException("VersioningChecker: Versioning Scheme could not be read. "
                            + "Possible options: " + ConfigConstants.VERSIONING_SCHEME_CLASS + " or "
                            + ConfigConstants.VERSIONING_SCHEME_PACKAGE);
                } else {
                    scheme = setting.getValue().trim();
                }
            }
        }

        return scheme;
    }

    public Set<String> getJavaResourcesFileInputStream() {
        return resourcesFileInputStream;
    }

    public void setJavaResourcesFileInputStream(Set<String> javaResources) {
        resourcesFileInputStream = javaResources;
    }

    public static boolean getIsDirectory() {
        return isDirectory;
    }

    public static void setIsDirectory(boolean isDirectory) {
        FileScanner.isDirectory = isDirectory;
    }

    /**
     * @return - Concatenated String of jars' local paths
     */
    public static String getSootPath() {
        for (String entry : sootPaths) {
            if (System.getProperty("os.name").startsWith("Windows")) {
                sootPath.append(entry);
                sootPath.append(";");
            } else {
                sootPath.append(entry);
                sootPath.append(":");
            }
        }
        return sootPath.toString().substring(0, sootPath.toString().length() - 1);
    }
}
