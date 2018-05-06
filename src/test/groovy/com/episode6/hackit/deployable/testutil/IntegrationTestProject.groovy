package com.episode6.hackit.deployable.testutil

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.TemporaryFolder

/**
 * Hold information about the project-under-test
 */
class IntegrationTestProject extends TemporaryFolder {

  File rootGradleBuildFile
  File rootGradlePropertiesFile
  File rootGradleSettingFile

  File snapshotMavenRepoDir
  File releaseMavenRepoDir

  TestProperties testProperties

  @Override
  protected void before() {
    super.before()
    testProperties = new TestProperties()

    rootGradleBuildFile = newFile("build.gradle")
    rootGradlePropertiesFile = newFile("gradle.properties")
    rootGradleSettingFile = newFile("settings.gradle")

    setSnapshotMavenRepoDir(newFolder("mavenSnapshot"))
    setReleaseMavenRepoDir(newFolder("mavenRelease"))
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

  void setRootProjectName(String rootProjectName) {
    rootGradleSettingFile << """
rootProject.name = '${rootProjectName}'
"""
  }

  File createNonEmptyJavaFileWithImports(String packageName, String imports, String javadocComent = "test comment") {
    return createNonEmptyJavaFile(packageName, "SampleClass", root, imports, javadocComent)
  }

  File createNonEmptyJavaFile(
      String packageName,
      String className = "SampleClass",
      File rootDir = root,
      String imports = "",
      String javadocComent = "test comment") {
    File dir = rootDir.newFolder("src", "main", "java").newFolderFromPackage(packageName)
    File nonEmptyJavaFile = dir.newFile("${className}.java")
    nonEmptyJavaFile << """
package ${packageName};

${imports}

/**
 * ${javadocComent}
 */
public class ${className} {

}
"""
    return nonEmptyJavaFile
  }

  File createNonEmptyKotlinFile(
      String packageName,
      String className = "SampleClass",
      File rootDir = root,
      String imports = "",
      String javadocComent = "test comment") {
    File dir = rootDir.newFolder("src", "main", "kotlin").newFolderFromPackage(packageName)
    File nonEmptyJavaFile = dir.newFile("${className}.kt")
    nonEmptyJavaFile << """
package ${packageName}

${imports}

/**
 * ${javadocComent}
 */
class ${className} {

}
"""
    return nonEmptyJavaFile
  }

  File createNonEmptyGroovyFile(String packageName, String className = "SampleClass", File rootDir = root, String imports = "") {
    File dir = rootDir.newFolder("src", "main", "groovy").newFolderFromPackage(packageName)
    File nonEmptyGroovyFile = dir.newFile("${className}.groovy")
    nonEmptyGroovyFile << """
package ${packageName}

${imports}

/**
 * A sample class for testing
 */
class ${className} {

}
"""
    return nonEmptyGroovyFile
  }

  BuildResult executeGradleTask(String... task) {
    return GradleRunner.create()
        .withProjectDir(root)
        .withPluginClasspath()
        .withArguments(task)
        .build()
  }

  BuildResult failGradleTask(String... task) {
    return GradleRunner.create()
        .withProjectDir(root)
        .withPluginClasspath()
        .withArguments(task)
        .buildAndFail()
  }
}
