package dev.fitiavana.learning_mgmt.features.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DiscoveryTest {
    private class FakeChannel : DiscoveryChannel {
        val received = MutableSharedFlow<Datagram>(extraBufferCapacity = 64)
        val broadcasts = mutableListOf<ByteArray>()
        val sent = mutableListOf<Pair<ByteArray, String>>()
        var closed = false

        override val incoming: Flow<Datagram> = received

        override suspend fun broadcast(bytes: ByteArray) {
            broadcasts += bytes
        }

        override suspend fun send(bytes: ByteArray, host: String) {
            sent += bytes to host
        }

        override fun close() {
            closed = true
        }
    }

    private val self = PeerInfo("self", "Brave Otter")
    private val other = PeerInfo("other", "Agile Ant")
    private val channel = FakeChannel()

    private fun TestScope.registry() = PeerRegistry("self", "grp", 15_000) { currentTime }

    private fun TestScope.discovery(registry: PeerRegistry) =
        Discovery(backgroundScope, channel, registry, self, port = 4000, group = "grp", announceIntervalMs = 5_000)

    private fun packet(
        info: PeerInfo = other,
        type: DiscoveryPacket.Type = DiscoveryPacket.Type.ANNOUNCE,
        group: String = "grp",
    ) = DiscoveryPacket(type, info, 5000, group)

    private suspend fun TestScope.hear(packet: DiscoveryPacket, host: String = "192.168.1.20") {
        channel.received.emit(Datagram(packet.encode(), host))
        runCurrent()
    }

    @Test
    fun announcesAtOnceThenEveryInterval() = runTest {
        val discovery = discovery(registry())

        discovery.start()
        runCurrent()
        assertEquals(1, channel.broadcasts.size)

        advanceTimeBy(5_001)
        assertEquals(2, channel.broadcasts.size)
        advanceTimeBy(5_000)
        assertEquals(3, channel.broadcasts.size)

        val announced = DiscoveryPacket.decode(channel.broadcasts.first())!!
        assertEquals(DiscoveryPacket.Type.ANNOUNCE, announced.type)
        assertEquals(self, announced.info)
        assertEquals(4000, announced.port)
        assertEquals("grp", announced.group)
        discovery.stop()
    }

    @Test
    fun anAnnouncingPeerAppearsInTheList() = runTest {
        val registry = registry()
        discovery(registry).start()
        runCurrent()

        hear(packet(), host = "192.168.1.20")

        val peer = registry.peers.value.single()
        assertEquals(other, peer.info)
        assertEquals("192.168.1.20", peer.host)
        assertEquals(5000, peer.port)
    }

    @Test
    fun ownAndForeignAndGarbagePacketsAreIgnored() = runTest {
        val registry = registry()
        discovery(registry).start()
        runCurrent()

        hear(packet(info = self))
        hear(packet(group = "someone-elses"))
        channel.received.emit(Datagram("garbage".toByteArray(), "192.168.1.30"))
        runCurrent()

        assertTrue(registry.peers.value.isEmpty())
    }

    @Test
    fun aQueryIsAnsweredDirectlyWithAnAnnouncementAndTheAskerIsListed() = runTest {
        val registry = registry()
        discovery(registry).start()
        runCurrent()

        hear(packet(type = DiscoveryPacket.Type.QUERY), host = "192.168.1.20")

        val (bytes, host) = channel.sent.single()
        assertEquals("192.168.1.20", host)
        assertEquals(DiscoveryPacket.Type.ANNOUNCE, DiscoveryPacket.decode(bytes)!!.type)
        assertEquals(self, DiscoveryPacket.decode(bytes)!!.info)
        assertEquals(listOf(other), registry.peers.value.map { it.info })
    }

    @Test
    fun aQueryFromAnotherGroupOrFromUsGetsNoAnswer() = runTest {
        discovery(registry()).start()
        runCurrent()

        hear(packet(type = DiscoveryPacket.Type.QUERY, group = "someone-elses"))
        hear(packet(info = self, type = DiscoveryPacket.Type.QUERY))

        assertTrue(channel.sent.isEmpty())
    }

    @Test
    fun aPeerThatGoesQuietDisappearsAndOneThatKeepsAnnouncingStays() = runTest {
        val registry = registry()
        discovery(registry).start()
        runCurrent()
        hear(packet(info = other))
        hear(packet(info = PeerInfo("quiet", "Quiet Quail")))

        repeat(4) {
            advanceTimeBy(4_000)
            hear(packet(info = other))
        }

        assertEquals(listOf("other"), registry.peers.value.map { it.info.deviceId })
    }

    @Test
    fun refreshForgetsEveryoneAndAsksWhoIsThere() = runTest {
        val registry = registry()
        val discovery = discovery(registry)
        discovery.start()
        runCurrent()
        hear(packet())

        discovery.refresh()
        runCurrent()

        assertTrue(registry.peers.value.isEmpty())
        val asked = DiscoveryPacket.decode(channel.broadcasts.last())!!
        assertEquals(DiscoveryPacket.Type.QUERY, asked.type)
        assertEquals(self, asked.info)
        assertEquals(4000, asked.port)
    }

    @Test
    fun peersAnswerARefreshAndAreListedAgain() = runTest {
        val registry = registry()
        val discovery = discovery(registry)
        discovery.start()
        runCurrent()
        hear(packet())
        discovery.refresh()
        runCurrent()

        hear(packet())

        assertEquals(listOf(other), registry.peers.value.map { it.info })
    }

    @Test
    fun stoppingEndsTheAnnouncementsAndClosesTheChannel() = runTest {
        val discovery = discovery(registry())
        discovery.start()
        runCurrent()
        val before = channel.broadcasts.size

        discovery.stop()
        advanceTimeBy(30_000)

        assertEquals(before, channel.broadcasts.size)
        assertTrue(channel.closed)
    }

    @Test
    fun stoppingForgetsThePeers() = runTest {
        val registry = registry()
        val discovery = discovery(registry)
        discovery.start()
        runCurrent()
        hear(packet())

        discovery.stop()

        assertFalse(registry.peers.value.isNotEmpty())
    }
}
