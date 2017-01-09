package com.episode6.hackit.deployable

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests DeployablePlugin.groovy
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

    keyRingInfo = KeyRingUtil.generateKeyRings(
        testProjectDir.newFolder("gpg"),
        "test@example.com",
        SIGNING_PASSWORD)
  }

  def "look for deploy task"() {
    given:
    gradleProperties << """
POM_DESCRIPTION=Test Description
POM_URL=https://example.com
POM_SCM_URL=extensible
POM_SCM_CONNECTION=scm:https://scm_connection.com
POM_SCM_DEV_CONNECTION=scm:https://scm_dev_connection.com
POM_LICENCE_NAME=The MIT License (MIT)
POM_LICENCE_URL=https://licence.com
POM_LICENCE_DIST=repo
POM_DEVELOPER_ID=DeveloperId
POM_DEVELOPER_NAME=DeveloperName

NEXUS_USERNAME=nexusUsername
NEXUS_PASSWORD=nexusPassword

NEXUS_RELEASE_REPOSITORY_URL=file://localhost${m2Folder.absolutePath}
NEXUS_SNAPSHOT_REPOSITORY_URL=file://localhost${m2Folder.absolutePath}

signing.keyId=${keyRingInfo.masterKeyIdHex}
signing.password=${SIGNING_PASSWORD}
signing.secretKeyRingFile=${keyRingInfo.secretKeyringFile.absolutePath}
 """

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
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
  }
}
