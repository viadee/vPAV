package de.viadee.bpm.vPAV.processing.dataflow;

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElementBasedPredicateBuilderImplTest {

    @Test
    public void testWithPrefixDoesNotFilterCorrectPrefix() {
        List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("ext_name").build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).withPrefix("ext_");

        assertThat(result.isFulfilled(), is(true));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString("ext_name"));
    }

    @Test
    public void testWithPrefixFiltersIncorrectPrefix() {
        List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().withName("int_name").build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).withPrefix("ext_");

        assertThat(result.isFulfilled(), is(false));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString("int_name"));
    }

    @Test
    public void testOfTypeDoesNotFilterCorrectType() {
        List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().ofType(UserTask.class).build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).ofType(UserTask.class);

        assertThat(result.isFulfilled(), is(true));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString(UserTask.class.getName()));
    }

    @Test
    public void testOfTypeFiltersIncorrectType() {
        List<BpmnElement> bpmnElements = Arrays.asList(new BpmnElementBuilder().ofType(ServiceTask.class).build());

        EvaluationResult<ProcessVariable> result = createPredicateBuilderOn(bpmnElements).ofType(UserTask.class);

        assertThat(result.isFulfilled(), is(false));
        assertThat(result.getMessage().isPresent(), is(true));
        assertThat(result.getMessage().get(), containsString(ServiceTask.class.getName()));
    }

    private static ElementBasedPredicateBuilderImpl<EvaluationResult<ProcessVariable>> createPredicateBuilderOn(List<BpmnElement> bpmnElements) {
        Function<DescribedPredicateEvaluator<ProcessVariable>, EvaluationResult<ProcessVariable>> conditionSetter = predicate ->
                predicate.evaluate(new ProcessVariable(""));
        Function<ProcessVariable, List<BpmnElement>> elementProvider = p -> bpmnElements;
        return new ElementBasedPredicateBuilderImpl<>(
                conditionSetter, elementProvider,  "element description");
    }

    private class BpmnElementBuilder {
        private String name = "element";
        private Class clazz = BaseElement.class;

        BpmnElementBuilder withName(String name) {
            this.name = name;
            return this;
        }

        BpmnElementBuilder ofType(Class clazz) {
            this.clazz = clazz;
            return this;
        }

        BpmnElement build() {
            BaseElement baseElement = (BaseElement) mock(clazz);
            when(baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME)).thenReturn(name);
            return new BpmnElement("processDefinition", baseElement);
        }
    }
}
