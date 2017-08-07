DMN Task Checker
=================================
The DMN Task Checker processes BPMN models and checks whether a BusinessRuleTask with DMN implementation references a DMN.


## Assumptions
----------------------------------------------
- The **BPMN-models** have to be in the **classpath** at build time
- The referenced **DMN-file** has to be in the **classpath** at build time

## Configuration
------------------------------------------
The rule should be configured as follows:
```xml
<rule>
  <name>DmnTaskChecker</name>
  <state>true</state>
</rule>

```

## Error messages
-----------------------------------------
**business rule task with dmn implementation without a decision ref**

_This message indicates that no decision reference to a DMN file was found._


## Examples
----------------------------------------

| **No Decision Ref**                                                                                    |
|:------------------------------------------------------------------------------------------------------:| 
|![No dec. ref specified](img/BusinessRuleTaskChecker_NoDecisionRef.PNG "No decision reference")         |

