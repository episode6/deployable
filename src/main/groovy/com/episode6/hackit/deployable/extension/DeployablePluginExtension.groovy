package com.episode6.hackit.deployable.extension

import org.gradle.api.Project

/**
 *
 */
class DeployablePluginExtension {
  private static final NAMESPACE = "deployable"

  private final Project project

  static class PomExtension extends NestedExtension {

    static class ScmExtension extends NestedExtension {
      String url = null
      String connection = null
      String developerConnection = null

      ScmExtension(Project project, String parentNamespace) {
        super(project, parentNamespace, "scm")
      }
    }

    static class LicenceExtension extends NestedExtension {
      String name = null
      String url = null
      String distribution = null

      LicenceExtension(Project project, String parentNamespace) {
        super(project, parentNamespace, "licence")
      }
    }

    static class DeveloperExtension extends NestedExtension {
      String id
      String name

      DeveloperExtension(Project project, String parentNamespace) {
        super(project, parentNamespace, "developer")
      }
    }

    String description = null
    String url = null

    ScmExtension scm
    LicenceExtension licence
    DeveloperExtension developer

    PomExtension(Project project, String parentNamespace) {
      super(project, parentNamespace, "pom")
      scm = new ScmExtension(project, namespace)
      licence = new LicenceExtension(project, namespace)
      developer = new DeveloperExtension(project, namespace)
    }
  }

  static class NexusExtension extends NestedExtension {
    String username = null
    String password = null
    String releaseRepoUrl = null
    String snapshotRepoUrl = null

    NexusExtension(Project project, String parentNamespace) {
      super(project, parentNamespace, "nexus")
    }

    String getReleaseRepoUrl() {
      if (releaseRepoUrl != null) {
        return releaseRepoUrl
      }

      String fromProperties = getOptionalProjectProperty("releaseRepoUrl")
      return fromProperties == null ?
          "https://oss.sonatype.org/service/local/staging/deploy/maven2/" :
          fromProperties
    }

    String getSnapshotRepoUrl() {
      if (snapshotRepoUrl != null) {
        return snapshotRepoUrl
      }

      String fromProperties = getOptionalProjectProperty("snapshotRepoUrl")
      return fromProperties == null ?
          "https://oss.sonatype.org/content/repositories/snapshots/" :
          fromProperties
    }
  }

  PomExtension pom
  NexusExtension nexus

  DeployablePluginExtension(Project project) {
    this.project = project
    pom = new PomExtension(project, NAMESPACE)
    nexus = new NexusExtension(project, NAMESPACE)
  }

  PomExtension pom(Closure closure) {
    return pom.applyClosure(closure)
  }

  NexusExtension nexus(Closure closure) {
    return nexus.applyClosure(closure)
  }
}
