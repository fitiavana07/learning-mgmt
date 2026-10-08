package dev.fitiavana.learning_mgmt.features.sync

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts and authenticates what two devices exchange, with a key derived from the shared
 * passphrase (PBKDF2, then AES-256-GCM with a fresh random nonce per message). A message that
 * does not decrypt means a wrong passphrase or tampering, and is never used.
 *
 * The derivation is deliberately slow, so create one instance per passphrase and reuse it.
 */
class SyncCrypto(passphrase: String) {
    class AuthenticationException(message: String) : Exception(message)

    private val key: SecretKeySpec

    /** Identifies the passphrase without revealing it, so devices of the same group find each other. */
    val groupTag: String

    init {
        require(passphrase.isNotBlank()) { "The passphrase must not be blank" }
        val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(passphrase.toCharArray(), SALT, ITERATIONS, KEY_BITS)).encoded
        key = SecretKeySpec(derived, "AES")
        val tag = Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(derived, "HmacSHA256"))
            doFinal(GROUP_LABEL)
        }
        groupTag = tag.take(GROUP_TAG_BYTES).joinToString("") { "%02x".format(it) }
    }

    /** The nonce followed by the ciphertext and its authentication tag. */
    fun encrypt(plain: ByteArray): ByteArray {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        return nonce + cipher.doFinal(plain)
    }

    fun decrypt(data: ByteArray): ByteArray {
        if (data.size < NONCE_BYTES + TAG_BITS / 8) throw AuthenticationException("The message is too short")
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, data.copyOfRange(0, NONCE_BYTES)))
            cipher.doFinal(data, NONCE_BYTES, data.size - NONCE_BYTES)
        } catch (e: GeneralSecurityException) {
            throw AuthenticationException("The message could not be authenticated")
        }
    }

    private companion object {
        val SALT = "learning-mgmt-sync-v1".toByteArray()
        val GROUP_LABEL = "learning-mgmt-group".toByteArray()
        val random = SecureRandom()
        const val ITERATIONS = 100_000
        const val KEY_BITS = 256
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
        const val GROUP_TAG_BYTES = 8
    }
}
