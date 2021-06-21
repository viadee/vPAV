---
parent: Development
title: Entry Point Scanner
---
# Entry Point Scanner
The entry point scanner is used to find process variables which are passed to a process when it is started.
A process can be started with the Camunda `RuntimeService` or the `ProcessInstantiationBuilder`.

Both classes allow the user to pass process variables. 
They are passed as Map or can be set individually in the case of the `ProcessInstantiationBuilder`.

vPAV scans the project and tries to detect method calls that start a process. For each call that is found, an entry point is created.
The process variables are then included in the analysis. 

If the process is started with the key e.g. with `startProcessInstanceByKey`, the entry point can be mapped to a BPMN model because the key is the process ID.
If the process is started with a deployment ID, it is not possible to find out the correct BPMN model. In this case, the entry point is only included if there is only one model that is analyzed.
Processes can be also started with a message. vPAV tries to find a model with a fitting message to match the entry point. However, expressions cannot be resolved and thus mistakes might occur.

The entry point scanner uses the `ObjectReader` to parse all Java classes. 
It functions as listener so that it is notified if a process is started via the `RuntimeService` or the `ProcessInstantiationBuilder`.

Currently, found variables are added in the process scope. 
The methods `setVariable(s)Local` are detected but the scope is not further taken into account.