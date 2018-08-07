package com.episode6.hackit.deployable.addon

import org.gradle.api.Incubating
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * EXPERIMENTAL
 * Plugin that applies the bintray plugin for you and handles configuring deployable's publication
 * and the artifact's version name. Everything else should be configured manually via the bintray
 * plugin for now, while this incubates
 *
 * Referenced as 'com.episode6.hackit.deployable.addon.bintray'
 */
@Incubating
class BintrayAddonPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.plugins.apply('com.jfrog.bintray')

    project.bintray {
      pkg {
        version {
          name = project.version
        }
      }
    }

    project.afterEvaluate {
      project.bintray {
        publications = ['mavenArtifacts']
      }
    }

    project.tasks.deploy.dependsOn project.tasks.bintrayUpload
  }

}
