package com.episode6.hackit.deployable

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

  def setup() {
    buildFile = testProjectDir.newFile("build.gradle")
    gradlePropertiesFile = testProjectDir.newFile("gradle.properties")

    mavenDir = testProjectDir.newFolder("mavenOut")
    mavenDir.mkdirs()
  }
}
