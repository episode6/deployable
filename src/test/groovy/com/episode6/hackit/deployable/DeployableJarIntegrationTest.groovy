package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.util.IntegrationTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests DeployableJarPlugin.groovy
 */
class DeployableJarIntegrationTest extends Specification {

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

  IntegrationTestProject testProject

  def setup() {
    testProject = new IntegrationTestProject(testProjectDir)
  }

  def "verify deploy tasks"() {
    given:
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << """
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
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signArchives").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
  }

  def "verify install tasks"() {
    given:
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << """
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
        .withArguments("install")
        .build()

    then:
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":install").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives") == null
  }
}
