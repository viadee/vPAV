XOR Convention Checker
=================================
The XOR Convention Checker processes BPMN models and checks XOR gateways on predefined naming conventions and an existing default-path.

## Assumptions
- The **BPMN-models** have to be in the **classpath** at build time

## Configuration
The rule should be configured as follows:
```xml
<rule>
	<name>XorNamingConventionChecker</name>
	<state>true</state>
	<settings>
		<setting name="requiredDefault">true</setting>
	</settings>
	<elementConventions>
		<elementConvention>
			<name>convention</name>
			<pattern>[A-ZÄÖÜ][a-zäöü]*\\?</pattern>
		</elementConvention>
		<elementConvention>
			<name>convention2</name>
			<pattern>[A-ZÄÖÜ][a-zäöü]*</pattern>
		</elementConvention>
	</elementConventions>
</rule>

```
The setting allows you to configure whether a default path is required.
The pattern can be any regular expression and is useful to enforce naming conventions.  
"convention" refers to the naming convention of xor gateways themselves.  
"convention2" refers to the outgoing edges of a xor gateway.

## Error messages:
**Naming convention of XOR gate '%gatewayName%' not correct.**

_The default naming convention requires XOR gateways to end with "?"._

## Examples

| **Correct use of naming convention**                                                                        | 
|:------------------------------------------------------------------------------------------------------:| 
|![Correct use of naming convention](img/XorNamingConventionChecker.PNG "Correct naming convention specified")|
| |

