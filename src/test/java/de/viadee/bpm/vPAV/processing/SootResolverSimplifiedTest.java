package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.SootResolverSimplified;
import org.junit.Assert;
import org.junit.Test;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;

import java.io.File;

public class SootResolverSimplifiedTest {

    @Test
    public void testGetBlockFromMethod() {
        Scene.v().loadBasicClasses();
        String currentPath = (new File(".")).toURI().getPath();
        Scene.v().extendSootClassPath(currentPath + "src/test/java");
        SootClass sc = Scene.v().forceResolve("de.viadee.bpm.vPAV.processing.SimpleObject", SootClass.SIGNATURES);
        SootMethod method =  sc.getMethodByName("method");
        Block block = SootResolverSimplified.getBlockFromMethod(method);
        Assert.assertEquals(2,  block.getBody().getUnits().size());
    }
}
