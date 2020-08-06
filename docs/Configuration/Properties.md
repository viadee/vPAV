---
parent: Configuration
title: Properties
---

# Properties
{: .no_toc }


There are some configurations that have nothing to do with checkers. To define these properties, you have to create a ``vPav.properties`` file and put it in your classpath e.g. at `src/test/resources`.
The properties file could look like this:
```
outputhtml=false
language=de_DE
basepath=src/test/resources/
parentRuleSet=ruleSets/parentRuleSet.xml
ruleSet=myRuleSet.xml
scanpath=target/test-classes/
userVariablesFilePath=subfolder/myVariables.xml
validationFolder=../myReports
```
{: .fs-6 .fw-300 }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---
## Html output
If the vPAV finds inconsistencies in the model, a report is generated. 
This report can be visualized as HTML page. By default, the HTML output is enabled. 
If you want to disable the output, add `outputhtml=false` to the properties.

## Language
As of version 2.5.0, we added localization for english and german users.
By specifying either **en_US** or **de_DE** as ``language`` property, you can choose to use either German or English as language for your visual output report. 
By leaving it blank, the validator grabs your systems locale and provides either German or English as default. 
Due to some refactoring, more languages can be added in the future by providing language files with the corresponding translations.

## Location of BPMN models
By default, the BPMN models have to be stored in the folder ``src/main/resources`` of your Camunda project. 
However, you can also load models from other locations of your local filesystem.
In the properties file, use the property ``basepath`` to define the path.
You can use relative paths (e.g. ```basepath=src/test/resources```) or absolute paths using the ``file:///`` scheme.

## Parent rule set
If you defined a parent rule set, you can customize the relative path and file name with `parentRuleSet=ruleSets/parentRuleSet.xml`.

## Rule set 
You might want to change the name of the rule set if you have multiple sets. 
The property `ruleSet=myRuleSet.xml` allows you to set a relative path to the defined base path.

## Scan path
vPAV scans the target folder to find compiled Java classes which are referenced in the BPMN model like Camunda delegates.
By default, `target/classes/` is used. But if you customized the location, you can use it with the property `scanpath=target/test-classes/`.

## User variables path
If you created a custom `variables.xml` file (see  [Defining variables](Configuration/UserVariables.mde)), you can modify the name and the relative path. 
To do so, add the property `userVariablesFilePath=subfolder/myVariables.xml`.

## Validation folder path
You can specify the validation folder where the vPAV generated output will be stored. By default `target/vPAV/` is set.
`target/vPAV/` ist not persistent and will be deleted when Maven executes the `clean` goal.