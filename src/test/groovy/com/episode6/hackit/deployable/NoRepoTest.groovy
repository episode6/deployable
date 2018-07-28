package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

/**
 *
 */
class NoRepoTest extends Specification {

  @Rule
  final IntegrationTestProject testProject = new IntegrationTestProject(initMavenRepos: false)

  private static String simpleBuildFile(
      String groupId,
      String versionName) {
    return """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}

group = '${groupId}'
version = '${versionName}'
"""
  }

  def "test install project with no repo"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")

    when:
    def result = testProject.executeGradleTask("install")

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":javadoc").outcome == TaskOutcome.SUCCESS
    result.task(":javadocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signMavenArtifactsPublication").outcome == TaskOutcome.SUCCESS
    result.task(":publishMavenArtifactsPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS
    result.task(":install").outcome == TaskOutcome.SUCCESS

    where:
    groupId                               | artifactId    | versionName
    "com.norepo.snapshot.example"         | "snapshotlib" | "0.0.3-SNAPSHOT"
    "com.norepo.release.override.example" | "releaselib"  | "0.0.2"
  }

  def "test deploy project with no repo"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":jar") == null
    result.task(":javadoc") == null
    result.task(":javadocJar") == null
    result.task(":sourcesJar") == null
    result.task(":validateDeployable") == null
    result.task(":signMavenArtifactsPublication") == null
    result.task(":publishMavenArtifactsPublicationToMavenRepository") == null
    result.task(":install") == null
    result.task(":publish").outcome == TaskOutcome.UP_TO_DATE
    result.task(":deploy").outcome == TaskOutcome.UP_TO_DATE

    where:
    groupId                               | artifactId    | versionName
    "com.norepo.snapshot.example"         | "snapshotlib" | "0.0.3-SNAPSHOT"
    "com.norepo.release.override.example" | "releaselib"  | "0.0.2"
  }

}
