package dev.fitiavana.learning_mgmt.features.sync

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.DatagramSocket

class UdpDiscoveryChannelTest {
    private val opened = mutableListOf<UdpDiscoveryChannel>()

    @After
    fun tearDown() = opened.forEach { it.close() }

    private fun freePort() = DatagramSocket(0).use { it.localPort }

    /** Two channels playing two devices on one machine: each sends to the other's port. */
    private fun pair(): Pair<UdpDiscoveryChannel, UdpDiscoveryChannel> {
        val (first, second) = freePort() to freePort()
        return UdpDiscoveryChannel(first, peerPort = second).also(opened::add) to
            UdpDiscoveryChannel(second, peerPort = first).also(opened::add)
    }

    @Test
    fun aDatagramSentToAHostArrivesWithItsSender() = runBlocking {
        val (a, b) = pair()
        val bytes = "hello".toByteArray()

        a.send(bytes, "127.0.0.1")

        val received = withTimeout(5_000) { b.incoming.first() }
        assertArrayEquals(bytes, received.bytes)
        assertEquals("127.0.0.1", received.host)
    }

    @Test
    fun datagramsSentBeforeListeningAreNotLost() = runBlocking {
        val (a, b) = pair()
        a.send("early".toByteArray(), "127.0.0.1")

        assertEquals("early", String(withTimeout(5_000) { b.incoming.first() }.bytes))
    }

    @Test
    fun theOtherDirectionWorksToo() = runBlocking {
        val (a, b) = pair()

        b.send("back".toByteArray(), "127.0.0.1")

        assertEquals("back", String(withTimeout(5_000) { a.incoming.first() }.bytes))
    }

    @Test
    fun closingEndsTheIncomingFlow() = runBlocking {
        val (a, _) = pair()
        a.close()

        assertEquals(emptyList<Datagram>(), withTimeout(5_000) { a.incoming.toList() })
    }

    @Test
    fun sendingAfterClosingOrBroadcastingWithoutANetworkDoesNotThrow() = runBlocking {
        val (a, _) = pair()

        a.broadcast("anyone".toByteArray())
        a.close()
        a.send("late".toByteArray(), "127.0.0.1")
        a.broadcast("late".toByteArray())
    }

    @Test
    fun theSamePortCanBeOpenedByTwoInstancesOfTheApp() {
        val port = freePort()

        val first = UdpDiscoveryChannel(port).also(opened::add)
        val second = UdpDiscoveryChannel(port).also(opened::add)

        assertEquals(first.javaClass, second.javaClass)
    }
}
