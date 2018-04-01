package com.episode6.hackit.deployable

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc

/**
 * Plugin to make Aars (android artifacts) deployable.
 * Referenced as 'com.episode6.hackit.deployable.aar'
 */
class DeployableAarPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.plugins.apply(DeployablePlugin).pomPackaging = "aar"

    project.afterEvaluate {
      project.task("androidJavadocs", type: Javadoc) {
        source = project.android.sourceSets.main.java.srcDirs
        classpath += project.files(project.android.getBootClasspath().join(File.pathSeparator))
        classpath += project.configurations.compile
      }
      project.task("androidJavadocsJar", type: Jar, dependsOn: project.androidJavadocs) {
        classifier = 'javadoc'
        from project.androidJavadocs
      }
      project.task("androidSourcesJar", type: Jar) {
        classifier = 'sources'
        from project.android.sourceSets.main.java.srcDirs
      }

      project.artifacts {
        archives project.androidSourcesJar
        archives project.androidJavadocsJar
      }

      // android libs appear to have a problem mapping implementation -> runtime
      // this re-writes the pom as needed
      project.uploadArchives.repositories.mavenDeployer.pom.whenConfigured { pom ->
        project.configurations.implementation.dependencies.each { dep ->
          pom.dependencies.find { pomDep ->
            pomDep.groupId == dep.group && pomDep.artifactId == dep.name
          }.scope = "runtime"
        }
      }
    }
  }
}
