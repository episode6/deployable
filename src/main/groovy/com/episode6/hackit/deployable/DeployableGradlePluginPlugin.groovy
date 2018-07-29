package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.addon.GroovyDocAddonPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

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

    // disable the java-gradle-plugin's build in publish tasks
    project.tasks.withType(PublishToMavenRepository).whenTaskAdded { t ->
      if (t.name.startsWith("publishPluginMaven")) {
        t.enabled = false
      }
    }
  }
}
