Embedded Groovy Script Checker
=================================
The Embedded Groovy Script Checker verifies embedded scripts in listeners and script tasks for validity.
For this purpose, it checks the following conditions:
- No script format is specified
- No script content is specified
- For Groovy only: The script content doesn't match the script format (syntax check)

## Assumptions
----------------------------------------------
- The **BPMN-models** have to be in the **classpath** at build time

## Configuration
------------------------------------------
The rule should be configured as follows:
```xml
<rule>
  <name>EmbeddedGroovyScriptChecker</name>
  <state>true</state>
</rule>
```

Via `<state>true</state>` the check can be enabled.

Via `<state>false</state>` the check can be disabled.

## Error messages
-----------------------------------------
**there is no script format for given script**

_There is no script format for an embedded script. A script format must be defined for the script._

**there is no script content for given script format**

_There is a script format, but no embedded script. A script must be created._

**there is an empty script reference**

_No script has been deposited. A script must be specified._

**[syntax checker message]**

_The syntax of the script is not valid. (for groovy only)_

## Examples
----------------------------------------

| **there is no script format for given script**                                                         | 
|:------------------------------------------------------------------------------------------------------:| 
|![No script format value](img/EmbeddedGroovyScriptChecker_EmptyScriptFormat.PNG "No script format")     |
| |

| **there is no script content for given script format**                                                 |
|:------------------------------------------------------------------------------------------------------:| 
| ![No script value](img/EmbeddedGroovyScriptChecker_EmptyScript.PNG "Script must provide a value")      |
| |

| **there is an empty script reference**                                                                 |
|:------------------------------------------------------------------------------------------------------:| 
![No script value](img/EmbeddedGroovyScriptChecker_EmptyScriptReference.PNG "Script must provide a value")|

| **startup failed: ....**                                                                               |
|:------------------------------------------------------------------------------------------------------:| 
![Syntax of groovy incorrect](img/EmbeddedGroovyScriptChecker_InvalidGroovyScript.PNG "Syntax error")    |

