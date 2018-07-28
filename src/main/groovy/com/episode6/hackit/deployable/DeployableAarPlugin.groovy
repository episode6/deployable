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

    project.deployable.mainArtifact {
      artifact project.bundleRelease
    }

    project.android.libraryVariants.all { variant ->
      def javadocsTask = project.task("android${variant.name.capitalize()}Javadocs", type: Javadoc) {
        description "Generates Javadoc for $variant.name."
        options.memberLevel = JavadocMemberLevel.PRIVATE
        options.links("http://docs.oracle.com/javase/7/docs/api/");
        options.links("http://developer.android.com/reference/");
        exclude '**/BuildConfig.java'
        exclude '**/R.java'

        dependsOn variant.javaCompile
        mustRunAfter variant.javaCompile

        source = variant.javaCompile.source
        doFirst {
          classpath += project.files(project.android.bootClasspath)
          classpath += project.files(variant.javaCompile.classpath)
        }
      }

      def javadocJarTask = project.task("android${variant.name.capitalize()}JavadocsJar", type: Jar, dependsOn: javadocsTask) {
        classifier = 'javadoc'
        from javadocsTask
      }

      def sourcesJarTask = project.task("android${variant.name.capitalize()}SourcesJar", type: Jar) {
        classifier = 'sources'
        from variant.javaCompile.source
      }

      if (variant.name == "release") {
        project.deployable.publication {
          artifact javadocJarTask
          artifact sourcesJarTask
        }
      }
    }
  }
}
