# viadee Process Application Validator (vPAV)

The tool checks Camunda projects for consistency and discovers errors in process-driven applications.
Called as a Maven plugin or JUnit test, it discovers esp. inconsistencies of a given BPMN model in the classpath and the sourcecode of an underlying java project, 
such as a delegate reference to a non-existing java class or a non-existing Spring bean.

Find a list of the consistency checks below.

We recommend to integrate the consistency check in your CI builds - you can't find these inconsistencies early enough.

# Features

## Checker
Consistency checks are performed by individual modules called checkers, which search for certain types of inconsistencies. Currently, the following checkers are implemented: 

| Checker                                                                              | Summary                                                                  | Status       |
| ------------------------------------------------------------------------------------ | ----------------------------------------------------------------------   | ------------ |
|[JavaDelegateChecker](JavaDelegateChecker.md)                                         | Is the implementation (or Spring bean reference) available and usable?   | Done         |
|[DmnTaskChecker](DmnTaskChecker.md)                                                   | Is the implementation available?                                         | Done         |
|[EmbeddedGroovyScriptChecker](EmbeddedGroovyScriptChecker.md)                         | Is the implementation available and does it look like a script?          | Done         |
|[ProcessVariablesModelChecker](ProcessVariablesModelChecker.md)                       | Are process variables in the model provided in the code for all paths?   | Experimental |
|[ProcessVariablesNameConventionChecker](ProcessVariablesNameConventionChecker.md)     | Do process variables in the model fit into a desired regex pattern?      | Done         |
|[TaskNamingConventionChecker](TaskNamingConventionChecker.md)                         | Do task names in the model fit into a desired regex pattern?             | Done         |
|[VersioningChecker](VersioningChecker.md)                                             | Are java classes implementing tasks fit to a version scheme?             | Done         |
|[XorNamingConventionChecker](XorNamingConventionChecker.md)                           | Are XOR gateways ending with "?"                                         | Done         |
|[NoScriptChecker](NoScriptChecker.md)                                                 | Is there any script in the model?                                        | Done         |

All of these can be switched on or off as required. Implementing further checkers is rather simple.

## Output

The result of the check is first of all a direct one: if at least one inconsistency is 
found on the ERROR level, it will break your build or count as a failed unit 
test which will break your build too.

Further, the consistency check will provide an XML version, a JSON version and
an visual version based on  [BPMN.io](https://bpmn.io/) of all errors and warnings found.

### Visual output
The header contains the name of the current model. Below the heading, you can select a different model of the project to be displayed.
You can zoom in and out with the mouse wheel and move the model by click and hold.
In the BPMN model, the elements with errors are highlighted. Error categories are indicated by color. 
An overlay specifies the number of errors found on an element. Details can be seen by clicking on the overlay.
All errors are laid out in a table below the model. Clicking on the _rulename opens_ the corresponding documentation.
Clicking on the _Element-Id_ or _invalid sequenzflow_ marks the corresponding element(s) in the model.
<br/><br/>
[Here](Output.md) you can find an example of the output. 

## Requirements
- Camunda BPM Engine 7.4.0 and above

## Installation/Usage
There are two ways of installation. We recommend to use the JUnit approach as follows.

### Maven
You can start the validation as a Maven plugin. Therefore, add the dependency to your POM:

```xml
<dependency>
  <groupId>de.viadee.bpm</groupId>
  <artifactId>viadeeProcessApplicationValidator</artifactId>
  <version>...</version>
</dependency>
```

Then, use the following maven goal to start the validation.  
```java
de.viadee.bpm:viadeeProcessApplicationValidator:2.0.0-SNAPSHOT:check
```
Please note: This approach is not useful, if you use Spring managed java delegates in your processes.

### JUnit
Configure a JUnit-4 Test to fire up your usual Spring context - esp. delegates referenced in the process, 
if you use Spring in your application or a simple test case otherwise to call the consistency check.

The recommended name for this class is ModelConsistencyTest, where you 
call the ProcessApplicationValidator by simply using code like the following:

```java
import de.viadee.bpm.vPAV.ProcessApplicationValidator;
...
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { SpringTestConfig.class })
public class ModelConsistencyTest{
        
    @Autowired
    private ApplicationContext ctx;   
    
    @Test
    public void errorsInModelMustBeFound() {
        assertTrue("Model inconsistency found. Please check target folder for validation output",
                ProcessApplicationValidator.findModelInconsistencies(ctx).isEmpty());
    }
}

```
Note, that the Validator receives the Spring context. Thereby, the validation can
check delegate Beans and their names.


If __no__ Spring context is used, jUnit can also be started without the context parameter:
```java
assertTrue("Model inconsistency found. Please check target folder for validation output",
                ProcessApplicationValidator.findModelInconsistencies().isEmpty());
````

## Commitments
This library will remain under an open source licence indefinately.

We follow the [semantic versioning](http://semver.org) scheme (2.0.0).

In the sense of semantic versioning, the resulting XML and JSON outputs are the _only public API_ provided here. 
We will keep these as stable as possible, in order to enable users to analyse and integrate results into the toolsets of their choice.

## Cooperation
Feel free to report issues, questions, ideas or patches. We are looking forward to it.

## Licenses
All licenses can be found on the [maven site] (viadeeProcessApplicationValidator/docs/MavenSite/project-info.html)

**Additional license:** 

[BPMN.io](https://bpmn.io/license/)

**License (BSD4)** <br/>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. All advertising materials mentioning features or use of this software
    must display the following acknowledgement:
    This product includes software developed by the viadee Unternehmensberatung GmbH.
 4. Neither the name of the viadee Unternehmensberatung GmbH nor the
    names of its contributors may be used to endorse or promote products
    derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
