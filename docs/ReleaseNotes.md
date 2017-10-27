# Release notes
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
