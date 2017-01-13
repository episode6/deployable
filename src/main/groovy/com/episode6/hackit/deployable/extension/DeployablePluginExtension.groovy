package com.episode6.hackit.deployable.extension

import org.gradle.api.Project

/**
 * Root deployable plugin extension
 */
class DeployablePluginExtension extends NestedExtension {
  static class PomExtension extends NestedExtension {

    static class ScmExtension extends NestedExtension {
      String url = null
      String connection = null
      String developerConnection = null

      ScmExtension(NestedExtension parent) {
        super(parent, "scm")
      }
    }

    static class LicenseExtension extends NestedExtension {
      String name = null
      String url = null
      String distribution = null

      LicenseExtension(NestedExtension parent) {
        super(parent, "license")
      }
    }

    static class DeveloperExtension extends NestedExtension {
      String id
      String name

      DeveloperExtension(NestedExtension parent) {
        super(parent, "developer")
      }
    }

    String description = null
    String url = null

    ScmExtension scm
    LicenseExtension license
    DeveloperExtension developer

    PomExtension(NestedExtension parent) {
      super(parent, "pom")
      scm = new ScmExtension(this)
      license = new LicenseExtension(this)
      developer = new DeveloperExtension(this)
    }
  }

  static class NexusExtension extends NestedExtension {
    String username = null
    String password = null
    String releaseRepoUrl = null
    String snapshotRepoUrl = null

    NexusExtension(NestedExtension parent) {
      super(parent, "nexus")
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
}
