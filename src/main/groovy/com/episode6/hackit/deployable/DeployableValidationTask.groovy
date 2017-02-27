package com.episode6.hackit.deployable

import org.gradle.api.DefaultTask
import org.gradle.api.Nullable
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask

/**
 * task that validates deployable project properties
 */
class DeployableValidationTask extends DefaultTask implements VerificationTask {

  @Input boolean ignoreFailures = false

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

  private fail(Throwable failure) {
    if (ignoreFailures) {
      println failure.message
    } else {
      throw failure
    }
  }

  private static boolean isPropertyValid(@Nullable String prop) {
    prop = prop?.trim()?.toLowerCase()
    return prop && prop != "unspecified"
  }
}
