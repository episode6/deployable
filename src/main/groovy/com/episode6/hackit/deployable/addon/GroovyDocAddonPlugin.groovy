package com.episode6.hackit.deployable.addon

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Addon plugin to package and upload groovydocs. Use this in conjunction with `com.episode6.hackit.deployable.jar'
 * Referenced as 'com.episode6.hackit.deployable.addon.groovydocs'
 */
class GroovyDocAddonPlugin implements Plugin<Project>{
  @Override
  void apply(Project project) {
    project.task("groovydocJar", type: Jar, dependsOn: project.groovydoc) {
      classifier = 'groovydoc'
      from project.groovydoc
    }

    project.artifacts {
      archives project.groovydocJar
    }
  }
}
