---
parent: Available Checkers title: LinterChecker
---
LinterChecker
=================================
The LinterChecker processes BPMN models and checks for predefined rules regarding the process model, e.g. conditional sequenceflows.

## Assumptions

- The **BPMN-models** have to be in the **classpath** at build time

## Configuration

The rule should be configured as follows:

```xml

<rule>
    <name>LinterChecker</name>
    <state>true</state>
    <settings>
        <setting name="conditional-sequenceflows">conditional-sequenceflows</setting>
    </settings>
</rule>

```

## Error messages

**Sequenceflow 'Sequenceflow_123' is not defined as default and/or conditional.**

_This message indicates that the sequenceflow does not have a condition and/or is not defined as default path._

## Further information

**Currently only the "conditional-sequenceflows" check is implemented. The LinterChecker is awaiting additional linter rules to enhance usage**
