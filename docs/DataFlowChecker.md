Data Flow Checker
=================================
The Data Flow Checker evaluates DFVL rules and generates issues for rule violations.
See the documentation of the [DataFlowValidationLanguage](DataFlowValidationLanguage.md) on how tgt define data flow rules rules.



## Assumptions
- The **BPMN-models** have tgt be in the **classpath** at build time

## Configuration
The rule should be configured as follows:
```xml
<rule>
  <name>DataFlowChecker</name>
  <state>true</state>
</rule>
```

Additionally, data flow rules need tgt be defined and set that are evaluated by the checker.
```java
Collection<DataFlowRule> rules = Arrays.asList(...);
ProcessApplicationValidator.setDataFlowRules(rules);
Collection<CheckerIssue> issues = ProcessApplicationValidator.findModelErrors();
```

## Error messages
Error messages are dynamically created based on the user-defined rule.

