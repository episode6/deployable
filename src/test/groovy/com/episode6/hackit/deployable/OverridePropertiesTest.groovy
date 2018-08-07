package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

/**
 * Tests overriding deployable's gradle.properties in the build file
 */
class OverridePropertiesTest extends Specification {

  private static String overridePropertiesBuildFile(
      String groupId,
      String versionName,
      DeployablePluginExtension deployable) {
    return """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}

group = '${groupId}'
version = '${versionName}'

deployable {
  pom {
    description "${deployable.pom.description}"
    url "${deployable.pom.url}"

    scm {
      url "${deployable.pom.scm.url}"
      connection "${deployable.pom.scm.connection}"
      developerConnection "${deployable.pom.scm.developerConnection}"
    }
    license {
      name "${deployable.pom.license.name}"
      url "${deployable.pom.license.url}"
      distribution "${deployable.pom.license.distribution}"
    }
    developer {
      id "${deployable.pom.developer.id}"
      name "${deployable.pom.developer.name}"
      email "${deployable.pom.developer.email}"
    }
  }

  nexus {
    username "${deployable.nexus.username}"
    password "${deployable.nexus.password}"
    snapshotRepoUrl "${deployable.nexus.snapshotRepoUrl}"
    releaseRepoUrl "${deployable.nexus.releaseRepoUrl}"
  }
}

 """
  }

  @Rule final IntegrationTestProject testProject = new IntegrationTestProject()

  def "test override gradle.settings"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)

    // write current properties to gradle.properties
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat

    //change the properties
    testProject.snapshotMavenRepoDir = testProject.newFolder("overrideMavenSnapshot")
    testProject.releaseMavenRepoDir = testProject.newFolder("overrideMavenRelease")
    testProject.testProperties.deployable {
      pom {
        description "OVERRIDE Test POM Description"
        url "https://OVERRIDE.pom_url.com"

        scm {
          url "OVERRIDEextensible"
          connection "scm:https://OVERRIDE.scm_connection.com"
          developerConnection "scm:https://OVERRIDE.scm_dev_connection.com"
        }
        license {
          name "The MIT OVERRIDE License (MIT)"
          url "https://OVERRIDE.license.com"
          distribution "repoOVERRIDE"
        }
        developer {
          id "DeveloperIdOVERRIDE"
          name "DeveloperNameOVERRIDE"
          email "TestEmail@Nowhere.com"
        }
      }

      nexus {
        username "nexusUsernameOVERRIDE"
        password "nexusPasswordOVERRIDE"
      }
    }

    // write the build file with the overridden properties in it
    testProject.rootGradleBuildFile << overridePropertiesBuildFile(groupId, versionName, testProject.testProperties.deployable)

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":javadoc").outcome == TaskOutcome.SUCCESS
    result.task(":javadocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signMavenArtifactsPublication").outcome == TaskOutcome.SUCCESS
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    // check one of the properties to make sure it was overridden, let mavenOutputVerifier handle the rest
    mavenOutputVerifier.getArtifactFile("pom").asXml().licenses.license.name.text() == "The MIT OVERRIDE License (MIT)"
    mavenOutputVerifier.verifyStandardOutput()

    where:
    groupId                          | artifactId    | versionName
    "com.snapshot.override.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.override.example"   | "releaselib"  | "0.0.2"
  }

  def "test override main artifact"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}

group = '${groupId}'
version = '${versionName}'

deployable.publication.main {
}
"""

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    mavenOutputVerifier.verifyStandardOutputSkipMainArtifact()

    where:
    groupId                          | artifactId    | versionName
    "com.snapshot.override.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.override.example"   | "releaselib"  | "0.0.2"
  }
}
