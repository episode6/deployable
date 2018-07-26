package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.maven.MavenPublication

/**
 *
 */
class MavenConfigurator {
  Project project

  private ConfigToScopeMapper mapper = new ConfigToScopeMapper()
  private Map<String, CustomConfigMapping> mappedConfigs = new HashMap<>()

  void prepare() {
    project.configurations {
      mavenOptional
      mavenProvided
      mavenProvidedOptional
    }

    putConfigMapping("implementation", "runtime")
    putConfigMapping("api", "compile")
    putConfigMapping("testImplementation", "test")
    putConfigMapping("mavenProvided", "provided")
    putConfigMapping("mavenProvidedOptional", "provided", true)
    putConfigMapping("mavenOptional", "runtime", true)
  }


  void configure(DeployablePluginExtension deployable, String pomPackaging) {
    project.configurations {
      compileOnly {
        extendsFrom(
            mavenOptional,
            mavenProvided,
            mavenProvidedOptional)
      }
    }

    configurePom(deployable, pomPackaging)
    configureSigning()
  }

  void mapConfigs(Closure closure) {
    closure.setDelegate(mapper)
    closure.setResolveStrategy(Closure.DELEGATE_FIRST)
    closure.call()
  }

  class ConfigToScopeMapper implements GroovyInterceptable {

    void unmap(String gradleConfigName) {
      mappedConfigs.remove(gradleConfigName)
    }

    void unmap(Configuration gradleConfig) {
      mappedConfigs.remove(gradleConfig.name)
    }

    void map(String gradleConfigName, String mavenScope) {
      putConfigMapping(gradleConfigName, mavenScope)
    }

    void mapOptional(String gradleConfigName, String mavenScope) {
      putConfigMapping(gradleConfigName, mavenScope, true)
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

          configurePublicationArtifacts(it, deployable)

          pom {
            name project.name
            description deployable.pom.description
            url deployable.pom.url
            packaging pomPackaging
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
            scm {
              url deployable.pom.scm.url
              connection deployable.pom.scm.connection
              developerConnection deployable.pom.scm.developerConnection
            }
          }
        }
      }

      pom.withXml {
        def root = asNode()
        def deps = root.dependencies
        deps.children.clear() // start with no deps
        mappedConfigs.values().each { mappedConfig ->
          def config = project.configurations.findByName(mappedConfig.gradleConfig)
          if (config != null) {
            config.allDependencies.each {
              def depNode = deps.appendNode('dependency')
              depNode.appendNode('groupId', it.group)
              depNode.appendNode('artifactId', it.name)
              depNode.appendNode('version', it.version)
              depNode.appendNode('scope', mappedConfig.mavenScope)
              if (mappedConfig.optional) {
                depNode.appendNode('optional', true)
              }
            }
          }
        }
      }

      repositories {
        maven {
          url DeployablePlugin.isReleaseBuild(project) ? deplodeployable.nexus.releaseRepoUrl : deployable.nexus.snapshotRepoUrl
          if (deployable.nexus.username != null) {
            credentials {
              username deployable.nexus.username
              password deployable.nexus.password
            }
          }
        }
      }
    }
  }

  private void configureSigning() {
    project.signing {
      required {
        DeployablePlugin.isReleaseBuild(project) && project.gradle.taskGraph.hasTask("publishMavenArtifactsPublicationToRepository")
      }
      sign project.publishing.publications.mavenArtifacts
    }
  }

  private void putConfigMapping(String gradleConfig, String mavenScope, boolean optional = false) {
    CustomConfigMapping mapping = new CustomConfigMapping(
        gradleConfig: gradleConfig,
        mavenScope: mavenScope,
        optional: optional)
    mappedConfigs.put(mapping.gradleConfig, mapping)
  }

  private static class CustomConfigMapping {
    String gradleConfig
    String mavenScope
    boolean optional
  }

  private static void configurePublicationArtifacts(MavenPublication publication, DeployablePluginExtension deployable) {
    deployable.publicationClosures.each { closure ->
      closure.setDelegate(publication)
      closure.setResolveStrategy(Closure.DELEGATE_FIRST)
      closure.call()
    }
  }

}
