package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.MavenPlugin
import org.gradle.plugins.signing.SigningPlugin

/**
 * Base deployable plugin. It is not referenced directly in gradle, but applied by either the jar or aar plugin
 */
class DeployablePlugin implements Plugin<Project> {

  String pomPackaging = null

  MavenConfigurator mavenConfig;

  static isReleaseBuild(Project project) {
    return project.version.contains("SNAPSHOT") == false
  }

  void apply(Project project) {
    project.plugins.apply(MavenPlugin)
    project.plugins.apply(SigningPlugin)

    mavenConfig = new MavenConfigurator(project: project)
    mavenConfig.prepare()

    project.ext.mavenDependencies = { Closure closure ->
      mavenConfig.mapConfigs(closure)
    }

    DeployablePluginExtension deployable = project.extensions.create(
        "deployable",
        DeployablePluginExtension,
        project)

    project.task("validateDeployable", type: DeployableValidationTask) {
      description = "Validates this project's deployable properties to ensure it can generate a valid pom."
      group = "verification"
    }
    
    // add deploy alias for uploadArchives task (because it's more fun to type)
    project.task("deploy", dependsOn: project.uploadArchives) {
      description = "A simple alias for uploadArchives, because it's more fun to say."
      group = "upload"
    }

    project.afterEvaluate {
      mavenConfig.configure(deployable, pomPackaging)
      project.tasks.findByPath("install")?.dependsOn project.validateDeployable
      project.tasks.findByPath("check")?.dependsOn project.validateDeployable
      project.tasks.findByPath("test")?.dependsOn project.validateDeployable
    }
  }
}
