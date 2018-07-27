package com.episode6.hackit.deployable

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.*

class MavenDependencyConfigurator {

  Project project

  private Map<String, CustomConfigMapping> mappedConfigs = new HashMap<>()

  void putConfigMapping(String gradleConfig, String mavenScope, boolean optional = false) {
    CustomConfigMapping mapping = new CustomConfigMapping(gradleConfig: gradleConfig,
        mavenScope: mavenScope,
        optional: optional)
    mappedConfigs.put(mapping.gradleConfig, mapping)
  }

  void removeConfigMapping(String gradleConfig) {
    mappedConfigs.remove(gradleConfig)
  }

  void configureDependencies(Node pomRoot) {
    def depsNode = getDependencyNode(pomRoot)
    depsNode.children.clear() // start with no deps

    mappedConfigs.values().each { mappedConfig ->
      def config = project.configurations.findByName(mappedConfig.gradleConfig)
      if (config == null) {
        return
      }

      eachDependency(config) { ModuleDependency unresolvedDep, DepId depId ->
        def depNode = depsNode.appendNode('dependency')
        depNode.appendNode('groupId', depId.group)
        depNode.appendNode('artifactId', depId.name)
        depNode.appendNode('version', depId.version)
        depNode.appendNode('scope', mappedConfig.mavenScope)
        if (mappedConfig.optional) {
          depNode.appendNode('optional', true)
        }
        if (!unresolvedDep.excludeRules.isEmpty()) {
          def exclusionsNode = depNode.appendNode('exclusions')
          unresolvedDep.excludeRules.each { exRule ->
            def exNode = exclusionsNode.appendNode('exclusion')
            exNode.appendNode('groupId', exclusionValue(exRule.group))
            exNode.appendNode('artifactId', exclusionValue(exRule.module))
          }
        }
      }
    }
  }

  private static String exclusionValue(String input) {
    return input == null ? "*" : input
  }

  private static void eachDependency(Configuration config, DepToXmlMapper mapper) {
    def unresolvedDeps = config.dependencies
    def resolvedDeps = getResolvedDeps(config)

    unresolvedDeps.findAll { it instanceof ModuleDependency }.collect { (ModuleDependency) it }.each { unresolvedDep ->
      def resolvedDep = resolvedDeps.find { unresolvedDep.group == it.moduleGroup && unresolvedDep.name == it.moduleName }
      def depId = getDepId(unresolvedDep, resolvedDep)
      mapper.map(unresolvedDep, depId)
    }
  }

  private static Set<ResolvedDependency> getResolvedDeps(Configuration config) {
    config = config.copyRecursive().setTransitive(false)
    config.setCanBeResolved(true)
    return config.resolvedConfiguration.lenientConfiguration.getFirstLevelModuleDependencies()
  }

  private static Node getDependencyNode(Node pomRoot) {
    def deps = pomRoot.dependencies
    if (deps == null || deps.isEmpty()) {
      return pomRoot.appendNode('dependencies')
    } else {
      return pomRoot.dependencies[0]
    }
  }

  private static DepId getDepId(Dependency unresolvedDep, ResolvedDependency resolvedDep) {
    if (unresolvedDep instanceof ProjectDependency) {
      Project depProj = unresolvedDep.getDependencyProject()
      return new DepId(group: depProj.group, name: depProj.name, version: depProj.version)
    } else if (resolvedDep != null) {
      return new DepId(group: resolvedDep.moduleGroup, name: resolvedDep.moduleName, version: resolvedDep.moduleVersion)
    } else {
      throw new GradleException("Couldn't figure out dependency: ${unresolvedDep}")
    }
  }

  private static class DepId {
    String group
    String name
    String version
  }

  private static class CustomConfigMapping {
    String gradleConfig
    String mavenScope
    boolean optional
  }

  private interface DepToXmlMapper {
    void map(Dependency unresolvedDep, DepId depId)
  }
}