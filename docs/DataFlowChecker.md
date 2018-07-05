Data Flow Checker
=================================
The Data Flow Checker validates accesses of process variables against defined data flow rules.


## Assumptions
- The **BPMN-models** have to be in the **classpath** at build time

## Configuration
The rule should be configured as follows:
```xml
<rule>
  <name>DataFlowChecker</name>
  <state>true</state>
</rule>

```

Additionally, data flow rules need to be defined and set that are evaluated by the checker.
```java
public class InitialProcessVariables extends InitialProcessVariablesBase {

    String filename;
    
}
```

## Error messages
Error messages are dynamically created based on the user-defined rule.

## Examples

