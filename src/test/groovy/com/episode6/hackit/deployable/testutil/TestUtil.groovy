package com.episode6.hackit.deployable.testutil

/**
 *
 */
class TestUtil {
  static String mavenScopeForGradleConfig(String gradleConfig) {
    switch (gradleConfig) {
      case "implementation":
      case "mavenOptional":
        return "runtime"
      case "compile":
      case "api":
        return "compile"
      case "mavenProvided":
      case "mavenProvidedOptional":
        return "provided"
      default:
        throw new RuntimeException("Invalid gradleConfig: $gradleConfig")
    }
  }

  static boolean isGradleScopeOptional(String gradleConfig) {
    switch (gradleConfig) {
      case "compile":
      case "api":
      case "implementation":
      case "mavenProvided":
        return false
      case "mavenOptional":
      case "mavenProvidedOptional":
        return true
      default:
        throw new RuntimeException("Invalid gradleConfig: $gradleConfig")
    }
  }
}
