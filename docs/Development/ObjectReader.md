---
parent: Development
title: Object Reader
---
# Object Reader
The `ObjectReader` is the core class of the process variable discovery process in Java implementations. 
It is executed when variables in e.g. Java Delegates should be discovered.

The `ObjectReader` uses Soot to iterate over a CFG of the Java method. 
It models the current state of the Java program and saves current variable values.
By statically executing the Java program, variable values can be resolved and the name of process variables can be discovered.

## Representing variables
Java variables are represented by objects of the types `StringVariable` and `ObjectVariable`.
A string variable represents a variable of the type `String`. This type is important because process variable names are always Strings.
Object variables represent any objects and possess String and object fields. This is used to resolve String values which are object fields.

There are two child classes of object variables: `MapVariable` and `FluentBuilderVariable`. 
These are two special types of objects where operations are understood and processed by the object reader.
The object reader can e.g. resolve values which are put or removed from a Java Map. 

## Block processing
A method is represented by a Soot block. A block is processed by the method `processBlock`. 
The two variables `localStringVariables` and `localObjectVariables` contain local variables that are created in a method.

Each type of instruction e.g. `IdentityStmt` or `AssignStmt` is handled in its own method.
The general procedure is that first variables are resolved. The variables are resolved by checking the type and the locality. 
Depending on the characteristics, the local variables, fields of an object or static variables are checked. 
If the variable cannot be resolved, `null` is returned. 

If an instruction is encountered that has a special meaning in the camunda context like `setVariable`, a listener is notified. 
Depending on the type of instrunction and state of analysis, either an entry point is created or a process variable operation is added to the CFG.

Each block is processed only once. If a block is visited a second time, an edge is added to the existing block. 
This prevents stack overflows if a recursion or loop is included in the method.
