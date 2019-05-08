package de.viadee.bpm.vPAV.processing.code;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.model.data.Anomaly;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class AnomaliesSetCreationTest {

    private static final String BASE_PATH = "src/test/resources/";

    @BeforeClass
    public static void setup() throws IOException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
        RuntimeConfig.getInstance().setTest(true);
    }

    @Test
    public void findDD() {
        final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyDD.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next());
        final ControlFlowGraph cg = new ControlFlowGraph();
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.DelegateAnomalyDD", element, null, null, null, cg));
        cg.analyze(element);

        Anomaly anomaly = element.getAnomalies().entrySet().iterator().next().getValue().iterator().next().getAnomaly();
        assertEquals("Expected 1 anomalie but found " + element.getAnomalies().size(), 1, element.getAnomalies().size());
        assertEquals("Expected a DD anomaly but found " + anomaly, Anomaly.DD, anomaly);
    }

    @Test
    public void findDU() {
        final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyDU.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next());
        final ControlFlowGraph cg = new ControlFlowGraph();
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.DelegateAnomalyDU", element, null, null, null, cg));
        cg.analyze(element);

        Anomaly anomaly = element.getAnomalies().entrySet().iterator().next().getValue().iterator().next().getAnomaly();
        assertEquals("Expected 1 anomalie but found " + element.getAnomalies().size(), 1, element.getAnomalies().size());
        assertEquals("Expected a DU anomaly but found " + anomaly, Anomaly.DU, anomaly);
    }

    @Test
    public void findUR() {
        final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyUR.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next());
        final ControlFlowGraph cg = new ControlFlowGraph();
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.DelegateAnomalyUR", element, null, null, null, cg));
        cg.analyze(element);

        Anomaly anomaly = element.getAnomalies().entrySet().iterator().next().getValue().iterator().next().getAnomaly();
        assertEquals("Expected 1 anomalie but found " + element.getAnomalies().size(), 1, element.getAnomalies().size());
        assertEquals("Expected a UR anomaly but found " + anomaly, Anomaly.UR, anomaly);
    }

    @Test
    public void findUU() {
        final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyUU.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next());
        final ControlFlowGraph cg = new ControlFlowGraph();
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.DelegateAnomalyUU", element, null, null, null, cg));
        cg.analyze(element);

        Anomaly anomaly = element.getAnomalies().entrySet().iterator().next().getValue().iterator().next().getAnomaly();
        assertEquals("Expected 1 anomalie but found " + element.getAnomalies().size(), 1, element.getAnomalies().size());
        assertEquals("Expected a UU anomaly but found " + anomaly, Anomaly.UU, anomaly);
    }

    @Test
    public void findDNOP() {

    }



    @Test
    public void findNOPR() {

    }

}
