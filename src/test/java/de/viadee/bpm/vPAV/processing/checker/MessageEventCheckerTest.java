package de.viadee.bpm.vPAV.processing.checker;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;

public class MessageEventCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static MessageEventChecker checker;

    private static ClassLoader cl;

    private final Rule rule = new Rule("MessageEventChecker", true, null, null, null);

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
    }

    /**
     * Case: StartEvent has been set with correct message
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testStartEvent() {
        final String PATH = BASE_PATH + "MessageEventChecker_testStartEvent.bpmn";
        checker = new MessageEventChecker(rule);

        // parse bpmn model
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement);
            issues.addAll(checker.check(element));
        }

        if (issues.size() > 0) {
            Assert.fail("correct model generates an issue");
        }
    }

    /**
     * Case: EndEvent has been set with wrong message
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testEndEvent() {
        final String PATH = BASE_PATH + "MessageEventChecker_testEndEvent.bpmn";
        checker = new MessageEventChecker(rule);

        // parse bpmn model
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        }
    }

    /**
     * Case: Mixed events with several faults
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testMixedEvents() {
        final String PATH = BASE_PATH + "MessageEventChecker_testMixedEvents.bpmn";
        checker = new MessageEventChecker(rule);

        // parse bpmn model
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        }
    }

}
