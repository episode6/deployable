package com.episode6.hackit.deployable

import org.gradle.api.GradleException

/**
 * thrown when validateDeployable task fails
 */
class DeployableValidationException extends GradleException {

  DeployableValidationException(List<String> missingProperties) {
    super("deployable validation failure - missing required properties: ${missingProperties}")
  }
}
