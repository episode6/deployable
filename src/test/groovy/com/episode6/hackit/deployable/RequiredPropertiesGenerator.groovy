package com.episode6.hackit.deployable

import com.episode6.hackit.deployable.keyring.KeyRingInfo

/**
 * Generates the contents of grandle.properties for tests
 */
class RequiredPropertiesGenerator {

  static String generateGradleProperties(File mavenRepoDir, KeyRingInfo keyRingInfo) {
    return """
POM_DESCRIPTION=Test Description
POM_URL=https://example.com
POM_SCM_URL=extensible
POM_SCM_CONNECTION=scm:https://scm_connection.com
POM_SCM_DEV_CONNECTION=scm:https://scm_dev_connection.com
POM_LICENCE_NAME=The MIT License (MIT)
POM_LICENCE_URL=https://licence.com
POM_LICENCE_DIST=repo
POM_DEVELOPER_ID=DeveloperId
POM_DEVELOPER_NAME=DeveloperName

NEXUS_USERNAME=nexusUsername
NEXUS_PASSWORD=nexusPassword

NEXUS_RELEASE_REPOSITORY_URL=file://localhost${mavenRepoDir.absolutePath}
NEXUS_SNAPSHOT_REPOSITORY_URL=file://localhost${mavenRepoDir.absolutePath}

signing.keyId=${keyRingInfo.masterKeyIdHex}
signing.password=${keyRingInfo.password}
signing.secretKeyRingFile=${keyRingInfo.secretKeyringFile.absolutePath}
"""
  }
}
