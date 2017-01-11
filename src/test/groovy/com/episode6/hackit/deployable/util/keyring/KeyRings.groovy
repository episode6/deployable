package com.episode6.hackit.deployable.util.keyring

import groovy.transform.Memoized
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.bcpg.sig.Features
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
import org.bouncycastle.openpgp.PGPEncryptedData
import org.bouncycastle.openpgp.PGPKeyPair
import org.bouncycastle.openpgp.PGPKeyRing
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

import java.security.SecureRandom

/**
 * Utility class to generate maven-compatible keyrings for testing
 */
class KeyRings {

  @Memoized
  static KeyRingBundle sharedBundle() {
    File folder = new File("build/tmp/shared-testing-keyring")
    if (folder.exists()) {
      folder.deleteDir()
    }
    folder.mkdirs()
    return generateKeyRingsForMaven(folder, "test@example.com", "fakePassword")
  }

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

  static KeyRingBundle generateKeyRingsForMaven(
      File keyringDirectory,
      String uid,
      String password) {

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

    long masterKeyId = masterKeyPair.keyID
    String masterKeyIdHex = Integer.toUnsignedString((int)(masterKeyId - ((masterKeyId >> 32) << 32)), 16)

    keyringDirectory.mkdirs()
    PGPPublicKeyRing publicKeyRing = keyRingGenerator.generatePublicKeyRing()
    File publicKeyringFile = createKeyringFile(
        keyringDirectory,
        publicKeyRing,
        "pubring.gpg")
    PGPSecretKeyRing secretKeyRing = keyRingGenerator.generateSecretKeyRing()
    File secretKeyringFile = createKeyringFile(
        keyringDirectory,
        secretKeyRing,
        "secring.gpg")

    return new KeyRingBundle(
        keyringDirectory: keyringDirectory,
        uid: uid,
        password: password,
        keyRingGenerator: keyRingGenerator,
        masterKeyId: masterKeyId,
        masterKeyIdHex: masterKeyIdHex.toUpperCase(),
        publicKeyRing: publicKeyRing,
        secretKeyRing: secretKeyRing,
        publicKeyringFile: publicKeyringFile,
        secretKeyringFile: secretKeyringFile)
  }

  private static File createKeyringFile(File keyringDir, PGPKeyRing keyRing, String fileName) {
    File keyringFile = new File(keyringDir, fileName)
    BufferedOutputStream out = keyringFile.newOutputStream()
    keyRing.encode(out)
    out.close()
    return keyringFile
  }
}
