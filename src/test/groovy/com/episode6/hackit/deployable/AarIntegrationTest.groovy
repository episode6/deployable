package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests {@link DeployableAarPlugin}
 */
class AarIntegrationTest extends Specification {

  private static String simpleManifest(String groupId, String artifactId) {
    return """
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${groupId}.${artifactId}">
    <application>
    </application>
</manifest>
"""
  }

  private static String simpleBuildFile(String groupId, String versionName) {
    return """
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.android.tools.build:gradle:2.2.3'
  }
}

plugins {
 id 'com.episode6.hackit.deployable.aar'
}

apply plugin: 'com.android.library'

group = '${groupId}'
version = '${versionName}'

android {
  compileSdkVersion 19
  buildToolsVersion "25.0.2"
}
 """
  }

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

  IntegrationTestProject testProject

  def setup() {
    testProject = new IntegrationTestProject(testProjectDir)
  }

  def "verify aar deploy tasks and output"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")
    testProject.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments("deploy")
        .build()

    then:
    result.task(":androidJavadocs").outcome == TaskOutcome.SUCCESS
    result.task(":androidJavadocsJar").outcome == TaskOutcome.SUCCESS
    result.task(":androidSourcesJar").outcome == TaskOutcome.SUCCESS
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signArchives").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    mavenOutputVerifier.verifyStandardOutput("aar")

    where:
    groupId                         | artifactId    | versionName
    "com.android.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.android.release.example"   | "releaselib"  | "0.0.1"
  }
}
