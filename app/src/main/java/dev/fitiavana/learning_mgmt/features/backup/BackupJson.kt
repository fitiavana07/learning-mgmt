package dev.fitiavana.learning_mgmt.features.backup

import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON form of a [BackupData]. Decoding is strict: `org.json` coerces types (7 becomes "7"), so
 * every field is type-checked here and a hand-edited or foreign file is rejected, not guessed at.
 */
object BackupJson {
    sealed interface DecodeResult {
        data class Success(val data: BackupData) : DecodeResult
        data class Invalid(val message: String) : DecodeResult
    }

    private class FormatException(message: String) : Exception(message)

    fun encode(data: BackupData): String = JSONObject().apply {
        put("schemaVersion", data.schemaVersion)
        put("exportedAt", data.exportedAt)
        put("selectedCurriculumId", data.selectedCurriculumId)
        put("curricula", array(data.curricula) { put("id", it.id).put("name", it.name) })
        put("phases", array(data.phases) {
            put("id", it.id)
                .put("curriculumId", it.curriculumId)
                .put("number", it.number)
                .put("name", it.name)
                .put("description", it.description)
        })
        put("phaseStatuses", array(data.phaseStatuses) {
            put("phaseId", it.phaseId).put("status", it.status.name)
        })
        put("topics", array(data.topics) {
            put("id", it.id)
                .put("phaseId", it.phaseId)
                .put("number", it.number)
                .put("name", it.name)
                .put("total", it.total)
                .put("unit", it.unit)
        })
        put("topicProgress", array(data.topicProgress) {
            put("topicId", it.topicId).put("status", it.status.name).put("done", it.done)
        })
    }.toString(2)

    /**
     * Just the schema version, or null if the file has none. A file from another app version may
     * not decode at all, so this is checked first to report a version mismatch, not a format error.
     */
    fun schemaVersion(json: String): Int? = try {
        JSONObject(json).int("schemaVersion")
    } catch (e: FormatException) {
        null
    } catch (e: JSONException) {
        null
    }

    fun decode(json: String): DecodeResult = try {
        DecodeResult.Success(parse(JSONObject(json)))
    } catch (e: FormatException) {
        DecodeResult.Invalid(e.message.orEmpty())
    } catch (e: JSONException) {
        DecodeResult.Invalid("The file is not valid JSON")
    }

    private fun parse(root: JSONObject) = BackupData(
        schemaVersion = root.int("schemaVersion"),
        exportedAt = root.string("exportedAt"),
        selectedCurriculumId = root.optionalString("selectedCurriculumId"),
        curricula = root.rows("curricula") { Curriculum(string("id"), string("name")) },
        phases = root.rows("phases") {
            Phase(string("id"), string("curriculumId"), int("number"), string("name"), string("description"))
        },
        phaseStatuses = root.rows("phaseStatuses") { PhaseStatus(string("phaseId"), status()) },
        topics = root.rows("topics") {
            Topic(
                string("id"), string("phaseId"), int("number"), string("name"),
                optionalInt("total"), optionalString("unit"),
            )
        },
        topicProgress = root.rows("topicProgress") { TopicProgress(string("topicId"), status(), int("done")) },
    )

    private fun <T> array(items: List<T>, row: JSONObject.(T) -> JSONObject) =
        JSONArray().apply { items.forEach { put(JSONObject().row(it)) } }

    private fun <T> JSONObject.rows(key: String, row: JSONObject.() -> T): List<T> {
        val array = value(key) as? JSONArray ?: throw FormatException("'$key' must be a list")
        return List(array.length()) { i ->
            val item = array.opt(i) as? JSONObject ?: throw FormatException("'$key' has a row that is not an object")
            item.row()
        }
    }

    private fun JSONObject.value(key: String): Any =
        if (has(key)) get(key) else throw FormatException("Missing '$key'")

    private fun JSONObject.string(key: String): String =
        value(key) as? String ?: throw FormatException("'$key' must be a string")

    private fun JSONObject.int(key: String): Int =
        value(key) as? Int ?: throw FormatException("'$key' must be a whole number")

    private fun JSONObject.optionalString(key: String): String? =
        if (isNull(key)) null else string(key)

    private fun JSONObject.optionalInt(key: String): Int? =
        if (isNull(key)) null else int(key)

    private fun JSONObject.status(): Status {
        val name = string("status")
        return Status.entries.firstOrNull { it.name == name }
            ?: throw FormatException("Unknown status '$name'")
    }
}
