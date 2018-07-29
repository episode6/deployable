package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency

class MavenDependencyConfigurator {

  Project project
  DeployablePluginExtension deployable

  void configureDependencies(Node pomRoot) {
    def depsNode = getDependencyNode(pomRoot)
    depsNode.children.clear() // start with no deps

    deployable.pom.dependencyConfigurations.map.values().each { mappedConfig ->
      def config = project.configurations.findByName(mappedConfig.gradleConfig)
      if (config == null) {
        return
      }

      eachDependency(config) { DepId depId ->
        def depNode = depsNode.appendNode('dependency')
        depNode.appendNode('groupId', depId.group)
        depNode.appendNode('artifactId', depId.name)
        depNode.appendNode('version', depId.version)
        depNode.appendNode('scope', mappedConfig.mavenScope)
        if (mappedConfig.optional) {
          depNode.appendNode('optional', true)
        }
        if (!depId.unresolved.excludeRules.isEmpty()) {
          def exclusionsNode = depNode.appendNode('exclusions')
          depId.unresolved.excludeRules.each { exRule ->
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

    unresolvedDeps.findAll { it instanceof ModuleDependency }.collect {
      ModuleDependency unresolvedDep = (ModuleDependency) it
      ResolvedDependency resolvedDep = resolvedDeps.find { unresolvedDep.group == it.moduleGroup && unresolvedDep.name == it.moduleName }
      return getDepId(unresolvedDep, resolvedDep)
    }.findAll { it != null }.each {
      mapper.map(it)
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

  private static DepId getDepId(ModuleDependency unresolvedDep, ResolvedDependency resolvedDep) {
    if (unresolvedDep instanceof ProjectDependency) {
      Project depProj = unresolvedDep.getDependencyProject()
      return new DepId(group: depProj.group, name: depProj.name, version: depProj.version, unresolved: unresolvedDep)
    } else if (resolvedDep != null) {
      return new DepId(group: resolvedDep.moduleGroup, name: resolvedDep.moduleName, version: resolvedDep.moduleVersion, unresolved: unresolvedDep)
    }

    println "Warning: Deployable skipped mapping dependency: ${unresolvedDep}"
    return null
  }

  private static class DepId {
    ModuleDependency unresolved
    String group
    String name
    String version
  }

  private interface DepToXmlMapper {
    void map(DepId depId)
  }
}