package com.episode6.hackit.deployable.testutil

/**
 *
 */
class TestUtil {
  public static String mavenScopeForGradleConfig(String gradleConfig) {
    switch (gradleConfig) {
      case "compile":
      case "api":
      case "implementation":
        return "compile"
      case "provided":
      case "compileOnly":
        return "provided"
      default:
        throw new RuntimeException("Invalid gradleConfig: $gradleConfig")
    }
  }
}
