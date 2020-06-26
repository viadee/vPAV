---
title: Available Checkers
has_children: true
has_toc: false
nav_order: 5
---
# Available Checkers
Consistency checks are performed by individual modules called checkers, which search for certain types of inconsistencies. 
All of these can be switched on or off in the ruleset as required. Implementing further checkers is rather simple.
Currently, the following checkers are implemented: 

| Checker                                                                              | Summary                                                                  | Status       |
| ------------------------------------------------------------------------------------ | ----------------------------------------------------------------------   | ------------ |
|[JavaDelegateChecker](avaDelegateChecker.md)                                         | Is the implementation (or Spring bean reference) available and usable?   | Done         |
|[DmnTaskChecker](DmnTaskChecker.md)                                                   | Is the implementation available?                                         | Done         |
|[EmbeddedGroovyScriptChecker](EmbeddedGroovyScriptChecker.md)                         | Is the implementation available and does it look like a script?          | Done         |
|[ProcessVariablesModelChecker](ProcessVariablesModelChecker.md)                       | Are process variables in the model provided in the code for all paths?   | Done |
|[ProcessVariablesNameConventionChecker](ProcessVariablesNameConventionChecker.md)     | Do process variables in the model fit into a desired regex pattern?      | Done         |
|[TaskNamingConventionChecker](TaskNamingConventionChecker.md)                         | Do task names in the model fit into a desired regex pattern?             | Done         |
|[VersioningChecker](VersioningChecker.md)                                             | Do java classes implementing tasks fit  a version scheme?             | Done         |
|[XorConventionChecker](XorConventionChecker.md)                           		| Are XOR gateways ending with "?" or have default path                                         | Done         |
|[NoScriptChecker](NoScriptChecker.md)                                                 | Is there any script in the model?                                        | Done         |
|[ElementIdConventionChecker](ElementIdConventionChecker.md)                           | Do task ids in the model fit into a desired regex pattern?           | Done         |
|[TimerExpressionChecker](TimerExpressionChecker.md)                                   | Are time events following the ISO 8601 scheme?                                        | Done         |
|[NoExpressionChecker](NoExpressionChecker.md)                                   | Are expressions used against common best-practices?                                        | Done         |
|[MessageEventChecker](MessageEventChecker.md)                                   | Are MessageEvents referencing messages and do they provide message names?                                  | Done         |
|[SignalEventChecker](SignalEventChecker.md)                                   | Are SignalEvents referencing signals and do they provide signal names? Are signal names used more than once in StartEvents?                                  | Done         |
|[DataFlowChecker](DataFlowChecker.md)                                  | Does your model adhere to your configurable rules (read, writes, deletions) ?                                  | Done         |
|[ExtensionChecker](ExtensionChecker.md)                                  | Do tasks using key-value pairs in the extension panel fit into a desired pattern?                                  | Done         |
|[OverlapChecker](OverlapChecker.md)                                   | Are there redundant sequence flows (some may be invisible due to overlap)?              | Done         
|[BoundaryErrorChecker](BoundaryErrorChecker.md)                                   | Do tasks with attached BoundaryErrorEvents use the correct ErrorCode and do the corresponding classes exist?                                  | Experimental         |
