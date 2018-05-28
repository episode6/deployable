package com.episode6.hackit.deployable

import org.gradle.api.Project

/**
 * The android library plugin causes some odd things to happen in the MavenPlugin.
 * Handle them here (so we can share with kotlin-android plugin)
 */
class AndroidHacks {

  static void applyPomImplementationOverride(Project project, DeployablePlugin deployablePlugin) {
    // android libs appear to have a problem changing the mapping for 'implementation'
    // this block lets us overwrite that stickyness
    project.uploadArchives.repositories.mavenDeployer.pom.whenConfigured { pom ->
      String mavenScopeForImplementation = deployablePlugin.mavenConfig
          .getMavenScopeForGradleConfig("implementation")

      project.configurations.implementation.dependencies.each { dep ->
        def pomDep = pom.dependencies.find { pomDep ->
          pomDep.groupId == dep.group && pomDep.artifactId == dep.name && pomDep.scope == "compile"
        }
        if (mavenScopeForImplementation) {
          pomDep.scope = mavenScopeForImplementation
        } else {
          pom.dependencies.remove(pomDep)
        }
      }

    }
  }
}
