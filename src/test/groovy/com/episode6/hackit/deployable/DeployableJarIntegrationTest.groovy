package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.keyring.KeyRingInfo
import com.episode6.hackit.deployable.keyring.KeyRings
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests DeployableJarPlugin.groovy
 */
class DeployableJarIntegrationTest extends Specification {

  static final String SIGNING_PASSWORD = "fakePassword"

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile
  File gradleProperties
  File m2Folder
  KeyRingInfo keyRingInfo

  def setup() {
    buildFile = testProjectDir.newFile("build.gradle")
    gradleProperties = testProjectDir.newFile("gradle.properties")

    m2Folder = testProjectDir.newFolder("m2")
    m2Folder.mkdirs()

    keyRingInfo = KeyRings.generateKeyRingsForMaven(
        testProjectDir.newFolder("gpg"),
        "test@example.com",
        SIGNING_PASSWORD)
  }

  def "verify deploy task"() {
    given:
    gradleProperties << RequiredPropertiesGenerator.generateGradleProperties(m2Folder, keyRingInfo)
    buildFile << """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}

group = 'com.example.groupid'
version = '0.0.1-SNAPSHOT'
 """

    when:
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withPluginClasspath()
      .withArguments("deploy")
      .build()

    then:
    result.task(":signArchives").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
  }
}
