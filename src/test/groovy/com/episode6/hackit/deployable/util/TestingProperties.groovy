package com.episode6.hackit.deployable.util

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import com.episode6.hackit.deployable.extension.NestedExtension
import com.episode6.hackit.deployable.util.keyring.KeyRingBundle
import com.episode6.hackit.deployable.util.keyring.KeyRings

/**
 *
 */
class TestingProperties {
  DeployablePluginExtension deployable = new DeployablePluginExtension(null)
  KeyRingBundle keyRingBundle = KeyRings.sharedBundle()

  TestingProperties() {
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

  void applyLocalMavenRepo(File mavenRepoDir) {
    deployable {
      nexus {
        releaseRepoUrl "file://localhost${mavenRepoDir.absolutePath}"
        snapshotRepoUrl "file://localhost${mavenRepoDir.absolutePath}"
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
    if (keyRingBundle == null) {
      return stringBuilder
    }
    stringBuilder.append("signing.keyId=")
      .append(keyRingBundle.masterKeyIdHex)
      .append("\nsigning.password=")
      .append(keyRingBundle.password)
      .append("\nsigning.secretKeyRingFile=")
      .append(keyRingBundle.secretKeyringFile.absolutePath)
      .append("\n")
  }
}
