package com.episode6.hackit.deployable

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Tests DeployablePlugin.groovy
 */
class DeployablePluginTest extends Specification {

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile

  def setup() {
    buildFile = testProjectDir.newFile("build.gradle")
  }

  def "look for deploy task"() {
    given:
    buildFile << """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}

task hi() {
  doLast {
    println 'hi there'
  }
}
 """

    when:
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withPluginClasspath()
      .withArguments("hi")
      .build()

    then:
    result.output.contains('hi there')
    result.task(":hi").outcome == TaskOutcome.SUCCESS
  }
}
