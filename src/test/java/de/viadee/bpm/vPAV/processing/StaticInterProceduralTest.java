/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung GmbH
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

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.Runner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

public class StaticInterProceduralTest {

  private static Runner runner;

  private static ClassLoader oldClassLoader;

  @BeforeClass
  public static void setup() throws MalformedURLException {
    // Prepare for post-test cleanup
    oldClassLoader = RuntimeConfig.getInstance().getClassLoader();
    RuntimeConfig.getInstance().setClassLoader(StaticInterProceduralTest.class.getClassLoader());
    RuntimeConfig.getInstance().setTest(true);
    runner = new Runner();
  }

  @AfterClass
  public static void tearDown() {
    RuntimeConfig.getInstance().setClassLoader(oldClassLoader);
  }

  @Test
  public void testInterProceduralAnalysis()
      throws ParserConfigurationException, SAXException, IOException {
    // Given
    runner.viadeeProcessApplicationValidator(ConfigConstants.TEST_JAVAPATH);

    // When
    final Map<String, ProcessVariableOperation> variables =
        new JavaReaderStatic()
            .getVariablesFromJavaDelegate(
                "de.viadee.bpm.vPAV.delegates.TestDelegateStaticInterProc", null, null, null, null);
    // Then
    assertEquals(
        "Static reader should also find variable from TestInterProcAnother class and TestInterPocOther",
        5,
        variables.size());
  }
}
