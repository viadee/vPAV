package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.processing.code.flow.ObjectVariable;
import de.viadee.bpm.vPAV.processing.code.flow.StringVariable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import soot.AbstractSootFieldRef;
import soot.RefType;
import soot.SootClass;
import soot.Value;
import soot.jimple.ParameterRef;
import soot.jimple.StringConstant;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JimpleLocal;

import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConstructorReaderTest {

    private ConstructorReader constructorReader;

    private ObjectVariable objVariable1 = new ObjectVariable();

    @Before
    public void setupTestVariables() {
        // Create local string variables
        HashMap<String, StringVariable> localStringVariables = new HashMap<>();
        localStringVariables.put("variable1", new StringVariable("value1"));
        localStringVariables.put("variable2", new StringVariable("value2"));

        // Create local objects
        HashMap<String, ObjectVariable> localObjectVariables = new HashMap<>();
        localObjectVariables.put("objVariable1", objVariable1);
        constructorReader = new ConstructorReader(null, null, localStringVariables, localObjectVariables);
    }

    @Test
    public void testHandleIdentityStmt() {
        // Tests that string parameters are included as local variables
        Value localVal = new JimpleLocal("r2", RefType.v("java.lang.String"));
        Value identityVal = new ParameterRef(RefType.v("java.lang.String"), 1);
        JIdentityStmt stmt = new JIdentityStmt(localVal, identityVal);

        ArrayList<Value> args = new ArrayList<>();
        args.add(new JimpleLocal("r1", RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution")));
        args.add(StringConstant.v("myVariableValue"));
        ConstructorReader cr = new ConstructorReader(null, args);
        cr.handleIdentityStmt(stmt);
        Assert.assertEquals("myVariableValue", cr.getLocalStringVariables().get("r2").getValue());
    }

    @Test
    public void testResolveStringValue() {
        // Test resolving of String Constants
        Assert.assertEquals("aValue", constructorReader.resolveStringValue(StringConstant.v("aValue")));

        // Test resolving of local string variables
        Assert.assertEquals("value1",
                constructorReader.resolveStringValue(new JimpleLocal("variable1", RefType.v("java.lang.String"))));
    }

    @Test
    public void testResolveObjectVariable() {
        // Test resolving of local object variables
        Value rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        Assert.assertSame(objVariable1, constructorReader.resolveObjectVariable(rightValue));

        // Test resolving of newly created objects
        rightValue = new JNewExpr(RefType.v("java.lang.Object"));
        ObjectVariable ob = constructorReader.resolveObjectVariable(rightValue);
        Assert.assertEquals(0, ob.getStringFields().size());
        Assert.assertEquals(0, ob.getObjectFields().size());
    }

    @Test
    public void testHandleLocalAssignment() {
        Value rightValue, leftValue;

        // Test string assignment
        rightValue = StringConstant.v("aValue");
        leftValue = new JimpleLocal("variable1", RefType.v("java.lang.String"));
        constructorReader.handleLocalAssignment(leftValue, rightValue);
        Assert.assertEquals("aValue", constructorReader.getLocalStringVariables().get("variable1").getValue());

        // Test object assignment
        rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        leftValue = new JimpleLocal("$r3", RefType.v("java.lang.Object"));
        constructorReader.handleLocalAssignment(leftValue, rightValue);
        Assert.assertSame(objVariable1, constructorReader.getLocalObjectVariables().get("$r3"));
    }

    @Test
    public void testHandleFieldAssignment() {
        Value rightValue, leftValue;

        // Test string assignment
        rightValue = StringConstant.v("anotherValue");
        Value base = new JimpleLocal("r0", RefType.v("java.lang.Object"));
        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn("<de.viadee.package.TestObject: java.langObject myStringField>");
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "myStringField",
                RefType.v("java.lang.String"), false);
        leftValue = new JInstanceFieldRef(base, fieldRef);
        constructorReader.handleFieldAssignment(leftValue, rightValue);
        Assert.assertEquals("anotherValue", constructorReader.getThisObject().getStringField("myStringField").getValue());

        // Test object assignment
        // TODO add test
    }
}
