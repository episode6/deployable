package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

/**
 * Tests {@link DeployableJarPlugin}
 */
class JarIntegrationTest extends Specification {

  static final String CHOP_IMPORT = """
import com.episode6.hackit.chop.Chop;

"""

  private static String simpleBuildFile(String groupId, String versionName) {
    return """
plugins {
 id 'java-library'
 id 'com.episode6.hackit.deployable.jar'
}

group = '${groupId}'
version = '${versionName}'
 """
  }

  @Rule final IntegrationTestProject testProject = new IntegrationTestProject()

  def "verify jar deploy tasks and output"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":javadoc").outcome == TaskOutcome.SUCCESS
    result.task(":javadocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signArchives").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify jar install tasks"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")

    when:
    def result = testProject.executeGradleTask("install")

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":javadoc").outcome == TaskOutcome.SUCCESS
    result.task(":javadocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signArchives").outcome == TaskOutcome.SUCCESS
    result.task(":install").outcome == TaskOutcome.SUCCESS
    result.task(":uploadArchives") == null

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.3-SNAPSHOT"
  }

  def "verify implementation dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
repositories {
  jcenter()
}

dependencies {
  implementation 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency("com.episode6.hackit.chop", "chop-core", "0.1.8", "compile")

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify api dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
repositories {
  jcenter()
}

dependencies {
  api 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency("com.episode6.hackit.chop", "chop-core", "0.1.8", "compile")

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify provided dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
repositories {
  jcenter()
}

dependencies {
  mavenProvided 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency("com.episode6.hackit.chop", "chop-core", "0.1.8", "provided")

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify optional dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
repositories {
  jcenter()
}

dependencies {
  implementation 'com.episode6.hackit.chop:chop-core:0.1.8', optional
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "compile",
        true)

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify optional dependencies with exclude"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFile("${groupId}.${artifactId}")
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
repositories {
  jcenter()
}

dependencies {
  implementation('org.spockframework:spock-core:1.1-groovy-2.4-rc-3') {
    optional(it)
    exclude module: 'groovy-all'
  }
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency(
        "org.spockframework",
        "spock-core",
        "1.1-groovy-2.4-rc-3",
        "compile",
        true)
    mavenOutputVerifier.verifyPomDependencyExclusion(
        "org.spockframework",
        "spock-core",
        "*",
        "groovy-all")

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }


  def "verify provided optional dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT)
    MavenOutputVerifier mavenOutputVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: artifactId,
        versionName: versionName,
        testProject: testProject)
    testProject.rootGradleBuildFile << """
repositories {
  jcenter()
}

dependencies {
  mavenProvided 'com.episode6.hackit.chop:chop-core:0.1.8', optional
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":uploadArchives").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "provided",
        true)

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }
}
