package com.episode6.hackit.deployable.addon

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests {@link GroovyDocAddonPlugin}
 */
class DeployableGroovyAddonIntegrationTest extends Specification {

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

  IntegrationTestProject testProject

  def setup() {
    testProject = new IntegrationTestProject(testProjectDir)
    testProject.createNonEmptyGroovyFile("com.example.groupid.testlib")
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << """
plugins {
 id 'groovy'
 id 'com.episode6.hackit.deployable.jar'
 id 'com.episode6.hackit.deployable.addon.groovydocs'
}

group = 'com.example.groupid'
version = '0.0.1-SNAPSHOT'

dependencies {
  compile localGroovy()
}
 """
  }

  def "verify deploy tasks (groovy)"() {
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
    result.task(":install") == null
  }

  def "verify install tasks (groovy)"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments("install")
        .build()

    then:
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signArchives").outcome == TaskOutcome.SUCCESS
    result.task(":install").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives") == null
  }

  def "verify groovy specific tasks"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments("signArchives")
        .build()

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":groovydoc").outcome == TaskOutcome.SUCCESS
    result.task(":groovydocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
  }
}
