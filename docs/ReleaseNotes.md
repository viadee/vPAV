# Release notes

## 2.5.2-SNAPSHOT

### Features
**vPAV-168** Added information for severity, added unique issue ID to modal

### Fixes

### Misc

## 2.5.1-SNAPSHOT

### Features

### Fixes
**vPAV-160** Fixed a bug where wrongly name checkers have not been identified  
**vPAV-191** Fixed a bug where issues have not been whitelisted properly  
**vPAV-192** Removed automatic linkage for external checker   

### Misc
**vPAV-183** SonarQube cleanup  
**vPAV-174** Changed logos and other visuals  

## 2.5.0-SNAPSHOT

### Features
**vPAV-167** Added localization for DE and EN  
**vPAV-185** Added SignalEventChecker  

### Fixes

### Misc
**vPAV-187** Changed from BSD4 to BSD3 license  
**vPAV-172** Refactored CheckerIssue creation  

## 2.4.4-SNAPSHOT

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

## 2.4.2-SNAPSHOT

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

## 2.4.1-SNAPSHOT

### Features
**vPAV-161** Added possibility for downloading the model

### Fixes
**vPAV-157** Fixed HTML output bug

### Misc
**vPAV-158** Cleaned internal maven warnings

## 2.4.0-SNAPSHOT

### Features
**vPAV-135** Added FieldInjectionChecker

### Fixes
**vPAV-155** Added warning for interface ActivityBehavior  
**vPAV-136** Added check for expressions in FieldInjectionChecker

### Misc
**vPAV-100** Added more documentation  
**vPAV-149** Check test coverage

## 2.3.0-SNAPSHOT

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

## 2.2.3-SNAPSHOT

### Features

### Fixes
**vPAV-129** Classloader could not find files  

### Misc
**vPAV-34** Blogpost on https://blog.camunda.org/  
**vPAV-104** Grooming: Formal estimation of backlog items  
**vPAV-134** Cleaned up Glasbruchprozess for testing/presentation purposes  
**vPAV-122** Stopped support as Maven-Plugin  

## 2.2.2-SNAPSHOT

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

## 2.2.1-SNAPSHOT

### Features
**vPAV-65** ExpressionChecker added  
**vPAV-120** ElementIdConventionChecker added  
**vPAV-121** Added CRON parsing to TimerEvents  

### Fixes
**vPAV-75** Improved stability of XML scan  

### Misc
**vPAV-109** Updated Camunda projects to latest vPAV version  
**vPAV-126** Fixed NullPointer for missing messages in MessageEvents  

## 2.2.0-SNAPSHOT

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

## 2.1.0-SNAPSHOT

### Features

### Fixes
**vPAV-108** Inconsistencies with level WARNING don't cause build failures  

### Misc
**vPAV-24** HTML output improved  
**vPAV-78** Move project to GitHub  
**vPAV-87** Release blogpost on java.viadee.de  
**vPAV-85** Updated checklist for releases on Maven Central  
