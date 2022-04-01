/*
 * BSD 3-Clause License
 *
 * Copyright © 2022, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vpav.config.reader;

import de.viadee.bpm.vpav.constants.ConfigConstants;
import de.viadee.bpm.vpav.exceptions.InvalidPropertiesConfigurationException;
import de.viadee.bpm.vpav.exceptions.InvalidPropertiesParameterException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Used to read the properties file (vPav.properties) and extract the configured rules
 * Requirements: Existing vPav.properties in src/test/resources
 */
public class PropertiesReader {

    private static final Logger LOGGER = Logger.getLogger(PropertiesReader.class.getName());

    public Properties initProperties() {
        Properties properties = readPropertiesFromFile();
        this.validateProperties(properties);
        return properties;
    }

    Properties readPropertiesFromFile() {
        InputStream input = null;
        Properties properties = new Properties();
        try {
            Optional<Path> propertiesPath = findPropertiesPath();
            if (propertiesPath.isEmpty()) {
                LOGGER.info("vPav.properties file could not be found. Falling back to default values...");
            } else {
                input = Files.newInputStream(propertiesPath.get());
                properties.load(input);
            }
        } catch (IOException e) {
            LOGGER.warning("Could not read vPav.properties file. Falling back to default values...");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.warning("InputStream from vPav.properties could not be closed.");
                }
            }
        }

        return properties;
    }

    private Optional<Path> findPropertiesPath() {
        Optional<Path> path = Optional.empty();
        try (Stream<Path> walk = Files.walk(Paths.get(""))) {
            path = walk.filter(f ->
                    f.toString().endsWith("vpav.properties")
                            || f.toString().endsWith("vPAV.properties")
                            || f.toString().endsWith("vPav.properties"))
                    .findFirst();
        } catch (IOException ignored) {
            LOGGER.warning("IOException occured during properties scan.");
        }

        return path;
    }

    void validateProperties(Properties properties) {
        List<String> allowedProperties = Arrays.asList("outputhtml", "language", "basepath", "parentRuleSet", "ruleSet",
                "scanpath", "userVariablesFilePath", "validationFolder", "multiProjectReport", "generatedReports");
        properties.keySet().forEach(key -> {
            if (!allowedProperties.contains(key)) {
                throw new InvalidPropertiesParameterException("Not allowed property: " + key);
            }
            if (StringUtils.isEmpty(properties.getProperty((String) key)) ||
                    StringUtils.isBlank(properties.getProperty((String) key))) {
                throw new InvalidPropertiesParameterException("Empty property: " + key);
            }
        });

        //Validate properties regarding multi project report support
        if (properties.containsKey("multiProjectReport")) {
            if (properties.get("multiProjectReport").equals("true")) {
                if (properties.containsKey("generatedReports")) {
                    String[] paths = properties.get("generatedReports").toString().split(",");
                    if (properties.containsKey("outputhtml") && properties.get("outputhtml").equals("false")) {
                        throw new InvalidPropertiesConfigurationException(
                                "Multi project scan not allowed when HTML output is disabled");
                    }
                    if (paths.length < 2) {
                        throw new InvalidPropertiesParameterException(
                                "At least 2 external report folders must be defined");
                    }
                    for (String stringPath : paths) {
                        Path path = Paths
                                .get(FilenameUtils.separatorsToSystem(stringPath +
                                        File.separator + ConfigConstants.DATA_FOLDER));
                        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                            throw new InvalidPropertiesParameterException(
                                    String.format("No %s folder found in reports folder: ",
                                            ConfigConstants.DATA_FOLDER) + stringPath);
                        }
                    }
                } else
                    throw new InvalidPropertiesConfigurationException(
                            "Enabled multi report scan needs defined external reports paths");
            } else if (properties.get("multiProjectReport").equals("false") && properties
                    .containsKey("generatedReports")) {
                throw new InvalidPropertiesConfigurationException(
                        "External reports paths not allowed with disabled multi report scan");
            }
        }

    }
}
