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
package de.viadee.bpm.vPAV.beans;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

/**
 * Helper methods for Maven Plugin CamundaStaticValidator
 */
public class BeanMappingGenerator {

    /**
     * Generates bean mapping HashMap for jUnit start
     *
     * @param ctx
     *            ApplicationContext
     * @return beanNameToClassMap contains beanmapping
     */
    public static Map<String, String> generateBeanMappingFile(final ApplicationContext ctx) {

        final Map<String, String> beanNameToClassMap = new HashMap<String, String>();

        // read bean names
        final String[] beanDefinitionNames = ctx.getBeanDefinitionNames();
        for (final String beanName : beanDefinitionNames) {
            // don't add spring own classes
            if (!beanName.startsWith("org.springframework")) {
                final Object obj = ctx.getBean(beanName);
                if (obj != null) {
                    if (obj.getClass().getName().contains("$$")) {
                        String name = obj.getClass().getName();
                        while (name.contains("$$")) {
                            name = name.substring(0, name.lastIndexOf("$$"));
                        }
                        beanNameToClassMap.put(beanName, name);
                    } else {
                        beanNameToClassMap.put(beanName, obj.getClass().getName());
                    }
                }
            }
        }
        return beanNameToClassMap;
    }
}
