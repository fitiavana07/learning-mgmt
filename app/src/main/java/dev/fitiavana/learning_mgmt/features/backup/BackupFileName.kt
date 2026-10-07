package dev.fitiavana.learning_mgmt.features.backup

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")

/** The suggested file name in the save picker, e.g. `learning-mgmt-backup_2026-10-06_183000.json`. */
fun backupFileName(now: LocalDateTime): String = "learning-mgmt-backup_${now.format(FILE_NAME_TIME)}.json"
