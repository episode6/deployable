package com.episode6.hackit.deployable

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Plugin to make Jars deployable
 */
class DeployableJarPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.plugins.apply(DeployablePlugin).pomPackaging = "jar"

    project.task("javadocJar", type: Jar, dependsOn: project.javadoc) {
      classifier = 'javadoc'
      from project.javadoc
    }

    project.task("sourcesJar", type: Jar) {
      from project.sourceSets.main.allSource
      classifier = 'sources'
    }

    project.artifacts {
      archives project.jar
      archives project.javadocJar
      archives project.sourcesJar
    }
  }
}
