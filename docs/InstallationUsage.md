---
title: Installation and Usage
nav_order: 2
---
# Installation
If you use Maven in your project, getting started with the viadee Process Application Validator is very easy.
If you don´t use Maven, you can download the source code [here](https://github.com/viadee/vPAV/releases).

## Requirements
- JDK 8
- Camunda BPM Engine 7.4.0 and above

## Maven
Add the dependency to your POM:

```xml
<dependency>
  <groupId>de.viadee</groupId>
  <artifactId>viadeeProcessApplicationValidator</artifactId>
  <version>...</version>
  <scope>test</scope>
</dependency>
```

# Usage
To validate a model, create a JUnit-4 test. 
Don´t forget to add your usual Spring context, if you use Spring in your application.
To find the validation results, have a look at the [Output](Output.md) documentation.

The recommended name for this class is ModelConsistencyTest where you 
call the ProcessApplicationValidator by simply using code like the following:

```java
import de.viadee.bpm.vpav.ProcessApplicationValidator;
...
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { SpringTestConfig.class })
public class ModelConsistencyTest{
        
    @Autowired
    private ApplicationContext ctx;   
    
    @Test
    public void validateModel() {
        assertTrue("Model inconsistency found. Please check target folder for validation output",
                ProcessApplicationValidator.findModelErrors(ctx).isEmpty());
    }
}

```
Note, that the Validator receives the Spring context. Thereby, the validation can
check delegate Beans and their names. Passing `ctx` is only necessary for Spring applications. 

## Methods
The `ctx` parameter is optional. If **no** Spring context is used, JUnit can also be started without the context parameter.

- `findModelErrors(ctx)` finds all model inconsistencies with **ERROR** status.
- `findModelInconsistencies(ctx)` finds **all** model inconsistencies (Error, Warning, Info).


## SpringTestConfig

In order to evaluate beans in a Spring environment, you should specify a config class for your JUnit test

```java

import ServiceTaskOneDelegate;
import ServiceTaskTwoDelegate;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringTestConfig {

    public SpringTestConfig() {
        MockitoAnnotations.initMocks(this);
    }

    @InjectMocks
    private ServiceTaskOneDelegate serviceTaskOneDelegate;

    @InjectMocks
    private ServiceTaskTwoDelegate serviceTaskTwoDelegate;

    @Bean
    public ServiceTaskOneDelegate serviceTaskOneDelegate() {
        return serviceTaskOneDelegate;
    }

    @Bean
    public ServiceTaskTwoDelegate serviceTaskTwoDelegate() {
        return serviceTaskTwoDelegate;
    }

}
```

## Additionally required dependencies
```xml
<dependency>
	<groupId>org.mockito</groupId>
	<artifactId>mockito-all</artifactId>
	<version>...</version>
	<scope>test</scope>
</dependency>

<dependency>	
	<groupId>org.springframework</groupId>
	<artifactId>spring-test</artifactId>
	<version>..</version>
</dependency>
		
<dependency>
	<groupId>org.springframework</groupId>
	<artifactId>spring-beans</artifactId>
	<version>...</version>
</dependency>

<dependency>
	<groupId>junit</groupId>
	<artifactId>junit</artifactId>
	<version>4.13</version>
</dependency>
```
