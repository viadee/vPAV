package de.viadee.bpm.vPAV;

import soot.*;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ClassicCompleteBlockGraph;

import java.util.List;

public class SootResolverSimplified {

    public static Block getBlockFromMethod(SootMethod method) {
        Body body = method.retrieveActiveBody();
        BlockGraph graph = new ClassicCompleteBlockGraph(body);
        List<Block> graphHeads = graph.getHeads();
        assert (graphHeads.size() == 1);

        return graphHeads.get(0);
    }
}
