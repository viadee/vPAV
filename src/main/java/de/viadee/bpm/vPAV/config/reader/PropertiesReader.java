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
package de.viadee.bpm.vPAV.config.reader;

import de.viadee.bpm.vPAV.exceptions.InvalidPropertiesParameterException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

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

    protected Properties readPropertiesFromFile() {
        InputStream input = null;
        Properties properties = new Properties();
        try {
            Path propertiesPath = findPropertiesPath();
            if (propertiesPath == null) {
                LOGGER.info("vPav.properties file could not be found. Falling back to default values...");
            } else {
                input = Files.newInputStream(propertiesPath);
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

    protected Path findPropertiesPath() throws IOException {
        final Path[] foundFile = new Path[1];
        String pattern = "{vPav, vpav, vPAV}.properties";
        FileSystem fs = FileSystems.getDefault();
        PathMatcher matcher = fs.getPathMatcher("glob:" + pattern);
        FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attribs) {
                Path name = file.getFileName();
                if (matcher.matches(name)) {
                    foundFile[0] = file;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(Paths.get(""), matcherVisitor);
        return foundFile[0];
    }

    protected void validateProperties(Properties properties) {
        List<String> allowedProperties = Arrays.asList("outputhtml", "language", "basepath", "parentRuleSet", "ruleSet",
                "scanpath", "userVariablesFilePath", "validationFolder");
        properties.keySet().forEach(key -> {
            if (!allowedProperties.contains(key)) {
                throw new InvalidPropertiesParameterException("Not allowed property: " + key);
            }
            if (StringUtils.isEmpty(properties.getProperty((String) key)) ||
                    StringUtils.isBlank(properties.getProperty((String) key))) {
                throw new InvalidPropertiesParameterException("Empty property: " + key);
            }
        });
    }
}
