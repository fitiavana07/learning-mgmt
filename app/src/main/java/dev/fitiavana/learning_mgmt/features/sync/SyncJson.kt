package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.features.FormatException
import dev.fitiavana.learning_mgmt.features.array
import dev.fitiavana.learning_mgmt.features.backup.BackupJson
import dev.fitiavana.learning_mgmt.features.boolean
import dev.fitiavana.learning_mgmt.features.long
import dev.fitiavana.learning_mgmt.features.obj
import dev.fitiavana.learning_mgmt.features.rows
import dev.fitiavana.learning_mgmt.features.string
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON form of a [SyncSnapshot]: the rows in the backup format, plus the stamps. Decoding is
 * strict like the backup's, and only checks the shape; consistency is [SyncSnapshot.violation].
 */
object SyncJson {
    sealed interface DecodeResult {
        data class Success(val snapshot: SyncSnapshot) : DecodeResult
        data class Invalid(val message: String) : DecodeResult
    }

    fun encode(snapshot: SyncSnapshot): String = JSONObject().apply {
        put("data", JSONObject(BackupJson.encode(snapshot.data)))
        put("meta", array(snapshot.meta.entries) { (key, meta) ->
            put("kind", key.kind.name)
                .put("id", key.id)
                .put("stampTime", meta.stamp.time)
                .put("stampDevice", meta.stamp.deviceId)
                .put("deleted", meta.deleted)
        })
    }.toString()

    fun decode(json: String): DecodeResult = try {
        DecodeResult.Success(parse(JSONObject(json)))
    } catch (e: FormatException) {
        DecodeResult.Invalid(e.message.orEmpty())
    } catch (e: JSONException) {
        DecodeResult.Invalid("The data is not valid JSON")
    }

    private fun parse(root: JSONObject): SyncSnapshot {
        val data = when (val decoded = BackupJson.decode(root.obj("data").toString())) {
            is BackupJson.DecodeResult.Success -> decoded.data
            is BackupJson.DecodeResult.Invalid -> throw FormatException(decoded.message)
        }
        val entries = root.rows("meta") {
            RecordKey(kind(), string("id")) to Meta(Stamp(long("stampTime"), string("stampDevice")), boolean("deleted"))
        }
        if (entries.map { it.first }.toSet().size != entries.size) {
            throw FormatException("The same record is listed twice in 'meta'")
        }
        return SyncSnapshot(data, entries.toMap())
    }

    private fun JSONObject.kind(): SyncKind {
        val name = string("kind")
        return SyncKind.entries.firstOrNull { it.name == name } ?: throw FormatException("Unknown kind '$name'")
    }
}
