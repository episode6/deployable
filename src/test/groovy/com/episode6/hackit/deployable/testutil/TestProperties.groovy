package com.episode6.hackit.deployable.testutil

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import com.episode6.hackit.deployable.testutil.keyring.KeyRingBundle
import com.episode6.hackit.deployable.testutil.keyring.KeyRings
import com.episode6.hackit.nestable.NestablePluginExtension

/**
 *
 */
class TestProperties {
  DeployablePluginExtension deployable
  KeyRingBundle keyrings

  TestProperties(DeployablePluginExtension deployable, KeyRingBundle keyrings) {
    this.deployable = deployable
    this.keyrings = keyrings
  }

  TestProperties() {
    this(new DeployablePluginExtension(null), KeyRings.sharedBundle())
    deployable {
      pom {
        description "Test POM Description"
        url "https://pom_url.com"

        scm {
          url "extensible"
          connection "scm:https://scm_connection.com"
          developerConnection "scm:https://scm_dev_connection.com"
        }
        license {
          name "The MIT License (MIT)"
          url "https://license.com"
          distribution "repo"
        }
        developer {
          id "DeveloperId"
          name "DeveloperName"
        }
      }
    }
  }

  DeployablePluginExtension deployable(Closure closure) {
    return deployable.applyClosure(closure)
  }

  void setReleaseRepo(File releaseMavenRepoFile) {
    deployable.nexus.releaseRepoUrl = "file://${releaseMavenRepoFile.absolutePath}"
  }

  void setSnapshotRepo(File snapshotMavenRepoFile) {
    deployable.nexus.snapshotRepoUrl = "file://${snapshotMavenRepoFile.absolutePath}"
  }

  String getInGradlePropertiesFormat() {
    StringBuilder builder = new StringBuilder()
    builder = buildGradlePropertiesForNestedExtension(deployable, builder)
    builder = buildGradlePropertiesForKeyringBundle(builder)
        .append("\n")
        .append("org.gradle.jvmargs=-XX:MaxMetaspaceSize=2048m")
    return builder.toString()
  }

  private StringBuilder buildGradlePropertiesForNestedExtension(NestablePluginExtension nestedExtension, StringBuilder stringBuilder) {
    nestedExtension.getProperties().each { key, value ->
      if (value instanceof String) {
        stringBuilder.append(nestedExtension.getNamespace())
          .append(".")
          .append(key)
          .append("=")
          .append(value)
          .append("\n")
      } else if (value instanceof NestablePluginExtension) {
        buildGradlePropertiesForNestedExtension(value, stringBuilder)
      }
    }
    return stringBuilder
  }

  private StringBuilder buildGradlePropertiesForKeyringBundle(StringBuilder stringBuilder) {
    if (keyrings == null) {
      return stringBuilder
    }
    stringBuilder.append("signing.keyId=")
      .append(keyrings.masterKeyIdHex)
      .append("\nsigning.password=")
      .append(keyrings.password)
      .append("\nsigning.secretKeyRingFile=")
      .append(keyrings.secretKeyringFile.absolutePath)
      .append("\n")
  }
}
