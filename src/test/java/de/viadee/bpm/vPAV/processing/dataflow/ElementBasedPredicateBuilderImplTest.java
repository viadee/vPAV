/**
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing.dataflow;

import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElementBasedPredicateBuilderImplTest {

	@Test
	public void testWithPrefixDoesNotFilterCorrectPrefix() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("ext_name").build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).withPrefix("ext_");

		assertTrue(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString("ext_name"));
	}

	@Test
	public void testWithPrefixFiltersIncorrectPrefix() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("int_name").build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).withPrefix("ext_");

		assertFalse(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString("int_name"));
	}

	@Test
	public void testWithPostfixDoesNotFilterCorrectPostfix() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("name_post").build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).withPostfix("_post");

		assertTrue(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString("name_post"));
	}

	@Test
	public void testWithPostfixFiltersIncorrectPostfix() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("name_nopost").build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).withPostfix("_post");

		assertFalse(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString("name_nopost"));
	}

	@Test
	public void testOfTypeDoesNotFilterCorrectType() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().ofType(UserTask.class).build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).ofType(UserTask.class);

		assertTrue(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString(UserTask.class.getSimpleName()));
	}

	@Test
	public void testOfTypeFiltersIncorrectType() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().ofType(ServiceTask.class).build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).ofType(UserTask.class);

		assertFalse(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString(ServiceTask.class.getSimpleName()));
	}

	@Test
	public void testWithPropertyDoesNotFilterCorrectProperty() {
		List<BpmnElement> bpmnElements = Arrays
				.asList(new BpmnElementBuilder().withProperty("correctProperty").build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements)
				.withProperty("correctProperty");

		assertTrue(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString("present at 'element'"));
	}

	@Test
	public void testWithPropertyFiltersIncorrectProperty() {
		List<BpmnElement> bpmnElements = Arrays
				.asList(new BpmnElementBuilder().withProperty("incorrectProperty").build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements)
				.withProperty("correctProperty");

		assertFalse(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString("not present at 'element'"));
	}

	@Test
	public void testWithPropertyFiltersWithoutAnyProperty() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements)
				.withProperty("correctProperty");

		assertFalse(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString("not present at 'element'"));
	}

	@Test
	public void testWithNameMatchingDoesNotFilterMatch() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("hasAMATCH9").build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements)
				.withNameMatching(".*MATCH[0-9][A-Y]?");

		assertTrue(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString("hasAMATCH9"));
	}

	@Test
	public void testWithNameMatchingFiltersNoMatch() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("hasNoMATC9").build());

		EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements)
				.withNameMatching(".*MATCH[0-9][A-Y]?");

		assertFalse(result.isFulfilled());
		assertThat(result.getMessage().orElse(null), containsString("hasNoMATC9"));
	}

	@Test
	public void testThatFulfillDoesFilterCorrectlyForOnlyFlagInCaseOfSuccess() {
		List<BpmnElement> bpmnElements = Arrays.asList(
				new BpmnElementBuilder().withName("e1").ofType(ServiceTask.class).build(),
				new BpmnElementBuilder().withName("e2").ofType(ServiceTask.class).build(),
				new BpmnElementBuilder().withName("e3").ofType(ServiceTask.class).build());
		Function<DescribedPredicateEvaluator<ProcessVariable>, Object> conditionSetter = predicate -> {
			EvaluationResult<ProcessVariable> result = predicate.evaluate(new ProcessVariable(""));
			assertTrue(result.isFulfilled());
			assertThat(result.getMessage().orElse(null), is("e1, e2, e3"));
			return new Object();
		};
		Function<ProcessVariable, List<BpmnElement>> elementProvider = p -> bpmnElements;
		ElementBasedPredicateBuilderImpl<Object> predicateBuilder = new ElementBasedPredicateBuilderImpl<>(
				conditionSetter, elementProvider, true, "");

		predicateBuilder.thatFulfill(new DescribedPredicateEvaluator<>(
				e -> new EvaluationResult<>(ServiceTask.class.isInstance(e.getBaseElement()), e,
						e.getBaseElement().getAttributeValue("name")),
				""));
	}

	@Test
	public void testThatFulfillDoesFilterCorrectlyForOnlyFlagInCaseOfFailure() {
		List<BpmnElement> bpmnElements = Arrays.asList(
				new BpmnElementBuilder().withName("e1").ofType(ServiceTask.class).build(),
				new BpmnElementBuilder().withName("e2").ofType(UserTask.class).build(),
				new BpmnElementBuilder().withName("e3").ofType(ServiceTask.class).build());
		Function<DescribedPredicateEvaluator<ProcessVariable>, Object> conditionSetter = predicate -> {
			EvaluationResult<ProcessVariable> result = predicate.evaluate(new ProcessVariable(""));
			assertFalse(result.isFulfilled());
			assertThat(result.getMessage().orElse(null), is("e2"));
			return new Object();
		};
		Function<ProcessVariable, List<BpmnElement>> elementProvider = p -> bpmnElements;
		ElementBasedPredicateBuilderImpl<Object> predicateBuilder = new ElementBasedPredicateBuilderImpl<>(
				conditionSetter, elementProvider, true, "");

		predicateBuilder.thatFulfill(new DescribedPredicateEvaluator<>(
				e -> new EvaluationResult<>(ServiceTask.class.isInstance(e.getBaseElement()), e,
						e.getBaseElement().getAttributeValue("name")),
				""));
	}

	@Test
	public void testThatFulfillSetsCorrectDescription() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("e1").build());

		Function<DescribedPredicateEvaluator<ProcessVariable>, Object> conditionSetter = predicate -> {
			assertThat(predicate.getDescription(), is("all elements fulfilling this"));
			return new Object();
		};
		Function<ProcessVariable, List<BpmnElement>> elementProvider = p -> bpmnElements;
		ElementBasedPredicateBuilderImpl<Object> predicateBuilder = new ElementBasedPredicateBuilderImpl<>(
				conditionSetter, elementProvider, false, "all elements");

		predicateBuilder
				.thatFulfill(new DescribedPredicateEvaluator<>(EvaluationResult::forViolation, "fulfilling this"));
	}

	@Test
	public void testThatFulfillIncludesAllSuccessMessagesIfSuccess() {
		List<BpmnElement> bpmnElements = Arrays.asList(
				new BpmnElementBuilder().withName("e1").ofType(ServiceTask.class).build(),
				new BpmnElementBuilder().withName("e2").ofType(UserTask.class).build(),
				new BpmnElementBuilder().withName("e3").ofType(ServiceTask.class).build());
		Function<DescribedPredicateEvaluator<ProcessVariable>, Object> conditionSetter = predicate -> {
			EvaluationResult<ProcessVariable> result = predicate.evaluate(new ProcessVariable(""));
			assertThat(predicate.getDescription(), is("all elements fulfilling this"));
			assertTrue(result.isFulfilled());
			assertThat(result.getMessage().orElse(null), is("e1, e3"));
			return new Object();
		};
		Function<ProcessVariable, List<BpmnElement>> elementProvider = p -> bpmnElements;
		ElementBasedPredicateBuilderImpl<Object> predicateBuilder = new ElementBasedPredicateBuilderImpl<>(
				conditionSetter, elementProvider, false, "all elements");

		predicateBuilder.thatFulfill(new DescribedPredicateEvaluator<>(
				e -> new EvaluationResult<>(ServiceTask.class.isInstance(e.getBaseElement()), e,
						e.getBaseElement().getAttributeValue("name")),
				"fulfilling this"));
	}

	@Test
	public void testThatFulfillIncludesAllViolationsForNonSuccess() {
		List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("e1").build(),
				new BpmnElementBuilder().withName("e2").build(), new BpmnElementBuilder().withName("e3").build());
		Function<DescribedPredicateEvaluator<ProcessVariable>, Object> conditionSetter = predicate -> {
			EvaluationResult<ProcessVariable> result = predicate.evaluate(new ProcessVariable(""));
			assertFalse(result.isFulfilled());
			assertThat(result.getMessage().orElse(null), is("e1, e2, e3"));
			return new Object();
		};
		Function<ProcessVariable, List<BpmnElement>> elementProvider = p -> bpmnElements;
		ElementBasedPredicateBuilderImpl<Object> predicateBuilder = new ElementBasedPredicateBuilderImpl<>(
				conditionSetter, elementProvider, false, "");

		predicateBuilder.thatFulfill(new DescribedPredicateEvaluator<>(
				e -> EvaluationResult.forViolation(e.getBaseElement().getAttributeValue("name"), e), ""));
	}

	private static ElementBasedPredicateBuilderImpl<EvaluationResult<ProcessVariable>> createPredicateBuilderOn(
			List<BpmnElement> bpmnElements) {
		Function<DescribedPredicateEvaluator<ProcessVariable>, EvaluationResult<ProcessVariable>> conditionSetter = predicate -> predicate
				.evaluate(new ProcessVariable(""));
		Function<ProcessVariable, List<BpmnElement>> elementProvider = p -> bpmnElements;
		return new ElementBasedPredicateBuilderImpl<>(conditionSetter, elementProvider, false, "element description");
	}

	private class BpmnElementBuilder {
		private String name = "element";
		private Class<?> clazz = BaseElement.class;
		private String property;

		BpmnElementBuilder withName(String name) {
			this.name = name;
			return this;
		}

		BpmnElementBuilder ofType(Class<?> clazz) {
			this.clazz = clazz;
			return this;
		}

		BpmnElementBuilder withProperty(String property) {
			this.property = property;
			return this;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		BpmnElement build() {
			BaseElement baseElement = (BaseElement) mock(clazz);
			when(baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME)).thenReturn(name);
			// mocking a property is not so nice, but I could not find a nicer way
			if (property != null) {
				CamundaProperty camundaProperty = mock(CamundaProperty.class);
				when(camundaProperty.getCamundaName()).thenReturn(property);

				CamundaProperties properties = mock(CamundaProperties.class);
				when(properties.getCamundaProperties()).thenReturn(Arrays.asList(camundaProperty));

				Query query = mock(Query.class);
				when(query.count()).thenReturn(1);
				when(query.filterByType(CamundaProperties.class)).thenReturn(query);
				when(query.singleResult()).thenReturn(properties);

				ExtensionElements elements = mock(ExtensionElements.class);
				when(elements.getElementsQuery()).thenReturn(query);
				when(baseElement.getExtensionElements()).thenReturn(elements);
			}
			return new BpmnElement("processDefinition", baseElement, new ControlFlowGraph(), new FlowAnalysis());
		}
	}
}
