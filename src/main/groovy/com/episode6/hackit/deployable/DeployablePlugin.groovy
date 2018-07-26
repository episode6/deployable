package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

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
    project.task("deploy", dependsOn: project.publish) {
      description = "A simple alias for publish, because it's more fun to say."
      group = "publishing"
    }

    // add install alias for publishToMavenLocal to maintain compatibility with old versions of deployable
    project.task('install', dependsOn: project.publishToMavenLocal) {
      description = 'A simple alias for publishToMavenLocal to maintain compatibility with old versions of deployable.'
      group = 'publishing'
    }

    // disable any publications created by the java-gradle-plugin
    // TODO: figure out a saner way to do this
    project.tasks.withType(PublishToMavenRepository).whenTaskAdded { t ->
      if (t.name.startsWith("publishPluginMaven")) {
        t.enabled = false
      }
    }

    project.afterEvaluate {
      mavenConfig.configure(deployable, pomPackaging)
      // TODO fix tasks
      project.tasks.findByPath("publishMavenArtifactsPublicationToMavenRepository")?.dependsOn project.validateDeployable
      project.tasks.findByPath("publishMavenArtifactsPublicationToMavenLocal")?.dependsOn project.validateDeployable
      project.tasks.findByPath("check")?.dependsOn project.validateDeployable
      project.tasks.findByPath("test")?.dependsOn project.validateDeployable
    }
  }
}
