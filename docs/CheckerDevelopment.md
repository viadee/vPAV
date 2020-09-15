---
title: Checker Development
nav_order: 6
---
# Checker Development
We try to cover a broad range of possible inconsistencies 
but if you are missing some checks, you can simply add your own checker.
There are two types of checkers: Element checker and Model checkers.
Element checkers are applied to each BPMN element whereas model checkers operate on the complete BPMN model.

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

## Element checkers
Element checkers must extend the class *AbstractElementChecker*.
Only one parameter from the abstract class `de.viadee.bpm.vPAV.config.model.Rule` is allowed in the constructor.

You can find an example project [here](https://github.com/viadee/vPAV_checker_plugin_example).

## Model checkers
If you use a model checker in your project, vPAV will automatically analyze the BPMN model and process variables.
The `FlowAnalysis` is passed to each model checker which includes a graph of the BPMN model. 
If you want to implement a model checker, the best way to understand the parameters is to debug a project and inspect the objects.

Model checkers must extend the class *AbstractModelChecker*. The model checker could look like this:

```java
 public ExternalChecker(Rule rule,
            Map<AnomalyContainer, List<Path>> invalidPathsMap,
            Collection<ProcessVariable> processVariables,
            FlowAnalysis flowAnalysis) {
        super(rule, invalidPathsMap, processVariables, flowAnalysis);
    }

    @Override public Collection<CheckerIssue> check() {
        // TODO implement your checks
        return new HashSet<>();
    }
```

## Checker instructions
For both types of checkers, you have to return a collection of `de.viadee.bpm.vPAV.processing.model.data.CheckerIssue`.

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
* @param implementationDetails 
*           like the Java class of a Delegate if the issue is related to source code
*/
 ```
