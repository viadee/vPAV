package de.viadee.bpm.vPAV;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;

import org.junit.Test;

public class RuntimeConfigTest {

    @Test
    public void runtimeConfigMustBeInitialized() {
        // Given
        RuntimeConfig rc = RuntimeConfig.getInstance();

        // Then
        assertNotNull("Runtime Config is not initialized", rc);
    }

    @Test
    public void beanMappingMustBeInitialized() {

        // Given
        RuntimeConfig rc = RuntimeConfig.getInstance();
        // When
        String beanClass = rc.findBeanByName("nonexistingBean");
        // Then
        assertNull("Bean mapping is not null safe", beanClass);

        // Given
        HashMap<String, String> beanMap = new HashMap<String, String>();
        // When
        beanMap.put("existingBean", "foo.class");
        rc.setBeanMapping(beanMap);
        String beanClass2 = rc.findBeanByName("existingBean");
        // Then
        assertEquals("Bean mapping is incomplete", "foo.class", beanClass2);

        // Given
        // rc.s
        // When

        // Then

    }

}
