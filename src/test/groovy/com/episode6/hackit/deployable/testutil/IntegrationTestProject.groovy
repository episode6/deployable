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

  File newFile(Object... paths) {
    if (paths.length < 1) {
      throw new IllegalArgumentException("can't create file with empty path")
    }

    String fileName = paths[paths.length-1]
    File transDir = buildFolder.getRoot()
    for (int i = 0; i < paths.length-1; i++) {
      transDir = new File(transDir, paths[i])
      transDir.mkdir()
    }
    return new File(transDir, fileName)
  }

  File createNonEmptyJavaFile(String... packageSegments) {
    List<String> paths = new ArrayList<>()
    paths.addAll(["src", "main", "java"])
    paths.addAll(packageSegments)
    paths.add("SampleClass.java")

    File nonEmptyJavaFile = newFile(paths.toArray())
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

    File nonEmptyGroovyFile = newFile(paths.toArray())
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
