package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.testutil.IntegrationTestProject
import com.episode6.hackit.deployable.testutil.MavenOutputVerifier
import com.episode6.hackit.deployable.testutil.MyDependencyMap
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

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

  private static String chopDep(String config = "implementation", boolean optional = false) {
    return """
  ${config} 'com.episode6.hackit.chop:chop-core:0.1.8'${optional ? ", optional" : ""}
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
 id 'java'
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
  buildToolsVersion "26.0.2"
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

  @Rule final IntegrationTestProject testProject = new IntegrationTestProject()

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
    groovylibVerifier.verifyPomDependency(groupId, "javalib", versionName)

    result.task(":androidlib:deploy").outcome == TaskOutcome.SUCCESS
    androidlibVerifier.verifyStandardOutput("aar")
    androidlibVerifier.verifyPomDependency(groupId, "javalib", versionName)
    androidlibVerifier.verifyPomDependency(groupId, "groovylib", versionName)

    where:
    groupId                              | versionName
    "com.multiproject.snapshot.example"  | "0.0.1-SNAPSHOT"
    "com.multiproject.release.example"   | "0.0.1"
  }

  // this test our extra configurations (provided, compileOptional, providedOptional) and
  // ensures that multi-projects will exclude their optional/provided dependencies from their
  // sibling dependent projects
  def "test multi-project extra configurations"(LibType libType, boolean pass, String config, boolean optional) {
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
    String parentDeps = chopDep(config, optional)
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
      println("BUILD FAIL libType: $libType, pass: $pass, config: $config, optional: $optional")
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
        optional)

    if (pass) {
      childResult.task(":childlib:deploy").outcome == TaskOutcome.SUCCESS
      childlibVerifier.verifyStandardOutput(packaging)
      childlibVerifier.verifyPomDependency(groupId, "parentlib", versionName)
    } else {
      childResult.output.contains("import com.episode6.hackit.chop.Chop")
    }

    where:
    libType         | pass  | config     | optional
    LibType.JAVA    | true  | "provided" | true
    LibType.JAVA    | false | "provided" | true
    LibType.JAVA    | true  | "provided" | false
    LibType.JAVA    | false | "provided" | false
    LibType.JAVA    | true  | "implementation"  | true
    LibType.JAVA    | false | "implementation"  | true
    LibType.JAVA    | true  | "implementation"  | false
    LibType.JAVA    | false | "implementation"  | false
    LibType.GROOVY  | true  | "provided" | true
    LibType.GROOVY  | false | "provided" | true
    LibType.GROOVY  | true  | "provided" | false
    LibType.GROOVY  | false | "provided" | false
    LibType.GROOVY  | true  | "implementation"  | true
    LibType.GROOVY  | false | "implementation"  | true
    LibType.GROOVY  | true  | "implementation"  | false
    LibType.GROOVY  | false | "implementation"  | false
    LibType.ANDROID | true  | "provided" | true
    LibType.ANDROID | false | "provided" | true
    LibType.ANDROID | true  | "provided" | false
    LibType.ANDROID | false | "provided" | false
    LibType.ANDROID | true  | "implementation"  | true
    LibType.ANDROID | false | "implementation"  | true
    LibType.ANDROID | true  | "implementation"  | false
    LibType.ANDROID | false | "implementation"  | false

  }

  private static String projectDeps(String... dependentProjectNames) {
    StringBuilder builder = new StringBuilder()
    dependentProjectNames.each {
      builder.append("  implementation project(\":").append(it).append("\")\n")
    }
    return builder.toString()
  }
}
