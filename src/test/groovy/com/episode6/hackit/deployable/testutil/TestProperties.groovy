package com.episode6.hackit.deployable.testutil

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import com.episode6.hackit.deployable.extension.NestedExtension
import com.episode6.hackit.deployable.testutil.keyring.KeyRingBundle
import com.episode6.hackit.deployable.testutil.keyring.KeyRings

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
        licence {
          name "The MIT License (MIT)"
          url "https://licence.com"
          distribution "repo"
        }
        developer {
          id "DeveloperId"
          name "DeveloperName"
        }
      }

      nexus {
        username "nexusUsername"
        password "nexusPassword"
      }
    }
  }

  DeployablePluginExtension deployable(Closure closure) {
    return deployable.applyClosure(closure)
  }

  void applyMavenRepos(File releaseMavenRepoFile, File snapshotMavenRepoFile) {
    deployable {
      nexus {
        releaseRepoUrl "file://localhost${releaseMavenRepoFile.absolutePath}"
        snapshotRepoUrl "file://localhost${snapshotMavenRepoFile.absolutePath}"
      }
    }
  }

  String getInGradlePropertiesFormat() {
    StringBuilder builder = new StringBuilder()
    builder = buildGradlePropertiesForNestedExtension(deployable, builder)
    builder = buildGradlePropertiesForKeyringBundle(builder)
    return builder.toString()
  }

  private StringBuilder buildGradlePropertiesForNestedExtension(NestedExtension nestedExtension, StringBuilder stringBuilder) {
    nestedExtension.getProperties().each { key, value ->
      if (value instanceof String) {
        stringBuilder.append(nestedExtension.getNamespace())
          .append(".")
          .append(key)
          .append("=")
          .append(value)
          .append("\n")
      } else if (value instanceof NestedExtension) {
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