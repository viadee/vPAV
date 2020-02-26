package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.processing.ObjectReader;
import de.viadee.bpm.vPAV.processing.code.flow.AnalysisElement;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import de.viadee.bpm.vPAV.processing.model.data.OutSetCFG;
import de.viadee.bpm.vPAV.processing.model.data.VariableBlock;
import soot.Value;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VariablesReader {
    // Only called for top-level block -> Rename
    VariableBlock blockIterator(final Set<String> classPaths, final CallGraph cg, final Block block,
            final OutSetCFG outSet, final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String filePath, final String scopeId,
            String assignmentStmt, final List<Value> args,
            final AnalysisElement[] predecessor, final String thisObject) {

        VariableBlock variableBlock = new VariableBlock(block, new ArrayList<>());

        ObjectReader objectReader = new ObjectReader(this);
        objectReader.processBlock(block, args);

        return variableBlock;
    }

    public void handleProcessVariableManipulation() {
        // This method adds process variables one by one
    }
}
