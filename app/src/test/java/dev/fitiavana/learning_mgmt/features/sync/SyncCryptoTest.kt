package dev.fitiavana.learning_mgmt.features.sync

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCryptoTest {
    private val crypto = SyncCrypto("correct horse")
    private val message = "progress 12/40 é ✓".toByteArray()

    @Test
    fun whatIsEncryptedDecryptsBackWithTheSamePassphrase() {
        assertArrayEquals(message, crypto.decrypt(crypto.encrypt(message)))
        assertArrayEquals(message, SyncCrypto("correct horse").decrypt(crypto.encrypt(message)))
    }

    @Test
    fun emptyAndLargeMessagesRoundTrip() {
        val large = ByteArray(2_000_000) { (it % 251).toByte() }

        assertArrayEquals(ByteArray(0), crypto.decrypt(crypto.encrypt(ByteArray(0))))
        assertArrayEquals(large, crypto.decrypt(crypto.encrypt(large)))
    }

    @Test
    fun theCiphertextHidesTheMessageAndDiffersEveryTime() {
        val first = crypto.encrypt(message)
        val second = crypto.encrypt(message)

        assertNotEquals(first.toList(), second.toList())
        assertFalse(String(first).contains("progress"))
    }

    @Test
    fun aDifferentPassphraseCannotDecrypt() {
        val encrypted = crypto.encrypt(message)

        assertThrows(SyncCrypto.AuthenticationException::class.java) { SyncCrypto("wrong").decrypt(encrypted) }
    }

    @Test
    fun aTamperedMessageIsRejected() {
        val encrypted = crypto.encrypt(message)
        encrypted[encrypted.size - 1] = (encrypted.last() + 1).toByte()

        assertThrows(SyncCrypto.AuthenticationException::class.java) { crypto.decrypt(encrypted) }
    }

    @Test
    fun aTruncatedMessageIsRejected() {
        assertThrows(SyncCrypto.AuthenticationException::class.java) { crypto.decrypt(ByteArray(5)) }
        assertThrows(SyncCrypto.AuthenticationException::class.java) { crypto.decrypt(ByteArray(0)) }
    }

    @Test
    fun theGroupTagIsTheSameForTheSamePassphraseAndDifferentOtherwise() {
        assertEquals(crypto.groupTag, SyncCrypto("correct horse").groupTag)
        assertNotEquals(crypto.groupTag, SyncCrypto("correct horse ").groupTag)
        assertTrue(crypto.groupTag.matches(Regex("[0-9a-f]{16}")))
    }

    @Test
    fun theGroupTagDoesNotGiveTheKeyAway() {
        // Another encryption key is derived from the same passphrase, the tag only identifies the group.
        assertFalse(crypto.groupTag.contains("correct"))
        assertThrows(SyncCrypto.AuthenticationException::class.java) {
            SyncCrypto(crypto.groupTag).decrypt(crypto.encrypt(message))
        }
    }

    @Test
    fun aBlankPassphraseIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { SyncCrypto("   ") }
        assertThrows(IllegalArgumentException::class.java) { SyncCrypto("") }
    }
}
