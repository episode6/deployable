package com.episode6.hackit.deployable.testutil

import com.episode6.hackit.gdmc.data.DependencyMap
import com.episode6.hackit.gdmc.data.DependencyMapImpl
import com.episode6.hackit.gdmc.data.GdmcDependency
import groovy.transform.Memoized

/**
 * Utility class to lookup mapped dependencies in our own gdmc dependency map.
 */
class MyDependencyMap {
  @Memoized
  static DependencyMap get() {
    File gdmcFile = new File("./gdmc/gdmc.json")
    return new DependencyMapImpl(gdmcFile)
  }

  static String lookupDep(String key) {
    List<GdmcDependency> mapped = get().lookupWithOverrides(key)
    if (!mapped) {
      throw new NullPointerException("Could not find version for key: ${key}")
    }
    if (mapped.size() > 1) {
      throw new NullPointerException("Found multiple entries for key: ${key}, values: ${mapped}")
    }

    return mapped[0].fullMavenKey
  }
}
