package com.episode6.hackit.deployable.testutil

/**
 *
 */
class MavenOutputVerifier {
  String groupId
  String artifactId
  String versionName

  MavenOutputVerifier() {
    TestingCategories.initIfNeeded()
  }

  boolean isRelease() {
    return versionName.contains("SNAPSHOT")
  }

  def verifyAll(File mavenRepo) {
    verifyRootMavenMetaData(mavenRepo)
    verifyVersionSpecificMavenMetaData(mavenRepo)
    return true
  }

  def verifyRootMavenMetaData(File mavenRepoDir) {
    File conatinerFolder = mavenRepoDir.newFolderFromPackage("${groupId}.${artifactId}")
    def mavenMetaData = new XmlSlurper().parse(conatinerFolder.newFile("maven-metadata.xml"))

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.versioning.versions.size() == 1
    assert mavenMetaData.versioning.versions.version.text() == versionName
    assert mavenMetaData.versioning.lastUpdated != null
  }

  def verifyVersionSpecificMavenMetaData(File mavenRepoDir) {
    File conatinerFolder = mavenRepoDir.newFolderFromPackage("${groupId}.${artifactId}").newFolder(versionName)
    def mavenMetaData = new XmlSlurper().parse(conatinerFolder.newFile("maven-metadata.xml"))

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.version.text() == versionName
    assert mavenMetaData.versioning.snapshot.size() == 1
    assert mavenMetaData.versioning.snapshot.timestamp != null
    assert mavenMetaData.versioning.snapshot.buildNumber.text() == "1"
    assert mavenMetaData.versioning.lastUpdated != null
  }
}
