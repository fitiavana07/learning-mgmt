package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.features.backup.BackupData
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
class SyncJsonTest {
    private val data = BackupData(
        schemaVersion = 3,
        exportedAt = "",
        selectedCurriculumId = null,
        curricula = listOf(Curriculum("c1", "Kotlin", 1)),
        phases = listOf(Phase("p1", "c1", 1, "Basics", "# Intro é")),
        phaseStatuses = listOf(PhaseStatus("p1", Status.IN_PROGRESS)),
        topics = listOf(Topic("t1", "p1", 1, "Reading", 40, "pages")),
        topicProgress = listOf(TopicProgress("t1", Status.IN_PROGRESS, 12)),
    )

    private val snapshot = SyncSnapshot(
        data = data,
        meta = data.recordKeys().associateWith { Meta(Stamp(1_790_000_000_000, "dev-a"), deleted = false) } +
            (RecordKey(SyncKind.TOPIC, "old") to Meta(Stamp(7, "dev-b"), deleted = true)),
    )

    private fun decoded(json: String) = (SyncJson.decode(json) as SyncJson.DecodeResult.Success).snapshot

    private fun invalid(json: String) = (SyncJson.decode(json) as SyncJson.DecodeResult.Invalid).message

    @Test
    fun roundTripKeepsRowsAndStampsIncludingTimesBeyondTheIntRange() {
        assertEquals(snapshot, decoded(SyncJson.encode(snapshot)))
    }

    @Test
    fun roundTripOfAnEmptySnapshot() {
        val empty = SyncSnapshot(data.copy(curricula = emptyList(), phases = emptyList(), phaseStatuses = emptyList(), topics = emptyList(), topicProgress = emptyList()), emptyMap())

        assertEquals(empty, decoded(SyncJson.encode(empty)))
    }

    @Test
    fun notJsonIsInvalid() {
        assertTrue(invalid("nope").isNotBlank())
    }

    @Test
    fun missingDataOrMetaIsInvalid() {
        val noMeta = JSONObject(SyncJson.encode(snapshot)).apply { remove("meta") }
        val noData = JSONObject(SyncJson.encode(snapshot)).apply { remove("data") }

        assertTrue(invalid(noMeta.toString()).contains("meta"))
        assertTrue(invalid(noData.toString()).contains("data"))
    }

    @Test
    fun anInvalidRowInsideTheDataIsReported() {
        val broken = JSONObject(SyncJson.encode(snapshot)).apply {
            getJSONObject("data").getJSONArray("phases").getJSONObject(0).remove("curriculumId")
        }

        assertTrue(invalid(broken.toString()).contains("curriculumId"))
    }

    @Test
    fun anUnknownKindIsInvalid() {
        val broken = SyncJson.encode(snapshot).replace("\"TOPIC_PROGRESS\"", "\"SOMETHING\"")

        assertTrue(invalid(broken).contains("SOMETHING"))
    }

    @Test
    fun wrongTypesInAStampAreInvalidNotCoerced() {
        val stringTime = JSONObject(SyncJson.encode(snapshot)).apply {
            getJSONArray("meta").getJSONObject(0).put("stampTime", "12")
        }
        val numberDevice = JSONObject(SyncJson.encode(snapshot)).apply {
            getJSONArray("meta").getJSONObject(0).put("stampDevice", 5)
        }
        val stringDeleted = JSONObject(SyncJson.encode(snapshot)).apply {
            getJSONArray("meta").getJSONObject(0).put("deleted", "false")
        }

        assertTrue(invalid(stringTime.toString()).contains("stampTime"))
        assertTrue(invalid(numberDevice.toString()).contains("stampDevice"))
        assertTrue(invalid(stringDeleted.toString()).contains("deleted"))
    }

    @Test
    fun theSameRecordListedTwiceIsInvalid() {
        val duplicated = JSONObject(SyncJson.encode(snapshot)).apply {
            val meta = getJSONArray("meta")
            meta.put(JSONObject(meta.getJSONObject(0).toString()))
        }

        assertTrue(invalid(duplicated.toString()).contains("twice"))
    }
}
