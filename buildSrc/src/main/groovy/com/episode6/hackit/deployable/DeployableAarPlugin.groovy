package com.episode6.hackit.deployable

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.JavadocMemberLevel

/**
 * Plugin to make Aars (android artifacts) deployable.
 * Referenced as 'com.episode6.hackit.deployable.aar'*/
class DeployableAarPlugin implements Plugin<Project> {
  void apply(Project project) {
    DeployablePlugin deployablePlugin = project.plugins.apply(DeployablePlugin)
    deployablePlugin.pomPackaging = "aar"

    project.deployable.publication {
      main {
        artifact project.bundleReleaseAar
      }
      amendSources {
        artifact project.androidReleaseSourcesJar
      }
      amendDocs {
        artifact project.androidReleaseJavadocsJar
      }
    }

    project.android.libraryVariants.all { variant ->
      def javadocsTask = project.task("android${variant.name.capitalize()}Javadocs", type: Javadoc) {
        description "Generates Javadoc for $variant.name."
        options.memberLevel = JavadocMemberLevel.PRIVATE
        options.links("http://docs.oracle.com/javase/7/docs/api/");
        options.links("http://developer.android.com/reference/");
        exclude '**/BuildConfig.java'
        exclude '**/R.java'

        dependsOn variant.javaCompileProvider.get()
        mustRunAfter variant.javaCompileProvider.get()

        println variant.javaCompileProvider.get().source

        source = variant.javaCompileProvider.get().source.collect() + project.android.sourceSets.main.java.srcDirs.collect() +
            project.android.sourceSets.findByName(variant.dirName)?.java?.srcDirs?.collect()
        doFirst {
          classpath += project.files(project.android.bootClasspath)
          classpath += project.files(variant.javaCompileProvider.get().classpath)
        }
      }

      project.task("android${variant.name.capitalize()}JavadocsJar", type: Jar, dependsOn: javadocsTask) {
        classifier = 'javadoc'
        from javadocsTask
      }

      project.task("android${variant.name.capitalize()}SourcesJar", type: Jar) {
        classifier = 'sources'
        from variant.javaCompileProvider.get().source.collect() + project.android.sourceSets.main.java.srcDirs.collect() +
            project.android.sourceSets.findByName(variant.dirName)?.java?.srcDirs?.collect()
      }
    }
  }
}
