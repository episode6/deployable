package com.episode6.hackit.deployable

import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing

/**
 * An object containing details about PGP Keyrings.
 */
class KeyRingInfo {
  String uid
  String password

  PGPKeyRingGenerator keyRingGenerator
  long masterKeyId
  String masterKeyIdHex

  PGPPublicKeyRing publicKeyRing
  PGPSecretKeyRing secretKeyRing

  File publicKeyringFile
  File secretKeyringFile


  KeyRingInfo(
      String uid,
      String password,
      PGPKeyRingGenerator keyRingGenerator,
      long masterKeyId,
      String masterKeyIdHex,
      PGPPublicKeyRing publicKeyRing,
      PGPSecretKeyRing secretKeyRing,
      File publicKeyringFile,
      File secretKeyringFile) {
    this.uid = uid
    this.password = password
    this.keyRingGenerator = keyRingGenerator
    this.masterKeyId = masterKeyId
    this.masterKeyIdHex = masterKeyIdHex
    this.publicKeyRing = publicKeyRing
    this.secretKeyRing = secretKeyRing
    this.publicKeyringFile = publicKeyringFile
    this.secretKeyringFile = secretKeyringFile
  }
}
