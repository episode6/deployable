package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests {@link DeployableJarPlugin}
 */
class DeployableJarIntegrationTest extends Specification {

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

  IntegrationTestProject testProject

  def setup() {
    testProject = new IntegrationTestProject(testProjectDir)
    testProject.createNonEmptyJavaFile( "com", "example", "groupid", "testlib")
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleSettingFile << """
rootProject.name = 'testlib'
"""
    testProject.rootGradleBuildFile << """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}

group = 'com.example.groupid'
version = '0.0.1-SNAPSHOT'

 """
  }

  def "verify deploy tasks (jar)"() {
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

  def "verify install tasks (jar)"() {
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

  def "verify jar specific tasks"() {
   when:
   def result = GradleRunner.create()
       .withProjectDir(testProjectDir.root)
       .withPluginClasspath()
       .withArguments("signArchives")
       .build()

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":javadoc").outcome == TaskOutcome.SUCCESS
    result.task(":javadocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
  }

  def "verify uploaded files"() {
    given:
    File TEMPMAVENDIR = new File("build/m2/snapshot")
    TEMPMAVENDIR.mkdirs()
    testProject.rootGradleBuildFile << """

deployable {
  nexus {
    snapshotRepoUrl "file://localhost${TEMPMAVENDIR.absolutePath}"
  }
}
"""

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments("deploy")
        .build()

    then:
    true
    File mavenOutputRoot = TEMPMAVENDIR.newFile("com", "example", "groupid", "testlib")
    File mavenMetaDataXml = mavenOutputRoot.newFile("maven-metadata.xml")
    def mavenMetaData = new XmlSlurper().parse(mavenMetaDataXml)
    mavenMetaData.groupId.text() == "com.example.groupid"
    mavenMetaData.artifactId.text() == "testlib"
    mavenMetaData.versioning.versions.size() == 1
    mavenMetaData.versioning.versions.version.text() == "0.0.1-SNAPSHOT"
    mavenMetaData.versioning.lastUpdated != null

  }
}
