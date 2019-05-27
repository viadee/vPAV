package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.constants.ConfigConstants;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ProcessApplicationValidatorTest {
    private static ClassLoader cl;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        // Set custom basepath.
        Properties myProperties = new Properties();
        myProperties.put("basepath", "src/test/resources/ProcessApplicationValidatorTest/");
        ConfigConstants.getInstance().setProperties(myProperties);

        // Bean-Mapping
        final Map<String, String> beanMapping = new HashMap<String, String>();
        beanMapping.put("testDelegate", "de.viadee.bpm.vPAV.TestDelegate");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);

        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = {classUrl};
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
    }

    /**
     * This test fails if soot is not able to process Lambda expressions.
     */
    @Test
    public void testLamdbaExpression() {
        ProcessApplicationValidator.findModelInconsistencies((HashMap<String, String>) RuntimeConfig.getInstance().getBeanMapping());
    }
}
