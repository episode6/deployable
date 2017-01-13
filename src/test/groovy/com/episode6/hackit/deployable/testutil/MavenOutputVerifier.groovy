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

  File getRepo() {
    return isRelease() ? releaseRepo : snapshotRepo
  }

  File getMavenProjectDir() {
    return getRepo().newFolderFromPackage(groupId).newFolder(artifactId)
  }

  def verifyAll() {
    verifyRootMavenMetaData()
    verifyVersionSpecificMavenMetaData()
    return true
  }

  def verifyRootMavenMetaData() {
    def mavenMetaData = new XmlSlurper().parse(getMavenProjectDir().newFile("maven-metadata.xml"))

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.versioning.versions.size() == 1
    assert mavenMetaData.versioning.versions.version.text() == versionName
    assert mavenMetaData.versioning.lastUpdated != null
  }

  def verifyVersionSpecificMavenMetaData() {
    def mavenMetaData = new XmlSlurper().parse(getMavenProjectDir().newFile(versionName, "maven-metadata.xml"))

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.version.text() == versionName
    assert mavenMetaData.versioning.snapshot.size() == 1
    assert mavenMetaData.versioning.snapshot.timestamp != null
    assert mavenMetaData.versioning.snapshot.buildNumber.text() == "1"
    assert mavenMetaData.versioning.lastUpdated != null
  }
}
