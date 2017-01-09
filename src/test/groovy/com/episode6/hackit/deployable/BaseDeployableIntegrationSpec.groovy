package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.util.keyring.KeyRingBundle
import com.episode6.hackit.deployable.util.keyring.KeyRings
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Base class for integration tests
 */
class BaseDeployableIntegrationSpec extends Specification {

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

  File buildFile
  File gradlePropertiesFile
  File mavenDir

  KeyRingBundle keyRingBundle

  def setup() {
    buildFile = testProjectDir.newFile("build.gradle")
    gradlePropertiesFile = testProjectDir.newFile("gradle.properties")

    mavenDir = testProjectDir.newFolder("mavenOut")
    mavenDir.mkdirs()

    keyRingBundle = KeyRings.generateKeyRingsForMaven(
        testProjectDir.newFolder("gpg"),
        "test@example.com",
        "fakePassword")
  }
}
