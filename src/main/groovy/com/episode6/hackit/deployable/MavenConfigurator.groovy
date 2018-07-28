package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.maven.MavenPublication

/**
 **/
class MavenConfigurator {
  Project project

  private ConfigToScopeMapper mapper = new ConfigToScopeMapper()
  private MavenDependencyConfigurator dependencyConfigurator;

  void prepare() {
    dependencyConfigurator = new MavenDependencyConfigurator(project: project)

    project.configurations {
      mavenOptional
      mavenProvided
      mavenProvidedOptional
    }

    dependencyConfigurator.putConfigMapping("implementation", "runtime")
    dependencyConfigurator.putConfigMapping("api", "compile")
    dependencyConfigurator.putConfigMapping("mavenProvided", "provided")
    dependencyConfigurator.putConfigMapping("mavenProvidedOptional", "provided", true)
    dependencyConfigurator.putConfigMapping("mavenOptional", "runtime", true)
  }

  void configure(DeployablePluginExtension deployable, String pomPackaging) {
    project.configurations {
      compileOnly {
        extendsFrom(mavenOptional,
            mavenProvided,
            mavenProvidedOptional)
      }
    }

    configurePom(deployable, pomPackaging)
    configureRepositories(deployable)
    configureSigning()
  }

  void mapConfigs(Closure closure) {
    closure.setDelegate(mapper)
    closure.setResolveStrategy(Closure.DELEGATE_FIRST)
    closure.call()
  }

  class ConfigToScopeMapper implements GroovyInterceptable {

    void unmap(String gradleConfigName) {
      dependencyConfigurator.removeConfigMapping(gradleConfigName)
    }

    void unmap(Configuration gradleConfig) {
      dependencyConfigurator.removeConfigMapping(gradleConfig.name)
    }

    void map(String gradleConfigName, String mavenScope) {
      dependencyConfigurator.putConfigMapping(gradleConfigName, mavenScope)
    }

    void mapOptional(String gradleConfigName, String mavenScope) {
      dependencyConfigurator.putConfigMapping(gradleConfigName, mavenScope, true)
    }

    void map(Configuration gradleConfig, String mavenScope) {
      map(gradleConfig.name, mavenScope)
    }

    void mapOptional(Configuration gradleConfig, String mavenScope) {
      mapOptional(gradleConfig.name, mavenScope)
    }
  }

  private void configurePom(DeployablePluginExtension deployable, String pomPackaging) {

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

          configurePublicationArtifacts(it, deployable)

          pom.withXml {
            def rootPom = asNode()
            dependencyConfigurator.configureDependencies(rootPom)
            configurePublicationPomXml(rootPom, deployable)
          }
        }
      }
    }
  }

  private void configureRepositories(DeployablePluginExtension deployable) {
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

  private static void configurePublicationArtifacts(MavenPublication publication, DeployablePluginExtension deployable) {
    deployable.publicationClosures.each { closure ->
      closure.setDelegate(publication)
      closure.setResolveStrategy(Closure.DELEGATE_FIRST)
      closure.call()
    }
  }

  private static void configurePublicationPomXml(Node rootPom, DeployablePluginExtension deployable) {
    deployable.pom.xmlClosures.each { closure ->
      closure.setDelegate(rootPom)
      closure.setResolveStrategy(Closure.DELEGATE_FIRST)
      closure.call()
    }
  }
}
