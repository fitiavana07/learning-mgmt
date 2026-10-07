package dev.fitiavana.learning_mgmt.features.backup

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class BackupFileNameTest {
    @Test
    fun namesTheFileWithTheLocalDateAndTime() {
        val name = backupFileName(LocalDateTime.of(2026, 10, 6, 18, 30, 0))

        assertEquals("learning-mgmt-backup_2026-10-06_183000.json", name)
    }

    @Test
    fun padsSingleDigitFields() {
        val name = backupFileName(LocalDateTime.of(2026, 1, 2, 3, 4, 5))

        assertEquals("learning-mgmt-backup_2026-01-02_030405.json", name)
    }
}
