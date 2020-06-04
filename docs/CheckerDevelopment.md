---
title: Checker Development
nav_order: 6
---
# Checker Development
We try to cover a broad range of possible inconsistencies 
but if you are missing some checks, you can simply add your own checker.
You can find an example project [here](https://github.com/viadee/vPAV_checker_plugin_example).
In your projects you have to add the dependency to the project with the checker-class(es) (see example below):

```xml
<dependency>
	<groupId>de.viadee</groupId>
	<artifactId>vPAV_checker_plugin_example</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```
Run this project (e.g. vPAV_checker_plugin_example) as maven install (make sure to package as jar). 

The `setting name="external_Location"` defines the checker as an external checker.
The value specifies the location of the checkerclass.

```xml
<rule>
	<name>TaskNamingConventionCheckerExtern</name>
	<state>true</state>
	<settings>
		<setting name="external_Location">de.viadee.vPAV_checker_plugin_example</setting>
	</settings>
	<elementConventions>
		<elementConvention>
			<name>convention</name>
			<pattern>[A-ZÄÖÜ][a-zäöü\\\-\\\s]+</pattern>
		</elementConvention>
	</elementConventions>
</rule>
```
## Requirements
- Your checker-class have to extends the *AbstractElementChecker*. 
- Only the parameters from the abstract class (`de.viadee.bpm.vPAV.config.model.Rule` and `de.viadee.bpm.vPAV.BPMNScanner`) are allowed in the constructor.

## Checker instructions
You have to return a collection of `de.viadee.bpm.vPAV.processing.model.data.CheckerIssue`.

``` java
/**
* CheckerIssue
* 
* @param ruleName
*            Name of the Rule
* @param classification
*            Classification (Info, Warning or Error) of the rule
* @param bpmnFile
*            Path to the BPMNFile
* @param resourceFile
*            Path to resource file (e.g. dmn oder java)
* @param elementId
*            Id of the Element with issue
* @param elementName
*            Name of the Element with issue
* @param variable
*            Name of variable
* @param anomaly
*            Type of anomaly (DD, DU, UR)
* @param invalidPaths
*            Invalid path
* @param message
*            Issue message
*/
 ```
