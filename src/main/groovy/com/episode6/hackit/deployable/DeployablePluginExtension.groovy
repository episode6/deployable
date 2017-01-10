package com.episode6.hackit.deployable

import org.gradle.api.Project

/**
 *
 */
class DeployablePluginExtension {
  private static final NAMESPACE = "deployable"

  private final Project project

  static class PomExtension extends BaseExtension {
    String description = null
    String url = null

    static class ScmExtension extends BaseExtension {
      String url = null
      String connection = null
      String developerConnection = null

      ScmExtension(Project project, String parentNamespace) {
        super(project, parentNamespace, "scm")
      }
    }

    static class LicenceExtension extends BaseExtension {
      String name = null
      String url = null
      String distribution = null

      LicenceExtension(Project project, String parentNamespace) {
        super(project, parentNamespace, "licence")
      }
    }

    static class DeveloperExtension extends BaseExtension {
      String id
      String name

      DeveloperExtension(Project project, String parentNamespace) {
        super(project, parentNamespace, "developer")
      }
    }

    ScmExtension scm
    LicenceExtension licence
    DeveloperExtension developer

    PomExtension(Project project, String parentNamespace) {
      super(project, parentNamespace, "pom")
      scm = new ScmExtension(project, namespace)
      licence = new LicenceExtension(project, namespace)
      developer = new DeveloperExtension(project, namespace)
    }

    ScmExtension scm(Closure closure) {
      project.configure(scm, closure)
      return scm
    }

    LicenceExtension licence(Closure closure) {
      project.configure(licence, closure)
      return licence
    }

    DeveloperExtension developer(Closure closure) {
      project.configure(developer, closure)
      return developer
    }

  }

  static class NexusExtension extends BaseExtension {
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
    project.configure(pom, closure)
    return pom
  }

  NexusExtension nexus(Closure closure) {
    project.configure(pom, closure)
    return pom
  }
}
