/*
 * BSD 3-Clause License
 *
 * Copyright © 2020, viadee Unternehmensberatung AG
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.ProcessVariablesCreator;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.SootResolverSimplified;
import de.viadee.bpm.vPAV.processing.code.flow.MapVariable;
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
import java.util.*;

import static org.mockito.Mockito.*;

public class ObjectReaderTest {

    private ObjectReader objectReader;

    private static SootClass thisSootClass;

    private static SootClass anotherSootClass;

    private static SootClass anonymousInnerSootClass;

    private static String testClassName = "de.viadee.package.TestObject";

    private ObjectVariable objVariable1 = new ObjectVariable();

    private ObjectVariable objVariableField1 = new ObjectVariable();

    private ObjectVariable staticObjField = new ObjectVariable();

    private ObjectVariable anotherObject = new ObjectVariable();

    private HashMap<String, StringVariable> localStringVariables;

    private HashMap<String, ObjectVariable> localObjectVariables;

    @BeforeClass
    public static void setupSoot() {
        RuntimeConfig.getInstance().setTest(true);
        FileScanner.setupSootClassPaths(new LinkedList<>());
        JavaReaderStatic.setupSoot();
        Scene.v().loadNecessaryClasses();
        String currentPath = (new File(".")).toURI().getPath();
        Scene.v().extendSootClassPath(currentPath + "src/test/java");
        Scene.v().defaultClassPath();
        thisSootClass = Scene.v().forceResolve("de.viadee.bpm.vPAV.processing.SimpleObject", SootClass.SIGNATURES);
        anotherSootClass = Scene.v()
                .forceResolve("de.viadee.bpm.vPAV.AnotherSimpleObject", SootClass.SIGNATURES);
        anonymousInnerSootClass = Scene.v()
                .forceResolve("de.viadee.bpm.vPAV.delegates.TestDelegateAnonymousInnerClass", SootClass.SIGNATURES);
    }

    @Before
    public void setupTestVariables() {
        ProcessVariablesCreator processVariablesCreator = mock(ProcessVariablesCreator.class);
        when(processVariablesCreator.getScopeId()).thenReturn("Process_1");

        // Create local string variables
        localStringVariables = new HashMap<>();
        localStringVariables.put("variable1", new StringVariable("value1"));
        localStringVariables.put("variable2", new StringVariable("value2"));

        // Create local objects
        localObjectVariables = new HashMap<>();
        localObjectVariables.put("objVariable1", objVariable1);
        localObjectVariables.put("$r2", anotherObject);
        objectReader = new ObjectReader(new ObjectVariable(),
                processVariablesCreator, thisSootClass);

        // Create string fields
        objectReader.getThisObject().updateStringField("variableField1", "valueField1");

        // Create object fields
        objectReader.getThisObject().putObjectField("objVariableField1", objVariableField1);

        // Create static fields
        ObjectVariable staticTestClass = new ObjectVariable();
        staticTestClass.updateStringField("staticStringField", "valueStaticStringField");
        staticTestClass.putObjectField("staticObjectField", staticObjField);
        objectReader.getStaticObjectVariables().put(testClassName, staticTestClass);
    }

    @Test
    public void testGetThisNameFromUnit() {
        ObjectReader cr = new ObjectReader(null, thisSootClass);
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
        ArrayList<Object> argValues = objectReader.resolveArgs(args, null, localStringVariables, localObjectVariables);
        ObjectReader cr = new ObjectReader(null, thisSootClass);
        cr.handleIdentityStmt(stmt, argValues, localStringVariables, localObjectVariables);
        Assert.assertEquals("myVariableValue", localStringVariables.get("r2").getValue());
    }

    @Test
    public void testResolveStringValue() {
        // Test resolving of String Constants
        Assert.assertEquals("aValue", objectReader.resolveStringValue(null, StringConstant.v("aValue"), "this",
                localStringVariables, localObjectVariables));

        // Test resolving of local string variables
        Assert.assertEquals("value1",
                objectReader
                        .resolveStringValue(null, new JimpleLocal("variable1", RefType.v("java.lang.String")), "this",
                                localStringVariables, localObjectVariables));

        // Test resolving of string fields
        Value base = new JimpleLocal("r0", RefType.v("java.lang.String"));
        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn(testClassName);
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "variableField1",
                RefType.v("java.lang.String"), false);
        Value value = new JInstanceFieldRef(base, fieldRef);
        Assert.assertEquals("valueField1", objectReader.resolveStringValue(null, value, "r0", localStringVariables,
                localObjectVariables));

        // Test resolving of static fields
        when(sc.getName()).thenReturn(testClassName);
        fieldRef = new AbstractSootFieldRef(sc, "staticStringField",
                RefType.v("java.lang.String"), true);
        StaticFieldRef valueStatic = mock(StaticFieldRef.class, CALLS_REAL_METHODS);
        valueStatic.setFieldRef(fieldRef);
        Assert.assertEquals("valueStaticStringField", objectReader.resolveStringValue(null, valueStatic, null,
                localStringVariables, localObjectVariables));
    }

    @Test
    public void testResolveObjectVariable() {
        // Test resolving of local object variables
        Value rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        Assert.assertSame(objVariable1, objectReader.resolveObjectVariable(null, rightValue, "", localStringVariables,
                localObjectVariables));

        // Test resolving of newly created objects
        rightValue = new JNewExpr(RefType.v("java.lang.Object"));
        ObjectVariable ob = objectReader.resolveObjectVariable(null, rightValue, "", localStringVariables,
                localObjectVariables);
        Assert.assertEquals(0, ob.getStringFields().size());
        Assert.assertEquals(0, ob.getObjectFields().size());

        // Test resolving of object fields
        Value base = new JimpleLocal("r0", RefType.v("java.lang.String"));
        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn(testClassName);
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "objVariableField1",
                RefType.v("java.lang.Object"), false);
        Value value = new JInstanceFieldRef(base, fieldRef);
        Assert.assertSame(objVariableField1, objectReader.resolveObjectVariable(null, value, "r0", localStringVariables,
                localObjectVariables));

        // Test resolving of static fields
        when(sc.getName()).thenReturn(testClassName);
        fieldRef = new AbstractSootFieldRef(sc, "staticObjectField",
                RefType.v("java.lang.String"), true);
        StaticFieldRef valueStatic = mock(StaticFieldRef.class, CALLS_REAL_METHODS);
        valueStatic.setFieldRef(fieldRef);
        Assert.assertSame(staticObjField, objectReader.resolveObjectVariable(null, valueStatic, null,
                localStringVariables, localObjectVariables));
    }

    @Test
    public void testMapVariableOperations() {
        // Test that all units are correctly processed in the right order
        SootMethod method = thisSootClass.getMethodByName("mapMethod");
        MapVariable map = (MapVariable) objectReader.processBlock(SootResolverSimplified.getBlockFromMethod(method), new ArrayList<>(), null);
        Assert.assertEquals("Map should contain exactly 2 variables.", 2, map.getValues().size());
        Assert.assertNull("Map should not contain variable 'first'.", map.getValues().get("first"));
        Assert.assertNotNull("Map should contain variable 'second'.", map.getValues().get("second"));
        Assert.assertNotNull("Map should contain variable 'third'.", map.getValues().get("third"));
    }

    @Test
    public void testHandleLocalAssignment() {
        Value rightValue, leftValue;

        // Test string assignment
        rightValue = StringConstant.v("aValue");
        leftValue = new JimpleLocal("variable1", RefType.v("java.lang.String"));
        objectReader.handleLocalAssignment(null, leftValue, rightValue, "this", localStringVariables,
                localObjectVariables);
        Assert.assertEquals("aValue", localStringVariables.get("variable1").getValue());

        // Test object assignment
        rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        leftValue = new JimpleLocal("$r3", RefType.v("java.lang.Object"));
        objectReader.handleLocalAssignment(null, leftValue, rightValue, "this", localStringVariables,
                localObjectVariables);
        Assert.assertSame(objVariable1, localObjectVariables.get("$r3"));
    }

    @Test
    public void testHandleFieldAssignment() {
        Value rightValue, leftValue;

        // Test string assignment
        rightValue = StringConstant.v("anotherValue");
        Value base = new JimpleLocal("r0", RefType.v("java.lang.Object"));
        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn(testClassName);
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "myStringField",
                RefType.v("java.lang.String"), false);
        leftValue = new JInstanceFieldRef(base, fieldRef);
        objectReader.handleFieldAssignment(null, leftValue, rightValue, "r0", localStringVariables,
                localObjectVariables);
        Assert.assertEquals("anotherValue",
                objectReader.getThisObject().getStringField("myStringField").getValue());

        // Test object assignment
        rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        base = new JimpleLocal("r0", RefType.v("java.lang.Object"));
        sc = mock(SootClass.class);
        when(sc.getName()).thenReturn(testClassName);
        fieldRef = new AbstractSootFieldRef(sc, "myObjectField",
                RefType.v("java.lang.Object"), false);
        leftValue = new JInstanceFieldRef(base, fieldRef);
        objectReader.handleFieldAssignment(null, leftValue, rightValue, "r0", localStringVariables,
                localObjectVariables);
        Assert.assertSame(objVariable1, objectReader.getThisObject().getObjectField("myObjectField"));
    }

    @Test
    public void testHandleStaticFieldAssignment() {
        Value rightValue;

        // Test string assignment
        rightValue = StringConstant.v("anotherValue");

        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn(testClassName);
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "myStaticStringField",
                RefType.v("java.lang.String"), true);
        StaticFieldRef leftValue = mock(StaticFieldRef.class, CALLS_REAL_METHODS);
        leftValue.setFieldRef(fieldRef);
        objectReader.handleStaticFieldAssigment(null, leftValue, rightValue, null, localStringVariables,
                localObjectVariables);
        Assert.assertEquals("anotherValue",
                objectReader.getStaticObjectVariables().get(testClassName).getStringField("myStaticStringField")
                        .getValue());

        // Test object assignment
        rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        fieldRef = new AbstractSootFieldRef(sc, "myStaticObjectField",
                RefType.v("java.lang.Object"), true);
        leftValue = mock(StaticFieldRef.class, CALLS_REAL_METHODS);
        leftValue.setFieldRef(fieldRef);
        objectReader.handleStaticFieldAssigment(null, leftValue, rightValue, null, localStringVariables,
                localObjectVariables);
        Assert.assertSame(objVariable1,
                objectReader.getStaticObjectVariables().get(testClassName).getObjectField("myStaticObjectField"));
    }

    @Test
    public void testHandleInvokeExpr() {
        // Test that invoke expr returns correct value when executed
        SootMethod method = thisSootClass.getMethodByName("methodWithReturn");
        SootMethodRef methodRef = mock(SootMethodRef.class);
        when(methodRef.resolve()).thenReturn(method);
        when(methodRef.getName()).thenReturn("methodWithReturn");
        when(methodRef.declaringClass()).thenReturn(method.getDeclaringClass());
        when(methodRef.getDeclaringClass()).thenReturn(method.getDeclaringClass());
        InvokeExpr invokeExpr = new JVirtualInvokeExpr(new JimpleLocal("this", RefType.v("java.lang.Object")),
                methodRef, new ArrayList<>());
        Object returnValue = objectReader.handleInvokeExpr(null, invokeExpr, "this", localStringVariables,
                localObjectVariables);
        Assert.assertEquals("it_works", returnValue);

        // Test that invoke expr manipulates string fields of object
        method = thisSootClass.getMethodByName("method");
        methodRef = mock(SootMethodRef.class);
        when(methodRef.resolve()).thenReturn(method);
        when(methodRef.declaringClass()).thenReturn(method.getDeclaringClass());
        when(methodRef.getDeclaringClass()).thenReturn(method.getDeclaringClass());
        invokeExpr = new JVirtualInvokeExpr(new JimpleLocal("this", RefType.v("java.lang.Object")), methodRef,
                new ArrayList<>());
        returnValue = objectReader.handleInvokeExpr(null, invokeExpr, "this", localStringVariables,
                localObjectVariables);
        Assert.assertNull(returnValue);
        Assert.assertEquals("bye", objectReader.getThisObject().getStringField("myStringField").getValue());

        // Test that invoke expr manipulates another object
        method = anotherSootClass.getMethodByName("<init>");
        methodRef = mock(SootMethodRef.class);
        when(methodRef.resolve()).thenReturn(method);
        when(methodRef.declaringClass()).thenReturn(method.getDeclaringClass());
        when(methodRef.getDeclaringClass()).thenReturn(method.getDeclaringClass());
        invokeExpr = new JVirtualInvokeExpr(
                new JimpleLocal("$r2", RefType.v("de.viadee.bpm.vPAV.AnotherSimpleObject")), methodRef,
                new ArrayList<>());
        returnValue = objectReader.handleInvokeExpr(null, invokeExpr, "this", localStringVariables,
                localObjectVariables);
        Assert.assertNull(returnValue);
        Assert.assertEquals("it's_snowing",
                localObjectVariables.get("$r2").getStringField("anotherVariable").getValue());

        // Test that invoke expr manipulates string fields of object when passing a value
        method = thisSootClass.getMethodByName("methodWithParameter");
        methodRef = mock(SootMethodRef.class);
        when(methodRef.resolve()).thenReturn(method);
        when(methodRef.declaringClass()).thenReturn(method.getDeclaringClass());
        when(methodRef.getDeclaringClass()).thenReturn(method.getDeclaringClass());
        ArrayList<Value> args = new ArrayList<>();
        args.add(new JimpleLocal("localString", RefType.v("java.lang.String")));
        invokeExpr = new JVirtualInvokeExpr(new JimpleLocal("this", RefType.v("java.lang.Object")), methodRef,
                args);
        localStringVariables.put("localString", new StringVariable("stringValue"));
        returnValue = objectReader.handleInvokeExpr(null, invokeExpr, "this", localStringVariables,
                localObjectVariables);
        Assert.assertNull(returnValue);
        Assert.assertEquals("stringValue", objectReader.getThisObject().getStringField("parameterString").getValue());
    }

    @Test
    public void testHandleReturnStmt() {
        // Test String return
        ReturnStmt stringStmt = new JReturnStmt(new JimpleLocal("variable1", RefType.v("java.lang.String")));
        Assert.assertEquals("value1", objectReader.handleReturnStmt(null, stringStmt, "this", localStringVariables,
                localObjectVariables));

        // Test Object return
        ReturnStmt objectStmt = new JReturnStmt(new JimpleLocal("objVariable1", RefType.v("java.lang.Object")));
        Assert.assertSame(objVariable1, objectReader.handleReturnStmt(null, objectStmt, "this", localStringVariables,
                localObjectVariables));
    }

    @Test
    public void testProcessBlock() {
        // Test that all units are correctly processed in the right order
        SootMethod method = thisSootClass.getMethodByName("<init>");
        List<Value> args = new ArrayList<>();
        args.add(StringConstant.v("passedValue"));
        List<Object> argValues = new ArrayList<>();
        argValues.add("passedValue");
        Object returnValue = objectReader
                .processBlock(SootResolverSimplified.getBlockFromMethod(method), argValues, null);
        Assert.assertNull(returnValue);
        ObjectVariable simpleObject = objectReader.getThisObject();
        Assert.assertEquals("bye", simpleObject.getStringField("myStringField").getValue());
        Assert.assertEquals("passedValue", simpleObject.getStringField("parameterString").getValue());
        Assert.assertEquals("it's_snowing", simpleObject.getStringField("anotherObjectString").getValue());
    }

    @Test
    public void testResolveAnonymousInnerClasses() {
        // Test that the anonymous inner class is correctly resolved
        SootMethod method = anonymousInnerSootClass.getMethodByName("execute");
        Block block = SootResolverSimplified.getBlockFromMethod(method);

        // Load invoke expression
        Iterator<Unit> iter = block.getBody().getUnits().iterator();
        iter.next();
        iter.next();
        objectReader.handleAssignStmt(block, iter.next(), "this", localStringVariables, localObjectVariables);
        Assert.assertNotNull(localObjectVariables.get("$r0").getImplementation());
    }

    @Test
    public void testResolveArgs() {
        ArrayList<Value> args = new ArrayList<>();
        args.add(new JimpleLocal("r1", RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution")));
        args.add(StringConstant.v("myVariableValue"));
        args.add(new JimpleLocal("objVariable1", RefType.v("java.lang.Object")));
        ArrayList<Object> argValues = objectReader.resolveArgs(args, "this", localStringVariables, localObjectVariables);

        Assert.assertNull(argValues.get(0));
        Assert.assertEquals("myVariableValue", argValues.get(1));
        Assert.assertSame(objVariable1, argValues.get(2));
    }
}
