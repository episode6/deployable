package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests DeployableAarPlugin.groovy
 */
class DeployableAarIntegrationTest extends Specification {

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

  IntegrationTestProject testProject

  def setup() {
    testProject = new IntegrationTestProject(testProjectDir)
    testProject.rootGradlePropertiesFile << testProject.testProperties.getInGradlePropertiesFormat()
    testProject.createNonEmptyJavaFile("com", "example", "groupid", "testlib")
    testProject.rootGradleSettingFile << """
rootProject.name = 'testlib'
"""
    testProject.newFile("src", "main", "AndroidManifest.xml") << """
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.groupid.testlib">
    <application>
    </application>
</manifest>
"""
    testProject.rootGradleBuildFile << """
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

group = 'com.example.groupid'
version = '0.0.1-SNAPSHOT'

android {
  compileSdkVersion 19
  buildToolsVersion "25.0.2"
}

 """
  }

  def "verify deploy tasks (aar)"() {
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

  def "verify aar-specific tasks"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments("signArchives")
        .build()

    then:
    result.task(":androidJavadocs").outcome == TaskOutcome.SUCCESS
    result.task(":androidJavadocsJar").outcome == TaskOutcome.SUCCESS
    result.task(":androidSourcesJar").outcome == TaskOutcome.SUCCESS
  }
}
