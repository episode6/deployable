package com.episode6.hackit.deployable.testutil

import groovy.util.slurpersupport.GPathResult

/**
 * Testing Categories
 */
class TestingExtension {

  static File newFile(final File self, String... paths) {
    if (paths.length < 1) {
      throw new IllegalArgumentException("can't create file with empty path")
    }

    String fileName = paths[paths.length-1]
    File transDir = self
    for (int i = 0; i < paths.length-1; i++) {
      transDir = new File(transDir, paths[i])
      transDir.mkdirs()
    }
    return new File(transDir, fileName)
  }

  static File newFolder(final File self, String... paths) {
    File transDir = self
    paths.each { folderName ->
      transDir = new File(transDir, folderName)
      transDir.mkdirs()
    }
    return transDir
  }

  static File newFolderFromPackage(final File self, String packageName) {
    return newFolder(self, (String[])packageName.tokenize('.').toArray())
  }

  static GPathResult asXml(final File self) {
    return new XmlSlurper().parse(self)
  }
}
