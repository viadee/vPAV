---
parent: Configuration
title: Defining variables
---
# Defining your own variables
vPAV is not able to discover all usages of variables yet. Therefore it is annoying if you use a variable and and an error is raised although it is properly defined.
A single variable can cause multiple errors if the definition can't be found. 
Instead of including all errors in the ignore file, vPAV offers a better solution.

You can create a `variables.xml` file in your resource folder. Inside the file variable operations can be defined which should be included in the graph analysis.
You have to provide at least the name of the variable. Moreover, you can define the process id if you're using multiple processes in your BPMN model.
It is also possible to provide the creation point (= when the operation is executed in the process graph) and the scope. Thus it is possible to e.g. limit the availability to a specific element. 
By default, the `write` operation is used. However, by adding the XML element `operation` you can define `read` or `delete` operations.

**Example**
```
<variable>
    <name>numberEntities</name>
    <process>Process_1</process>
    <creationPoint>ServiceTask_108g52x</creationPoint>
    <scope>ServiceTask_108g52x</scope>
    <operation>write</operation>
</variable>
```
In this case the variable definition would be included in the process graph at the beginning of the `ServiceTask_108g52x` and it would be available until the end of the task.
