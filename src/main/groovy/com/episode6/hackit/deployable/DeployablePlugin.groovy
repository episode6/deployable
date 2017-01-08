package com.episode6.hackit.deployable

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

  def getReleaseRepositoryUrl(Project project) {
    if (project.hasProperty("NEXUS_RELEASE_REPOSITORY_URL")) {
      return project.NEXUS_RELEASE_REPOSITORY_URL
    }
    return "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
  }

  def getSnapshotRepositoryUrl(Project project) {
    if (project.hasProperty("NEXUS_SNAPSHOT_REPOSITORY_URL")) {
      return project.NEXUS_SNAPSHOT_REPOSITORY_URL
    }
    return "https://oss.sonatype.org/content/repositories/snapshots/"
  }

  void apply(Project project) {
    project.plugins.apply(MavenPlugin)
    project.plugins.apply(SigningPlugin)

    // add deploy alias for uploadArchives task (because it's more fun to type)
    project.task("deploy", dependsOn: project.uploadArchives) {}

    project.afterEvaluate {
      project.uploadArchives {
        repositories {
          mavenDeployer {
            beforeDeployment { MavenDeployment deployment -> project.signing.signPom(deployment) }

            repository(url: getReleaseRepositoryUrl(project)) {
              authentication(userName: project.NEXUS_USERNAME, password: project.NEXUS_PASSWORD)
            }
            snapshotRepository(url: getSnapshotRepositoryUrl(project)) {
              authentication(userName: project.NEXUS_USERNAME, password: project.NEXUS_PASSWORD)
            }

            pom.project {
              name project.name
              packaging pomPackaging
              description project.POM_DESCRIPTION
              url project.POM_URL

              scm {
                url project.POM_SCM_URL
                connection project.POM_SCM_CONNECTION
                developerConnection project.POM_SCM_DEV_CONNECTION
              }

              licenses {
                license {
                  name project.POM_LICENCE_NAME
                  url project.POM_LICENCE_URL
                  distribution project.POM_LICENCE_DIST
                }
              }

              developers {
                developer {
                  id project.POM_DEVELOPER_ID
                  name project.POM_DEVELOPER_NAME
                }
              }
            }
          }
        }
      }

      project.signing {
        required { isReleaseBuild(project) && project.gradle.taskGraph.hasTask("uploadArchives") }
        sign project.configurations.archives
      }
    }
  }
}
