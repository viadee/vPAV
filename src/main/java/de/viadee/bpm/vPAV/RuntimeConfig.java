/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the viadee Unternehmensberatung GmbH.
 * 4. Neither the name of the viadee Unternehmensberatung GmbH nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <viadee Unternehmensberatung GmbH> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV;

import java.net.MalformedURLException;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

public class RuntimeConfig {

    private static RuntimeConfig instance;

    private Map<String, String> beanMap;

    private ClassLoader classLoader;

    private boolean test = false;

    private final String[] allRules = { "ProcessVariablesLocation", "XorNamingConventionChecker",
            "TimerExpressionChecker", "JavaDelegateChecker", "NoScriptChecker", "NoExpressionChecker",
            "EmbeddedGroovyScriptChecker", "VersioningChecker", "DmnTaskChecker", "ProcessVariablesModelChecker",
            "ProcessVariablesNameConventionChecker", "TaskNamingConventionChecker", "ElementIdConventionChecker" };

    private RuntimeConfig() {
    }

    public static RuntimeConfig getInstance() {
        if (RuntimeConfig.instance == null) {
            RuntimeConfig.instance = new RuntimeConfig();
        }
        return RuntimeConfig.instance;
    }

    public String findBeanByName(String string) {
        if (string != null && !string.isEmpty() && beanMap != null && !beanMap.isEmpty()) {
            return beanMap.get(string);
        } else
            return null;
    }

    public void setBeanMapping(Map<String, String> beanMap) {
        this.beanMap = beanMap;
    }

    public Map<String, String> getBeanMapping() {
        return beanMap;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ClassLoader getClassLoader(MavenProject project)
            throws MalformedURLException, DependencyResolutionRequiredException {
        return FileScanner.getClassLoader(project);
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public String[] getAllRules() {
        return allRules;
    }

}
