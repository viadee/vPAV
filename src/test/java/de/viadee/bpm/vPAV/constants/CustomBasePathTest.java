/**
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vPAV.constants;

import de.viadee.bpm.vPAV.ProcessApplicationValidator;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;

public class CustomBasePathTest {

    @Test
    public void testCustomBasePathExists() {
        // Set custom basepath.
        ConfigConstants instance = Mockito.mock(ConfigConstants.class);
        ConfigConstants.setInstance(instance);
        Mockito.when(instance.getBasepath()).thenReturn("src/test/resources/PropertiesTest/");
        Assert.assertEquals("BasePath could not be successfully injected", "src/test/resources/PropertiesTest/",
                ConfigConstants.getInstance().getBasepath());

        // Run validator.
        ArrayList<CheckerIssue> inconsistencies = (ArrayList<CheckerIssue>) ProcessApplicationValidator
                .findModelInconsistencies();
        Assert.assertEquals("Wrong bpmn file was analyzed.",
                "src" + File.separator + "test" + File.separator + "resources" + File.separator + "PropertiesTest"
                        + File.separator + "PropertiesTest_Wrong.bpmn",
                inconsistencies.get(0).getBpmnFile());
        Assert.assertFalse("BasePath was changed but BPMN model was not found",
                inconsistencies.isEmpty());
    }
}