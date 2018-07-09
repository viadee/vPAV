package de.viadee.bpm.vPAV.processing.model.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class IssueIDHashingTest {

	/**
	 * Hashes are used to identify (and ignore) issues. Hence, they need to depend
	 * on BPMN element IDs.
	 */
	@Test
	public void testElementIDsShouldImpactHash() {
		final CheckerIssue issue1 = new CheckerIssue("ruleName", "ruleDescription", CriticalityEnum.ERROR,
				"folder/file.bpmn", "elementId123", "elementName Some Name", "Message to be displayed");
		final CheckerIssue issue2 = new CheckerIssue("ruleName", "ruleDescription", CriticalityEnum.ERROR,
				"folder/file.bpmn", "elementId888", "elementName Some Name", "Message to be displayed");

		assertNotEquals("Element ID does not impact an issues hash", issue1.getId(), issue2.getId());
	}

	/**
	 * Hashes are used to identify (and ignore) issues on development and CI systems
	 * with different operating systems. Hence, they may not depend on operating
	 * system specific path delimiters.
	 */
	@Test
	public void testPathDelimitersShouldNotImpactHash() {
		final CheckerIssue issue1 = new CheckerIssue("ruleName", "ruleDescription", CriticalityEnum.ERROR,
				"folder/file.bpmn", "elementId123", "elementName Some Name", "Message to be displayed");
		final CheckerIssue issue2 = new CheckerIssue("ruleName", "ruleDescription", CriticalityEnum.ERROR,
				"folder\\file.bpmn", "elementId123", "elementName Some Name", "Message to be displayed");

		assertEquals("Issue hash may not depend on operating systeme specific path delimiters", issue1.getId(),
				issue2.getId());
	}

}
