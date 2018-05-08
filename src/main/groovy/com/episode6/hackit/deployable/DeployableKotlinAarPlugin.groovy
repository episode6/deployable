package com.episode6.hackit.deployable

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Plugin to make Kotlin Jars deployable
 * Referenced as 'com.episode6.hackit.deployable.kt.jar'
 */
class DeployableKotlinAarPlugin implements Plugin<Project> {
  void apply(Project project) {
    DeployablePlugin deployablePlugin = project.plugins.apply(DeployablePlugin)
    deployablePlugin.pomPackaging = "aar"

    project.afterEvaluate {
      project.plugins.apply('org.jetbrains.dokka-android')
//      project.android.sourceSets.main.java.srcDirs += 'src/main/kotlin'

      project.task("javadocJar", type: Jar, dependsOn: project.dokka) {
        classifier = 'javadoc'
        from project.dokka
      }

      project.artifacts {
        archives project.javadocJar
      }

      project.android.libraryVariants.all { variant ->

        def sourcesJarTask = project.task("android${variant.name.capitalize()}SourcesJar", type: Jar) {
          classifier = 'sources'
          from variant.javaCompile.source.collect() +
              project.android.sourceSets.main.java.srcDirs.collect() +
              project.android.sourceSets.findByName(variant.dirName)?.java?.srcDirs?.collect()
        }

        if (variant.name == "release") {
          project.artifacts {
            archives sourcesJarTask
          }
        }
      }

      AndroidHacks.applyPomImplementationOverride(project, deployablePlugin)
    }
  }
}
