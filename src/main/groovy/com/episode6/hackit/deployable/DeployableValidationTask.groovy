package com.episode6.hackit.deployable

import org.gradle.api.DefaultTask
import org.gradle.api.Nullable
import org.gradle.api.tasks.TaskAction

/**
 * task that validates deployable project properties
 */
class DeployableValidationTask extends DefaultTask {

  @TaskAction
  def validate() {
    List<String> missingProps = new LinkedList<>()
    if (!isPropertyValid(project.name)) {
      missingProps.add("Project Property: name")
    }
    if (!isPropertyValid(project.group)) {
      missingProps.add("Project Property: group")
    }
    if (!isPropertyValid(project.version)) {
      missingProps.add("Project Property: version")
    }
    missingProps.addAll(project.deployable.findMissingProperties())
    if (!missingProps.isEmpty()) {
      throw new DeployableValidationException(missingProps)
    }
  }

  private static boolean isPropertyValid(@Nullable String prop) {
    prop = prop?.trim()?.toLowerCase()
    return prop && prop != "unspecified"
  }
}
