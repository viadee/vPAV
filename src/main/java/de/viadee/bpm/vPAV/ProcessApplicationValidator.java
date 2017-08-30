package de.viadee.bpm.vPAV;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.context.ApplicationContext;

import de.viadee.bpm.vPAV.beans.BeanMappingGenerator;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class ProcessApplicationValidator extends AbstractRunner {

    /**
     * find issues with given ApplicationContext (Spring)
     * 
     * @param ctx
     *            spring context
     * @return all issues
     */
    public static Collection<CheckerIssue> findModelInconsistencies(ApplicationContext ctx) {

        RuntimeConfig.getInstance().setBeanMapping(BeanMappingGenerator.generateBeanMappingFile(ctx));
        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());
        run_vPAV();

        return AbstractRunner.getfilteredIssues();
    }

    /**
     * find issues with given ApplicationContext (Spring)
     * 
     * @param ctx
     *            spring context
     * @return issues with status error
     */
    public static Collection<CheckerIssue> findModelErrors(ApplicationContext ctx) {

        RuntimeConfig.getInstance().setBeanMapping(BeanMappingGenerator.generateBeanMappingFile(ctx));
        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());
        run_vPAV();

        return filterErrors(AbstractRunner.getfilteredIssues(), CriticalityEnum.ERROR);
    }

    /**
     * find model errors without spring context
     * 
     * @return all issues
     */
    public static Collection<CheckerIssue> findModelInconsistencies() {

        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());
        run_vPAV();

        return AbstractRunner.getfilteredIssues();
    }

    /**
     * find model errors without spring context
     * 
     * @return issues with status error
     */
    public static Collection<CheckerIssue> findModelErrors() {

        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());
        run_vPAV();

        return filterErrors(AbstractRunner.getfilteredIssues(), CriticalityEnum.ERROR);
    }

    /**
     * filter an issue collection by status
     * 
     * @param filteredIssues
     * @param status
     * @return issues with status
     */
    private static Collection<CheckerIssue> filterErrors(Collection<CheckerIssue> filteredIssues,
            CriticalityEnum status) {
        Collection<CheckerIssue> filteredErrors = new ArrayList<CheckerIssue>();

        for (CheckerIssue issue : filteredIssues) {
            if (issue.getClassification().equals(status)) {
                filteredErrors.add(issue);
            }
        }
        return filteredErrors;
    }
}
