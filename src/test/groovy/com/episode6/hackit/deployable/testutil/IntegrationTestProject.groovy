package com.episode6.hackit.deployable.testutil

import org.junit.rules.TemporaryFolder

/**
 * Hold information about the project-under-test
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
    testProperties = new TestProperties()

    rootGradleBuildFile = buildFolder.newFile("build.gradle")
    rootGradlePropertiesFile = buildFolder.newFile("gradle.properties")
    rootGradleSettingFile = buildFolder.newFile("settings.gradle")

    setSnapshotMavenRepoDir(buildFolder.newFolder("mavenSnapshot"))
    setReleaseMavenRepoDir(buildFolder.newFolder("mavenRelease"))
  }

  void setSnapshotMavenRepoDir(File snapshotMavenRepoDir) {
    snapshotMavenRepoDir.mkdirs()
    testProperties.setSnapshotRepo(snapshotMavenRepoDir)
    this.snapshotMavenRepoDir = snapshotMavenRepoDir
  }

  void setReleaseMavenRepoDir(File releaseMavenRepoDir) {
    releaseMavenRepoDir.mkdirs()
    testProperties.setReleaseRepo(releaseMavenRepoDir)
    this.releaseMavenRepoDir = releaseMavenRepoDir
  }

  File newFile(String... paths) {
    return buildFolder.getRoot().newFile(paths)
  }

  File createNonEmptyJavaFile(String... packageSegments) {
    List<String> paths = new ArrayList<>()
    paths.addAll(["src", "main", "java"])
    paths.addAll(packageSegments)
    paths.add("SampleClass.java")

    File nonEmptyJavaFile = newFile((String[])paths.toArray())
    nonEmptyJavaFile << """
package ${packageSegments.join(".")};

/**
 * A sample class for testing
 */
public class SampleClass {

}
"""
    return nonEmptyJavaFile
  }

  File createNonEmptyGroovyFile(String... packageSegments) {
    List<String> paths = new ArrayList<>()
    paths.addAll(["src", "main", "groovy"])
    paths.addAll(packageSegments)
    paths.add("SampleClass.groovy")

    File nonEmptyGroovyFile = newFile((String[])paths.toArray())
    nonEmptyGroovyFile << """
package ${packageSegments.join(".")}

/**
 * A sample class for testing
 */
class SampleClass {

}
"""
    return nonEmptyGroovyFile
  }
}
