package com.episode6.hackit.deployable

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Plugin to make Kotlin Jars deployable
 * Referenced as 'com.episode6.hackit.deployable.kt.jar'*/
class DeployableKotlinJarPlugin implements Plugin<Project> {
  void apply(Project project) {

    if (project.configurations.findByName("api") == null) {
      project.configurations {
        api
        compile.extendsFrom api
      }
    }

    project.plugins.apply(DeployablePlugin).pomPackaging = "jar"
    project.plugins.apply('org.jetbrains.dokka')

    project.tasks.dokka {
      reportUndocumented = false
    }

    project.task("javadocJar", type: Jar, dependsOn: project.dokka) {
      archiveClassifier = 'javadoc'
      from project.dokka
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
