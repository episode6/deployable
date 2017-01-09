package com.episode6.hackit.deployable

import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.bcpg.sig.Features
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
import org.bouncycastle.openpgp.PGPEncryptedData
import org.bouncycastle.openpgp.PGPKeyPair
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.PGPDigestCalculator
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.security.SecureRandom

/**
 * Tests DeployablePlugin.groovy
 */
class DeployableJarIntegrationTest extends Specification {

  static int[] SYMMETRIC_KEY_ALOGS = [
      SymmetricKeyAlgorithmTags.AES_256,
      SymmetricKeyAlgorithmTags.AES_192,
      SymmetricKeyAlgorithmTags.AES_128]

  static int[] HASH_ALGOS = [
      HashAlgorithmTags.SHA256,
      HashAlgorithmTags.SHA1,
      HashAlgorithmTags.SHA384,
      HashAlgorithmTags.SHA512,
      HashAlgorithmTags.SHA224]

  static final String SIGNING_PASSWORD = "fakePassword"

  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile
  File gradleProperties
  File publicKeyringFile
  File secretKeyringFile
  long masterKeyId
  String masterKeyIdHex
  File m2Folder

  def setup() {
    buildFile = testProjectDir.newFile("build.gradle")
    gradleProperties = testProjectDir.newFile("gradle.properties")
    secretKeyringFile = testProjectDir.newFile("secring.gpg")
    publicKeyringFile = testProjectDir.newFile("pubring.gpg")
    m2Folder = testProjectDir.newFolder("m2")
    m2Folder.mkdirs()
    println "m2Folder: ${m2Folder.absolutePath}"

    PGPKeyRingGenerator pgpKeyRingGenerator = generateKeyRingGenerator("test@example.com", SIGNING_PASSWORD)

    PGPPublicKeyRing publicKeyRing = pgpKeyRingGenerator.generatePublicKeyRing()
    BufferedOutputStream pubOut = publicKeyringFile.newOutputStream()
    publicKeyRing.encode(pubOut)
    pubOut.close()

    PGPSecretKeyRing secretKeyRing = pgpKeyRingGenerator.generateSecretKeyRing();
    BufferedOutputStream secOut = secretKeyringFile.newOutputStream()
    secretKeyRing.encode(secOut)
    secOut.close()

    long tempLong = ((masterKeyId >> 32) << 32)
    int smallMasterKeyId = (int)(masterKeyId - tempLong)
    masterKeyIdHex = Integer.toUnsignedString(smallMasterKeyId, 16)
  }

  def "look for deploy task"() {
    given:
    gradleProperties << """
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

NEXUS_RELEASE_REPOSITORY_URL=https://release_repo.com
NEXUS_SNAPSHOT_REPOSITORY_URL=file://localhost${m2Folder.absolutePath}

signing.keyId=${masterKeyIdHex}
signing.password=${SIGNING_PASSWORD}
signing.secretKeyRingFile=${secretKeyringFile.absolutePath}
 """

    buildFile << """
plugins {
 id 'java'
 id 'com.episode6.hackit.deployable.jar'
}

group = 'com.example.groupid'
version = '0.0.1-SNAPSHOT'
 """

    when:
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withPluginClasspath()
      .withArguments("deploy")
      .build()

    then:
    result.task(":deploy").outcome == TaskOutcome.SUCCESS
  }

  private PGPKeyRingGenerator generateKeyRingGenerator(String uid, String password) {
    int s2kcount = 0xc0
    RSAKeyPairGenerator rsaKeyPairGenerator = new RSAKeyPairGenerator()
    rsaKeyPairGenerator.init(
        new RSAKeyGenerationParameters(
            BigInteger.valueOf(0x10001),
            new SecureRandom(),
            2048,
            12))

    PGPKeyPair masterKeyPair = new BcPGPKeyPair(
        PGPPublicKey.RSA_SIGN,
        rsaKeyPairGenerator.generateKeyPair(),
        new Date())
    PGPKeyPair subKeyPair = new BcPGPKeyPair(
        PGPPublicKey.RSA_ENCRYPT,
        rsaKeyPairGenerator.generateKeyPair(),
        new Date())

    PGPSignatureSubpacketGenerator sigHashGenerator = new PGPSignatureSubpacketGenerator()
    sigHashGenerator.setKeyFlags(false, KeyFlags.SIGN_DATA|KeyFlags.CERTIFY_OTHER)
    sigHashGenerator.setPreferredSymmetricAlgorithms(false, SYMMETRIC_KEY_ALOGS)
    sigHashGenerator.setPreferredHashAlgorithms(false, HASH_ALGOS)
    sigHashGenerator.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION)

    PGPSignatureSubpacketGenerator encHashGenerator = new PGPSignatureSubpacketGenerator()
    encHashGenerator.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS|KeyFlags.ENCRYPT_STORAGE)

    PGPDigestCalculatorProvider digestCalculatorProvider = new BcPGPDigestCalculatorProvider()
    PGPDigestCalculator sha1Calculator = digestCalculatorProvider.get(HashAlgorithmTags.SHA1)
    PGPDigestCalculator sha256Calculator = digestCalculatorProvider.get(HashAlgorithmTags.SHA256)

    PGPContentSignerBuilder keySignerBuilder = new BcPGPContentSignerBuilder(
        masterKeyPair.getPublicKey().getAlgorithm(),
        HashAlgorithmTags.SHA1)

    PBESecretKeyEncryptor secretKeyEncryptor = (new BcPBESecretKeyEncryptorBuilder(
        PGPEncryptedData.AES_256,
        sha256Calculator,
        s2kcount))
        .build(password.toCharArray())

    PGPKeyRingGenerator keyRingGenerator = new PGPKeyRingGenerator(
        PGPSignature.POSITIVE_CERTIFICATION,
        masterKeyPair,
        uid,
        sha1Calculator,
        sigHashGenerator.generate(),
        null,
        keySignerBuilder,
        secretKeyEncryptor)
    keyRingGenerator.addSubKey(subKeyPair, encHashGenerator.generate(), null)

    masterKeyId = masterKeyPair.keyID
    return keyRingGenerator
  }
}
