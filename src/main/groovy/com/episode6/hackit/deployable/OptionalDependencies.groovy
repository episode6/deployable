/**
 * Code for optional dependencies copied from nebula-plugins/gradle-extra-configurations-plugin
 * https://github.com/nebula-plugins/gradle-extra-configurations-plugin/blob/d0df4526508350cb6b073c533e99bd860d4f3987/src/main/groovy/nebula/plugin/extraconfigurations/OptionalBasePlugin.groovy
 * Code has been adapted to fit this plugin, and only the applicable bits have been copied.
 * Original license left intact below.
 */
/*
 * Copyright 2014-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.episode6.hackit.deployable

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 * Prepare a project to accept optional dependencies.
 */
class OptionalDependencies {

  /**
   * Sets up the optionalDeps array and optional method to add to it
   * This is called at the start of Plugin.apply()
   * @param project The project to prepare.
   */
  static void prepareProjectForOptionals(Project project) {
    project.ext.optionalDeps = []
    project.ext.optional = { Dependency dep ->
      project.ext.optionalDeps << dep
      // exclude optional dependency when resolving dependencies between projects
      project.configurations.default.exclude(group: dep.group, module: dep.name)
      return dep
    }
  }

  /**
   * Applies the optional flag (in maven) to deps that are in the optionalDeps list
   * This is called in a project.afterEvaluate block
   * @param project
   */
  static void applyOptionals(Project project) {
    project.tasks.uploadArchives.repositories*.activePomFilters.flatten()*.pomTemplate*.whenConfigured { pom ->
      project.ext.optionalDeps.each { optionalDep ->
        pom.dependencies.find {
          dep -> dep.groupId == optionalDep.group && dep.artifactId == optionalDep.name
        }.optional = true
      }
    }
  }


  static void assertNoApiOptionals(Project project) {
    def apiConf = project.configurations.findByName("api")
    if (apiConf != null) {
      def optionalApiDeps = apiConf.dependencies.findAll {project.ext.optionalDeps.contains(it)}
      if (!optionalApiDeps.isEmpty()) {
        throw apiOptionalException(optionalApiDeps)
      }
    }
  }

  private static GradleException apiOptionalException(Set<Dependency> deps) {
    String depList = deps.collect {"$it.group:$it.name:$it.version"}.join(", ")
    return new GradleException("api dependencies are not allowed to be optional ($depList)")
  }
}
