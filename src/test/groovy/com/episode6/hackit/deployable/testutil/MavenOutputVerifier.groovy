package com.episode6.hackit.deployable.testutil

import com.episode6.hackit.deployable.extension.DeployablePluginExtension

/**
 *
 */
class MavenOutputVerifier {
  String groupId
  String artifactId
  String versionName

  File snapshotRepo
  File releaseRepo

  MavenOutputVerifier() {
    TestingCategories.initIfNeeded()
  }

  boolean isRelease() {
    return !versionName.contains("SNAPSHOT")
  }

  File getRepo() {
    return isRelease() ? releaseRepo : snapshotRepo
  }

  File getMavenProjectDir() {
    return getRepo().newFolderFromPackage(groupId).newFolder(artifactId)
  }

  File getPom() {
    def versionSpecificMavenMetaData = getMavenProjectDir().newFile(versionName, "maven-metadata.xml").asXml()
    String snapshotTimestamp = versionSpecificMavenMetaData.versioning.snapshot.timestamp.text()
    String snapshotBuildNumber = versionSpecificMavenMetaData.versioning.snapshot.buildNumber.text()
    return getMavenProjectDir().newFile(versionName, "${artifactId}-${versionName.replace("-SNAPSHOT", "")}-${snapshotTimestamp}-${snapshotBuildNumber}.pom")
  }

  def verifyAll(DeployablePluginExtension.PomExtension expectedPom) {
    verifyRootMavenMetaData()
    verifyVersionSpecificMavenMetaData()
    verifyPomData(expectedPom)
    return true
  }

  def verifyRootMavenMetaData() {
    println getMavenProjectDir().newFile("maven-metadata.xml").text
    def mavenMetaData = getMavenProjectDir().newFile("maven-metadata.xml").asXml()

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.versioning.versions.size() == 1
    assert mavenMetaData.versioning.versions.version.text() == versionName
    assert mavenMetaData.versioning.lastUpdated != null
  }

  def verifyVersionSpecificMavenMetaData() {
    println getMavenProjectDir().newFile(versionName, "maven-metadata.xml").text
    def mavenMetaData = getMavenProjectDir().newFile(versionName, "maven-metadata.xml").asXml()

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.version.text() == versionName
    assert mavenMetaData.versioning.snapshot.size() == 1
    assert mavenMetaData.versioning.snapshot.timestamp != null
    assert mavenMetaData.versioning.snapshot.buildNumber.text() == "1"
    assert mavenMetaData.versioning.lastUpdated != null
  }

  def verifyPomData(DeployablePluginExtension.PomExtension expectedPom) {
    println getPom().text
    def pom = getPom().asXml()

    assert pom.name() == "project"
    assert pom.modelVersion.text() == "4.0.0"
    assert pom.groupId.text() == groupId
    assert pom.artifactId.text() == artifactId
    assert pom.version.text() == versionName
    assert pom.name.text() == artifactId
    assert pom.description.text() == expectedPom.description
    assert pom.url.text() == expectedPom.url
    assert pom.licenses.size() == 1
    assert pom.licenses.license.name.text() == expectedPom.license.name
    assert pom.licenses.license.url.text() == expectedPom.license.url
    assert pom.licenses.license.distribution.text() == expectedPom.license.distribution
    assert pom.developers.size() == 1
    assert pom.developers.developer.id.text() == expectedPom.developer.id
    assert pom.developers.developer.name.text() == expectedPom.developer.name
    assert pom.scm.connection.text() == expectedPom.scm.connection
    assert pom.scm.developerConnection.text() == expectedPom.scm.developerConnection
    assert pom.scm.url.text() == expectedPom.scm.url
  }

}
