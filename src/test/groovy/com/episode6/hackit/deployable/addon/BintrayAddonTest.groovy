package com.episode6.hackit.deployable.addon

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

/**
 * Tests the {@link BintrayAddonPlugin}
 */
class BintrayAddonTest extends Specification {

  private static String simpleBuildFile(String groupId, String versionName) {
    return """
plugins {
 id 'java-library'
 id 'com.episode6.hackit.deployable.jar'
 id 'com.episode6.hackit.deployable.addon.bintray'
}

group = '${groupId}'
version = '${versionName}'
 """
  }

  @Rule final IntegrationTestProject testProject = new IntegrationTestProject()

  def "verify jar deploy tasks and output"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)

    when:
    def result = testProject.failGradleTask('deploy')

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":javadoc").outcome == TaskOutcome.SUCCESS
    result.task(":javadocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signMavenArtifactsPublication").outcome == TaskOutcome.SUCCESS
    result.task(":publishMavenArtifactsPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS
    result.task(":bintrayUpload").outcome == TaskOutcome.SUCCESS
    result.task(":bintrayPublish").outcome == TaskOutcome.FAILED
    result.task(":deploy") == null
    result.output.contains('Repository name, package name or version name are null for project')

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }
}
