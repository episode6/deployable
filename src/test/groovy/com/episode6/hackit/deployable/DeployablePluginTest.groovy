package com.episode6.hackit.deployable

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests DeployablePlugin.groovy
 */
class DeployablePluginTest extends Specification {

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile
  File gradleProperties

  def setup() {
    buildFile = testProjectDir.newFile("build.gradle")
    gradleProperties = testProjectDir.newFile("gradle.properties")
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

NEXUS_RELEASE_REPOSITORY_URL=https://release_repo.com
NEXUS_SNAPSHOT_REPOSITORY_URL=https://snapshot_repo.com

signing.keyId=123456789
signing.password=signingPassword
signing.secretKeyRingFile=/path/to/keyring
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
