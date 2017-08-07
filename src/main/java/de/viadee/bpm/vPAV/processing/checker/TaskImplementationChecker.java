package de.viadee.bpm.vPAV.processing.checker;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.AbstractRunner;
import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.ConstantsConfig;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;

public class TaskImplementationChecker {

    /**
     * retrieves implementation of single bpmn element with help of the scanner
     *
     * @param element
     *            BpmnElement
     */

    public static void getTaskImplementation(BpmnElement element) {

        final BPMNScanner scan;

        try {

            scan = new BPMNScanner();
            final BaseElement bpmnElement = element.getBaseElement();

            String path;
            for (final String output : AbstractRunner.getModelPath()) {
                path = ConstantsConfig.BASEPATH + output;
                CheckerFactory.implementation = scan.getImplementation(path, bpmnElement.getId());
            }

        } catch (SAXException | IOException | ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
