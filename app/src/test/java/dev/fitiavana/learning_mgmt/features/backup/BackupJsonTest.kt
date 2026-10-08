package dev.fitiavana.learning_mgmt.features.backup

import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupJsonTest {
    private val data = BackupData(
        schemaVersion = 2,
        exportedAt = "2026-10-07T10:00:00Z",
        selectedCurriculumId = "c2",
        curricula = listOf(Curriculum("c2", "Spanish", 1), Curriculum("c1", "Math", 2)),
        phases = listOf(
            Phase("p1", "c2", 1, "Basics", "# Intro\n\"quoted\" é"),
            Phase("p2", "c2", 2, "Verbs", ""),
        ),
        phaseStatuses = listOf(PhaseStatus("p1", Status.IN_PROGRESS)),
        topics = listOf(
            Topic("t1", "p1", 1, "Greetings", null, null),
            Topic("t2", "p1", 2, "Reading", 40, "pages"),
        ),
        topicProgress = listOf(
            TopicProgress("t1", Status.COMPLETED, 0),
            TopicProgress("t2", Status.IN_PROGRESS, 12),
        ),
    )

    private fun decodeData(json: String): BackupData =
        (BackupJson.decode(json) as BackupJson.DecodeResult.Success).data

    private fun invalid(json: String): String =
        (BackupJson.decode(json) as BackupJson.DecodeResult.Invalid).message

    @Test
    fun roundTripKeepsEverythingAndOrder() {
        assertEquals(data, decodeData(BackupJson.encode(data)))
    }

    @Test
    fun roundTripWithNoSelectionAndNoData() {
        val empty = BackupData(2, "2026-10-07T10:00:00Z", null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(empty, decodeData(BackupJson.encode(empty)))
    }

    @Test
    fun schemaVersionIsReadWithoutDecodingTheRest() {
        assertEquals(7, BackupJson.schemaVersion("""{"schemaVersion": 7, "somethingNew": []}"""))
        assertEquals(2, BackupJson.schemaVersion(BackupJson.encode(data)))
    }

    @Test
    fun schemaVersionIsNullWhenMissingMalformedOrNotJson() {
        assertEquals(null, BackupJson.schemaVersion("""{"exportedAt": "x"}"""))
        assertEquals(null, BackupJson.schemaVersion("""{"schemaVersion": "2"}"""))
        assertEquals(null, BackupJson.schemaVersion("not json"))
    }

    @Test
    fun notJsonIsInvalid() {
        assertTrue(invalid("not json").isNotBlank())
    }

    @Test
    fun missingSchemaVersionIsInvalid() {
        val json = JSONObject(BackupJson.encode(data)).apply { remove("schemaVersion") }
        assertTrue(invalid(json.toString()).contains("schemaVersion"))
    }

    @Test
    fun missingTableIsInvalid() {
        val json = JSONObject(BackupJson.encode(data)).apply { remove("phases") }
        assertTrue(invalid(json.toString()).contains("phases"))
    }

    @Test
    fun unknownStatusIsInvalid() {
        val json = BackupJson.encode(data).replace("IN_PROGRESS", "SOMETIMES")
        assertTrue(invalid(json).contains("SOMETIMES"))
    }

    @Test
    fun wrongTypeIsInvalidNotCoerced() {
        // org.json would silently coerce 7 to "7" and "12" to 12; the file must be rejected.
        val numberName = JSONObject(BackupJson.encode(data)).apply {
            getJSONArray("curricula").getJSONObject(0).put("name", 7)
        }
        assertTrue(invalid(numberName.toString()).contains("name"))

        val stringDone = JSONObject(BackupJson.encode(data)).apply {
            getJSONArray("topicProgress").getJSONObject(1).put("done", "12")
        }
        assertTrue(invalid(stringDone.toString()).contains("done"))
    }

    @Test
    fun aCurriculumWithoutANumberIsInvalid() {
        val json = JSONObject(BackupJson.encode(data)).apply {
            getJSONArray("curricula").getJSONObject(0).remove("number")
        }
        assertTrue(invalid(json.toString()).contains("number"))
    }

    @Test
    fun missingRowFieldIsInvalid() {
        val json = JSONObject(BackupJson.encode(data)).apply {
            getJSONArray("phases").getJSONObject(0).remove("curriculumId")
        }
        assertTrue(invalid(json.toString()).contains("curriculumId"))
    }
}
