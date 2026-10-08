package dev.fitiavana.learning_mgmt.features.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoveryPacketTest {
    private val packet = DiscoveryPacket(
        type = DiscoveryPacket.Type.ANNOUNCE,
        info = PeerInfo("3f2b9c1e-1111", "Strategic Banana"),
        port = 40123,
        group = "0123456789abcdef",
    )

    @Test
    fun aPacketRoundTrips() {
        assertEquals(packet, DiscoveryPacket.decode(packet.encode()))
        val query = packet.copy(type = DiscoveryPacket.Type.QUERY)
        assertEquals(query, DiscoveryPacket.decode(query.encode()))
    }

    @Test
    fun aPacketIsSmallEnoughForOneDatagram() {
        assertTrue(packet.encode().size < 512)
    }

    @Test
    fun packetsThatAreNotOursAreIgnored() {
        assertNull(DiscoveryPacket.decode("hello".toByteArray()))
        assertNull(DiscoveryPacket.decode(ByteArray(0)))
        assertNull(DiscoveryPacket.decode("""{"app": "someone-else", "protocol": 1}""".toByteArray()))
        assertNull(DiscoveryPacket.decode(ByteArray(64) { it.toByte() }))
    }

    @Test
    fun aPacketOfAnotherProtocolVersionIsIgnored() {
        val other = JSONObject(String(packet.encode())).put("protocol", PROTOCOL_VERSION + 1)

        assertNull(DiscoveryPacket.decode(other.toString().toByteArray()))
    }

    @Test
    fun aPacketWithMissingOrWrongFieldsIsIgnored() {
        fun without(key: String) = JSONObject(String(packet.encode())).apply { remove(key) }.toString().toByteArray()

        listOf("type", "deviceId", "name", "port", "group").forEach { assertNull(key(it), DiscoveryPacket.decode(without(it))) }
        val wrongPort = JSONObject(String(packet.encode())).put("port", "40123").toString().toByteArray()
        val badPort = JSONObject(String(packet.encode())).put("port", 70_000).toString().toByteArray()
        val badType = JSONObject(String(packet.encode())).put("type", "shout").toString().toByteArray()
        assertNull(DiscoveryPacket.decode(wrongPort))
        assertNull(DiscoveryPacket.decode(badPort))
        assertNull(DiscoveryPacket.decode(badType))
    }

    private fun key(name: String) = "without $name"
}
