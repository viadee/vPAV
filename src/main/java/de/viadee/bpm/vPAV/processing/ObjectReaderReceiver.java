package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.processing.code.flow.BasicNode;
import de.viadee.bpm.vPAV.processing.code.flow.Node;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import soot.SootClass;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.toolkits.graph.Block;

import java.util.List;

public abstract class ObjectReaderReceiver {

    public void handleProcessVariableManipulation(Block block, ProcessVariableOperation pvo, SootClass javaClass) {
    }

    public BasicNode addNodeIfNotExisting(Block block, SootClass javaClass) {
        return null;
    }

    public void visitBlockAgain(Block block) {
    }

    public Node getNodeOfBlock(Block block, SootClass javaClass) {
        return null;
    }

    public String getScopeId() {
        return "";
    }

    public String getScopeIdOfChild() {
        return "";
    }

    public void pushNodeToStack(BasicNode blockNode) {
    }

    public void addEntryPoint(String className, InvokeExpr expr, List<Object> args) {
    }

}
