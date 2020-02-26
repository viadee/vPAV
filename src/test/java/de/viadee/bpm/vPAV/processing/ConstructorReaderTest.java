package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.SootResolverSimplified;
import de.viadee.bpm.vPAV.processing.code.flow.ObjectVariable;
import de.viadee.bpm.vPAV.processing.code.flow.StringVariable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.Block;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.mockito.Mockito.*;

// vPAV reworked
// TODO test exception handling e.g. variable does not exist
public class ConstructorReaderTest {

    private ConstructorReader constructorReader;

    private static SootClass sootClass;

    private ObjectVariable objVariable1 = new ObjectVariable();

    private ObjectVariable objVariableField1 = new ObjectVariable();

    @BeforeClass
    public static void setupSoot() {
        Scene.v().loadBasicClasses();
        String currentPath = (new File(".")).toURI().getPath();
        Scene.v().extendSootClassPath(currentPath + "src/test/java");
        sootClass = Scene.v().forceResolve("de.viadee.bpm.vPAV.processing.SimpleObject", SootClass.SIGNATURES);
    }

    @Before
    public void setupTestVariables() {
        // Create local string variables
        HashMap<String, StringVariable> localStringVariables = new HashMap<>();
        localStringVariables.put("variable1", new StringVariable("value1"));
        localStringVariables.put("variable2", new StringVariable("value2"));

        // Create local objects
        HashMap<String, ObjectVariable> localObjectVariables = new HashMap<>();
        localObjectVariables.put("objVariable1", objVariable1);
        constructorReader = new ConstructorReader(localStringVariables, localObjectVariables, new ObjectVariable());

        // Create string fields
        constructorReader.getThisObject().updateStringField("variableField1", "valueField1");

        // Create object fields
        constructorReader.getThisObject().putObjectField("objVariableField1", objVariableField1);
    }

    @Test
    public void testGetThisNameFromUnit() {
        ConstructorReader cr = new ConstructorReader();
        // Test that name of reference to this object is correctly resolved
        Value localVal = new JimpleLocal("this", RefType.v("java.lang.Object"));
        Value identityVal = new ThisRef(RefType.v("java.lang.Object"));
        JIdentityStmt stmt = new JIdentityStmt(localVal, identityVal);
        Assert.assertEquals("this", cr.getThisNameFromUnit(stmt));
    }

    @Test
    public void testHandleIdentityStmt() {
        // Test that string parameters are included as local variables
        Value localVal = new JimpleLocal("r2", RefType.v("java.lang.String"));
        Value identityVal = new ParameterRef(RefType.v("java.lang.String"), 1);
        JIdentityStmt stmt = new JIdentityStmt(localVal, identityVal);

        ArrayList<Value> args = new ArrayList<>();
        args.add(new JimpleLocal("r1", RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution")));
        args.add(StringConstant.v("myVariableValue"));
        ConstructorReader cr = new ConstructorReader();
        cr.handleIdentityStmt(stmt, args);
        Assert.assertEquals("myVariableValue", cr.getLocalStringVariables().get("r2").getValue());
    }

    @Test
    public void testResolveStringValue() {
        // Test resolving of String Constants
        Assert.assertEquals("aValue", constructorReader.resolveStringValue(StringConstant.v("aValue"), "this"));

        // Test resolving of local string variables
        Assert.assertEquals("value1",
                constructorReader
                        .resolveStringValue(new JimpleLocal("variable1", RefType.v("java.lang.String")), "this"));

        // Test resolving of string fields
        Value base = new JimpleLocal("r0", RefType.v("java.lang.String"));
        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn("<de.viadee.package.TestObject: java.lang.String myStringField>");
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "variableField1",
                RefType.v("java.lang.String"), false);
        Value value = new JInstanceFieldRef(base, fieldRef);
        Assert.assertEquals("valueField1", constructorReader.resolveStringValue(value, "r0"));
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

        // Test resolving of object fields
        Value base = new JimpleLocal("r0", RefType.v("java.lang.String"));
        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn("<de.viadee.package.TestObject: java.lang.Object objVariableField1>");
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "objVariableField1",
                RefType.v("java.lang.Object"), false);
        Value value = new JInstanceFieldRef(base, fieldRef);
        Assert.assertSame(objVariableField1, constructorReader.resolveObjectVariable(value));
    }

    @Test
    public void testHandleLocalAssignment() {
        Value rightValue, leftValue;

        // Test string assignment
        rightValue = StringConstant.v("aValue");
        leftValue = new JimpleLocal("variable1", RefType.v("java.lang.String"));
        constructorReader.handleLocalAssignment(leftValue, rightValue, "this");
        Assert.assertEquals("aValue", constructorReader.getLocalStringVariables().get("variable1").getValue());

        // Test object assignment
        rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        leftValue = new JimpleLocal("$r3", RefType.v("java.lang.Object"));
        constructorReader.handleLocalAssignment(leftValue, rightValue, "this");
        Assert.assertSame(objVariable1, constructorReader.getLocalObjectVariables().get("$r3"));
    }

    @Test
    public void testHandleFieldAssignment() {
        Value rightValue, leftValue;

        // Test string assignment
        rightValue = StringConstant.v("anotherValue");
        Value base = new JimpleLocal("r0", RefType.v("java.lang.Object"));
        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn("<de.viadee.package.TestObject: java.lang.String myStringField>");
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "myStringField",
                RefType.v("java.lang.String"), false);
        leftValue = new JInstanceFieldRef(base, fieldRef);
        constructorReader.handleFieldAssignment(leftValue, rightValue, "r0");
        Assert.assertEquals("anotherValue",
                constructorReader.getThisObject().getStringField("myStringField").getValue());

        // Test object assignment
        rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        base = new JimpleLocal("r0", RefType.v("java.lang.Object"));
        sc = mock(SootClass.class);
        when(sc.getName()).thenReturn("<de.viadee.package.TestObject: java.lang.Object myObjectField>");
        fieldRef = new AbstractSootFieldRef(sc, "myObjectField",
                RefType.v("java.lang.Object"), false);
        leftValue = new JInstanceFieldRef(base, fieldRef);
        constructorReader.handleFieldAssignment(leftValue, rightValue, "r0");
        Assert.assertSame(objVariable1, constructorReader.getThisObject().getObjectField("myObjectField"));

        // TODO no difference between static and object attributes -> don't forget, write in readme
    }

    @Test
    public void testHandleInvokeExpr() {
        // Test that invoke expr returns correct value when executed
        SootMethod method = sootClass.getMethodByName("methodWithReturn");
        SootMethodRef methodRef = mock(SootMethodRef.class);
        when(methodRef.resolve()).thenReturn(method);
        when(methodRef.declaringClass()).thenReturn(method.getDeclaringClass());
        InvokeExpr invokeExpr = new JVirtualInvokeExpr(new JimpleLocal("this", RefType.v("java.lang.Object")) , methodRef, new ArrayList<>());
        String returnValue = (String) constructorReader.handleInvokeExpr(invokeExpr, "this");
        Assert.assertEquals("it_works", returnValue);

        // Test that invoke expr manipulates string fields of object
        method = sootClass.getMethodByName("method");
        methodRef = mock(SootMethodRef.class);
        when(methodRef.resolve()).thenReturn(method);
        when(methodRef.declaringClass()).thenReturn(method.getDeclaringClass());
        invokeExpr = new JVirtualInvokeExpr(new JimpleLocal("this", RefType.v("java.lang.Object")) , methodRef, new ArrayList<>());
        returnValue = (String) constructorReader.handleInvokeExpr(invokeExpr, "this");
        Assert.assertNull(returnValue);
        Assert.assertEquals("bye", constructorReader.getThisObject().getStringField("myStringField").getValue());
    }

    @Test
    public void testHandleReturnStmt() {
        // Test String return
        ReturnStmt stringStmt = new JReturnStmt(new JimpleLocal("variable1", RefType.v("java.lang.String")));
        Assert.assertEquals("value1", constructorReader.handleReturnStmt(stringStmt, "this"));

        // Test Object return
        ReturnStmt objectStmt = new JReturnStmt(new JimpleLocal("objVariable1", RefType.v("java.lang.Object")));
        Assert.assertSame(objVariable1, constructorReader.handleReturnStmt(objectStmt, "this"));
    }
}
