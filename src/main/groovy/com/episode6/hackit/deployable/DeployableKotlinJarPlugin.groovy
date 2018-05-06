package com.episode6.hackit.deployable

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Plugin to make Kotlin Jars deployable
 * Referenced as 'com.episode6.hackit.deployable.kt.jar'
 */
class DeployableKotlinJarPlugin implements Plugin<Project> {
  void apply(Project project) {

    project.configurations {
      api
      compile.extendsFrom api
    }

    project.plugins.apply(DeployablePlugin).pomPackaging = "jar"

    project.afterEvaluate {
      project.plugins.apply('org.jetbrains.dokka')

      project.task("javadocJar", type: Jar, dependsOn: project.dokka) {
        classifier = 'javadoc'
        from project.dokka
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
}
