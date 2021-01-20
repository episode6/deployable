package com.episode6.hackit.deployable

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Plugin to make Jars deployable
 * Referenced as 'com.episode6.hackit.deployable.jar'
 */
class DeployableJarPlugin implements Plugin<Project> {
  void apply(Project project) {

    project.plugins.apply(DeployablePlugin).pomPackaging = "jar"

    project.task("javadocJar", type: Jar, dependsOn: project.javadoc) {
      archiveClassifier = 'javadoc'
      from project.javadoc
    }

    project.task("sourcesJar", type: Jar) {
      from project.sourceSets.main.allSource
      archiveClassifier = 'sources'
    }

    project.deployable.publication {
      main {
        artifact project.jar
      }
      amendSources {
        artifact project.sourcesJar
      }
      amendDocs {
        artifact project.javadocJar
      }
    }
  }
}
