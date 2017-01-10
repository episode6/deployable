package com.episode6.hackit.deployable.util.keyring

import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing

/**
 * An object containing details about PGP Keyrings.
 */
class KeyRingBundle {
  File keyringDirectory

  String uid
  String password

  PGPKeyRingGenerator keyRingGenerator
  long masterKeyId
  String masterKeyIdHex

  PGPPublicKeyRing publicKeyRing
  PGPSecretKeyRing secretKeyRing

  File publicKeyringFile
  File secretKeyringFile
}
