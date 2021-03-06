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

  static final String CHOP_JAVADOC = "Here is a link to {@link Chop} for ya."

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

  @Rule IntegrationTestProject testProject = new IntegrationTestProject()

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
    def result = testProject.executeGradleTask("deploy", '--stacktrace')

    then:
    result.task(":jar").outcome == TaskOutcome.SUCCESS
    result.task(":javadoc").outcome == TaskOutcome.SUCCESS
    result.task(":javadocJar").outcome == TaskOutcome.SUCCESS
    result.task(":sourcesJar").outcome == TaskOutcome.SUCCESS
    result.task(":validateDeployable").outcome == TaskOutcome.SUCCESS
    result.task(":signMavenArtifactsPublication").outcome == TaskOutcome.SUCCESS
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
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
    result.task(":signMavenArtifactsPublication").outcome == TaskOutcome.SUCCESS
    result.task(":publishMavenArtifactsPublicationToMavenLocal").outcome == TaskOutcome.SUCCESS
    result.task(":install").outcome == TaskOutcome.SUCCESS

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.3-SNAPSHOT"
  }

  def "verify implementation dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
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
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency("com.episode6.hackit.chop", "chop-core", "0.1.8", "runtime")

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
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
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
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
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
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
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
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
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
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
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
  mavenOptional 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "runtime",
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
  mavenOptional('org.spockframework:spock-core:1.1-groovy-2.4-rc-3') {
    exclude module: 'groovy-all'
  }
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency(
        "org.spockframework",
        "spock-core",
        "1.1-groovy-2.4-rc-3",
        "runtime",
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
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
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
  mavenProvidedOptional 'com.episode6.hackit.chop:chop-core:0.1.8'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
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

  def "verify mapped dependencies"(String groupId, String artifactId, String versionName) {
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

configurations {
  someConfig
  someOtherConfig
}

dependencies {
  someConfig 'org.spockframework:spock-core:1.1-groovy-2.4'
  someOtherConfig 'com.episode6.hackit.chop:chop-core:0.1.8'
}

deployable.pom.dependencyConfigurations {
  map configurations.someConfig, "provided"
  map "someOtherConfig", "compile"
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "compile")
    mavenOutputVerifier.verifyPomDependency(
        "org.spockframework",
        "spock-core",
        "1.1-groovy-2.4",
        "provided")

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify mapped optional dependencies"(String groupId, String artifactId, String versionName) {
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

configurations {
  someConfig
  someOtherConfig
}

dependencies {
  someConfig 'org.spockframework:spock-core:1.1-groovy-2.4'
  someOtherConfig 'com.episode6.hackit.chop:chop-core:0.1.8'
}

deployable.pom.dependencyConfigurations {
  mapOptional configurations.someConfig, "compile"
  mapOptional "someOtherConfig", "provided"
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        "provided",
        true)
    mavenOutputVerifier.verifyPomDependency(
        "org.spockframework",
        "spock-core",
        "1.1-groovy-2.4",
        "compile",
        true)

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify all built-in config type"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
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
  implementation 'io.reactivex.rxjava2:rxjava:2.1.12'
  mavenOptional 'org.assertj:assertj-core:3.9.1'
  mavenProvided 'org.mockito:mockito-core:2.9.0'
  mavenProvidedOptional 'com.squareup.dagger:dagger:1.2.5'
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency("com.episode6.hackit.chop", "chop-core", "0.1.8", "compile")
    mavenOutputVerifier.verifyPomDependency("io.reactivex.rxjava2", "rxjava", "2.1.12", "runtime")
    mavenOutputVerifier.verifyPomDependency("org.assertj", "assertj-core", "3.9.1", "runtime", true)
    mavenOutputVerifier.verifyPomDependency("org.mockito", "mockito-core", "2.9.0", "provided")
    mavenOutputVerifier.verifyPomDependency("com.squareup.dagger", "dagger", "1.2.5", "provided", true)

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify unmap dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
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

deployable.pom.dependencyConfigurations {
  unmap "implementation"
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyNoDependencies()

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify unmap custom dependencies"(String groupId, String artifactId, String versionName) {
    given:
    testProject.rootProjectName = artifactId
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    testProject.rootGradleBuildFile << simpleBuildFile(groupId, versionName)
    testProject.createNonEmptyJavaFileWithImports("${groupId}.${artifactId}", CHOP_IMPORT, CHOP_JAVADOC)
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
  mavenOptional 'com.episode6.hackit.chop:chop-core:0.1.8'
}

deployable.pom.dependencyConfigurations {
  unmap "mavenOptional"
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyNoDependencies()

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify re-mapped dependencies from built in config"(String groupId, String artifactId, String versionName) {
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
  implementation 'org.spockframework:spock-core:1.1-groovy-2.4'
}

deployable.pom.dependencyConfigurations {
  map configurations.implementation, "provided"
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency(
        "org.spockframework",
        "spock-core",
        "1.1-groovy-2.4",
        "provided")
    mavenOutputVerifier.verifyNumberOfDependencies(1)

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }

  def "verify re-mapped dependencies from built in custom config"(String groupId, String artifactId, String versionName) {
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
  mavenProvidedOptional 'org.spockframework:spock-core:1.1-groovy-2.4'
}

deployable.pom.dependencyConfigurations {
  map "mavenProvidedOptional", "compile"
}
"""
    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":publishMavenArtifactsPublicationToMavenRepository").outcome == TaskOutcome.SUCCESS
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
    result.task(":install") == null
    mavenOutputVerifier.verifyStandardOutput()
    mavenOutputVerifier.verifyPomDependency(
        "org.spockframework",
        "spock-core",
        "1.1-groovy-2.4",
        "compile")
    mavenOutputVerifier.verifyNumberOfDependencies(1)

    where:
    groupId                 | artifactId    | versionName
    "com.snapshot.example"  | "snapshotlib" | "0.0.1-SNAPSHOT"
    "com.release.example"   | "releaselib"  | "0.0.2"
  }
}
