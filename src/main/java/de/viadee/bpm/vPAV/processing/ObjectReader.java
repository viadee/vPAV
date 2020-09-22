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

import de.viadee.bpm.vPAV.SootResolverSimplified;
import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.model.data.CamundaEntryPointFunctions;
import de.viadee.bpm.vPAV.processing.model.data.CamundaProcessVariableFunctions;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.Block;

import java.util.*;

import static de.viadee.bpm.vPAV.processing.model.data.CamundaProcessVariableFunctions.PutValue;
import static de.viadee.bpm.vPAV.processing.model.data.CamundaProcessVariableFunctions.PutValueTyped;

public class ObjectReader {

    private HashSet<Integer> processedBlocks = new HashSet<>();

    private ObjectVariable thisObject = new ObjectVariable();

    private HashMap<String, StringVariable> localStringVariables = new HashMap<>();

    private HashMap<String, ObjectVariable> localObjectVariables = new HashMap<>();

    private static HashMap<String, ObjectVariable> staticObjectVariables = new HashMap<>();

    private static final RefType StringType = RefType.v("java.lang.String");

    private ObjectReaderReceiver objectReaderReceiver;

    public BasicNode returnNode;

    private SootClass currentJavaClass;

    private String currentMethod;

    // Only used for testing purposes
    ObjectReader(HashMap<String, StringVariable> localStrings,
            HashMap<String, ObjectVariable> localObjects, ObjectVariable thisObject,
            ObjectReaderReceiver objectReaderReceiver, SootClass currentJavaClass) {
        this.localStringVariables = localStrings;
        this.localObjectVariables = localObjects;
        this.thisObject = thisObject;
        this.objectReaderReceiver = objectReaderReceiver;
        this.currentJavaClass = currentJavaClass;
    }

    /**
     * onstructor that is called the first time when starting an analysis.
     *
     * @param objectReaderReceiver that is used for creating the data flow graph
     * @param currentJavaClass     that contains the block which will be analyzed
     */
    public ObjectReader(ObjectReaderReceiver objectReaderReceiver, SootClass currentJavaClass) {
        this.objectReaderReceiver = objectReaderReceiver;
        this.currentJavaClass = currentJavaClass;
    }

    /**
     * Constructor that is called the first time when starting an analysis.
     *
     * @param objectReaderReceiver that is used for creating the data flow graph
     * @param currentJavaClass     that contains the block which will be analyzed
     * @param currentMethod        that contains thte block which will be analyzed
     */
    public ObjectReader(ObjectReaderReceiver objectReaderReceiver, SootClass currentJavaClass,
            String currentMethod) {
        this.objectReaderReceiver = objectReaderReceiver;
        this.currentJavaClass = currentJavaClass;
        this.currentMethod = currentMethod;
    }

    /**
     * Constructor that is called when another block is entered during the analysis.
     *
     * @param objectReaderReceiver that is used for creating the data flow graph
     * @param thisObject           ObjectVariable that refers to the object that contains the block
     */
    private ObjectReader(ObjectReaderReceiver objectReaderReceiver, ObjectVariable thisObject,
            SootClass currentJavaClass, String sootMethod) {
        this.objectReaderReceiver = objectReaderReceiver;
        this.thisObject = thisObject;
        this.currentJavaClass = currentJavaClass;
        this.currentMethod = sootMethod;
    }

    /**
     * Loops through the units of a block and notifies the ProcessVariablesCreator of used process variables.
     *
     * @param block     the block that is processed
     * @param args      the types of arguments that were passed to the method and are available in the block
     * @param argValues the argument values (or in case of objects, references to the variable)
     * @param thisName  the local name of the current object
     * @return ObjectVariable / StringVariable that is returned by the method
     */
    public Object processBlock(Block block, List<Value> args, List<Object> argValues, String thisName) {

        if (block == null) {
            return null;
        }

        // Eventually add objects, arrays and int
        if (processedBlocks.contains(hashBlock(block))) {
            objectReaderReceiver.visitBlockAgain(block);
            return null;
        }
        processedBlocks.add(hashBlock(
                block));

        final Iterator<Unit> unitIt = block.iterator();

        if (thisName == null) {
            // Find out variable name of this reference, it's always defined in the first unit
            // If not, it´s a static method
            if (block.iterator().next() instanceof IdentityStmt) {
                thisName = getThisNameFromUnit(unitIt.next());
            }
        }

        Unit unit;
        while (unitIt.hasNext()) {
            unit = unitIt.next();
            // e. g. r2 := @parameter1: org.camunda.bpm.engine.delegate.DelegateExecution
            if (unit instanceof IdentityStmt) {
                handleIdentityStmt(unit, args, argValues);
            }
            // e. g. $r2 = staticinvoke ... (Assignment)
            else if (unit instanceof AssignStmt) {
                handleAssignStmt(block, unit, thisName);
            }
            // e. g. specialinvoke $r3.<de.viadee.bpm ... (Constuctor call of new object)
            else if (unit instanceof InvokeStmt) {
                InvokeExpr expr = ((InvokeStmt) unit).getInvokeExpr();
                handleInvokeExpr(block, expr, thisName);
            }
            // e. g. return temp$3
            else if (unit instanceof ReturnStmt) {
                Object returnValue = handleReturnStmt(block, unit, thisName);
                returnNode = objectReaderReceiver.addNodeIfNotExisting(block, currentJavaClass);
                return returnValue;
            }
            // return
            else if (unit instanceof ReturnVoidStmt) {
                returnNode = objectReaderReceiver.addNodeIfNotExisting(block, currentJavaClass);
                return null;
            }
        }

        // Process successors e.g. if or loop
        if (!block.getSuccs().isEmpty()) {
            Node blockNode = null;
            for (Block succ : block.getSuccs()) {
                if (blockNode == null) {
                    blockNode = objectReaderReceiver.getNodeOfBlock(block, currentJavaClass);
                }

                objectReaderReceiver.pushNodeToStack(blockNode);
                this.processBlock(succ, args, argValues, thisName);
            }
            if (returnNode != null) {
                objectReaderReceiver.pushNodeToStack(returnNode);
            }
        }

        return null;
    }

    /**
     * The first identity statement assigns the current object to a variable which name is extracted.
     *
     * @param unit Identitfy statement (e.g. this := @this:java.lang.Object)
     * @return name of current object or null if the unit didn´t referred to a this reference
     */
    String getThisNameFromUnit(Unit unit) {
        IdentityStmt identityStmt = (IdentityStmt) unit;
        if (identityStmt.getRightOp() instanceof ThisRef) {
            return ((JimpleLocal) identityStmt.getLeftOp()).getName();
        } else {
            return null;
        }
    }

    /**
     * Resolves the return value.
     *
     * @param block    Current block
     * @param unit     Current unit (return statement)
     * @param thisName Name of current object
     * @return String or ObjectVariable depending on return value
     */
    Object handleReturnStmt(Block block, Unit unit, String thisName) {
        ReturnStmt returnStmt = (ReturnStmt) unit;
        if (returnStmt.getOp().getType().equals(StringType)) {
            return resolveStringValue(block, returnStmt.getOp(), thisName);
        } else {
            return resolveObjectVariable(block, returnStmt.getOp(), thisName);
        }
    }

    /**
     * Handles identity statements like r2 := @parameter1:java.lang.String and translates them into local variables
     *
     * @param unit      Identity statement
     * @param args      Argument soot values passed to the method
     * @param argValues "real" argument values i.e. variables that represent the argument
     */
    void handleIdentityStmt(Unit unit, List<Value> args, List<Object> argValues) {
        IdentityStmt identityStmt = (IdentityStmt) unit;
        // Resolve method parameters
        if (identityStmt.getRightOp() instanceof ParameterRef) {
            int idx = ((ParameterRef) identityStmt.getRightOp()).getIndex();
            Type type = identityStmt.getRightOp().getType();
            if (type.equals(StringType)) {
                StringVariable var;
                if (argValues == null || argValues.size() < idx + 1) {
                    var = new StringVariable("(unknown)");
                } else {
                    var = new StringVariable((String) argValues.get(idx));
                }
                localStringVariables.put(((JimpleLocal) identityStmt.getLeftOp()).getName(), var);
            }
            // Camunda objects which access process variables are not resolved
            else if (!type.equals(CamundaMethodServices.DELEGATE_EXECUTION_TYPE) &&
                    !type.equals(CamundaMethodServices.MAP_VARIABLES_TYPE) &&
                    !type.equals(CamundaMethodServices.VARIABLE_SCOPE_TYPE)
            ) {
                ObjectVariable var;
                if (argValues == null || argValues.size() < idx + 1) {
                    var = new ObjectVariable();
                } else {
                    var = (ObjectVariable) argValues.get(idx);
                }
                localObjectVariables
                        .put(((JimpleLocal) identityStmt.getLeftOp()).getName(), var);
            }
        }
    }

    /**
     * Creates / Updates the variable that is changed by an assignment statement.
     *
     * @param block    Current block
     * @param unit     Current unit
     * @param thisName Name of the current object
     */
    public void handleAssignStmt(Block block, Unit unit, String thisName) {
        AssignStmt assignUnit = (AssignStmt) unit;
        Value leftValue = assignUnit.getLeftOpBox().getValue();
        Value rightValue = assignUnit.getRightOpBox().getValue();

        if (leftValue instanceof JimpleLocal) {
            handleLocalAssignment(block, leftValue, rightValue, thisName);
        } else if (leftValue instanceof JInstanceFieldRef) {
            handleFieldAssignment(block, leftValue, rightValue, thisName);
        } else if (leftValue instanceof StaticFieldRef) {
            handleStaticFieldAssigment(block, (StaticFieldRef) leftValue, rightValue, thisName);
        }
    }

    /**
     * Resolves invoke expressions by laoding and processing the corresponding blocks.
     * Checks for process variable manipulations.
     *
     * @param block    Current block
     * @param expr     Invoke expression
     * @param thisName Name of current object
     * @return String / ObjectVariable if the method returns something, null otherwise
     */
    Object handleInvokeExpr(Block block, InvokeExpr expr, String thisName) {
        CamundaProcessVariableFunctions foundMethod = CamundaProcessVariableFunctions
                .findByNameAndNumberOfBoxes(expr.getMethodRef().getName(),
                        expr.getMethodRef().getDeclaringClass().getName(), expr.getArgCount());

        CamundaEntryPointFunctions foundEntryPoint = CamundaEntryPointFunctions
                .findEntryPoint(expr.getMethodRef().getName(), expr.getMethodRef().getDeclaringClass(),
                        expr.getArgCount());

        if (foundMethod != null) {
            // Process variable is manipulated
            notifyVariablesReader(block, expr, foundMethod);

            // Also handle as Map (continue processing)
            if (!(foundMethod.equals(PutValue) || foundMethod.equals(PutValueTyped))) {
                return null;
            }
        }

        List<Value> args = expr.getArgs();
        List<Object> argValues = resolveArgs(args, thisName);

        if (foundEntryPoint != null) {
            if (foundEntryPoint.isFluentBuilder()) {
                return handleFluentBuilderOperation(foundEntryPoint, expr, argValues);
            } else {
                // Process entry point
                notifyEntryPointProcessor(foundEntryPoint, expr, thisName);
                return null;
            }
        }

        ObjectVariable targetObj;
        SootMethod method = expr.getMethod();

        if (expr instanceof AbstractInstanceInvokeExpr) {
            // Instance method is called
            String targetObjName = ((AbstractInstanceInvokeExpr) expr).getBase().toString();

            // Method on this object is called
            if (targetObjName.equals(thisName)) {
                if (expr.getMethod() != null && expr.getMethod().getDeclaringClass() == this.currentJavaClass) {
                    Block nextBlock = SootResolverSimplified.getBlockFromMethod(expr.getMethod());

                    if (nextBlock != null) {
                        return this
                                .processBlock(SootResolverSimplified.getBlockFromMethod(expr.getMethod()), args,
                                        argValues,
                                        null);
                    }

                }
                // Search method in class hierarchy
                return this
                        .processBlock(SootResolverSimplified.getBlockFromMethod(
                                findMethodInHierachy(this.currentJavaClass, expr.getMethodRef())), args,
                                argValues,
                                null);

            } else {
                // Method on another object is called
                targetObj = localObjectVariables.get(targetObjName);

                if (targetObj == null) {
                    targetObj = new ObjectVariable();
                } else if (targetObj instanceof MapVariable) {
                    // Handle operation on map variable
                    handleMapOperation((MapVariable) targetObj, method, expr, block, thisName);
                    return targetObj;
                } else if (targetObj instanceof FluentBuilderVariable) {
                    return targetObj;
                }

                SootMethod resolvedMethod = resolveAnonymousInnerClasses(expr, targetObj);
                method = (resolvedMethod != null) ? resolvedMethod : method;
            }
        } else {
            // Check if Camunda VariableMap is created
            if (method.getDeclaringClass().getName().equals("org.camunda.bpm.engine.variable.Variables") && method
                    .getName().equals("createVariables")) {
                return new MapVariable();

            } else {
                // Static method is called -> create phantom variable
                targetObj = new ObjectVariable();
            }
        }

        // Process method from another class/object
        if (method.getDeclaringClass().getPackageName().startsWith("java.") || method.getDeclaringClass()
                .getPackageName().startsWith("org.camunda.")) {
            // Skip native java classes
            return null;
        }
        Block nextBlock = SootResolverSimplified.getBlockFromMethod(method);
        if (nextBlock == null) {
            return null;
        }
        ObjectReader or = new ObjectReader(objectReaderReceiver, targetObj,
                method.getDeclaringClass(), method.getName());
        return or.processBlock(SootResolverSimplified.getBlockFromMethod(method), args, argValues, null);
    }

    /**
     * Resolves invoke expressions that could refer to a method of an anonymous inner class as these are differently resolved.
     *
     * @param expr Invoke expression
     * @return SootMethod or null if resolving was not possible or necessary
     */
    SootMethod resolveAnonymousInnerClasses(InvokeExpr expr, ObjectVariable objectVariable) {
        List<Value> args = expr.getArgs();
        List<Type> argTypes = argsToTypes(args, expr);

        // Resolving for inner classes does not work with above call
        // Use saved implementation
        if (objectVariable.getImplementation() != null) {
            try {
                // Use that for init method
                return objectVariable.getImplementation()
                        .getMethodByName(expr.getMethod().getName());
            } catch (AmbiguousMethodException e) {
                return objectVariable.getImplementation()
                        .getMethod(expr.getMethod().getName(), argTypes);
            }
        }
        return null;
    }

    SootMethod findMethodInHierachy(SootClass currentClass, SootMethodRef methodRef) {
        if (currentClass
                .declaresMethod(methodRef.getName(), methodRef.getParameterTypes(), methodRef.getReturnType())) {
            return currentClass
                    .getMethod(methodRef.getName(), methodRef.getParameterTypes(), methodRef.getReturnType());
        }
        // Search in super class
        return findMethodInHierachy(currentClass.getSuperclass(), methodRef);
    }

    /**
     * Returns the types of arguments.
     *
     * @param args List of arguments
     * @return List of argument types
     */
    private List<Type> argsToTypes(List<Value> args, InvokeExpr expr) {
        ArrayList<Type> types = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals(NullConstant.v())) {
                // Use type of method instead of null type to make resolving work
                types.add(expr.getMethodRef().getParameterTypes().get(i));
            } else {
                types.add(args.get(i).getType());
            }
        }
        return types;
    }

    /**
     * Resolves assignments to local variables.
     *
     * @param block      Current block
     * @param leftValue  Value of left side of assignment
     * @param rightValue Value of right side of assignment
     * @param thisName   Name of current object
     */
    void handleLocalAssignment(Block block, Value leftValue, Value rightValue, String thisName) {
        // Local string variable is updated
        if (leftValue.getType().equals(StringType)) {
            String newValue = resolveStringValue(block, rightValue, thisName);
            localStringVariables.put(leftValue.toString(), new StringVariable(newValue));
        }
        // Object variable is updated/created
        else {
            ObjectVariable ob = resolveObjectVariable(block, rightValue, thisName);
            if (ob != null) {
                localObjectVariables.put(leftValue.toString(), ob);
            }
        }
    }

    /**
     * Resolves assignments to fields.
     *
     * @param block      Current block
     * @param leftValue  Value of left side of assignment
     * @param rightValue Value of right side of assignment
     * @param thisName   Name of current object
     */
    void handleFieldAssignment(Block block, Value leftValue, Value rightValue, String thisName) {
        // String field of object is updated
        if (leftValue.getType().equals(StringType)) {
            String newValue = resolveStringValue(block, rightValue, thisName);
            String objIdentifier = getObjIdentifierFromFieldRef(leftValue);
            String varName = getVarNameFromFieldRef(leftValue);

            if (objIdentifier.equals(thisName)) {
                // this object is referenced (ignore all other objects at the moment)
                thisObject.updateStringField(varName, newValue);
            }
        }
        // Object field is updated
        else {
            String objIdentifier = getObjIdentifierFromFieldRef(leftValue);
            String varName = getVarNameFromFieldRef(leftValue);
            ObjectVariable objectVar = resolveObjectVariable(block, rightValue, thisName);
            // Only consider this object at the moment
            if (objIdentifier.equals(thisName) && objectVar != null) {
                thisObject.putObjectField(varName, objectVar);
            }
        }
    }

    /**
     * Resolves assignments to static fields.
     *
     * @param block      Current block
     * @param leftValue  Value of left side of assignment
     * @param rightValue Value of right side of assignment
     * @param thisName   Name of current object
     */
    void handleStaticFieldAssigment(Block block, StaticFieldRef leftValue, Value rightValue, String thisName) {
        String classname = leftValue.getFieldRef().declaringClass().getName();

        if (!staticObjectVariables.containsKey(classname)) {
            staticObjectVariables.put(classname, new ObjectVariable());
        }
        ObjectVariable staticClass = staticObjectVariables.get(classname);

        // String field of object is updated
        if (leftValue.getType().equals(StringType)) {
            String newValue = resolveStringValue(block, rightValue, thisName);
            String varName = getVarNameFromFieldRef(leftValue);
            staticClass.updateStringField(varName, newValue);
        }
        // Object field is updated
        else {
            String varName = getVarNameFromFieldRef(leftValue);
            ObjectVariable objectVar = resolveObjectVariable(block, rightValue, thisName);
            if (objectVar != null) {
                staticClass.putObjectField(varName, objectVar);
            }
        }
    }

    /**
     * Resolves the value of a variable / call / constant / etc. that returns a string.
     *
     * @param block      Current block
     * @param rightValue Value / expression to be resolved
     * @param thisName   Name of current object
     * @return Current string value
     */
    String resolveStringValue(Block block, Value rightValue, String thisName) {
        if (rightValue instanceof StringConstant) {
            return getValueFromStringConstant(rightValue);
        } else if (rightValue instanceof JimpleLocal) {
            return localStringVariables.containsKey(rightValue.toString()) ?
                    localStringVariables.get(rightValue.toString()).getValue() :
                    null;
        } else if (rightValue instanceof InstanceFieldRef) {
            // FieldRefs other than the current object are currently not resolved
            if (((InstanceFieldRef) rightValue).getBase().toString().equals(thisName)) {
                StringVariable field = thisObject.getStringField(getVarNameFromFieldRef(rightValue));
                if (field != null) {
                    return field.getValue();
                }
                return null;
            }
            return null;
        } else if (rightValue instanceof StaticFieldRef) {
            String className = ((StaticFieldRef) rightValue).getFieldRef().declaringClass().getName();
            String varName = ((StaticFieldRef) rightValue).getFieldRef().name();
            ObjectVariable staticClass = staticObjectVariables.get(className);
            if (staticClass == null) {
                return null;
            }
            StringVariable field = staticClass.getStringField(varName);
            if (field != null) {
                return field.getValue();
            }
            return null;
        } else if (rightValue instanceof InvokeExpr) {
            return (String) handleInvokeExpr(block, (InvokeExpr) rightValue, thisName);
        } else if (rightValue instanceof CastExpr) {
            return resolveStringValue(block, ((CastExpr) rightValue).getOp(), thisName);
        } else {
            return null;
        }
    }

    /**
     * Resolves the value of a variable / call / constant / etc. that returns an object.
     *
     * @param block      Current block
     * @param rightValue Value / expression to be resolved
     * @param thisName   Name of current object
     * @return ObjectVariable that refers to the object
     */
    ObjectVariable resolveObjectVariable(Block block, Value rightValue, String thisName) {
        if (rightValue instanceof JimpleLocal) {
            String localVar = rightValue.toString();
            return localObjectVariables.get(localVar);
        } else if (rightValue instanceof InstanceFieldRef) {
            // FieldRefs other than the current object are currently not resolved
            if (((InstanceFieldRef) rightValue).getBase().toString().equals(thisName)) {
                return thisObject.getObjectField(getVarNameFromFieldRef(rightValue));
            }
            return null;
        } else if (rightValue instanceof StaticFieldRef) {
            String className = ((StaticFieldRef) rightValue).getFieldRef().declaringClass().getName();
            String varName = ((StaticFieldRef) rightValue).getFieldRef().name();
            if (staticObjectVariables.containsKey(className)) {
                return staticObjectVariables.get(className).getObjectField(varName);
            } else {
                return null;
            }
        } else if (rightValue instanceof NewExpr) {
            // New object is instantiated, we add an empty object as constructors are not resolved yet

            // If Map is created, created map variable, otherwise normal object variable
            ObjectVariable ob;
            if (((RefType) rightValue.getType()).getSootClass().getInterfaces()
                    .contains(Scene.v().forceResolve("java.util.Map", 0))) {
                ob = new MapVariable();
            } else {
                ob = new ObjectVariable();
            }

            // If it is an inner class we also add the reference to it
            if (((NewExpr) rightValue).getBaseType().getSootClass().hasOuterClass()) {
                ob.setImplementation(((NewExpr) rightValue).getBaseType().getSootClass());
            }
            return ob;
        } else if (rightValue instanceof InvokeExpr) {
            return (ObjectVariable) handleInvokeExpr(block, (InvokeExpr) rightValue, thisName);
        } else if (rightValue instanceof CastExpr) {
            return resolveObjectVariable(block, ((CastExpr) rightValue).getOp(), thisName);
        } else {
            return null;
        }
    }

    // TODO add test
    public void notifyVariablesReader(Block block, InvokeExpr expr, CamundaProcessVariableFunctions camundaMethod) {
        int location = camundaMethod.getLocation() - 1;
        VariableOperation type = camundaMethod.getOperationType();
        String variableName = resolveStringValue(block, expr.getArgBox(location).getValue(), null);

        ProcessVariableOperation pvo;
        // Variable map maps variables to child of call activity
        if (camundaMethod.getService().equals(CamundaMethodServices.VARIABLE_MAP)) {
            pvo = new ProcessVariableOperation(variableName, type,
                    objectReaderReceiver.getScopeIdOfChild());
        } else {
            pvo = new ProcessVariableOperation(variableName, type,
                    objectReaderReceiver.getScopeId());
        }

        objectReaderReceiver.handleProcessVariableManipulation(block, pvo, currentJavaClass);
    }

    public void notifyEntryPointProcessor(CamundaEntryPointFunctions func, InvokeExpr expr, String thisName) {
        objectReaderReceiver
                .addEntryPoint(func, this.currentJavaClass.getName(), this.currentMethod, expr,
                        resolveArgs(expr.getArgs(), thisName));
    }

    public void notifyEntryPointProcessor(FluentBuilderVariable fluentBuilder) {
        objectReaderReceiver.addEntryPoint(fluentBuilder, this.currentJavaClass.getName(), this.currentMethod);
    }

    /**
     * Resolves arguments by resolving fields and local variables.
     *
     * @param args     Arguments
     * @param thisName Name of current object
     * @return List of argument values
     */
    public ArrayList<Object> resolveArgs(List<Value> args, String thisName) {
        ArrayList<Object> list = new ArrayList<>();
        for (Value arg : args) {
            if (arg.getType().equals(StringType)) {
                list.add(resolveStringValue(null, arg, thisName));
            } else if (arg.getType().equals(CamundaMethodServices.DELEGATE_EXECUTION_TYPE) ||
                    arg.getType().equals(CamundaMethodServices.MAP_VARIABLES_TYPE) ||
                    arg.getType().equals(CamundaMethodServices.VARIABLE_SCOPE_TYPE)) {
                list.add(null);
            } else {
                list.add(resolveObjectVariable(null, arg, thisName));
            }
        }
        return list;
    }

    public void handleMapOperation(MapVariable map, SootMethod method, InvokeExpr expr, Block block, String thisName) {
        if (method.getName().equals("put") || method.getName().equals("putValue")) {
            // Resolve name of variable
            String variableName = resolveStringValue(block, expr.getArg(0), thisName);
            map.put(variableName, expr.getArg(1));
        } else if (method.getName().equals("remove")) {
            // Delete variable
            String variableName = resolveStringValue(block, expr.getArg(0), thisName);
            map.remove(variableName);
        }
        // TODO support putall
    }

    public FluentBuilderVariable handleFluentBuilderOperation(CamundaEntryPointFunctions foundEntryPoint,
            InvokeExpr expr, List<Object> argValues) {
        FluentBuilderVariable flbv;
        switch (foundEntryPoint) {
            case Execute:
            case ExecuteWithVariablesInReturn:
            case SetVariable:
            case SetVariableLocal:
            case SetVariables:
            case SetVariablesLocal:
                String targetObjName = ((AbstractInstanceInvokeExpr) expr).getBase().toString();
                flbv = (FluentBuilderVariable) localObjectVariables.get(targetObjName);
                break;
            default:
                flbv = new FluentBuilderVariable(foundEntryPoint);
                break;
        }

        if (foundEntryPoint.equals(CamundaEntryPointFunctions.Execute) || foundEntryPoint
                .equals(CamundaEntryPointFunctions.ExecuteWithVariablesInReturn)) {
            flbv.setWasExecuted(true);
            notifyEntryPointProcessor(flbv);
        } else if (foundEntryPoint.equals(CamundaEntryPointFunctions.CreateProcessInstanceByKey)) {
            flbv.setProcessDefinitionKey(argValues.get(0).toString());
        } else if (foundEntryPoint.equals(CamundaEntryPointFunctions.SetVariable) || foundEntryPoint
                .equals(CamundaEntryPointFunctions.SetVariableLocal)) {
            // Local variables are currently not supported TODO
            flbv.addVariable(argValues.get(0).toString());
        } else if (foundEntryPoint.equals(CamundaEntryPointFunctions.SetVariables) || foundEntryPoint
                .equals(CamundaEntryPointFunctions.SetVariablesLocal)) {
            flbv.addAllVariables((MapVariable) argValues.get(0));
        }
        return flbv;
    }

    public static int hashBlock(Block block) {
        return Objects.hash(block.getHead(), block.getTail(), block.getBody(),
                block.getIndexInMethod());
    }

    private String getObjIdentifierFromFieldRef(Value value) {
        return ((JInstanceFieldRef) value).getBase().toString();
    }

    private String getVarNameFromFieldRef(Value value) {
        return ((FieldRef) value).getFieldRef().name();
    }

    private String getValueFromStringConstant(Value value) {
        return ((StringConstant) value).value;
    }

    HashMap<String, StringVariable> getLocalStringVariables() {
        return localStringVariables;
    }

    HashMap<String, ObjectVariable> getLocalObjectVariables() {
        return localObjectVariables;
    }

    ObjectVariable getThisObject() {
        return thisObject;
    }

    HashMap<String, ObjectVariable> getStaticObjectVariables() {
        return staticObjectVariables;
    }
}