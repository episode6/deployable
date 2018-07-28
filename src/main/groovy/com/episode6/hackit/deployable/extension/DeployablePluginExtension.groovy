package com.episode6.hackit.deployable.extension

import com.episode6.hackit.nestable.NestablePluginExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Deployable plugin extension. Stores/retreives info that is used
 * to build the pom and upload the artifacts.
 */
class DeployablePluginExtension extends NestablePluginExtension {
  private static final String[] OPTIONAL_PROPERTIES = [
      "deployable.nexus.username",
      "deployable.nexus.password",
      "deployable.nexus.releaseRepoUrl",
      "deployable.nexus.snapshotRepoUrl"]

  static class PomExtension extends NestablePluginExtension {

    static class ScmExtension extends NestablePluginExtension {
      String url = null
      String connection = null
      String developerConnection = null

      ScmExtension(NestablePluginExtension parent) {
        super(parent, "scm")
      }
    }

    static class LicenseExtension extends NestablePluginExtension {
      String name = null
      String url = null
      String distribution = null

      LicenseExtension(NestablePluginExtension parent) {
        super(parent, "license")
      }
    }

    static class DeveloperExtension extends NestablePluginExtension {
      String id
      String name

      DeveloperExtension(NestablePluginExtension parent) {
        super(parent, "developer")
      }
    }

    static class DependencyConfigurationsExtension extends NestablePluginExtension {

      final Map<String, CustomConfigMapping> map = new HashMap<>()

      DependencyConfigurationsExtension(NestablePluginExtension parent) {
        super(parent, "dependencyConfigurations")
      }

      void clear() {
        map.clear()
      }

      void unmap(String gradleConfigName) {
        map.remove(gradleConfigName)
      }

      void unmap(Configuration gradleConfig) {
        unmap(gradleConfig.name)
      }

      void map(String gradleConfigName, String mavenScope) {
        map.put(gradleConfigName, new CustomConfigMapping(
            gradleConfig: gradleConfigName,
            mavenScope: mavenScope,
            optional: false
        ))
      }

      void map(Configuration gradleConfig, String mavenScope) {
        map(gradleConfig.name, mavenScope)
      }

      void mapOptional(String gradleConfigName, String mavenScope) {
        map.put(gradleConfigName, new CustomConfigMapping(
            gradleConfig: gradleConfigName,
            mavenScope: mavenScope,
            optional: true
        ))
      }

      void mapOptional(Configuration gradleConfig, String mavenScope) {
        mapOptional(gradleConfig.name, mavenScope)
      }

      static class CustomConfigMapping {
        String gradleConfig
        String mavenScope
        boolean optional
      }
    }

    String description = null
    String url = null

    ScmExtension scm
    LicenseExtension license
    DeveloperExtension developer
    DependencyConfigurationsExtension dependencyConfigurations

    final List<Closure> xmlClosures = new LinkedList<>()

    PomExtension(NestablePluginExtension parent) {
      super(parent, "pom")
      scm = new ScmExtension(this)
      license = new LicenseExtension(this)
      developer = new DeveloperExtension(this)
      dependencyConfigurations = new DependencyConfigurationsExtension(this)
    }

    void withXml(Closure closure) {
      xmlClosures.add(closure)
    }

    DependencyConfigurationsExtension dependencyConfigurations(Closure closure) {
      return dependencyConfigurations.applyClosure(closure)
    }
  }

  static class NexusExtension extends NestablePluginExtension {
    String username = null
    String password = null
    String releaseRepoUrl = null
    String snapshotRepoUrl = null

    NexusExtension(NestablePluginExtension parent) {
      super(parent, "nexus")
    }
  }

  static class PublicationExtension extends NestablePluginExtension {

    Closure main = {}
    final List<Closure> additionalConfigurationClosures = new LinkedList<>()

    PublicationExtension(NestablePluginExtension parent) {
      super(parent, "publication")
    }

    void main(Closure closure) {
      main = closure
    }

    void amend(Closure closure) {
      additionalConfigurationClosures.add(closure)
    }

    void clear() {
      additionalConfigurationClosures.clear()
    }
  }


  PomExtension pom
  NexusExtension nexus
  PublicationExtension publication

  DeployablePluginExtension(Project project) {
    super(project, "deployable")
    pom = new PomExtension(this)
    nexus = new NexusExtension(this)
    publication = new PublicationExtension(this)
  }

  PomExtension pom(Closure closure) {
    return pom.applyClosure(closure)
  }

  NexusExtension nexus(Closure closure) {
    return nexus.applyClosure(closure)
  }

  PublicationExtension publication(Closure closure) {
    return publication.applyClosure(closure)
  }

  @Override
  List<String> findMissingProperties() {
    return findMissingPropertiesExcluding(OPTIONAL_PROPERTIES)
  }
}
