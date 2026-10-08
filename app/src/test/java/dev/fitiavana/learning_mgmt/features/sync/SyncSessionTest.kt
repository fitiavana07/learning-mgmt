package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.db.DB_VERSION
import dev.fitiavana.learning_mgmt.db.TestEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/** Whole sessions between two devices over a real loopback connection. */
@RunWith(RobolectricTestRunner::class)
class SyncSessionTest {
    @get:Rule
    val a = TestEnvironment("dev-a", "a-")

    @get:Rule
    val b = TestEnvironment("dev-b", "b-")

    private val peerA = PeerInfo("dev-a", "Strategic Banana")
    private val peerB = PeerInfo("dev-b", "Brave Otter")
    private val crypto = SyncCrypto("correct horse")
    private val loopback = InetAddress.getLoopbackAddress()

    private fun session(e: TestEnvironment, peer: PeerInfo, crypto: SyncCrypto = this.crypto) =
        SyncSession(e.sync, crypto, peer)

    /** Runs the responder on a server socket and the initiator against it; returns (initiator, responder). */
    private suspend fun run(initiator: SyncSession, responder: SyncSession): Pair<SessionResult, SessionResult> =
        coroutineScope {
            ServerSocket(0, 1, loopback).use { server ->
                val answering = async(Dispatchers.IO) {
                    server.accept().use { socket ->
                        socket.soTimeout = TIMEOUT
                        responder.respond(socket.getInputStream(), socket.getOutputStream())
                    }
                }
                val asking = withContext(Dispatchers.IO) {
                    Socket(loopback, server.localPort).use { socket ->
                        socket.soTimeout = TIMEOUT
                        initiator.initiate(socket.getInputStream(), socket.getOutputStream())
                    }
                }
                asking to answering.await()
            }
        }

    private suspend fun seed(e: TestEnvironment): String {
        val curriculum = e.curricula.create("Kotlin")
        val phase = e.phases.add(curriculum, "Basics", "")
        val topic = e.topics.add(phase, "Reading", total = 10, unit = "pages")
        e.progress.start(phase)
        e.progress.startTopic(topic)
        e.progress.recordTopicProgress(topic, 4)
        return topic
    }

    private suspend fun doneOf(e: TestEnvironment, topic: String) =
        e.db.topicProgressDao().getRowsByPhase(e.db.topicDao().get(topic)!!.phaseId).first { it.topic.id == topic }.done

    private suspend fun curriculumNames(e: TestEnvironment) = e.curricula.observeAll().first().map { it.name }

    @Test
    fun aSessionBringsBothDevicesToTheSameState() = runBlocking {
        val topic = seed(a)

        val (initiator, responder) = run(session(a, peerA), session(b, peerB))

        assertEquals(SessionResult.Synced(peerB, changed = false), initiator)
        assertEquals(SessionResult.Synced(peerA, changed = true), responder)
        assertEquals(listOf("Kotlin"), curriculumNames(b))
        assertEquals(4, doneOf(b, topic))
        assertEquals(a.sync.snapshot().meta, b.sync.snapshot().meta)
    }

    @Test
    fun progressTickedOnOneDeviceIsOnTheOtherAfterTheNextSession() = runBlocking {
        val topic = seed(a)
        run(session(a, peerA), session(b, peerB))

        b.now = 5_000
        b.progress.recordTopicProgress(topic, 9)
        val (initiator, _) = run(session(b, peerB), session(a, peerA))

        assertEquals(SessionResult.Synced(peerA, changed = false), initiator)
        assertEquals(9, doneOf(a, topic))
    }

    @Test
    fun theInitiatorLearnsWhatTheResponderAlreadyHad() = runBlocking {
        seed(a)
        b.now = 5_000
        b.curricula.create("Piano")

        val (initiator, _) = run(session(a, peerA), session(b, peerB))

        assertEquals(SessionResult.Synced(peerB, changed = true), initiator)
        assertEquals(setOf("Kotlin", "Piano"), curriculumNames(a).toSet())
        assertEquals(setOf("Kotlin", "Piano"), curriculumNames(b).toSet())
    }

    @Test
    fun aWrongPassphraseSyncsNothingAndTellsBothSides() = runBlocking {
        seed(a)

        val (initiator, responder) = run(session(a, peerA), session(b, peerB, SyncCrypto("other passphrase")))

        assertEquals(SessionResult.Failed(SessionFailure.NoAnswer), initiator)
        assertEquals(SessionResult.Failed(SessionFailure.WrongPassphrase), responder)
        assertEquals(emptyList<String>(), curriculumNames(b))
    }

    @Test
    fun aPeerOnAnotherDatabaseVersionRefusesWithAReason() = runBlocking {
        seed(a)
        val other = SyncSession(
            SyncRepository(b.db, b.db.backupDao(), b.db.syncDao(), b.db.syncMetaDao(), b.tracker, DB_VERSION + 1),
            crypto,
            peerB,
        )

        val (initiator, responder) = run(session(a, peerA), other)

        val refused = (initiator as SessionResult.Failed).failure as SessionFailure.Refused
        assertTrue(refused.message.contains("version"))
        assertTrue((responder as SessionResult.Failed).failure is SessionFailure.Refused)
        assertEquals(emptyList<String>(), curriculumNames(b))
    }

    @Test
    fun aPeerOnAnotherProtocolVersionIsToldSoAndNothingChanges() = runBlocking {
        ServerSocket(0, 1, loopback).use { server ->
            val answering = async(Dispatchers.IO) {
                server.accept().use { socket ->
                    socket.soTimeout = TIMEOUT
                    session(b, peerB).respond(socket.getInputStream(), socket.getOutputStream())
                }
            }
            val reply = withContext(Dispatchers.IO) {
                Socket(loopback, server.localPort).use { socket ->
                    socket.soTimeout = TIMEOUT
                    val request = JSONObject(SyncMessageJson.encode(SyncMessage.Request(peerA, a.sync.snapshot())))
                        .put("protocol", 99).toString()
                    FrameIO.write(socket.getOutputStream(), crypto.encrypt(request.toByteArray()))
                    String(crypto.decrypt(FrameIO.read(socket.getInputStream())))
                }
            }

            assertTrue(SyncMessageJson.decode(reply) is SyncMessageJson.DecodeResult.Success)
            assertTrue(((answering.await() as SessionResult.Failed).failure) is SessionFailure.Incompatible)
        }
    }

    @Test
    fun garbageFromAStrangerIsRejectedWithoutAReplyOrAChange() = runBlocking {
        ServerSocket(0, 1, loopback).use { server ->
            val answering = async(Dispatchers.IO) {
                server.accept().use { socket ->
                    socket.soTimeout = TIMEOUT
                    session(b, peerB).respond(socket.getInputStream(), socket.getOutputStream())
                }
            }
            withContext(Dispatchers.IO) {
                Socket(loopback, server.localPort).use { socket ->
                    FrameIO.write(socket.getOutputStream(), ByteArray(64) { it.toByte() })
                }
            }

            assertEquals(SessionResult.Failed(SessionFailure.WrongPassphrase), answering.await())
            assertEquals(emptyList<String>(), curriculumNames(b))
        }
    }

    @Test
    fun anOversizedFrameIsRefusedAsANetworkProblem() = runBlocking {
        ServerSocket(0, 1, loopback).use { server ->
            val answering = async(Dispatchers.IO) {
                server.accept().use { socket ->
                    socket.soTimeout = TIMEOUT
                    session(b, peerB).respond(socket.getInputStream(), socket.getOutputStream())
                }
            }
            withContext(Dispatchers.IO) {
                Socket(loopback, server.localPort).use { socket ->
                    socket.getOutputStream().write(byteArrayOf(0x7f, -1, -1, -1))
                    socket.getOutputStream().flush()
                }
            }

            assertTrue((answering.await() as SessionResult.Failed).failure is SessionFailure.Network)
        }
    }

    @Test
    fun aPeerThatGoesSilentTimesOutInsteadOfHanging() = runBlocking {
        ServerSocket(0, 1, loopback).use { server ->
            val silent = async(Dispatchers.IO) { server.accept().use { Thread.sleep(1_500) } }
            val result = withContext(Dispatchers.IO) {
                Socket(loopback, server.localPort).use { socket ->
                    socket.soTimeout = 300
                    session(a, peerA).initiate(socket.getInputStream(), socket.getOutputStream())
                }
            }

            assertTrue((result as SessionResult.Failed).failure is SessionFailure.Network)
            silent.await()
        }
    }

    private companion object {
        const val TIMEOUT = 5_000
    }
}
