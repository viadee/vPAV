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


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to read the properties file (vPav.properties) and extract the configured rules
 * Requirements: Existing vPav.properties in src/test/resources
 */
public class PropertiesReader {

    private static final Logger LOGGER = Logger.getLogger(PropertiesReader.class.getName());

    public Properties read() {
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

        //Validate properties regarding multi project report support
        //TODO redo checks
        if (properties.containsKey("multiProjectReport")) {
            String[] paths = { "" };
            try {
                paths = properties.get("generatedReports").toString().split(",");
            } catch (NullPointerException e) {
                throw new RuntimeException("No generated reports folders defined");
            }
            if (properties.get("multiProjectReport").equals("true") && paths.length < 1) {
                throw new RuntimeException("Invalid definition for generated reports folders");
            } else if (properties.get("multiProjectReport").equals("false") && paths.length > 0) {
                throw new RuntimeException(
                        "Generated reports folders not allowed when multi project report is disabled");
            }
            for (String path : paths) {
                try {
                    Paths.get(path);
                } catch (InvalidPathException ex) {
                    throw new RuntimeException("Invalid path in generated reports folder: " + path);
                }
            }
        }

        return properties;
    }

    private Path findPropertiesPath() throws IOException {
        final Path[] foundFile = new Path[1];
        String pattern = "{vPav, vpav, vPAV}.properties";
        FileSystem fs = FileSystems.getDefault();
        PathMatcher matcher = fs.getPathMatcher("glob:" + pattern);
        FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<Path>() {

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
}
