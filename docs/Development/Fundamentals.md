---
parent: Development
title: Fundamentals
---
# Fundamentals
vPAV consists of multiple checkers that can be applied to a BPMN model. 
However, the main functionality is the analysis of process variables that can be executed with the `ProcessVariablesModelChecker`.

For the analysis, the BPMN model is transformed into a graph of Java objects which represent the BPMN model. 
The elements of the graph contain the process variable operations.
This is done in the class `ElementGraphBuilder`.

After discovering the process variables, an algorithm is used that propagates the variables. 
If a variable is defined in the start event, the variable is propagated to the other elements because it is also known there once defined.

The last step of the analysis is the anomaly detection. E.g. if a variable is used but was not defined previously, an UR anomaly is created. 
Every element contains `in` and `out` sets of process variables so that an anomaly can be detected by comparing these sets.
This algorithm is implemented in the class `FlowAnalysis`.