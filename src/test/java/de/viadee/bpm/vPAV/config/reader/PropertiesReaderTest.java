package de.viadee.bpm.vPAV.config.reader;

import de.viadee.bpm.vPAV.exceptions.InvalidPropertiesParameterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Properties;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

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

    @AfterEach
    void tearDown() {
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
}