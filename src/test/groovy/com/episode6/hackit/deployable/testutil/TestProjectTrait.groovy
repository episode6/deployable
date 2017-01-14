package com.episode6.hackit.deployable.testutil

/**
 *
 */
trait TestProjectTrait {
  File snapshotMavenRepoDir
  File releaseMavenRepoDir

  TestProperties testProperties

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
}
