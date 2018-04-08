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

  private int scopePriority = 451

  private ConfigToScopeMapper mapper = new ConfigToScopeMapper()
  private Map<String, BuiltInConfig2ScopeMapping> builtInConfigs = new HashMap<>()
  private Set<Configuration> optionalConfigs = new HashSet<>()

  void prepare() {
    project.configurations {
      mavenOptional
      mavenProvided
      mavenProvidedOptional
    }

    putBuiltInConfig("implementation", "runtime")
    putBuiltInConfig("api", "compile")
    putBuiltInConfig("testImplementation", "test")
    putBuiltInConfig("mavenProvided", "provided")
    putBuiltInConfig("mavenProvidedOptional", "provided", true)
    putBuiltInConfig("mavenOptional", "runtime", true)
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

    new HashMap<String, BuiltInConfig2ScopeMapping>(builtInConfigs).each { name, config ->
      if (config.optional) {
        mapper.mapOptional(config.gradleConfig, config.mavenScope, config.priority)
      } else {
        mapper.map(config.gradleConfig, config.mavenScope, config.priority)
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

  String getMavenScopeForGradleConfig(String gradleConfigName) {
    if (builtInConfigs.containsKey(gradleConfigName)) {
      return builtInConfigs.get(gradleConfigName).mavenScope
    }
    def config = project.configurations.findByName(gradleConfigName)
    if (config == null) {
      return null
    }
    return getMavenScopeForGradleConfig(config)
  }

  String getMavenScopeForGradleConfig(Configuration config) {
    if (!project.conf2ScopeMappings.mappings.containsKey(config)) {
      return null
    }
    return project.conf2ScopeMappings.mappings.get(config).scope
  }

  class ConfigToScopeMapper implements GroovyInterceptable {

    void unmap(String gradleConfigName) {
      def config = project.configurations.findByName(gradleConfigName)
      if (config != null) {
        unmap(config)
      }
    }

    void unmap(Configuration gradleConfig) {
      builtInConfigs.remove(gradleConfig.name)
      project.conf2ScopeMappings.mappings.remove(gradleConfig)
      optionalConfigs.remove(gradleConfig)
    }

    void map(String gradleConfigName, String mavenScope) {
      def config = project.configurations.findByName(gradleConfigName)
      if (config != null) {
        map(config, mavenScope)
      }
    }

    void mapOptional(String gradleConfigName, String mavenScope) {
      def config = project.configurations.findByName(gradleConfigName)
      if (config != null) {
        mapOptional(config, mavenScope)
      }
    }

    void map(String gradleConfigName, String mavenScope, int priority) {
      def config = project.configurations.findByName(gradleConfigName)
      if (config != null) {
        map(config, mavenScope, priority)
      }
    }

    void mapOptional(String gradleConfigName, String mavenScope, int priority) {
      def config = project.configurations.findByName(gradleConfigName)
      if (config != null) {
        mapOptional(config, mavenScope, priority)
      }
    }

    void map(Configuration gradleConfig, String mavenScope) {
      map(gradleConfig, mavenScope, scopePriority)
      scopePriority++
    }

    void map(Configuration gradleConfig, String mavenScope, int priority) {
      unmap(gradleConfig)
      project.conf2ScopeMappings.addMapping(priority, gradleConfig, mavenScope)
    }

    void mapOptional(Configuration gradleConfig, String mavenScope) {
      map(gradleConfig, mavenScope)
      optionalConfigs.add(gradleConfig)
    }

    void mapOptional(Configuration gradleConfig, String mavenScope, int priority) {
      map(gradleConfig, mavenScope, priority)
      optionalConfigs.add(gradleConfig)
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
            optionalConfigs.each { Configuration gradleConfig ->
              String mavenScope = getMavenScopeForGradleConfig(gradleConfig)
              if (mavenScope) {
                gradleConfig.getAllDependencies().each { Dependency dep ->
                  pom.dependencies.find { pomDep ->
                    pomDep.groupId == dep.group && pomDep.artifactId == dep.name && pomDep.scope == mavenScope
                  }.optional = true
                }
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

  private void putBuiltInConfig(String gradleConfig, String mavenScope, boolean optional = false) {
    BuiltInConfig2ScopeMapping mapping = new BuiltInConfig2ScopeMapping(
        gradleConfig: gradleConfig,
        mavenScope: mavenScope,
        priority: scopePriority,
        optional: optional)
    scopePriority++
    builtInConfigs.put(mapping.gradleConfig, mapping)
  }

  private static class BuiltInConfig2ScopeMapping {
    String gradleConfig
    String mavenScope
    int priority
    boolean optional
  }

}
