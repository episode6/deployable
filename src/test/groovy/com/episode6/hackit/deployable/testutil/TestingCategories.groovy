package com.episode6.hackit.deployable.testutil

/**
 * Categories/Mix-ins for testing
 */
class TestingCategories {

  private static boolean isInitialized = false

  static synchronized initIfNeeded() {
    if (isInitialized) {
      return
    }
    isInitialized = true

    File.metaClass.newFile = { String... paths ->
      if (paths.length < 1) {
        throw new IllegalArgumentException("can't create file with empty path")
      }

      String fileName = paths[paths.length-1]
      File transDir = delegate
      for (int i = 0; i < paths.length-1; i++) {
        transDir = new File(transDir, paths[i])
        transDir.mkdirs()
      }
      return new File(transDir, fileName)
    }

    File.metaClass.newFolder = { String... paths ->
      File transDir = delegate
      paths.each { folderName ->
        transDir = new File(transDir, folderName)
        transDir.mkdirs()
      }
      return transDir
    }

    File.metaClass.newFolderFromPackage = { String packageName ->
      return delegate.newFolder((String[])packageName.tokenize('.').toArray())
    }
  }
}
