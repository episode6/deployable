package com.episode6.hackit.deployable.testutil

import com.episode6.hackit.deployable.extension.DeployablePluginExtension
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider

import java.util.jar.JarFile

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

  File getArtifactFile(String extension, String descriptor = null) {
    return getMavenVersionDir().newFile(getArtifactFileName(extension, descriptor))
  }

  boolean verifyAll(String artifactPackaging = "jar") {
    return verifyRootMavenMetaData() &&
        verifyVersionSpecificMavenMetaData() &&
        verifyPomData() &&
        verifyJarFile(null, artifactPackaging) &&
        verifyJarFile("sources") &&
        verifyJarFile("javadoc")
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
    File pomFile = getArtifactFile("pom")
    def pom = pomFile.asXml()

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

    assert verifySignatureOfFile(pomFile)

    return true
  }

  boolean verifyPomDependencies(MavenDependency... dependencies) {
    def pom = getArtifactFile("pom").asXml()
    dependencies.each { md ->
      def foundDep = pom.dependencies.dependency.find{ pd ->
        println "looking at ${pd} - ${pd.groupId.text()}"
        pd.groupId.text() == md.groupId &&
          pd.artifactId.text() == md.artifactId &&
          pd.version.text() == md.version &&
          pd.scope.text() == md.scope
      }
      assert foundDep.groupId.text() == md.groupId
      assert foundDep.artifactId.text() == md.artifactId
      assert foundDep.version.text() == md.version
      assert foundDep.scope.text() == md.scope
      println "found dep: ${foundDep.groupId.text()}:${foundDep.artifactId.text()}:${foundDep.version.text()}"
    }
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

    assert signature.verify()
    return true
  }

  boolean verifyJarFile(String descriptor = null, String extension = "jar") {
    File jarFile = getArtifactFile(extension, descriptor)

    JarFile jar = new JarFile(jarFile, true)
    jar.close()

    assert verifySignatureOfFile(jarFile)
    return true
  }

  private String getArtifactFileName(String extension, String descriptor = null) {
    String endOfFileName = descriptor == null ? ".${extension}" : "-${descriptor}.${extension}"
    if (isRelease()) {
      return "${artifactId}-${versionName}${endOfFileName}"
    }

    def versionSpecificMavenMetaData = getMavenVersionDir().newFile("maven-metadata.xml").asXml()
    String snapshotTimestamp = versionSpecificMavenMetaData.versioning.snapshot.timestamp.text()
    String snapshotBuildNumber = versionSpecificMavenMetaData.versioning.snapshot.buildNumber.text()
    return "${artifactId}-${versionName.replace("-SNAPSHOT", "")}-${snapshotTimestamp}-${snapshotBuildNumber}${endOfFileName}"
  }

  static class MavenDependency {
    String groupId
    String artifactId
    String version
    String scope = "compile"
  }
}
