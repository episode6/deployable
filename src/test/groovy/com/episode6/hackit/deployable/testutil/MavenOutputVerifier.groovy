package com.episode6.hackit.deployable.testutil

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

  def verifyAll() {
    verifyRootMavenMetaData()
    verifyVersionSpecificMavenMetaData()
    return true
  }

  def verifyRootMavenMetaData() {
    File conatinerFolder = getRepo().newFolderFromPackage("${groupId}.${artifactId}")
    def mavenMetaData = new XmlSlurper().parse(conatinerFolder.newFile("maven-metadata.xml"))

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.versioning.versions.size() == 1
    assert mavenMetaData.versioning.versions.version.text() == versionName
    assert mavenMetaData.versioning.lastUpdated != null
  }

  def verifyVersionSpecificMavenMetaData() {
    File conatinerFolder = getRepo().newFolderFromPackage("${groupId}.${artifactId}").newFolder(versionName)
    def mavenMetaData = new XmlSlurper().parse(conatinerFolder.newFile("maven-metadata.xml"))

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.version.text() == versionName
    assert mavenMetaData.versioning.snapshot.size() == 1
    assert mavenMetaData.versioning.snapshot.timestamp != null
    assert mavenMetaData.versioning.snapshot.buildNumber.text() == "1"
    assert mavenMetaData.versioning.lastUpdated != null
  }

  private File getRepo() {
    return isRelease() ? releaseRepo : snapshotRepo
  }
}
