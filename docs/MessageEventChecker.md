MessageEventChecker
=================================
The MessageEvent Checker processes BPMN models and checks whether a MessageEvent refers to a concrete message and names it accordingly 

- No message in ReceiveTask
- No message in IntermediateMessageCatchEvent
- No message name in IntermediateMessageCatchEvent
- No message in MessageEndEvent

## Assumptions
- The **BPMN-models** have to be in the **classpath** at build time

## Configuration
The rule should be configured as follows:
```xml
<rule>
  <name>MessageEventChecker</name>
  <state>true</state>
</rule>

```

## Error messages
**No message has been specified for %MessageEvent%**

_This message indicates that no message or message name was specified for a given MessageEvent._


## Examples

| **Correct usage**                                                                                    |
|:------------------------------------------------------------------------------------------------------:| 
|![Correct usage of message event](img/MessageEventChecker_correct.PNG "Message has been specified")         |


| **No Message Name**                                                                                    |
|:------------------------------------------------------------------------------------------------------:| 
|![No message name specified](img/MessageEventChecker_wrong1.PNG "No message name specified")         |


| **No Message**                                                                                    |
|:------------------------------------------------------------------------------------------------------:| 
|![No message at all specified](img/MessageEventChecker_wrong2.PNG "No message specified")         |
