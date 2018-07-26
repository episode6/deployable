package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.publish.maven.MavenPublication

/**
 **/
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
        extendsFrom(mavenOptional,
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
            configureDependencies(asNode())
          }
        }
      }


      repositories {
        maven {
          def repoUrl = DeployablePlugin.isReleaseBuild(project) ? deployable.nexus.releaseRepoUrl : deployable.nexus.snapshotRepoUrl
          url repoUrl

          if (URI.create(repoUrl).getScheme() == "file") {
            authentication {} // empty auth block removes an error when using file:// repos
          } else if (deployable.nexus.username != null) {
            credentials {
              username deployable.nexus.username
              password deployable.nexus.password
            }
          }
        }
      }
    }
  }

  private void configureDependencies(Node pomRoot) {
    def deps = pomRoot.dependencies
    if (deps == null || deps.isEmpty()) {
      deps = pomRoot.appendNode('dependencies')
    } else {
      deps = pomRoot.dependencies[0]
    }
    deps.children.clear() // start with no deps
    mappedConfigs.values().each { mappedConfig ->
      def config = project.configurations.findByName(mappedConfig.gradleConfig)
      if (config != null) {
        def unresolvedDeps = config.dependencies
        config = config.copyRecursive().setTransitive(false)
        config.setCanBeResolved(true)
        def resolvedDeps = config.resolvedConfiguration.lenientConfiguration.getFirstLevelModuleDependencies()
        unresolvedDeps.each { unresolvedDep ->
          def resolvedDep = resolvedDeps.find {unresolvedDep.group == it.moduleGroup && unresolvedDep.name == it.moduleName}
          if (unresolvedDep instanceof ModuleDependency) {

            def groupId
            def artifactId
            def version

            if (unresolvedDep instanceof ProjectDependency) {
              Project depProj = unresolvedDep.getDependencyProject()
              groupId = depProj.group
              artifactId = depProj.name
              version = depProj.version
            } else if (resolvedDep != null) {
              groupId = resolvedDep.moduleGroup
              artifactId = resolvedDep.moduleName
              version = resolvedDep.moduleVersion
            } else {
              throw new GradleException("Couldn't figure out dependency: ${unresolvedDep}")
            }

            def depNode = deps.appendNode('dependency')
            depNode.appendNode('groupId', groupId)
            depNode.appendNode('artifactId', artifactId)
            depNode.appendNode('version', version)
            depNode.appendNode('scope', mappedConfig.mavenScope)
            if (mappedConfig.optional) {
              depNode.appendNode('optional', true)
            }
            if (!unresolvedDep.excludeRules.isEmpty()) {
              def exclusionsNode = depNode.appendNode('exclusions')
              unresolvedDep.excludeRules.each {
                def exNode = exclusionsNode.appendNode('exclusion')
                exNode.appendNode('groupId', it.group == null ? "*" : it.group)
                exNode.appendNode('artifactId', it.module == null ? "*" : it.module)
              }
            }
          }
        }
      }
    }
  }

  private void configureSigning() {
    project.signing {
      required {
        DeployablePlugin.isReleaseBuild(project) && project.gradle.taskGraph.hasTask("publishMavenArtifactsPublicationToMavenRepository")
      }
      sign project.publishing.publications.mavenArtifacts
    }
  }

  private void putConfigMapping(String gradleConfig, String mavenScope, boolean optional = false) {
    CustomConfigMapping mapping = new CustomConfigMapping(gradleConfig: gradleConfig,
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
