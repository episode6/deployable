package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

/**
 * Tests {@link DeployableValidationTask}
 */
class DeployableValidationTaskTest extends Specification {


  private static final String BUILD_FILE_HEADER = """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}
"""

  private static String simpleBuildFile(String groupId, String versionName) {
    return """
${BUILD_FILE_HEADER}

group = '${groupId}'
version = '${versionName}'
 """
  }

  @Rule final IntegrationTestProject testProject = new IntegrationTestProject()


  def "fail on missing groupId"() {
    given:
    testProject.rootProjectName = "test-artifact"
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.createNonEmptyJavaFile("com.testing.example")
    testProject.rootGradleBuildFile << """
${BUILD_FILE_HEADER}

version = '0.0.1-SNAPSHOT'
"""

    when:
    def result = testProject.failGradleTask("validateDeployable")

    then:
    result.task(":validateDeployable").outcome == TaskOutcome.FAILED
    result.output.contains("deployable validation failure")
    result.output.contains("Project Property: group")
  }

  def "fail on missing version"() {
    given:
    testProject.rootProjectName = "test-artifact"
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.createNonEmptyJavaFile("com.testing.example")
    testProject.rootGradleBuildFile << """
${BUILD_FILE_HEADER}

group = 'com.testing.example'
"""

    when:
    def result = testProject.failGradleTask("validateDeployable")

    then:
    result.task(":validateDeployable").outcome == TaskOutcome.FAILED
    result.output.contains("deployable validation failure")
    result.output.contains("Project Property: version")
  }

  def "fail on missing name"() {
    given:
    testProject.rootProjectName = ""
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.createNonEmptyJavaFile("com.testing.example")
    testProject.rootGradleBuildFile << simpleBuildFile("com.testing.example", "0.0.1-SNAPSHOT")

    when:
    def result = testProject.failGradleTask("validateDeployable")

    then:
    // this is no longer legal in gradle, so validation never has a chance to run
    result.task(":validateDeployable") == null
    result.output.contains("The project name must not be empty")
  }

  def "fail on missing 2 project properties and a few deployable ones"() {
    given:
    testProject.rootProjectName = "proj-name"
    testProject.createNonEmptyJavaFile("com.testing.example")
    testProject.rootGradleBuildFile << BUILD_FILE_HEADER
    testProject.testProperties.deployable {
      pom {
        description = null
        developer {
          id = null
        }
      }
    }
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat

    when:
    def result = testProject.failGradleTask("validateDeployable")

    then:
    result.task(":validateDeployable").outcome == TaskOutcome.FAILED
    result.output.contains("deployable validation failure")
    result.output.contains("Project Property: version")
    result.output.contains("Project Property: group")
    result.output.contains("deployable.pom.description")
    result.output.contains("deployable.pom.developer.id")
  }

  def "pass if only missing optional properties"() {
    given:
    testProject.rootProjectName = "test-artifact"
    testProject.createNonEmptyJavaFile("com.testing.example")
    testProject.rootGradleBuildFile << simpleBuildFile("com.testing.example", "0.0.1-SNAPSHOT")
    testProject.testProperties.deployable {
      nexus {
        username = null
        password = null
      }
    }
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat

    when:
    def result = testProject.executeGradleTask("validateDeployable")

    then:
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
  }
}
