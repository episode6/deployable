package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.maven.MavenDeployment

/**
 *
 */
class MavenConfigurator {
  Project project

  private int scopePriority = 51

  void prepare() {
    project.ext.optionalConfigs = []

    project.configurations {
      mavenOptional
      mavenProvided
      mavenProvidedOptional
    }
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

    mapConfigs {
      mapOptional("mavenOptional", "compile")
      mapOptional("mavenProvidedOptional", "provided")
      map("mavenProvided", "provided")
      map("implementation", "compile")
      map("api", "compile")
      map("testImplementation", "test")
    }


    configurePom(deployable, pomPackaging)
    configureSigning()
  }

  void mapConfigs(Closure closure) {
    closure.setDelegate(new ConfigToScopeMapper())
    closure.setResolveStrategy(Closure.DELEGATE_FIRST)
    closure.call()
  }

  class ConfigToScopeMapper implements GroovyInterceptable {

    ConfigToScopeMapper map(String gradleConfigName, String mavenScope) {
      def config = project.configurations.findByName(gradleConfigName)
      if (config != null) {
        map(config, mavenScope)
      }
      return this
    }

    ConfigToScopeMapper mapOptional(String gradleConfigName, String mavenScope) {
      def config = project.configurations.findByName(gradleConfigName)
      if (config != null) {
        mapOptional(config, mavenScope)
      }
      return this
    }

    ConfigToScopeMapper map(Configuration gradleConfig, String mavenScope) {
      project.conf2ScopeMappings.addMapping(scopePriority, gradleConfig, mavenScope)
      scopePriority++
      return this
    }

    ConfigToScopeMapper mapOptional(Configuration gradleConfig, String mavenScope) {
      map(gradleConfig, mavenScope)
      project.ext.optionalConfigs << gradleConfig
      return this
    }
  }

  private void configurePom(DeployablePluginExtension deployable, String pomPackaging) {
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

          // apply optional dependencies
          pom.whenConfigured { pom ->
            project.optionalConfigs.each { Configuration gradleConfig ->
              gradleConfig.getAllDependencies().each { Dependency dep ->
                pom.dependencies.find { pomDep ->
                  pomDep.groupId == dep.group && pomDep.artifactId == dep.name
                }.optional = true
              }
            }
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

  private void configureSigning() {
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
