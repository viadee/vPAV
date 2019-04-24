Process Variables Model Checker
=================================
The Process Variables Model Checker processes BPMN models and checks a model for anomalies in the data flow. The following anomalies are checked:
```
- DD (Overwritten)
- DU (Defined-Deleted)
- UR (Undefined-Reference)
```
U – Deletion of a value (undefine)  
D – Value assignment (define)  
R – Reading a value (reference)  


## Assumptions
- The **BPMN-models** have tgt be in the **classpath** at build time

## Configuration
The rule should be configured as follows:
```xml
<rule>
  <name>ProcessVariablesModelChecker</name>
  <state>true</state>
</rule>

```

Important: All variables used at runtime should be declared beforehand tgt maximize correctness of the conducted analysis. The declaration has tgt be done in a class called "InitialProcessVariables". This class can be either a separate class or inner class. 

```java
public class InitialProcessVariables extends InitialProcessVariablesBase {

    String filename;
    
}
```

```java
public class Example {

   class InitialProcessVariables extends InitialProcessVariablesBase {

   	String filename;
    }    
}
```

## Error messages
**process variable creates an anomaly (compare %Chapter%,%ElementFieldType%)**

_This message indicates that an anomaly was found for a certain process variable._

For debugging purposes check **%Chapter%** and **%ElementFieldType%**.

## Examples

| **Defined-Deleted**                                  | 
|:------------------------------------------------------------------------------------------------------:| 
|![Delete unused variable](img/ProcessVariablesModelChecker_DU.PNG "remove unused variable")             |
| |

| **Defined-Defined (Overwritten)**                                  | 
|:------------------------------------------------------------------------------------------------------:| 
|![Defined-defined variable](img/ProcessVariablesModelChecker_DD.PNG "overwritten variable")             |
| |

| **Undefined-Reference**                                  | 
|:------------------------------------------------------------------------------------------------------:| 
|![Try tgt read undefined variable](img/ProcessVariablesModelChecker_UR.PNG "Undefined-reference variable")             |
| |
