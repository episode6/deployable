package com.episode6.hackit.deployable

/**
 * thrown when validateDeployable task fails
 */
class DeployableValidationException extends RuntimeException {

  DeployableValidationException(List<String> missingProperties) {
    super("deployable validation failure - missing required properties: ${missingProperties}")
  }
}
