package com.episode6.hackit.deployable.util

import com.episode6.hackit.deployable.util.keyring.KeyRingBundle

/**
 * Generates the contents of grandle.properties for tests
 */
class RequiredPropertiesGenerator {

  static String generateGradleProperties(File mavenRepoDir, KeyRingBundle keyRingInfo) {
    return """
deployable.pom.description=Test Description
deployable.pom.url=https://example.com
deployable.pom.scm.url=extensible
deployable.pom.scm.connection=scm:https://scm_connection.com
deployable.pom.scm.developerConnection=scm:https://scm_dev_connection.com
deployable.pom.licence.name=The MIT License (MIT)
deployable.pom.licence.url=https://licence.com
deployable.pom.licence.distribution=repo
deployable.pom.developer.id=DeveloperId
deployable.pom.developer.name=DeveloperName

deployable.nexus.username=nexusUsername
deployable.nexus.password=nexusPassword

deployable.nexus.releaseRepoUrl=file://localhost${mavenRepoDir.absolutePath}
deployable.nexus.snapshotRepoUrl=file://localhost${mavenRepoDir.absolutePath}

signing.keyId=${keyRingInfo.masterKeyIdHex}
signing.password=${keyRingInfo.password}
signing.secretKeyRingFile=${keyRingInfo.secretKeyringFile.absolutePath}
"""
  }
}
