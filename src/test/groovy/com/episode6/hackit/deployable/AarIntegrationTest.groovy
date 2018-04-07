package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import com.episode6.hackit.deployable.testutil.MyDependencyMap
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

/**
 * Tests {@link DeployableAarPlugin}
 */
class AarIntegrationTest extends Specification {

  static final String CHOP_IMPORT = """
import com.episode6.hackit.chop.Chop;

"""

  static final String CHOP_JAVADOC = "Here is a link to {@link Chop} for ya."

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
    google()
  }
  dependencies {
    classpath '${MyDependencyMap.lookupDep("com.android.tools.build:gradle")}'
  }
}

plugins {
 id 'com.episode6.hackit.deployable.aar'
}

apply plugin: 'com.android.library'

group = '${groupId}'
version = '${versionName}'

repositories {
  jcenter()
  google()
}

android {
  compileSdkVersion 19
}
 """
  }

  @Rule final IntegrationTestProject testProject = new IntegrationTestProject()

  def "verify aar deploy tasks and output"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")
    testProject.root.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":androidReleaseJavadocs").outcome == TaskOutcome.SUCCESS
    result.task(":androidReleaseJavadocsJar").outcome == TaskOutcome.SUCCESS
    result.task(":androidReleaseSourcesJar").outcome == TaskOutcome.SUCCESS
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

  def "verify implementation dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
    testProject.root.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
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
    def result = testProject.executeGradleTask("deploy", "--stacktrace")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    mavenOutputVerifier.verifyStandardOutput("aar")
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "runtime")

    where:
    groupId                         | artifactId    | versionName
    "com.android.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.android.release.example"   | "releaselib"  | "0.0.1"
  }

  def "verify api dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
    testProject.root.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
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
    mavenOutputVerifier.verifyStandardOutput("aar")
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "compile")

    where:
    groupId                         | artifactId    | versionName
    "com.android.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.android.release.example"   | "releaselib"  | "0.0.1"
  }

  def "verify optional dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
    testProject.root.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
dependencies {
  mavenOptional 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    mavenOutputVerifier.verifyStandardOutput("aar")
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "runtime",
        true)

    where:
    groupId                         | artifactId    | versionName
    "com.android.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.android.release.example"   | "releaselib"  | "0.0.1"
  }

  def "verify provided dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
    testProject.root.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
dependencies {
  mavenProvided 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    mavenOutputVerifier.verifyStandardOutput("aar")
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "provided")

    where:
    groupId                         | artifactId    | versionName
    "com.android.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.android.release.example"   | "releaselib"  | "0.0.1"
  }

  def "verify provided optional dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
    testProject.root.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """

dependencies {
  mavenProvidedOptional 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    mavenOutputVerifier.verifyStandardOutput("aar")
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "provided",
        true)

    where:
    groupId                         | artifactId    | versionName
    "com.android.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.android.release.example"   | "releaselib"  | "0.0.1"
  }

  def "verify aar dependencies"(String groupId, String artifactId, String versionName) {
    given:
    def chopAndroidImport = """
import com.episode6.hackit.chop.android.AndroidDebugTree;
"""
    def chopAndroidJavadoc = "Link to {@link AndroidDebugTree}"
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", chopAndroidImport, chopAndroidJavadoc)
    testProject.root.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """


dependencies {
  implementation 'com.episode6.hackit.chop:chop-android:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy", "--stacktrace")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    mavenOutputVerifier.verifyStandardOutput("aar")
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-android",
        "0.1.8",
        "runtime")

    where:
    groupId                         | artifactId    | versionName
    "com.android.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.android.release.example"   | "releaselib"  | "0.0.1"
  }

  def "verify unmap dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
    testProject.root.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """


dependencies {
  implementation 'com.episode6.hackit.chop:chop-core:0.1.8'
}

mavenDependencies {
  unmap "implementation"
}
"""
    when:
    def result = testProject.executeGradleTask("deploy", "--stacktrace")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    mavenOutputVerifier.verifyStandardOutput("aar")
    mavenOutputVerifier.verifyNoDependencies()

    where:
    groupId                         | artifactId    | versionName
    "com.android.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.android.release.example"   | "releaselib"  | "0.0.1"
  }

  def "verify unmap custom dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
    testProject.root.newFile("src", "main", "AndroidManifest.xml") << simpleManifest(groupId, artifactId)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """


dependencies {
  mavenOptional 'com.episode6.hackit.chop:chop-core:0.1.8'
}

mavenDependencies {
  unmap "mavenOptional"
}
"""
    when:
    def result = testProject.executeGradleTask("deploy", "--stacktrace")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    mavenOutputVerifier.verifyStandardOutput("aar")
    mavenOutputVerifier.verifyNoDependencies()

    where:
    groupId                         | artifactId    | versionName
    "com.android.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.android.release.example"   | "releaselib"  | "0.0.1"
  }
}
