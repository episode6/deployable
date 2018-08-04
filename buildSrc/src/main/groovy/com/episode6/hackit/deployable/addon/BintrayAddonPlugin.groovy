package com.episode6.hackit.deployable.addon

import org.gradle.api.Incubating
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * EXPERIMENTAL
 * Plugin that applies the bintray plugin for you and handles some of the default configuration
 * for data that is already present in deployable's config
 *
 * Referenced as 'com.episode6.hackit.deployable.addon.bintray'
 */
@Incubating
class BintrayAddonPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.plugins.apply('com.jfrog.bintray')
  }

}
