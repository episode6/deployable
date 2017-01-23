package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
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

  @Rule final IntegrationTestProject testProject = new IntegrationTestProject()

  def "test multi-project deployables"(String groupId, String versionName) {
    given:
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    File javalib = testProject.newFolder("javalib")
    File groovylib = testProject.newFolder("groovylib")
    File androidlib = testProject.newFolder("androidlib")
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
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":javalib:deploy").outcome == TaskOutcome.SUCCESS
    javalibVerifier.verifyStandardOutput()

    result.task(":groovylib:deploy").outcome == TaskOutcome.SUCCESS
    groovylibVerifier.verifyStandardOutput()
    groovylibVerifier.verifyJarFile("groovydoc")
    groovylibVerifier.verifyPomDependency(groupId, "javalib", versionName)

    result.task(":androidlib:deploy").outcome == TaskOutcome.SUCCESS
    androidlibVerifier.verifyStandardOutput("aar")
    androidlibVerifier.verifyPomDependency(groupId, "javalib", versionName)
    androidlibVerifier.verifyPomDependency(groupId, "groovylib", versionName)

    where:
    groupId                              | versionName
    "com.multiproject.snapshot.example"  | "0.0.1-SNAPSHOT"
    "com.multiproject.release.example"   | "0.0.1"
  }

  private static String convertDependentProjectsToDependencies(String... dependentProjectNames) {
    StringBuilder builder = new StringBuilder()
    dependentProjectNames.each {
      builder.append("  compile project(\":").append(it).append("\")\n")
    }
    return builder.toString()
  }
}
