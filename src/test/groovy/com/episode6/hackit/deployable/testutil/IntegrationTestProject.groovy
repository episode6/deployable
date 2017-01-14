package com.episode6.hackit.deployable.testutil

import org.junit.rules.TemporaryFolder

/**
 * Hold information about the project-under-test
 */
class IntegrationTestProject implements TestProjectTrait {

  public final TemporaryFolder buildFolder
  File rootGradleBuildFile
  File rootGradlePropertiesFile
  File rootGradleSettingFile


  IntegrationTestProject(TemporaryFolder buildFolder) {
    this.buildFolder = buildFolder
    testProperties = new TestProperties()

    rootGradleBuildFile = buildFolder.newFile("build.gradle")
    rootGradlePropertiesFile = buildFolder.newFile("gradle.properties")
    rootGradleSettingFile = buildFolder.newFile("settings.gradle")

    setSnapshotMavenRepoDir(buildFolder.newFolder("mavenSnapshot"))
    setReleaseMavenRepoDir(buildFolder.newFolder("mavenRelease"))
  }

  File newFile(String... paths) {
    return buildFolder.getRoot().newFile(paths)
  }

  void setRootProjectName(String rootProjectName) {
    rootGradleSettingFile << """
rootProject.name = '${rootProjectName}'
"""
  }

  File createNonEmptyJavaFile(String packageName, String className = "SampleClass", File rootDir = buildFolder.getRoot()) {
    File dir = rootDir.newFolder("src", "main", "java").newFolderFromPackage(packageName)
    File nonEmptyJavaFile = dir.newFile("${className}.java")
    nonEmptyJavaFile << """
package ${packageName};

/**
 * A sample class for testing
 */
public class ${className} {

}
"""
    return nonEmptyJavaFile
  }

  File createNonEmptyGroovyFile(String packageName, String className = "SampleClass", File rootDir = buildFolder.getRoot()) {
    File dir = rootDir.newFolder("src", "main", "groovy").newFolderFromPackage(packageName)
    File nonEmptyGroovyFile = dir.newFile("${className}.groovy")
    nonEmptyGroovyFile << """
package ${packageName}

/**
 * A sample class for testing
 */
class ${className} {

}
"""
    return nonEmptyGroovyFile
  }
}
