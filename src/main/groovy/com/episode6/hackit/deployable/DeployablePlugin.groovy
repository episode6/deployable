package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.plugins.MavenPlugin
import org.gradle.plugins.signing.SigningPlugin

/**
 * Base deployable plugin
 */
class DeployablePlugin implements Plugin<Project> {
  String pomPackaging = null

  def isReleaseBuild(Project project) {
    return project.version.contains("SNAPSHOT") == false
  }

  void apply(Project project) {
    project.plugins.apply(MavenPlugin)
    project.plugins.apply(SigningPlugin)

    DeployablePluginExtension deployable = project.extensions.create(
        "deployable",
        DeployablePluginExtension,
        project)

    project.task("validateDeployable") {
      doLast {
        List<String> missingProps = deployable.findMissingProps()
        if (!missingProps.isEmpty()) {
          throw new MissingPropertyException("Missing the following required properties: ${missingProps}")
        }
      }
    }

    if (project.tasks.findByPath("install") != null) {
      project.install.dependsOn project.validateDeployable
    }


    // add deploy alias for uploadArchives task (because it's more fun to type)
    project.task("deploy", dependsOn: project.uploadArchives) {}

    project.afterEvaluate {
      project.uploadArchives {
        dependsOn project.validateDeployable

        repositories {
          mavenDeployer {
            beforeDeployment { MavenDeployment deployment -> project.signing.signPom(deployment) }

            repository(url: deployable.nexus.releaseRepoUrl) {
              authentication(userName: deployable.nexus.username, password: deployable.nexus.password)
            }
            snapshotRepository(url: deployable.nexus.snapshotRepoUrl) {
              authentication(userName: deployable.nexus.username, password: deployable.nexus.password)
            }

            pom.project {
              name project.name
              packaging pomPackaging
              description deployable.pom.description
              url deployable.pom.url

              scm {
                url deployable.pom.scm.url
                connection deployable.pom.scm.connection
                developerConnection deployable.pom.scm.developerConnection
              }

              licenses {
                license {
                  name deployable.pom.license.name
                  url deployable.pom.license.url
                  distribution deployable.pom.license.distribution
                }
              }

              developers {
                developer {
                  id deployable.pom.developer.id
                  name deployable.pom.developer.name
                }
              }
            }
          }
        }
      }

      project.signing {
        required {
          isReleaseBuild(project) &&
              (project.gradle.taskGraph.hasTask("uploadArchives") ||
                  project.gradle.taskGraph.hasTask("uploadArchives")) }
        sign project.configurations.archives
      }
    }
  }
}
