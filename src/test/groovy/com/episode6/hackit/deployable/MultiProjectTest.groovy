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
    GROOVY,
    KOTLIN,
    KANDROID
  }

  static final String CHOP_IMPORT = """
import com.episode6.hackit.chop.Chop;

"""

  static final String CHOP_IMPORT_KOTLIN = """
import com.episode6.hackit.chop.Chop

"""

  private static String chopDep(String config = "implementation") {
    return """
  ${config} 'com.episode6.hackit.chop:chop-core:0.1.8'
"""
  }

  private static String rootBuildFile(String groupId, String versionName) {
    return """
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

  private static String kotlinBuildFile(String deps = "") {
    return """
plugins {
 id 'kotlin'
 id 'com.episode6.hackit.deployable.kt.jar'
}

dependencies {
  implementation '${MyDependencyMap.lookupDep("org.jetbrains.kotlin:kotlin-stdlib-jdk7")}'
${deps}
}
"""
  }


  private static String androidBuildFile(String deps = "") {
    return """
plugins {
 id 'com.android.library'
 id 'com.episode6.hackit.deployable.aar'
}

android {
  compileSdkVersion 19
}

dependencies {
${deps}
}
"""
  }

  private static String kotlinAndroidBuildFile(String deps = "") {
    return """
plugins {
 id 'com.android.library'
 id 'kotlin-android'
 id 'com.episode6.hackit.deployable.kt.aar'
}

android {
  compileSdkVersion 19
}

dependencies {
  implementation '${MyDependencyMap.lookupDep("org.jetbrains.kotlin:kotlin-stdlib-jdk7")}'
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

  private Map<String, MavenOutputVerifier> configureSimpleMultiProject(String groupId, String versionName, Map deps = [:]) {
    testProject.rootGradlePropertiesFile << testProject.testProperties.inGradlePropertiesFormat
    File javalib = testProject.newFolder("javalib")
    File groovylib = testProject.newFolder("groovylib")
    File kotlinlib = testProject.newFolder("kotlinlib")
    File androidlib = testProject.newFolder("androidlib")
    File kandroidlib = testProject.newFolder("kandroidlib")
    testProject.rootGradleSettingFile << """
include ':javalib', ':groovylib', ':androidlib', ':kotlinlib', ':kandroidlib'
"""
    testProject.rootGradleBuildFile << rootBuildFile(groupId, versionName)
    javalib.newFile("build.gradle") << javaBuildFile() + deps.javalib
    groovylib.newFile("build.gradle") << groovyBuildFile() + deps.groovylib
    kotlinlib.newFile("build.gradle") << kotlinBuildFile() + deps.kotlinlib
    androidlib.newFile("build.gradle") << androidBuildFile() + deps.androidlib
    kandroidlib.newFile("build.gradle") << kotlinAndroidBuildFile() + deps.kandroidlib

    testProject.createNonEmptyJavaFile("${groupId}.javalib", "SampleJavaClass", javalib)
    testProject.createNonEmptyGroovyFile("${groupId}.groovylib", "SampleGroovyClass", groovylib)
    testProject.createNonEmptyKotlinFile("${groupId}.kotlinlib", "SampleKotlinClass", kotlinlib)
    testProject.createNonEmptyJavaFile("${groupId}.androidlib", "SampleAndroidClass", androidlib)
    testProject.createNonEmptyKotlinFile("${groupId}.kandroidlib", "SampleKotlinAndroidClass", kandroidlib)
    androidlib.newFile("src", "main", "AndroidManifest.xml") << simpleAndroidManifest(groupId, "androidlib")
    kandroidlib.newFile("src", "main", "AndroidManifest.xml") << simpleAndroidManifest(groupId, "kandroidlib")
    return [
        javalibVerifier    : new MavenOutputVerifier(
            groupId: groupId,
            artifactId: "javalib",
            versionName: versionName,
            testProject: testProject),
        groovylibVerifier  : new MavenOutputVerifier(
            groupId: groupId,
            artifactId: "groovylib",
            versionName: versionName,
            testProject: testProject),
        kotlinlibVerifier  : new MavenOutputVerifier(
            groupId: groupId,
            artifactId: "kotlinlib",
            versionName: versionName,
            testProject: testProject),
        androidlibVerifier : new MavenOutputVerifier(
            groupId: groupId,
            artifactId: "androidlib",
            versionName: versionName,
            testProject: testProject),
        kandroidlibVerifier: new MavenOutputVerifier(
            groupId: groupId,
            artifactId: "kandroidlib",
            versionName: versionName,
            testProject: testProject)
    ]
  }

  @Rule
  final IntegrationTestProject testProject = new IntegrationTestProject()

  def "test multi-project deployables"(String groupId, String versionName) {
    given:
    def v = configureSimpleMultiProject(groupId, versionName, [
        groovylib: deps("javalib"),
        kotlinlib: deps("javalib"),
        androidlib: deps("javalib", "groovylib", "kotlinlib"),
        kandroidlib: deps("javalib", "groovylib", "kotlinlib", "androidlib"),
    ])

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":javalib:deploy").outcome == TaskOutcome.SUCCESS
    v.javalibVerifier.verifyStandardOutput()

    result.task(":groovylib:deploy").outcome == TaskOutcome.SUCCESS
    v.groovylibVerifier.verifyStandardOutput()
    v.groovylibVerifier.verifyJarFile("groovydoc")
    v.groovylibVerifier.verifyPomDependency(groupId, "javalib", versionName, "runtime")

    result.task(":kotlinlib:deploy").outcome == TaskOutcome.SUCCESS
    v.kotlinlibVerifier.verifyStandardOutput()
    v.kotlinlibVerifier.verifyPomDependency(groupId, "javalib", versionName, "runtime")

    result.task(":androidlib:deploy").outcome == TaskOutcome.SUCCESS
    v.androidlibVerifier.verifyStandardOutput("aar")
    v.androidlibVerifier.verifyPomDependency(groupId, "javalib", versionName, "runtime")
    v.androidlibVerifier.verifyPomDependency(groupId, "groovylib", versionName, "runtime")
    v.androidlibVerifier.verifyPomDependency(groupId, "kotlinlib", versionName, "runtime")

    result.task(":kandroidlib:deploy").outcome == TaskOutcome.SUCCESS
    v.kandroidlibVerifier.verifyStandardOutput("aar")
    v.kandroidlibVerifier.verifyPomDependency(groupId, "javalib", versionName, "runtime")
    v.kandroidlibVerifier.verifyPomDependency(groupId, "groovylib", versionName, "runtime")
    v.kandroidlibVerifier.verifyPomDependency(groupId, "kotlinlib", versionName, "runtime")
    v.kandroidlibVerifier.verifyPomDependency(groupId, "androidlib", versionName, "runtime")

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
      case LibType.KANDROID:
        parentlib.newFile("build.gradle") << kotlinAndroidBuildFile(parentDeps)
        childlib.newFile("build.gradle") << kotlinAndroidBuildFile(childDeps)
        parentlib.newFile("src", "main", "AndroidManifest.xml") << simpleAndroidManifest(groupId, "parentlib")
        childlib.newFile("src", "main", "AndroidManifest.xml") << simpleAndroidManifest(groupId, "childlib")
        testProject.createNonEmptyKotlinFile("${groupId}.parentlib", "SampleParentClass", parentlib, CHOP_IMPORT_KOTLIN)
        testProject.createNonEmptyKotlinFile("${groupId}.childlib", "SampleChildClass", childlib, CHOP_IMPORT_KOTLIN)
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
      case LibType.KOTLIN:
        parentlib.newFile("build.gradle") << kotlinBuildFile(parentDeps)
        childlib.newFile("build.gradle") << kotlinBuildFile(childDeps)
        testProject.createNonEmptyKotlinFile("${groupId}.parentlib", "SampleParentClass", parentlib, CHOP_IMPORT_KOTLIN)
        testProject.createNonEmptyKotlinFile("${groupId}.childlib", "SampleChildClass", childlib, CHOP_IMPORT_KOTLIN)
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
    String packaging = getPackaging(libType)
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
    LibType.KOTLIN | true  | "mavenProvidedOptional"
    LibType.KOTLIN | false | "mavenProvidedOptional"
    LibType.KOTLIN | true  | "mavenProvided"
    LibType.KOTLIN | false | "mavenProvided"
    LibType.KOTLIN | true  | "mavenOptional"
    LibType.KOTLIN | false | "mavenOptional"
    LibType.KOTLIN | true  | "implementation"
    LibType.KOTLIN | false | "implementation"
    LibType.KANDROID    | true  | "mavenProvidedOptional"
    LibType.KANDROID    | false | "mavenProvidedOptional"
    LibType.KANDROID    | true  | "mavenProvided"
    LibType.KANDROID    | false | "mavenProvided"
    LibType.KANDROID    | true  | "mavenOptional"
    LibType.KANDROID    | false | "mavenOptional"
    LibType.KANDROID    | true  | "implementation"
    LibType.KANDROID    | false | "implementation"
  }

  def "test multi-project publication.main overrides"(String groupId, String versionName) {
    given:
    def v = configureSimpleMultiProject(groupId, versionName)
    testProject.rootGradleBuildFile.text += """
subprojects {
  afterEvaluate {
    deployable.publication.main {}
  }
}
"""

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":javalib:deploy").outcome == TaskOutcome.SUCCESS
    v.javalibVerifier.verifyStandardOutputSkipMainArtifact()

    result.task(":groovylib:deploy").outcome == TaskOutcome.SUCCESS
    v.groovylibVerifier.verifyStandardOutputSkipMainArtifact()
    v.groovylibVerifier.verifyJarFile("groovydoc")

    result.task(":kotlinlib:deploy").outcome == TaskOutcome.SUCCESS
    v.kotlinlibVerifier.verifyStandardOutputSkipMainArtifact()

    result.task(":androidlib:deploy").outcome == TaskOutcome.SUCCESS
    v.androidlibVerifier.verifyStandardOutputSkipMainArtifact("aar")

    result.task(":kandroidlib:deploy").outcome == TaskOutcome.SUCCESS
    v.kandroidlibVerifier.verifyStandardOutputSkipMainArtifact("aar")

    where:
    groupId                             | versionName
    "com.multiproject.snapshot.example" | "0.0.1-SNAPSHOT"
    "com.multiproject.release.example"  | "0.0.1"
  }

  def "test multi-project dont publish docs"(String groupId, String versionName) {
    given:
    def v = configureSimpleMultiProject(groupId, versionName)
    testProject.rootGradlePropertiesFile << """

deployable.publication.includeDocs=false
"""

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":javalib:deploy").outcome == TaskOutcome.SUCCESS
    v.javalibVerifier.verifyStandardOutputSkipClassifiedJar("javadoc")

    result.task(":groovylib:deploy").outcome == TaskOutcome.SUCCESS
    v.groovylibVerifier.verifyStandardOutputSkipClassifiedJar("javadoc")
    v.groovylibVerifier.verifyMissingJarFile("groovydoc")

    result.task(":kotlinlib:deploy").outcome == TaskOutcome.SUCCESS
    v.kotlinlibVerifier.verifyStandardOutputSkipClassifiedJar("javadoc")

    result.task(":androidlib:deploy").outcome == TaskOutcome.SUCCESS
    v.androidlibVerifier.verifyStandardOutputSkipClassifiedJar("javadoc", "aar")

    result.task(":kandroidlib:deploy").outcome == TaskOutcome.SUCCESS
    v.kandroidlibVerifier.verifyStandardOutputSkipClassifiedJar("javadoc", "aar")

    where:
    groupId                             | versionName
    "com.multiproject.snapshot.example" | "0.0.1-SNAPSHOT"
    "com.multiproject.release.example"  | "0.0.1"
  }

  def "test multi-project dont publish sources"(String groupId, String versionName) {
    given:
    def v = configureSimpleMultiProject(groupId, versionName)
    testProject.rootGradlePropertiesFile << """

deployable.publication.includeSources=false
"""

    when:
    def result = testProject.executeGradleTask("deploy")

    then:
    result.task(":javalib:deploy").outcome == TaskOutcome.SUCCESS
    v.javalibVerifier.verifyStandardOutputSkipClassifiedJar("sources")

    result.task(":groovylib:deploy").outcome == TaskOutcome.SUCCESS
    v.groovylibVerifier.verifyStandardOutputSkipClassifiedJar("sources")
    v.groovylibVerifier.verifyJarFile("groovydoc")

    result.task(":kotlinlib:deploy").outcome == TaskOutcome.SUCCESS
    v.kotlinlibVerifier.verifyStandardOutputSkipClassifiedJar("sources")

    result.task(":androidlib:deploy").outcome == TaskOutcome.SUCCESS
    v.androidlibVerifier.verifyStandardOutputSkipClassifiedJar("sources", "aar")

    result.task(":kandroidlib:deploy").outcome == TaskOutcome.SUCCESS
    v.kandroidlibVerifier.verifyStandardOutputSkipClassifiedJar("sources", "aar")

    where:
    groupId                             | versionName
    "com.multiproject.snapshot.example" | "0.0.1-SNAPSHOT"
    "com.multiproject.release.example"  | "0.0.1"
  }

  private static String getPackaging(LibType type) {
    switch (type) {
      case LibType.ANDROID:
      case LibType.KANDROID:
        return "aar"
      default:
        return "jar"
    }
  }

  private static String deps(String... dependentProjectNames) {
    return """

dependencies {
${projectDeps(dependentProjectNames)}
}
"""
  }

  private static String projectDeps(String... dependentProjectNames) {
    StringBuilder builder = new StringBuilder()
    dependentProjectNames.each {
      builder.append("  implementation project(\":").append(it).append("\")\n")
    }
    return builder.toString()
  }
}
