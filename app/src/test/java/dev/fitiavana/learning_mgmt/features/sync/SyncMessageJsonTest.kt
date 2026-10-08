package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.features.backup.BackupData
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncMessageJsonTest {
    private val peer = PeerInfo("dev-a", "Strategic Banana")
    private val snapshot = SyncSnapshot(
        BackupData(3, "", null, listOf(Curriculum("c1", "Kotlin", 1)), emptyList(), emptyList(), emptyList(), emptyList()),
        mapOf(RecordKey(SyncKind.CURRICULUM, "c1") to Meta(Stamp(1_790_000_000_000, "dev-a"), deleted = false)),
    )

    private fun decoded(json: String) = (SyncMessageJson.decode(json) as SyncMessageJson.DecodeResult.Success).message

    @Test
    fun requestsAndResponsesRoundTrip() {
        val request = SyncMessage.Request(peer, snapshot)
        val response = SyncMessage.Response(peer, snapshot)

        assertEquals(request, decoded(SyncMessageJson.encode(request)))
        assertEquals(response, decoded(SyncMessageJson.encode(response)))
    }

    @Test
    fun aFailureRoundTrips() {
        val failure = SyncMessage.Failure("The other device uses database version 4")

        assertEquals(failure, decoded(SyncMessageJson.encode(failure)))
    }

    @Test
    fun theProtocolVersionTravelsInEveryMessage() {
        val json = JSONObject(SyncMessageJson.encode(SyncMessage.Failure("x")))

        assertEquals(PROTOCOL_VERSION, json.getInt("protocol"))
    }

    @Test
    fun anotherProtocolVersionIsReportedBeforeAnythingElseIsRead() {
        val result = SyncMessageJson.decode("""{"protocol": 99, "type": "something new"}""")

        assertEquals(SyncMessageJson.DecodeResult.Incompatible(99), result)
    }

    @Test
    fun malformedMessagesAreInvalid() {
        fun invalid(json: String) = SyncMessageJson.decode(json) is SyncMessageJson.DecodeResult.Invalid

        assertTrue(invalid("nope"))
        assertTrue(invalid("""{"type": "request"}"""))
        assertTrue(invalid("""{"protocol": 1, "type": "unknown"}"""))
        assertTrue(invalid("""{"protocol": 1, "type": "request", "deviceId": "a"}"""))
        assertTrue(invalid("""{"protocol": "1", "type": "request"}"""))
    }

    @Test
    fun aBadSnapshotInsideAMessageIsInvalid() {
        val json = JSONObject(SyncMessageJson.encode(SyncMessage.Request(peer, snapshot))).apply {
            getJSONObject("snapshot").remove("meta")
        }

        val result = SyncMessageJson.decode(json.toString()) as SyncMessageJson.DecodeResult.Invalid
        assertTrue(result.message.contains("meta"))
    }
}
