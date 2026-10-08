package dev.fitiavana.learning_mgmt.features.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SyncTransportTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val host = "127.0.0.1"

    @After
    fun tearDown() = scope.cancel()

    /** Answers every frame with the same bytes reversed. */
    private fun echoServer() = SyncServer(scope) { input, output ->
        FrameIO.write(output, FrameIO.read(input).reversedArray())
    }

    private suspend fun ask(port: Int, text: String) = SyncClient.connect(host, port) { input, output ->
        FrameIO.write(output, text.toByteArray())
        String(FrameIO.read(input))
    }

    @Test
    fun aClientTalksToTheServerOnTheReportedPort() = runBlocking {
        val server = echoServer()
        val port = server.start()

        assertEquals("olleh", ask(port, "hello"))
        server.stop()
    }

    @Test
    fun severalClientsAreServed() = runBlocking {
        val server = echoServer()
        val port = server.start()

        val answers = (1..5).map { async { ask(port, "msg$it") } }.awaitAll()

        assertEquals((1..5).map { "msg$it".reversed() }, answers)
        server.stop()
    }

    @Test
    fun aHandlerThatFailsDoesNotStopTheServer() = runBlocking {
        var first = true
        val server = SyncServer(scope) { input, output ->
            val frame = FrameIO.read(input)
            if (first) {
                first = false
                throw IOException("boom")
            }
            FrameIO.write(output, frame)
        }
        val port = server.start()

        runCatching { ask(port, "one") }
        assertEquals("two", ask(port, "two"))
        server.stop()
    }

    @Test
    fun aStoppedServerRefusesConnections() {
        val server = echoServer()
        val port = server.start()
        server.stop()

        assertThrows(IOException::class.java) { runBlocking { ask(port, "hello") } }
    }

    @Test
    fun connectionsBeyondTheLimitAreDroppedWhileTheOthersAreServed() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val server = SyncServer(scope) { input, output ->
            FrameIO.read(input)
            release.await()
            FrameIO.write(output, "served".toByteArray())
        }
        val port = server.start()
        val held = (1..SyncServer.MAX_CONNECTIONS).map {
            async { SyncClient.connect(host, port) { input, output -> FrameIO.write(output, byteArrayOf(1)); String(FrameIO.read(input)) } }
        }
        delay(500) // let them all be accepted and parked in the handler

        val extra = runCatching {
            SyncClient.connect(host, port) { input, output -> FrameIO.write(output, byteArrayOf(1)); FrameIO.read(input) }
        }
        release.complete(Unit)

        assertTrue(extra.isFailure)
        assertEquals(List(SyncServer.MAX_CONNECTIONS) { "served" }, held.awaitAll())
        server.stop()
    }

    @Test
    fun stoppingTwiceOrBeforeStartingIsHarmless() {
        val server = echoServer()

        server.stop()
        server.start()
        server.stop()
        server.stop()
    }

    @Test
    fun connectingToNobodyFailsInsteadOfHanging() {
        assertThrows(IOException::class.java) { runBlocking { ask(1, "hello") } }
    }

    @Test
    fun largeMessagesGoThrough() = runBlocking {
        val server = SyncServer(scope) { input, output -> FrameIO.write(output, FrameIO.read(input)) }
        val port = server.start()
        val payload = ByteArray(3_000_000) { (it % 251).toByte() }

        val echoed = SyncClient.connect(host, port) { input, output ->
            FrameIO.write(output, payload)
            FrameIO.read(input)
        }

        assertArrayEquals(payload, echoed)
        server.stop()
    }
}
