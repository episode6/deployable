package com.episode6.hackit.deployable

import org.gradle.api.Project

/**
 *
 */
class MavenConfig {

  static ConfigToScopeMapper configMapper(Project project) {
    return new ConfigToScopeMapper(project: project)
  }

  static class ConfigToScopeMapper {
    Project project

    ConfigToScopeMapper map(String gradleConfigName, String mavenScope) {
      def config = project.configurations.findByName(gradleConfigName)
      if (config != null) {
        project.conf2ScopeMappings.addMapping(0, config, mavenScope)
      }
      return this
    }
  }
}
