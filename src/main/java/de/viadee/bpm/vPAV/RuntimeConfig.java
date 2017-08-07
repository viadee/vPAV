package de.viadee.bpm.vPAV;

import java.net.MalformedURLException;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

public class RuntimeConfig {

    private static RuntimeConfig instance;

    private Map<String, String> beanMap;

    private ClassLoader classLoader;

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

}
