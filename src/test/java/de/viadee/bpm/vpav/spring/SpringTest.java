/*
 * BSD 3-Clause License
 *
 * Copyright © 2022, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vpav.spring;

import de.viadee.bpm.vpav.IssueService;
import de.viadee.bpm.vpav.ProcessApplicationValidator;
import de.viadee.bpm.vpav.RuntimeConfig;
import de.viadee.bpm.vpav.constants.ConfigConstants;
import de.viadee.bpm.vpav.processing.model.data.CheckerIssue;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import soot.G;

import java.util.Collection;
import java.util.Properties;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { SayHelloDelegate.class })
public class SpringTest {

    @Autowired
    private ApplicationContext ctx;

    @BeforeClass
    public static void setup() {
        G.reset();
        IssueService.getInstance().clear();

    }

    @Test
    public void validateModel() {
        RuntimeConfig.getInstance().setTest(true);
        Properties properties = new Properties();
        properties.put("scanpath", ConfigConstants.TARGET_TEST_PATH + "de/viadee/bpm/vpav/spring/");
        properties.put("basepath", ConfigConstants.BASE_PATH_TEST + "spring/");
        properties.put("ruleSetPath", ConfigConstants.BASE_PATH_TEST + "spring/");
        RuntimeConfig.getInstance().setProperties(properties);
        Collection<CheckerIssue> issues = ProcessApplicationValidator.findModelInconsistencies(ctx);

        Assert.assertEquals("There should be one UR issue.", 1, issues.size());
    }
}
