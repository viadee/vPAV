# Release notes

## 3.0.1

### Features  
[**Issue 133**](https://github.com/viadee/vPAV/issues/133) Added scope information to data flow analysis 

### Fixes

### Misc
[**Issue 130**](https://github.com/viadee/vPAV/issues/130) Redefined anomalies and improved calculations   

## 3.0.0

### Features
[**Issue 103**](https://github.com/viadee/vPAV/issues/103) Reworked entire ProcessVariableModelChecker to improve accuracy of data flow analysis  
[**Issue 109**](https://github.com/viadee/vPAV/issues/109) Created unique (global) identification mechanism for proper access to process variable operations  
[**Issue 119**](https://github.com/viadee/vPAV/issues/119) Separation of properties (introduction of a properties file) and rules  
[**Issue 121**](https://github.com/viadee/vPAV/issues/121) Multiple instances of checkers are now allowed (see [#59](https://github.com/viadee/vPAV/issues/59))  

### Fixes

### Misc

## 2.8.3

### Features
[**Issue 59**](https://github.com/viadee/vPAV/issues/59) Multiple instances of checkers are now allowed  
[**Issue 80**](https://github.com/viadee/vPAV/issues/80) Location of BPMN files now configurable  

### Fixes
[**Issue 120**](https://github.com/viadee/vPAV/issues/120) Fixed CheckerFactory warning  

### Misc
[**Issue 115**](https://github.com/viadee/vPAV/issues/115) Improved documentation for inheritance of ruleSets  
[**Issue 113**](https://github.com/viadee/vPAV/issues/113) Adjusted SonarQube rules

## 2.8.2

### Features
[**Issue 106**](https://github.com/viadee/vPAV/issues/106) Make bean mapping passable as argument to ProcessApplicationvalidator  
### Fixes  

### Misc
[**Issue 116**](https://github.com/viadee/vPAV/issues/16) Clean up of unit tests

## 2.8.1

### Features
[**Issue 60**](https://github.com/viadee/vPAV/issues/60) Support of transitive interface implementation  
[**Issue 94**](https://github.com/viadee/vPAV/issues/94) Added MessageChecker

### Fixes
[**Issue 104**](https://github.com/viadee/vPAV/issues/104) Resolved dependencies

### Misc
[**Issue 102**](https://github.com/viadee/vPAV/issues/102) Improved discovery of intermediate injection of process variables

## 2.8.0

### Features
[**Issue 95**](https://github.com/viadee/vPAV/issues/95) Refactoring of process variable discovery (adapted to Camunda injection order)

### Fixes

### Misc

## 2.7.3

### Features

### Fixes
[**Issue 93**](https://github.com/viadee/vPAV/issues/93) Fixed endless recursion and StackOverflow

### Misc

## 2.7.2

### Features
[**Issue 88**](https://github.com/viadee/vPAV/issues/88) Dynamically assess initial state of process variables to enhance accuracy  
[**Issue 91**](https://github.com/viadee/vPAV/issues/91) Retrieve possible entry points to correctly inject initial process variable status

### Fixes

### Misc
[**Issue 73**](https://github.com/viadee/vPAV/issues/73) Improved usability of HTML output to interactively add/remove issues

## 2.7.1

### Features
[**Issue 76**](https://github.com/viadee/vPAV/issues/76) Extendend the implementation of Soot to broaden scope of ProcessVariableModelChecker 

### Fixes

### Misc
[**Issue 83**](https://github.com/viadee/vPAV/issues/83) Cleaned JavaDoc and source code  
[**Issue 79**](https://github.com/viadee/vPAV/issues/79) Checked the support for build tools such as Gradle

## 2.7.0

### Features
[**Issue 69**](https://github.com/viadee/vPAV/issues/69) Integration of static process variable analysis (limited scope)   
[**Issue 78**](https://github.com/viadee/vPAV/issues/78) Further enhancements for the DataFlowChecker  

### Fixes
[**Issue 75**](https://github.com/viadee/vPAV/issues/75) Fixed bugged output in case no errors have been found

### Misc

## 2.6.1

### Features

### Fixes
[**Issue 64**](https://github.com/viadee/vPAV/issues/64) Fixed varying issue ids due to usage of path separators  
[**Issue 48**](https://github.com/viadee/vPAV/issues/48) Fixed a bug where validation failed due to wrong folder reference  

### Misc
[**Issue 56**](https://github.com/viadee/vPAV/issues/56) Refactored checker singletons due to wrong output  
[**Issue 47**](https://github.com/viadee/vPAV/issues/47) OWASP check added and cleaned  

## 2.6.0

### Features
[**Issue 46**](https://github.com/viadee/vPAV/issues/46) Added a DSL to formulate rules in order to validate data flows  
[**Issue 28**](https://github.com/viadee/vPAV/issues/28) Added visualization of reads/writes of process variables (data flow) to HTML output  
[**Issue 43**](https://github.com/viadee/vPAV/issues/43) Creation of HTML output now optional  

### Fixes
[**Issue 58**](https://github.com/viadee/vPAV/issues/58) Fixed duplicated entries in HTML output table  
[**Issue 53**](https://github.com/viadee/vPAV/issues/53) Fixed wrongly created issues by IssueWriter  

### Misc

## 2.5.4

### Features

### Fixes
[**Issue 20**](https://github.com/viadee/vPAV/issues/20) Fixed bugged visual HTML output under OSX  
[**Issue 37**](https://github.com/viadee/vPAV/issues/37) Fixed a bug, where only the first issue on a modal was added to the list  

### Misc
[**Issue 39**](https://github.com/viadee/vPAV/issues/39) Analysis of process variables now only performed if checker is activated  
[**Issue 26**](https://github.com/viadee/vPAV/issues/26) Added example JUnit test for external checker. 

## 2.5.3

### Features

### Fixes

### Misc
[**Issue 25**](https://github.com/viadee/vPAV/issues/25) Refactoring of checker initialization  
[**Issue 24**](https://github.com/viadee/vPAV/issues/24) Redesigned behavior of VersioningChecker for package-based versioning. 

## 2.5.2

### Features
**vPAV-168** Added information for severity, added unique issue ID to modal  

### Fixes

### Misc

## 2.5.1

### Features

### Fixes
**vPAV-160** Fixed a bug where wrongly name checkers have not been identified  
**vPAV-191** Fixed a bug where issues have not been whitelisted properly  
**vPAV-192** Removed automatic linkage for external checker   

### Misc
**vPAV-183** SonarQube cleanup  
**vPAV-174** Changed logos and other visuals  

## 2.5.0

### Features
**vPAV-167** Added localization for DE and EN  
**vPAV-185** Added SignalEventChecker  

### Fixes

### Misc
**vPAV-187** Changed from BSD4 to BSD3 license  
**vPAV-172** Refactored CheckerIssue creation  

## 2.4.4

### Features
**vPAV-176** Added check for redundant sequence flows  

### Fixes
**vPAV-170** Added documentation for release management  
**vPAV-171** Removed <ModelConvention>  

### Misc
**vPAV-173** Altered visual output  

## 2.4.3-SNAPSHOT

### Features
**vPAV-69** Added ExtensionChecker  
**vPAV-165** Expanded ruleSet to include description tags  

### Fixes

### Misc 
**vPAV-162** Improved documentation for whitelisting issues  
**vPAV-164** Extended documentation for CheckerIssue  
**vPAV-169** Added JUnit test for external checker + documentation  

## 2.4.2

### Features
**vPAV-156** Added check for expressions in MessageStartEvents  

### Fixes
**vPAV-179** Fixed bug, where wordwrap lead to false positive     
**vPAV-178** Fixed sample project for showcasing purpose  

### Misc
**vPAV-169** Added JUnit test with external checker  
**vPAV-166** Added GitHub analysis of repository  
**vPAV-159** Added SonarQube analysis  
**vPAV-162** Added more detailed description for whitglisting issues  

## 2.4.1

### Features
**vPAV-161** Added possibility for downloading the model

### Fixes
**vPAV-157** Fixed HTML output bug

### Misc
**vPAV-158** Cleaned internal maven warnings

## 2.4.0

### Features
**vPAV-135** Added FieldInjectionChecker

### Fixes
**vPAV-155** Added warning for interface ActivityBehavior  
**vPAV-136** Added check for expressions in FieldInjectionChecker

### Misc
**vPAV-100** Added more documentation  
**vPAV-149** Check test coverage

## 2.3.0

### Features
**vPAV-139** Added success stories to output  
**vPAV-141** JavaDelegateChecker able to work with Mockito proxies   
**vPAV-124** Check forms for variables  
**vPAV-144** HTML output: button "Mark all Issues"  
**vPAV-145** Create constructor of external checker dynamically  
**vPAV-146** Functionality of MessageEventChecker extended  
**vPAV-148** Default path for XOR-Gateway as best practise

### Fixes
**vPAV-154** XorConventionChecker redirection on documentation fixed

### Misc
**vPAV-67** Refactoring of CheckerFactory + Checker  
**vPAV-147** Refactoring BPMNScanner  
**vPAV-151** Cleaned up internal docu

## 2.2.3

### Features

### Fixes
**vPAV-129** Classloader could not find files  

### Misc
**vPAV-34** Blogpost on https://blog.camunda.org/  
**vPAV-104** Grooming: Formal estimation of backlog items  
**vPAV-134** Cleaned up Glasbruchprozess for testing/presentation purposes  
**vPAV-122** Stopped support as Maven-Plugin  

## 2.2.2

### Features
**vPAV-133** MessageEvents now checked for field "Implementation"  

### Fixes
**vPAV-128** Variables in messages and processes could not be found  
**vPAV-123** NullPointer if no message is specified in MessageEvents  
**vPAV-130** Fix ProcessVariablesModelChecker  

### Misc
**vPAV-131** Improve documentation with JavaDoc   
**vPAV-132** Check code coverage  
**vPAV-101** Translation to english  

## 2.2.1

### Features
**vPAV-65** ExpressionChecker added  
**vPAV-120** ElementIdConventionChecker added  
**vPAV-121** Added CRON parsing to TimerEvents  

### Fixes
**vPAV-75** Improved stability of XML scan  

### Misc
**vPAV-109** Updated Camunda projects to latest vPAV version  
**vPAV-126** Fixed NullPointer for missing messages in MessageEvents  

## 2.2.0

### Features
**vPAV-93** TimerExpressionChecker added  
**vPAV-99** Added configurability for NoScriptChecker  
**vPAV-113** Effective RuleSet now written in output folder  
**vPAV-118** Added check for DelegateExpression in Listeners for UserTasks  
**vPAV-84** Added configurability for naming convention for XOR gateways and outgoing edges  

### Fixes
**vPAV-114** Moved ruleSet to src/test/resources   
**vPAV-115** Fixed ClassNotFoundException  
**vPAV-116** Fixed XorNamingChecker  
**vPAV-80** Fixed resolving javaResources at runtime  
**vPAV-81** Fixed ClassCastException when using PowerMocks  
**vPAV-92** Fixed HTML encoding   

### Misc
**vPAV-83** Improved stability for ProcessVariablesLocation rule  
**vPAV-110** Changed several output messages  
**vPAV-106** Moved output to subfolder of /target and renamed output files  
**vPAV-107** Overhauled styling of HTML output  

## 2.1.0

### Features

### Fixes
**vPAV-108** Inconsistencies with level WARNING don't cause build failures  

### Misc
**vPAV-24** HTML output improved  
**vPAV-78** Move project to GitHub  
**vPAV-87** Release blogpost on java.viadee.de  
**vPAV-85** Updated checklist for releases on Maven Central  
