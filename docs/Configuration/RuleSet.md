---
parent: Configuration
title: Rule Set
---
# Rule Set
The viadee Process Application Validator uses a rule set to define which checks are executed.
The plugin comes with a default ruleSet.xml which provides some basic rules.
In order to customize the plugin, we recommend creating your own ruleSet.xml and store it in `src/test/resources`. 
This allows you to use your own set of rules for naming conventions or to de-/activate certain checkers.
To write your own rule set, you can follow the example of [ruleSetDefault.xml](https://github.com/viadee/vPAV/blob/master/src/main/resources/ruleSetDefault.xml).

## One set of rules to rule them all
Furthermore you can use the plugin to manage multiple projects.
A parent rule set can be shared among them so that you don't have to define the same checkers multiple times.
The parentRuleSet.xml will provide a basic set of rules for all projects that "inherit".
Local sets of rules will override inherited rules in order to allow for customization.

Just create a blank maven project with only the parentRuleSet.xml stored in  `src/main/resources` and run this project as maven install (make sure to package as jar).

```xml
<dependency>
	<groupId>de.viadee</groupId>
	<artifactId>parent_config</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>

<dependency>
	<groupId>de.viadee</groupId>
	<artifactId>viadeeProcessApplicationValidator</artifactId>
	<version>...</version>
</dependency>
```

In your child projects you have to add the dependency to the parent project and to vPAV.
The inheritance is only working if you define an own rule set in your child project.
You cannot use the default rule set because it does not include inheritance.

Make sure that inheritance is activated in the ruleSet.xml of your child project.
```xml 
<rule>
	<name>HasParentRuleSet</name>
	<state>true</state>
</rule>
```

## Multiple checker configurations
It might be useful to run a checker two times with different configurations. To prevent a rule from being overridden, you can define an ID.
```xml 
<rule id="xorChecker1">
    <name>XorConventionChecker</name>
	<state>true</state>
	<settings>
	    <setting name="requiredDefault">true</setting>
	</settings>
	<elementConventions>
		<elementConvention>
			<name>convention</name>
			<description>gateway name has to end with an question mark</description>
			<pattern>[A-ZÄÖÜ][a-zäöü]*\\?</pattern>
		</elementConvention>
	</elementConventions>
</rule>
```
If two rule sets are merged (e. g. parent and child rule set), the following inheritance rules apply:
- A parent rule will be overridden if a child rule has the same ID.
- A parent rule will be loaded if no rule with the same ID exists in the child rule set.
- If a rule does not have an ID, the name of the rule will be used and handled as ID.

The following checkers/rules can only be defined once. It it not possible to have different configurations:
- HasParentRuleSet
- VersioningChecker
- ProcessVariablesModelChecker
- DataFlowChecker