package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests deployable in a multi-project scenario
 */
class MultiProjectTest extends Specification {

  private static String rootBuildFile(String groupId, String versionName) {
    return """
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.android.tools.build:gradle:2.2.3'
  }
}
allprojects {
  repositories {
    jcenter()
  }
  group = '${groupId}'
  version = '${versionName}'
}
 """
  }

  private static String javaBuildFile() {
    return """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}
 """
  }

  private static String groovyBuildFile(String... dependentProjects) {

    return """
plugins {
 id 'groovy'
 id 'com.episode6.hackit.deployable.jar'
 id 'com.episode6.hackit.deployable.addon.groovydocs'
}

dependencies {
  compile localGroovy()
${convertDependentProjectsToDependencies(dependentProjects)}
}
 """
  }

  private static String androidBuildFile(String... dependentProjects) {
    return """
plugins {
 id 'com.episode6.hackit.deployable.aar'
}

apply plugin: 'com.android.library'

android {
  compileSdkVersion 19
  buildToolsVersion "25.0.2"
}

dependencies {
${convertDependentProjectsToDependencies(dependentProjects)}
}
"""
  }

  private static String simpleAndroidManifest(String groupId, String artifactId) {
    return """
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${groupId}.${artifactId}">
    <application>
    </application>
</manifest>
"""
  }

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

  IntegrationTestProject testProject

  def setup() {
    testProject = new IntegrationTestProject(testProjectDir)
  }

  def "test multi-project deployables"(String groupId, String versionName) {
    given:
    File tmpM2Fldr = new File("build/m2")
    testProject.snapshotMavenRepoDir = tmpM2Fldr.newFolder("snapshot")
    testProject.releaseMavenRepoDir = tmpM2Fldr.newFolder("release")

    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    File javalib = testProject.newFile("javalib")
    File groovylib = testProject.newFile("groovylib")
    File androidlib = testProject.newFile("androidlib")
    mkdirs(javalib, groovylib, androidlib)
    testProject.rootGradleSettingFile << """
include ':javalib', ':groovylib', ':androidlib'
"""
    testProject.rootGradleBuildFile << rootBuildFile(groupId, versionName)
    javalib.newFile("build.gradle") << javaBuildFile()
    groovylib.newFile("build.gradle") << groovyBuildFile("javalib")
    androidlib.newFile("build.gradle") << androidBuildFile("javalib", "groovylib")

    testProject.createNonEmptyJavaFile("${groupId}.javalib", "SampleJavaClass", javalib)
    testProject.createNonEmptyGroovyFile("${groupId}.groovylib", "SampleGroovyClass", groovylib)
    testProject.createNonEmptyJavaFile("${groupId}.androidlib", "SampleAndroidClass", androidlib)
    androidlib.newFile("src", "main", "AndroidManifest.xml") << simpleAndroidManifest(groupId, "androidlib")

    MavenOutputVerifier javalibVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: "javalib",
        versionName: versionName,
        testProject: testProject)
    MavenOutputVerifier groovylibVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: "groovylib",
        versionName: versionName,
        testProject: testProject)
    MavenOutputVerifier androidlibVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: "androidlib",
        versionName: versionName,
        testProject: testProject)

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments("deploy")
        .build()

    then:
    result.task(":javalib:deploy").outcome == TaskOutcome.SUCCESS
    result.task(":groovylib:deploy").outcome == TaskOutcome.SUCCESS
    result.task(":androidlib:deploy").outcome == TaskOutcome.SUCCESS
    javalibVerifier.verifyAll()
    groovylibVerifier.verifyAll()
    groovylibVerifier.verifyJarFile("groovydoc")
    androidlibVerifier.verifyAll("aar")
    groovylibVerifier.verifyPomDependencies(
        new MavenOutputVerifier.MavenDependency(groupId: groupId, artifactId: "javalib", version: versionName),
        new MavenOutputVerifier.MavenDependency(groupId: groupId, artifactId: "andfroidlib", version: versionName)
    )

    where:
    groupId                              | versionName
    "com.multiproject.snapshot.example"  | "0.0.1-SNAPSHOT"
    "com.multiproject.release.example"   | "0.0.1"
  }

  private static void mkdirs(File... dirs) {
    dirs.each {
      it.mkdirs()
    }
  }

  private static String convertDependentProjectsToDependencies(String... dependentProjectNames) {
    StringBuilder builder = new StringBuilder()
    dependentProjectNames.each {
      builder.append("  compile project(\":").append(it).append("\")\n")
    }
    return builder.toString()
  }
}
