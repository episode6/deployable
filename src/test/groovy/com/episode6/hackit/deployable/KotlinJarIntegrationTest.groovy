package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import com.episode6.hackit.deployable.testutil.MyDependencyMap
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

/**
 * Tests {@link DeployableKotlinJarPlugin}
 */
class KotlinJarIntegrationTest extends Specification {

  @Rule final IntegrationTestProject testProject = new IntegrationTestProject()

  static final String CHOP_IMPORT = """
import com.episode6.hackit.chop.Chop

"""

  static final String CHOP_JAVADOC = "Here is a link to [Chop] for ya."

  private static String simpleBuildFile(String groupId, String versionName) {
    return """

buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath '${MyDependencyMap.lookupDep("org.jetbrains.kotlin:kotlin-gradle-plugin")}'
    classpath '${MyDependencyMap.lookupDep("org.jetbrains.dokka:dokka-gradle-plugin")}'
  }
}

plugins {
 id 'com.episode6.hackit.deployable.kt.jar'
}

apply plugin: 'kotlin'

repositories {
  jcenter()
}

group = '${groupId}'
version = '${versionName}'

dependencies {
  implementation '${MyDependencyMap.lookupDep("org.jetbrains.kotlin:kotlin-stdlib-jdk7")}'
}
 """
  }


  def "verify jar deploy tasks and output"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyKotlinFile("${groupId}.${artifactId}")
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)

    when:
    def result = testProject.executeGradleTask("deploy", "--stacktrace")

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":dokka").outcome == TaskOutcome.SUCCESS
    result.task(":javadocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signArchives").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify jar install tasks"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyKotlinFile("${groupId}.${artifactId}")

    when:
    def result = testProject.executeGradleTask("install")

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":dokka").outcome == TaskOutcome.SUCCESS
    result.task(":javadocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signArchives").outcome == TaskOutcome.SUCCESS
    result.task(":install").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives") == null

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.3-SNAPSHOT"
  }

  def "verify implementation dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyKotlinFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
dependencies {
  implementation 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency("com.episode6.hackit.chop", "chop-core", "0.1.8", "runtime")

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify api dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyKotlinFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
dependencies {
  api 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency("com.episode6.hackit.chop", "chop-core", "0.1.8", "compile")

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }
}

