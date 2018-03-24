package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployment

/**
 *
 */
class MavenConfig {

  static ConfigToScopeMapper configMapper(Project project) {
    return new ConfigToScopeMapper(project: project)
  }

  static class ConfigToScopeMapper {
    Project project

    ConfigToScopeMapper map(String gradleConfigName, String mavenScope) {
      def config = project.configurations.findByName(gradleConfigName)
      if (config != null) {
        project.conf2ScopeMappings.addMapping(0, config, mavenScope)
      }
      return this
    }
  }

  static void configurePom(Project project, DeployablePluginExtension deployable, String pomPackaging) {
    project.uploadArchives {
      it.dependsOn project.validateDeployable

      it.repositories {
        it.mavenDeployer {
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
  }

  static void configureSigning(Project project) {
    project.signing {
      required {
        DeployablePlugin.isReleaseBuild(project) &&
            (project.gradle.taskGraph.hasTask("uploadArchives") ||
                project.gradle.taskGraph.hasTask("uploadArchives"))
      }
      sign project.configurations.archives
    }
  }
}
