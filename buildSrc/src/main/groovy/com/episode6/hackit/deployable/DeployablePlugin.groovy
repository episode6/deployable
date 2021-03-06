package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
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
    project.plugins.apply(MavenPublishPlugin)
    project.plugins.apply(SigningPlugin)

    DeployablePluginExtension deployable = project.extensions.create(
        "deployable",
        DeployablePluginExtension,
        project)

    mavenConfig = new MavenConfigurator(project: project, deployable: deployable)
    mavenConfig.prepare()

    project.task("validateDeployable", type: DeployableValidationTask) {
      description = "Validates this project's deployable properties to ensure it can generate a valid pom."
      group = "verification"
    }
    
    // add deploy alias for uploadArchives task (because it's more fun to type)
    project.task("deploy", dependsOn: project.publish) {
      description = "A simple alias for publish, because it's more fun to say."
      group = "publishing"
    }

    // add install alias for publishToMavenLocal to maintain compatibility with old versions of deployable
    project.task('install', dependsOn: project.publishToMavenLocal) {
      description = 'A simple alias for publishToMavenLocal to maintain compatibility with old versions of deployable.'
      group = 'publishing'
    }

    project.afterEvaluate {
      mavenConfig.configure(pomPackaging)
      // TODO fix tasks
      project.tasks.findByPath("publishMavenArtifactsPublicationToMavenRepository")?.dependsOn project.validateDeployable
      project.tasks.findByPath("publishMavenArtifactsPublicationToMavenLocal")?.dependsOn project.validateDeployable
      project.tasks.findByPath("check")?.dependsOn project.validateDeployable
      project.tasks.findByPath("test")?.dependsOn project.validateDeployable
    }
  }
}
