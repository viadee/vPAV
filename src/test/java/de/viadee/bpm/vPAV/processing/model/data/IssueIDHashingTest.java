/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung AG
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
