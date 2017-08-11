package de.viadee.bpm.vPAV;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;

public class BPMNScannerTest {

    private static final String BASE_PATH = "src/test/resources/";

    /**
     * Case: BPMN-Model in Version 1
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testModelVersionV1() throws SAXException, IOException, ParserConfigurationException {
        final String PATH = BASE_PATH + "BPMN_Model_Version_V1.bpmn";
        final String impClass = "camunda:class";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        BPMNScanner scanner = new BPMNScanner();
        String imp = scanner.getImplementation(PATH, element.getBaseElement().getId());

        assertTrue("Get unexpected implementation", imp.equals(impClass));
    }

    /**
     * Case: BPMN-Model in Version 2
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testModelVersionV2() throws SAXException, IOException, ParserConfigurationException {
        final String PATH = BASE_PATH + "BPMN_Model_Version_V2.bpmn";
        final String impEx = "camunda:expression";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        BPMNScanner scanner = new BPMNScanner();
        String imp = scanner.getImplementation(PATH, element.getBaseElement().getId());

        assertTrue("Get unexpected implementation", imp.equals(impEx));
    }

    /**
     * Case: BPMN-Model in Version 3
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testModelVersionV3() throws SAXException, IOException, ParserConfigurationException {
        final String PATH = BASE_PATH + "BPMN_Model_Version_V3.bpmn";
        final String impDel = "camunda:delegateExpression";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        BPMNScanner scanner = new BPMNScanner();
        String imp = scanner.getImplementation(PATH, element.getBaseElement().getId());

        assertTrue("Get unexpected implementation", imp.equals(impDel));
    }

    /**
     * Case: Test getScriptType
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testGetScriptType() throws SAXException, IOException, ParserConfigurationException {
        final String PATH = BASE_PATH + "BPMN_Model_Version_V3.bpmn";
        final String scriptType = "camunda:inputParameter";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        BPMNScanner scanner = new BPMNScanner();
        String script = scanner.getScriptType(PATH, element.getBaseElement().getId());

        assertTrue("Get unexpected implementation", script.equals(scriptType));
    }

    /**
     * Case: Test getXorGateWays
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testGetXorGateWays() throws SAXException, IOException, ParserConfigurationException {
        final String PATH = BASE_PATH + "BPMNScannerXorGateway.bpmn";
        final String gatewayId = "ExclusiveGateway_Id";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<Gateway> baseElements = modelInstance
                .getModelElementsByType(Gateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        BPMNScanner scanner = new BPMNScanner();
        String gwId = scanner.getXorGateWays(PATH, element.getBaseElement().getId());

        assertTrue("Get unexpected Element", gwId.equals(gatewayId));
    }

    /**
     * Case: Test getOutgoing
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testGetOutgoing() throws SAXException, IOException, ParserConfigurationException {
        final String PATH = BASE_PATH + "BPMNScannerXorGateway.bpmn";
        final int anzOut = 2;

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<Gateway> baseElements = modelInstance
                .getModelElementsByType(Gateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        BPMNScanner scanner = new BPMNScanner();
        int out = scanner.getOutgoing(PATH, element.getBaseElement().getId());

        assertTrue("More or less outgoing sequentflows as expected", out == anzOut);
    }

}
