package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.beans.BeanMappingGenerator;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.processing.BpmnModelDispatcher;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Is used for the Data Flow Validation Language.
 */
public class ProcessApplicationVariableParser {

    public static Collection<ProcessVariable> parseProcessVariables(File modelFile, ApplicationContext ctx) {
        RuntimeConfig.getInstance().setApplicationContext(ctx);
        RuntimeConfig.getInstance().setBeanMapping(BeanMappingGenerator.generateBeanMappingFile(ctx));
        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());

        // Retrieve BPMN elements
        FileScanner fileScanner = new FileScanner(new RuleSet());
        ProcessVariablesScanner variableScanner = readOuterProcessVariables(fileScanner);
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(modelFile);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(fileScanner.getDecisionRefToPathMap(),
                fileScanner.getProcessIdToPathMap(), variableScanner.getMessageIdToVariableMap(),
                variableScanner.getProcessIdToVariableMap());

        // create data flow graphs for bpmn model including creation of process variables
        Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                modelFile.getPath(), new ArrayList<>(), variableScanner, new FlowAnalysis());

        return BpmnModelDispatcher.getProcessVariables(graphCollection.iterator().next().getVertices());
    }

    /**
     * Scan process variables in external classes, which are not referenced from
     * model
     *
     * @param fileScanner FileScanner
     */
    private static ProcessVariablesScanner readOuterProcessVariables(final FileScanner fileScanner) {
        ProcessVariablesScanner variableScanner = new ProcessVariablesScanner(
                fileScanner.getJavaResourcesFileInputStream());
        variableScanner.scanProcessVariables();
        return variableScanner;
    }
}
