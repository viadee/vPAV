/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. All advertising materials mentioning features or use of this software must display the following
 * acknowledgement: This product includes software developed by the viadee Unternehmensberatung GmbH. 4. Neither the
 * name of the viadee Unternehmensberatung GmbH nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;

public class ProcessVariableReaderTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static ClassLoader cl;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java/");
        final URL resourcesUrl = new URL(currentPath + "src/test/resources/");
        final URL[] classUrls = { classUrl, resourcesUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
    }

    @Test
    public void testRecogniseVariablesInClass() {
        final String PATH = BASE_PATH + "ProcessVariableReaderTest_RecogniseVariablesInClass.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> allServiceTasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final ProcessVariableReader variableReader = new ProcessVariableReader(null);

        final BpmnElement element = new BpmnElement(PATH, allServiceTasks.iterator().next());
        final Map<String, ProcessVariable> variables = variableReader.getVariablesFromElement(element);

        Assert.assertEquals(2, variables.size());
    }

    @Test
    public void testRecogniseInputOutputAssociations() {
        final String PATH = BASE_PATH + "ProcessVariableReaderTest_InputOutputCallActivity.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<CallActivity> allServiceTasks = modelInstance
                .getModelElementsByType(CallActivity.class);

        final ProcessVariableReader variableReader = new ProcessVariableReader(null);

        final BpmnElement element = new BpmnElement(PATH, allServiceTasks.iterator().next());
        final Map<String, ProcessVariable> variables = variableReader.getVariablesFromElement(element);

        final ProcessVariable nameOfVariableInMainProcess = variables
                .get("nameOfVariableInMainProcess");
        Assert.assertNotNull(nameOfVariableInMainProcess);
        Assert.assertEquals(VariableOperation.WRITE, nameOfVariableInMainProcess.getOperation());

        final ProcessVariable nameOfVariableInMainProcess2 = variables
                .get("nameOfVariableInMainProcess2");
        Assert.assertNotNull(nameOfVariableInMainProcess2);
        Assert.assertEquals(VariableOperation.WRITE, nameOfVariableInMainProcess2.getOperation());

        final ProcessVariable someVariableInMainProcess = variables.get("someVariableInMainProcess");
        Assert.assertNotNull(someVariableInMainProcess);
        Assert.assertEquals(VariableOperation.READ, someVariableInMainProcess.getOperation());

        final ProcessVariable someVariableInMainProcess2 = variables.get("someVariableInMainProcess2");
        Assert.assertNotNull(someVariableInMainProcess2);
        Assert.assertEquals(VariableOperation.READ, someVariableInMainProcess2.getOperation());
    }
}
