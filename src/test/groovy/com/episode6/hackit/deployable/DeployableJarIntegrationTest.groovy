package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.util.RequiredPropertiesGenerator
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

/**
 * Tests DeployableJarPlugin.groovy
 */
class DeployableJarIntegrationTest extends BaseDeployableIntegrationSpec {

  def "verify deploy task"() {
    given:
    gradlePropertiesFile << RequiredPropertiesGenerator.generateGradleProperties(mavenDir, keyRingBundle)
    buildFile << """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}

group = 'com.example.groupid'
version = '0.0.1-SNAPSHOT'

deployable {
  pom {
    developer {
      name = "testDeveloperName"
    }
  }
  nexus {
    snapshotRepoUrl = "http://idevsix.com"
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
    result.task(":signArchives").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
  }
}
