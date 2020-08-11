package de.viadee.bpm.vPAV.config.reader;

import de.viadee.bpm.vPAV.exceptions.InvalidPropertiesConfiguration;
import de.viadee.bpm.vPAV.exceptions.InvalidPropertiesParameterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.configuration.ConfigurationType.PowerMock;

@RunWith(MockitoJUnitRunner.class)
class PropertiesReaderTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    PropertiesReader testSubject;
    Properties properties;

    @BeforeEach
    void setUp() {
        properties = new Properties();
        MockitoAnnotations.initMocks(this);
        when(testSubject.readPropertiesFromFile()).thenReturn(properties);
    }

    @Test
    void testValidatePropertiesIsCalled() {
        testSubject.initProperties();
        verify(testSubject, times(1)).validateProperties(properties);
    }

    @Test
    void testInvalidPropertyKey() {
        properties.put("nonExistentProperty", "foo");
        assertThrows(InvalidPropertiesParameterException.class, () -> {
            testSubject.initProperties();
        });
    }

    @Test
    void testEmptyPropertyValue() {
        properties.put("outputhtml", " ");
        assertThrows(InvalidPropertiesParameterException.class, () -> {
            testSubject.initProperties();
        });
    }

    @Test
    void testMultiReportsEnabledWhenOutputHtmlIsDisabled() {
        properties.put("multiProjectReport", "true");
        properties.put("outputhtml", "false");
        assertThrows(InvalidPropertiesConfiguration.class, () -> {
            testSubject.initProperties();
        });
    }

    @Test
    void testOnlyOneExternalReportPath() {
        properties.put("multiProjectReport", "true");
        properties.put("generatedReports", "./a/b");
        assertThrows(InvalidPropertiesParameterException.class, () -> {
            testSubject.initProperties();
        });
    }

    //TODO Static Mocking, maybe with Powermock?
    //    @Test
    //    void testValidationReportNotFound() {
    //        properties.put("multiProjectReport", "true");
    //        properties.put("generatedReports", "./a/b,/c/d");
    //        mockStatic(Files.class);
    //        doReturn(false)
    //        when(Files.exists(Paths.get("foo"), new LinkOption[] { LinkOption.NOFOLLOW_LINKS })).thenReturn(false);
    //        assertThrows(InvalidPropertiesParameterException.class, () -> {
    //            testSubject.initProperties();
    //        });
    //    }

    @Test
    void testMultiReportsEnabledWithoutGeneratedReportsPaths() {
        properties.put("multiProjectReport", "true");
        assertThrows(InvalidPropertiesConfiguration.class, () -> {
            testSubject.initProperties();
        });
    }

    @Test
    void testMultiReportsDisabeldWithGeneratedReportsPaths() {
        properties.put("multiProjectReport", "false");
        properties.put("generatedReports", "./a/b");
        assertThrows(InvalidPropertiesConfiguration.class, () -> {
            testSubject.initProperties();
        });
    }

}