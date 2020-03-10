/**
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
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
import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import de.viadee.bpm.vPAV.processing.code.flow.ObjectVariable;
import de.viadee.bpm.vPAV.processing.code.flow.StringVariable;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.*;

// vPAV reworked
// TODO test exception handling e.g. variable does not exist
public class ObjectReaderTest {

    private ObjectReader objectReader;

    private static SootClass thisSootClass;

    private static SootClass anotherSootClass;

    private ObjectVariable objVariable1 = new ObjectVariable();

    private ObjectVariable objVariableField1 = new ObjectVariable();

    private ObjectVariable anotherObject = new ObjectVariable();

    @BeforeClass
    public static void setupSoot() {
        RuntimeConfig.getInstance().setTest(true);
        FileScanner.setupSootClassPaths(new LinkedList<>());
        new JavaReaderStatic().setupSoot();
        Scene.v().loadNecessaryClasses();
        String currentPath = (new File(".")).toURI().getPath();
        Scene.v().extendSootClassPath(currentPath + "src/test/java");
        Scene.v().defaultClassPath();
        thisSootClass = Scene.v().forceResolve("de.viadee.bpm.vPAV.processing.SimpleObject", SootClass.SIGNATURES);
        anotherSootClass = Scene.v()
                .forceResolve("de.viadee.bpm.vPAV.AnotherSimpleObject", SootClass.SIGNATURES);
    }

    @Before
    public void setupTestVariables() {
        ProcessVariablesCreator processVariablesCreator = mock(ProcessVariablesCreator.class);
        when(processVariablesCreator.getScopeId()).thenReturn("Process_1");

        // Create local string variables
        HashMap<String, StringVariable> localStringVariables = new HashMap<>();
        localStringVariables.put("variable1", new StringVariable("value1"));
        localStringVariables.put("variable2", new StringVariable("value2"));

        // Create local objects
        HashMap<String, ObjectVariable> localObjectVariables = new HashMap<>();
        localObjectVariables.put("objVariable1", objVariable1);
        localObjectVariables.put("$r2", anotherObject);
        objectReader = new ObjectReader(localStringVariables, localObjectVariables, new ObjectVariable(), processVariablesCreator);

        // Create string fields
        objectReader.getThisObject().updateStringField("variableField1", "valueField1");

        // Create object fields
        objectReader.getThisObject().putObjectField("objVariableField1", objVariableField1);
    }

    @Test
    public void testGetThisNameFromUnit() {
        ObjectReader cr = new ObjectReader(null);
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
        ObjectReader cr = new ObjectReader(null);
        cr.handleIdentityStmt(stmt, args);
        Assert.assertEquals("myVariableValue", cr.getLocalStringVariables().get("r2").getValue());
    }

    @Test
    public void testResolveStringValue() {
        // Test resolving of String Constants
        Assert.assertEquals("aValue", objectReader.resolveStringValue(StringConstant.v("aValue"), "this"));

        // Test resolving of local string variables
        Assert.assertEquals("value1",
                objectReader
                        .resolveStringValue(new JimpleLocal("variable1", RefType.v("java.lang.String")), "this"));

        // Test resolving of string fields
        Value base = new JimpleLocal("r0", RefType.v("java.lang.String"));
        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn("<de.viadee.package.TestObject: java.lang.String myStringField>");
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "variableField1",
                RefType.v("java.lang.String"), false);
        Value value = new JInstanceFieldRef(base, fieldRef);
        Assert.assertEquals("valueField1", objectReader.resolveStringValue(value, "r0"));
    }

    @Test
    public void testResolveObjectVariable() {
        // Test resolving of local object variables
        Value rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        Assert.assertSame(objVariable1, objectReader.resolveObjectVariable(rightValue));

        // Test resolving of newly created objects
        rightValue = new JNewExpr(RefType.v("java.lang.Object"));
        ObjectVariable ob = objectReader.resolveObjectVariable(rightValue);
        Assert.assertEquals(0, ob.getStringFields().size());
        Assert.assertEquals(0, ob.getObjectFields().size());

        // Test resolving of object fields
        Value base = new JimpleLocal("r0", RefType.v("java.lang.String"));
        SootClass sc = mock(SootClass.class);
        when(sc.getName()).thenReturn("<de.viadee.package.TestObject: java.lang.Object objVariableField1>");
        AbstractSootFieldRef fieldRef = new AbstractSootFieldRef(sc, "objVariableField1",
                RefType.v("java.lang.Object"), false);
        Value value = new JInstanceFieldRef(base, fieldRef);
        Assert.assertSame(objVariableField1, objectReader.resolveObjectVariable(value));
    }

    @Test
    public void testHandleLocalAssignment() {
        Value rightValue, leftValue;

        // Test string assignment
        rightValue = StringConstant.v("aValue");
        leftValue = new JimpleLocal("variable1", RefType.v("java.lang.String"));
        objectReader.handleLocalAssignment(leftValue, rightValue, "this");
        Assert.assertEquals("aValue", objectReader.getLocalStringVariables().get("variable1").getValue());

        // Test object assignment
        rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        leftValue = new JimpleLocal("$r3", RefType.v("java.lang.Object"));
        objectReader.handleLocalAssignment(leftValue, rightValue, "this");
        Assert.assertSame(objVariable1, objectReader.getLocalObjectVariables().get("$r3"));
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
        objectReader.handleFieldAssignment(leftValue, rightValue, "r0");
        Assert.assertEquals("anotherValue",
                objectReader.getThisObject().getStringField("myStringField").getValue());

        // Test object assignment
        rightValue = new JimpleLocal("objVariable1", RefType.v("java.lang.Object"));
        base = new JimpleLocal("r0", RefType.v("java.lang.Object"));
        sc = mock(SootClass.class);
        when(sc.getName()).thenReturn("<de.viadee.package.TestObject: java.lang.Object myObjectField>");
        fieldRef = new AbstractSootFieldRef(sc, "myObjectField",
                RefType.v("java.lang.Object"), false);
        leftValue = new JInstanceFieldRef(base, fieldRef);
        objectReader.handleFieldAssignment(leftValue, rightValue, "r0");
        Assert.assertSame(objVariable1, objectReader.getThisObject().getObjectField("myObjectField"));

        // TODO no difference between static and object attributes -> don't forget, write in readme
    }

    @Test
    public void testHandleInvokeExpr() {
        // Test that invoke expr returns correct value when executed
        SootMethod method = thisSootClass.getMethodByName("methodWithReturn");
        SootMethodRef methodRef = mock(SootMethodRef.class);
        when(methodRef.resolve()).thenReturn(method);
        when(methodRef.declaringClass()).thenReturn(method.getDeclaringClass());
        InvokeExpr invokeExpr = new JVirtualInvokeExpr(new JimpleLocal("this", RefType.v("java.lang.Object")),
                methodRef, new ArrayList<>());
        Object returnValue = objectReader.handleInvokeExpr(invokeExpr, "this");
        Assert.assertEquals("it_works", returnValue);

        // Test that invoke expr manipulates string fields of object
        method = thisSootClass.getMethodByName("method");
        methodRef = mock(SootMethodRef.class);
        when(methodRef.resolve()).thenReturn(method);
        when(methodRef.declaringClass()).thenReturn(method.getDeclaringClass());
        invokeExpr = new JVirtualInvokeExpr(new JimpleLocal("this", RefType.v("java.lang.Object")), methodRef,
                new ArrayList<>());
        returnValue = objectReader.handleInvokeExpr(invokeExpr, "this");
        Assert.assertNull(returnValue);
        Assert.assertEquals("bye", objectReader.getThisObject().getStringField("myStringField").getValue());

        // Test that invoke expr manipulates another object
        method = anotherSootClass.getMethodByName("<init>");
        methodRef = mock(SootMethodRef.class);
        when(methodRef.resolve()).thenReturn(method);
        when(methodRef.declaringClass()).thenReturn(method.getDeclaringClass());
        invokeExpr = new JVirtualInvokeExpr(
                new JimpleLocal("$r2", RefType.v("de.viadee.bpm.vPAV.AnotherSimpleObject")), methodRef,
                new ArrayList<>());
        returnValue = objectReader.handleInvokeExpr(invokeExpr, "this");
        Assert.assertNull(returnValue);
        Assert.assertEquals("it's_snowing",
                objectReader.getLocalObjectVariables().get("$r2").getStringField("anotherVariable").getValue());
        // TODO did I checked invoke expressions with parameters?
    }

    @Test
    public void testHandleReturnStmt() {
        // Test String return
        ReturnStmt stringStmt = new JReturnStmt(new JimpleLocal("variable1", RefType.v("java.lang.String")));
        Assert.assertEquals("value1", objectReader.handleReturnStmt(stringStmt, "this"));

        // Test Object return
        ReturnStmt objectStmt = new JReturnStmt(new JimpleLocal("objVariable1", RefType.v("java.lang.Object")));
        Assert.assertSame(objVariable1, objectReader.handleReturnStmt(objectStmt, "this"));
    }

    @Test
    public void testHandleIfStmt() {
        // Test that all units are correctly processed in the right order
        SootMethod method = thisSootClass.getMethodByName("methodWithIf");
        List<Value> args = new ArrayList<>();
        args.add(new JimpleLocal("del_ex", RefType.v(CamundaMethodServices.DELEGATE)));
        objectReader.handleIfStmt(SootResolverSimplified.getBlockFromMethod(method), args, "r0");
        // TODO darauf hinweisen, dass variable manipulation in ifs oder loops zufällige ergebnisse liefert (immer abarbeitung von if und else)
        // Only check that all variables were processed
        Assert.assertEquals("notAvailableOutsideIf", objectReader.getLocalStringVariables().get("r4").getValue());
        Assert.assertEquals("notAvailableOutsideElse", objectReader.getLocalStringVariables().get("r5").getValue());
        Assert.assertEquals("afterIfElse", objectReader.getLocalStringVariables().get("r6").getValue());

    }

    @Test
    public void testProcessBlock() {
        // Test that all units are correctly processed in the right order
        SootMethod method = thisSootClass.getMethodByName("<init>");
        List<Value> args = new ArrayList<>();
        args.add(StringConstant.v("passedValue"));
        Object returnValue = objectReader.processBlock(SootResolverSimplified.getBlockFromMethod(method), args, null);
        Assert.assertNull(returnValue);
        ObjectVariable simpleObject = objectReader.getThisObject();
        Assert.assertEquals("bye", simpleObject.getStringField("myStringField").getValue());
        Assert.assertEquals("passedValue", simpleObject.getStringField("parameterString").getValue());
        Assert.assertEquals("it's_snowing", simpleObject.getStringField("anotherObjectString").getValue());
    }

    @Test
    public void testCreateProcessVariableOperationFromInvocation() {
        // TODO how to write this better without mocking so much?
        // Test that getVariable() method is correctly translated to a ProcessVariableOperation
        SootClass delegateExecutionClass = Scene.v().makeSootClass("org.camunda.bpm.engine.delegate.DelegateExecution");
        SootMethod method = mock(SootMethod.class);
        when(method.getName()).thenReturn("getVariable");
        when(method.getDeclaringClass()).thenReturn(delegateExecutionClass);
        InvokeExpr invokeExpr = mock(JInterfaceInvokeExpr.class);
        when(invokeExpr.getArgCount()).thenReturn(1);
        when(invokeExpr.getArgBox(0)).thenReturn(Jimple.v().newImmediateBox(StringConstant.v("processVariable")));
        when(invokeExpr.getMethod()).thenReturn(method);

        ProcessVariableOperation pvo = objectReader.createProcessVariableOperationFromInvocation(invokeExpr);
        Assert.assertEquals("processVariable", pvo.getName());
        Assert.assertEquals(VariableOperation.READ, pvo.getOperation());

        // Test setVariable()
        when(invokeExpr.getArgCount()).thenReturn(2);
        when(invokeExpr.getArgBox(0)).thenReturn(Jimple.v().newImmediateBox(StringConstant.v("processVariable")));
        when(method.getName()).thenReturn("setVariable");

        pvo = objectReader.createProcessVariableOperationFromInvocation(invokeExpr);
        Assert.assertEquals("processVariable", pvo.getName());
        Assert.assertEquals(VariableOperation.WRITE, pvo.getOperation());

        // Test removeVariable()
        when(invokeExpr.getArgCount()).thenReturn(1);
        when(invokeExpr.getArgBox(0)).thenReturn(Jimple.v().newImmediateBox(StringConstant.v("processVariable")));
        when(method.getName()).thenReturn("removeVariable");

        pvo = objectReader.createProcessVariableOperationFromInvocation(invokeExpr);
        Assert.assertEquals("processVariable", pvo.getName());
        Assert.assertEquals(VariableOperation.DELETE, pvo.getOperation());
    }
}
