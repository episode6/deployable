package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import com.episode6.hackit.deployable.testutil.MyDependencyMap
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

import static com.episode6.hackit.deployable.testutil.TestUtil.isGradleScopeOptional
import static com.episode6.hackit.deployable.testutil.TestUtil.mavenScopeForGradleConfig

/**
 * Tests deployable in a multi-project scenario
 */
class MultiProjectTest extends Specification {

  enum LibType {
    JAVA,
    ANDROID,
    GROOVY
  }

  static final String CHOP_IMPORT = """
import com.episode6.hackit.chop.Chop;

"""

  private static String chopDep(String config = "implementation") {
    return """
  ${config} 'com.episode6.hackit.chop:chop-core:0.1.8'
"""
  }

  private static String rootBuildFile(String groupId, String versionName) {
    return """
buildscript {
  repositories {
    jcenter()
    google()
  }
  dependencies {
    classpath '${MyDependencyMap.lookupDep("com.android.tools.build:gradle")}'
  }
}
allprojects {
  repositories {
    jcenter()
    google()
  }
  group = '${groupId}'
  version = '${versionName}'
}
 """
  }

  private static String javaBuildFile(String deps = "") {
    return """
plugins {
 id 'java-library'
 id 'com.episode6.hackit.deployable.jar'
}

dependencies {
${deps}
}
 """
  }

  private static String groovyBuildFile(String deps = "") {

    return """
plugins {
 id 'groovy'
 id 'com.episode6.hackit.deployable.jar'
 id 'com.episode6.hackit.deployable.addon.groovydocs'
}

dependencies {
  implementation localGroovy()
${deps}
}
 """
  }

  private static String androidBuildFile(String deps = "") {
    return """
plugins {
 id 'com.episode6.hackit.deployable.aar'
}

apply plugin: 'com.android.library'

android {
  compileSdkVersion 19
  buildToolsVersion "${MyDependencyMap.lookupVersion("android.buildtools")}"
}

dependencies {
${deps}
}
"""
  }

  private static String simpleAndroidManifest(String groupId, String artifactId) {
    return """
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${groupId}.${artifactId}">
    <application>
    </application>
</manifest>
"""
  }

  @Rule
  final IntegrationTestProject testProject = new IntegrationTestProject()

  def "test multi-project deployables"(String groupId, String versionName) {
    given:
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    File javalib = testProject.newFolder("javalib")
    File groovylib = testProject.newFolder("groovylib")
    File androidlib = testProject.newFolder("androidlib")
    testProject.rootGradleSettingFile << """
include ':javalib', ':groovylib', ':androidlib'
"""
    testProject.rootGradleBuildFile << rootBuildFile(groupId, versionName)
    javalib.newFile("build.gradle") << javaBuildFile()
    groovylib.newFile("build.gradle") << groovyBuildFile(projectDeps("javalib"))
    androidlib.newFile("build.gradle") << androidBuildFile(projectDeps("javalib", "groovylib"))

    testProject.createNonEmptyJavaFile("${groupId}.javalib", "SampleJavaClass", javalib)
    testProject.createNonEmptyGroovyFile("${groupId}.groovylib", "SampleGroovyClass", groovylib)
    testProject.createNonEmptyJavaFile("${groupId}.androidlib", "SampleAndroidClass", androidlib)
    androidlib.newFile("src", "main", "AndroidManifest.xml") << simpleAndroidManifest(groupId, "androidlib")

    MavenOutputVerifier javalibVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: "javalib",
        versionName: versionName,
        testProject: testProject)
    MavenOutputVerifier groovylibVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: "groovylib",
        versionName: versionName,
        testProject: testProject)
    MavenOutputVerifier androidlibVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: "androidlib",
        versionName: versionName,
        testProject: testProject)

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":javalib:deploy").outcome == TaskOutcome.SUCCESS
    javalibVerifier.verifyStandardOutput()

    result.task(":groovylib:deploy").outcome == TaskOutcome.SUCCESS
    groovylibVerifier.verifyStandardOutput()
    groovylibVerifier.verifyJarFile("groovydoc")
    groovylibVerifier.verifyPomDependency(groupId, "javalib", versionName, "runtime")

    result.task(":androidlib:deploy").outcome == TaskOutcome.SUCCESS
    androidlibVerifier.verifyStandardOutput("aar")
    androidlibVerifier.verifyPomDependency(groupId, "javalib", versionName, "runtime")
    androidlibVerifier.verifyPomDependency(groupId, "groovylib", versionName, "runtime")

    where:
    groupId                             | versionName
    "com.multiproject.snapshot.example" | "0.0.1-SNAPSHOT"
    "com.multiproject.release.example"  | "0.0.1"
  }

  // this test our extra configurations (provided, compileOptional, providedOptional) and
  // ensures that multi-projects will exclude their optional/provided dependencies from their
  // sibling dependent projects
  def "test multi-project extra configurations"(LibType libType, boolean pass, String config) {
    given:
    String groupId = "com.multiproject.release.example"
    String versionName = "0.2.7"

    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    File parentlib = testProject.newFolder("parentlib")
    File childlib = testProject.newFolder("childlib")
    testProject.rootGradleSettingFile << """
include ':parentlib', ':childlib'
"""
    testProject.rootGradleBuildFile << rootBuildFile(groupId, versionName)
    String parentDeps = chopDep(config)
    String childDeps = projectDeps("parentlib")
    if (pass) {
      childDeps = chopDep() + childDeps
    }

    switch (libType) {
      case LibType.ANDROID:
        parentlib.newFile("build.gradle") << androidBuildFile(parentDeps)
        childlib.newFile("build.gradle") << androidBuildFile(childDeps)
        parentlib.newFile("src", "main", "AndroidManifest.xml") << simpleAndroidManifest(groupId, "parentlib")
        childlib.newFile("src", "main", "AndroidManifest.xml") << simpleAndroidManifest(groupId, "childlib")
        testProject.createNonEmptyJavaFile("${groupId}.parentlib", "SampleParentClass", parentlib, CHOP_IMPORT)
        testProject.createNonEmptyJavaFile("${groupId}.childlib", "SampleChildClass", childlib, CHOP_IMPORT)
        break;
      case LibType.JAVA:
        parentlib.newFile("build.gradle") << javaBuildFile(parentDeps)
        childlib.newFile("build.gradle") << javaBuildFile(childDeps)
        testProject.createNonEmptyJavaFile("${groupId}.parentlib", "SampleParentClass", parentlib, CHOP_IMPORT)
        testProject.createNonEmptyJavaFile("${groupId}.childlib", "SampleChildClass", childlib, CHOP_IMPORT)
        break;
      case LibType.GROOVY:
        parentlib.newFile("build.gradle") << groovyBuildFile(parentDeps)
        childlib.newFile("build.gradle") << groovyBuildFile(childDeps)
        testProject.createNonEmptyGroovyFile("${groupId}.parentlib", "SampleParentClass", parentlib, CHOP_IMPORT)
        testProject.createNonEmptyGroovyFile("${groupId}.childlib", "SampleChildClass", childlib, CHOP_IMPORT)
        break;
    }

    MavenOutputVerifier parentlibVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: "parentlib",
        versionName: versionName,
        testProject: testProject)
    MavenOutputVerifier childlibVerifier = new MavenOutputVerifier(
        groupId: groupId,
        artifactId: "childlib",
        versionName: versionName,
        testProject: testProject)

    when:
    def parentResult
    def childResult
    try {
      parentResult = testProject.executeGradleTask(":parentlib:deploy");
      childResult = pass ? testProject.executeGradleTask(":childlib:deploy") : testProject.failGradleTask(":childlib:deploy")
    } catch (Throwable t) {
      println("BUILD FAIL libType: $libType, pass: $pass, config: $config")
      throw t
    }

    then:
    parentResult.task(":parentlib:deploy").outcome == TaskOutcome.SUCCESS
    String packaging = libType == LibType.ANDROID ? "aar" : "jar"
    parentlibVerifier.verifyStandardOutput(packaging)
    parentlibVerifier.verifyPomDependency(
        "com.episode6.hackit.chop",
        "chop-core",
        "0.1.8",
        mavenScopeForGradleConfig(config),
        isGradleScopeOptional(config))

    if (pass) {
      childResult.task(":childlib:deploy").outcome == TaskOutcome.SUCCESS
      childlibVerifier.verifyStandardOutput(packaging)
      childlibVerifier.verifyPomDependency(groupId, "parentlib", versionName, "runtime")
    } else {
      childResult.output.contains("import com.episode6.hackit.chop.Chop")
    }

    where:
    libType         | pass  | config
    LibType.JAVA    | true  | "mavenProvidedOptional"
    LibType.JAVA    | false | "mavenProvidedOptional"
    LibType.JAVA    | true  | "mavenProvided"
    LibType.JAVA    | false | "mavenProvided"
    LibType.JAVA    | true  | "mavenOptional"
    LibType.JAVA    | false | "mavenOptional"
    LibType.JAVA    | true  | "implementation"
    LibType.JAVA    | false | "implementation"
    LibType.GROOVY  | true  | "mavenProvidedOptional"
    LibType.GROOVY  | false | "mavenProvidedOptional"
    LibType.GROOVY  | true  | "mavenProvided"
    LibType.GROOVY  | false | "mavenProvided"
    LibType.GROOVY  | true  | "mavenOptional"
    LibType.GROOVY  | false | "mavenOptional"
    LibType.GROOVY  | true  | "implementation"
    LibType.GROOVY  | false | "implementation"
    LibType.ANDROID | true  | "mavenProvidedOptional"
    LibType.ANDROID | false | "mavenProvidedOptional"
    LibType.ANDROID | true  | "mavenProvided"
    LibType.ANDROID | false | "mavenProvided"
    LibType.ANDROID | true  | "mavenOptional"
    LibType.ANDROID | false | "mavenOptional"
    LibType.ANDROID | true  | "implementation"
    LibType.ANDROID | false | "implementation"

  }

  private static String projectDeps(String... dependentProjectNames) {
    StringBuilder builder = new StringBuilder()
    dependentProjectNames.each {
      builder.append("  implementation project(\":").append(it).append("\")\n")
    }
    return builder.toString()
  }
}
