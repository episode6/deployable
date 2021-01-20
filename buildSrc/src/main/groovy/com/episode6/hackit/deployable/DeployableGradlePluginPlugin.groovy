package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.addon.GroovyDocAddonPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin to make gradle plugins deployable
 * Referenced as 'com.episode6.hackit.deployable.gradle-plugin'
 */
class DeployableGradlePluginPlugin implements Plugin<Project> {
  void apply(Project project) {

    project.plugins.with {
      apply(DeployableJarPlugin)
      apply(GroovyDocAddonPlugin)
      apply('java-gradle-plugin')
    }

    project.afterEvaluate {
      // disable the java-gradle-plugin's build in publish tasks
      project.tasks.findByName("publishPluginMavenPublicationToMavenRepository")?.enabled = false
      project.tasks.findByName("publishPluginMavenPublicationToMavenLocal")?.enabled = false
      project.tasks.findByName("generatePomFileForPluginMavenPublication")?.enabled = false
      project.tasks.findByName("generateMetadataFileForPluginMavenPublication")?.enabled = false
    }
  }
}
