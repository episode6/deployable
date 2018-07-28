package com.episode6.hackit.deployable.extension

import com.episode6.hackit.nestable.NestablePluginExtension
import org.gradle.api.Project

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

    String description = null
    String url = null

    final List<Closure> xmlClosures = new LinkedList<>()

    ScmExtension scm
    LicenseExtension license
    DeveloperExtension developer

    PomExtension(NestablePluginExtension parent) {
      super(parent, "pom")
      scm = new ScmExtension(this)
      license = new LicenseExtension(this)
      developer = new DeveloperExtension(this)
    }

    void withXml(Closure closure) {
      xmlClosures.add(closure)
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

  Closure primaryPublication = {}
  final List<Closure> publicationClosures = new LinkedList<>()

  PomExtension pom
  NexusExtension nexus

  DeployablePluginExtension(Project project) {
    super(project, "deployable")
    pom = new PomExtension(this)
    nexus = new NexusExtension(this)
  }

  PomExtension pom(Closure closure) {
    return pom.applyClosure(closure)
  }

  NexusExtension nexus(Closure closure) {
    return nexus.applyClosure(closure)
  }

  void publication(Closure closure) {
    publicationClosures.add(closure)
  }

  @Override
  List<String> findMissingProperties() {
    return findMissingPropertiesExcluding(OPTIONAL_PROPERTIES)
  }
}
