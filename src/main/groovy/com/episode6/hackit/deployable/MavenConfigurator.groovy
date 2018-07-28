package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

/**
 **/
class MavenConfigurator {
  Project project
  DeployablePluginExtension deployable

  private MavenDependencyConfigurator dependencyConfigurator;

  void prepare() {
    dependencyConfigurator = new MavenDependencyConfigurator(project: project, deployable: deployable)

    project.configurations {
      mavenOptional
      mavenProvided
      mavenProvidedOptional
    }

    deployable.pom.dependencyConfigurations {
      map "implementation", "runtime"
      map "api", "compile"
      map "mavenProvided", "provided"
      mapOptional "mavenProvidedOptional", "provided"
      mapOptional "mavenOptional", "runtime"
    }
  }

  void configure(String pomPackaging) {
    project.configurations {
      compileOnly {
        extendsFrom(mavenOptional,
            mavenProvided,
            mavenProvidedOptional)
      }
    }

    configurePom(pomPackaging)
    configureRepositories()
    configureSigning()
  }

  private void configurePom(String pomPackaging) {

    project.publishing {
      publications {
        mavenArtifacts(MavenPublication) {
          groupId project.group
          artifactId project.name
          version project.version

          pom {
            name = project.name
            description = deployable.pom.description
            url = deployable.pom.url
            packaging pomPackaging
            licenses {
              license {
                name = deployable.pom.license.name
                url = deployable.pom.license.url
                distribution = deployable.pom.license.distribution
              }
            }
            developers {
              developer {
                id = deployable.pom.developer.id
                name = deployable.pom.developer.name
              }
            }
            scm {
              url = deployable.pom.scm.url
              connection = deployable.pom.scm.connection
              developerConnection = deployable.pom.scm.developerConnection
            }
          }

          deployable.publication.additionalConfigurationClosures.each { closure ->
            configureWithClosure(it, closure)
          }
          configureWithClosure(it, deployable.publication.main)

          pom.withXml {
            def rootPom = asNode()
            dependencyConfigurator.configureDependencies(rootPom)
            deployable.pom.xmlClosures.each { closure ->
              configureWithClosure(rootPom, closure)
            }
          }
        }
      }
    }
  }

  private void configureRepositories() {
    def repoUrl = getRepoUrl(deployable)

    if (!repoUrl) {
      return
    }

    project.publishing {
      repositories {
        maven {
          url repoUrl

          if (URI.create(repoUrl).getScheme() == "file") {
            authentication {} // empty auth block removes an error when using file:// repos
          } else if (deployable.nexus.username || deployable.nexus.password) {
            credentials {
              username deployable.nexus.username
              password deployable.nexus.password
            }
          }
        }
      }
    }
  }

  private String getRepoUrl(DeployablePluginExtension deployable) {
    if (deployable.nexus.releaseRepoUrl == null) {
      return deployable.nexus.snapshotRepoUrl
    }
    if (deployable.nexus.snapshotRepoUrl == null) {
      return deployable.nexus.releaseRepoUrl
    }
    return DeployablePlugin.isReleaseBuild(project) ? deployable.nexus.releaseRepoUrl : deployable.nexus.snapshotRepoUrl
  }

  private void configureSigning() {
    project.signing {
      required {
        DeployablePlugin.isReleaseBuild(project) && project.gradle.taskGraph.hasTask("publishMavenArtifactsPublicationToMavenRepository")
      }
      sign project.publishing.publications.mavenArtifacts
    }
  }

  private static void configureWithClosure(Object delegate, Closure closure) {
    closure.setDelegate(delegate)
    closure.setResolveStrategy(Closure.DELEGATE_FIRST)
    closure.call()
  }
}
