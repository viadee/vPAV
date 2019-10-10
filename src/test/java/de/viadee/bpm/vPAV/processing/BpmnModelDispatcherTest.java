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
package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.config.reader.ConfigReaderException;
import de.viadee.bpm.vPAV.config.reader.XmlConfigReader;
import de.viadee.bpm.vPAV.processing.checker.ElementChecker;
import de.viadee.bpm.vPAV.processing.checker.ExtensionChecker;
import de.viadee.bpm.vPAV.processing.checker.TimerExpressionChecker;
import de.viadee.bpm.vPAV.processing.checker.XorConventionChecker;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class BpmnModelDispatcherTest {
	private static ClassLoader cl;

	@BeforeClass
	public static void setup() throws MalformedURLException {
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java");
		final URL[] classUrls = { classUrl };
		cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().setTest(true);
	}

	@Test
	public void testCreateCheckerInstances() throws ConfigReaderException {
		// Load rule set.
		XmlConfigReader reader = new XmlConfigReader();
		RuleSet rules = reader.read("ruleSetChild.xml");

		BpmnScanner bpmnScanner = new BpmnScanner(
				(new File("src/test/resources/XorConventionChecker_false.bpmn")).getPath());

		FileScanner fileScanner = new FileScanner(rules);
		BpmnModelDispatcher dispatcher = new BpmnModelDispatcher();
		Collection<ElementChecker> checkerInstances = dispatcher
				.createCheckerInstances(fileScanner.getResourcesNewestVersions(), rules, bpmnScanner, null, null, null, null)[0];

		// Check if all checkers were created.
		assertEquals("Wrong number of loaded checkers.", 4, checkerInstances.size());
		int xor = 0, extension = 0, timer = 0;
		for (ElementChecker c : checkerInstances) {
			if (c instanceof XorConventionChecker) {
				xor++;
			} else if (c instanceof ExtensionChecker) {
				extension++;
			} else if (c instanceof TimerExpressionChecker) {
				timer++;
			}
		}
		assertEquals("Wrong number of loaded XorConventionCheckers.", 2, xor);
		assertEquals("Exactly one ExtensionChecker should exist.", 1, extension);
		assertEquals("Exactly one TimerExpressionChecker should exist.", 1, timer);
	}
}
