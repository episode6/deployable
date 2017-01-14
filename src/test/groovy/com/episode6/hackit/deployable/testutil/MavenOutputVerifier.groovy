package com.episode6.hackit.deployable.testutil

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider

/**
 * Verifies maven output
 */
class MavenOutputVerifier {
  String groupId
  String artifactId
  String versionName

  IntegrationTestProject testProject

  boolean isRelease() {
    return !versionName.contains("SNAPSHOT")
  }

  File getRepo() {
    return isRelease() ? testProject.releaseMavenRepoDir : testProject.snapshotMavenRepoDir
  }

  File getMavenProjectDir() {
    return getRepo().newFolderFromPackage(groupId).newFolder(artifactId)
  }

  File getMavenVersionDir() {
    return getMavenProjectDir().newFolder(versionName)
  }

  File getPom() {
    return getMavenVersionDir().newFile(getArtifactFileName("pom"))
  }

  boolean verifyAll() {
    return verifyRootMavenMetaData() &&
        verifyVersionSpecificMavenMetaData() &&
        verifyPomData() &&
        verifySignatureOfFile(getPom())
  }

  boolean verifyRootMavenMetaData() {
    def mavenMetaData = getMavenProjectDir().newFile("maven-metadata.xml").asXml()

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.versioning.versions.size() == 1
    assert mavenMetaData.versioning.versions.version.text() == versionName
    assert mavenMetaData.versioning.lastUpdated != null

    if (isRelease()) {
      assert mavenMetaData.versioning.release.text() == versionName
    }
    return true
  }

  boolean verifyVersionSpecificMavenMetaData() {
    if (isRelease()) {
      // this file is only generated for snaphots, skipping
      return true
    }
    def mavenMetaData = getMavenVersionDir().newFile("maven-metadata.xml").asXml()

    assert mavenMetaData.groupId.text() == groupId
    assert mavenMetaData.artifactId.text() == artifactId
    assert mavenMetaData.version.text() == versionName
    assert mavenMetaData.versioning.snapshot.size() == 1
    assert mavenMetaData.versioning.snapshot.timestamp != null
    assert mavenMetaData.versioning.snapshot.buildNumber.text() == "1"
    assert mavenMetaData.versioning.lastUpdated != null
    return true
  }

  boolean verifyPomData() {
    DeployablePluginExtension.PomExtension expectedPom = testProject.testProperties.deployable.pom
    def pom = getPom().asXml()

    assert pom.name() == "project"
    assert pom.modelVersion.text() == "4.0.0"
    assert pom.groupId.text() == groupId
    assert pom.artifactId.text() == artifactId
    assert pom.version.text() == versionName
    assert pom.name.text() == artifactId
    assert pom.description.text() == expectedPom.description
    assert pom.url.text() == expectedPom.url
    assert pom.licenses.size() == 1
    assert pom.licenses.license.name.text() == expectedPom.license.name
    assert pom.licenses.license.url.text() == expectedPom.license.url
    assert pom.licenses.license.distribution.text() == expectedPom.license.distribution
    assert pom.developers.size() == 1
    assert pom.developers.developer.id.text() == expectedPom.developer.id
    assert pom.developers.developer.name.text() == expectedPom.developer.name
    assert pom.scm.connection.text() == expectedPom.scm.connection
    assert pom.scm.developerConnection.text() == expectedPom.scm.developerConnection
    assert pom.scm.url.text() == expectedPom.scm.url
    return true
  }

  boolean verifySignatureOfFile(File file) {
    File signatureFile = new File(file.absolutePath + ".asc")

    InputStream signatureInputStream = PGPUtil.getDecoderStream(signatureFile.newInputStream())
    InputStream literalDataInputStreamStream = file.newInputStream()

    PGPPublicKey publicKey = testProject.testProperties.keyrings.publicKeyRing.publicKey
    PGPObjectFactory objectFactory = new PGPObjectFactory(signatureInputStream, new BcKeyFingerprintCalculator())
    PGPSignature signature = objectFactory.nextObject().get(0)

    signature.init(new BcPGPContentVerifierBuilderProvider(), publicKey)
    int ch
    while ((ch = literalDataInputStreamStream.read()) >= 0) {
      signature.update((byte)ch)
    }

    literalDataInputStreamStream.close()
    signatureInputStream.close()

    boolean result = signature.verify()
    assert result
    return result
  }

  private String getArtifactFileName(String extension) {
    if (isRelease()) {
      return "${artifactId}-${versionName}.${extension}"
    }

    def versionSpecificMavenMetaData = getMavenVersionDir().newFile("maven-metadata.xml").asXml()
    String snapshotTimestamp = versionSpecificMavenMetaData.versioning.snapshot.timestamp.text()
    String snapshotBuildNumber = versionSpecificMavenMetaData.versioning.snapshot.buildNumber.text()
    return "${artifactId}-${versionName.replace("-SNAPSHOT", "")}-${snapshotTimestamp}-${snapshotBuildNumber}.${extension}"
  }
}
