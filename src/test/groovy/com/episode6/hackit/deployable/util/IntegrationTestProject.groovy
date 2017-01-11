package com.episode6.hackit.deployable.util

import org.junit.rules.TemporaryFolder

/**
 *
 */
class IntegrationTestProject {

  public final TemporaryFolder buildFolder
  File rootGradleBuildFile
  File rootGradlePropertiesFile
  File rootGradleSettingFile

  File snapshotMavenRepoDir
  File releaseMavenRepoDir

  TestProperties testProperties

  IntegrationTestProject(TemporaryFolder buildFolder) {
    this.buildFolder = buildFolder
    rootGradleBuildFile = buildFolder.newFile("build.gradle")
    rootGradlePropertiesFile = buildFolder.newFile("gradle.properties")
    rootGradleSettingFile = buildFolder.newFile("settings.gradle")

    snapshotMavenRepoDir = buildFolder.newFolder("mavenSnapshot")
    snapshotMavenRepoDir.mkdirs()
    releaseMavenRepoDir = buildFolder.newFolder("mavenRelease")
    releaseMavenRepoDir.mkdirs()

    testProperties = new TestProperties()
    testProperties.applyMavenRepos(releaseMavenRepoDir, snapshotMavenRepoDir)
  }
}
