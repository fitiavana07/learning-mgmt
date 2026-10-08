package dev.fitiavana.learning_mgmt.features.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.fitiavana.learning_mgmt.db.TestEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Two devices, each with a database, settings and a coordinator, finding each other through a
 * fake network and syncing over real loopback connections.
 */
@RunWith(RobolectricTestRunner::class)
class SyncCoordinatorTest {
    @get:Rule
    val a = TestEnvironment("dev-a", "a-")

    @get:Rule
    val b = TestEnvironment("dev-b", "b-")

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Delivers broadcasts and unicasts to every other open channel, as if on the same Wi-Fi. */
    private class FakeNetwork {
        // Appended to by the coordinators' threads while the test reads them.
        val channels = CopyOnWriteArrayList<FakeChannel>()
        val broadcasts = CopyOnWriteArrayList<DiscoveryPacket>()

        fun channel() = FakeChannel(this).also(channels::add)

        class FakeChannel(private val network: FakeNetwork) : DiscoveryChannel {
            private val received = MutableSharedFlow<Datagram>(extraBufferCapacity = 64)
            var closed = false

            /** While true, nothing reaches this channel: the device does not hear anyone. */
            @Volatile
            var deaf = false

            override val incoming: Flow<Datagram> = received

            override suspend fun broadcast(bytes: ByteArray) {
                DiscoveryPacket.decode(bytes)?.let { network.broadcasts += it }
                deliver(bytes)
            }

            override suspend fun send(bytes: ByteArray, host: String) = deliver(bytes)

            override fun close() {
                closed = true
            }

            fun hear(bytes: ByteArray) {
                if (!closed && !deaf) received.tryEmit(Datagram(bytes, "127.0.0.1"))
            }

            private fun deliver(bytes: ByteArray) {
                if (closed) return
                network.channels.filter { it !== this }.forEach { it.hear(bytes) }
            }
        }
    }

    private class Device(val env: TestEnvironment, val settings: SyncSettingsStore, val coordinator: SyncCoordinator)

    private val network = FakeNetwork()
    private val devices = mutableListOf<Device>()
    private var counter = 0

    private fun device(env: TestEnvironment, deviceId: String, passphrase: String? = "correct horse", autoSync: Boolean = true): Device {
        val file = File(folder.root, "settings-${counter++}.preferences_pb")
        val settings = SyncSettingsStore(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
            newId = { deviceId },
        )
        runBlocking {
            settings.setPassphrase(passphrase)
            settings.setAutoSync(autoSync)
        }
        val coordinator = SyncCoordinator(
            scope = scope,
            repository = env.sync,
            settings = settings,
            changes = env.tracker.changes,
            newChannel = network::channel,
            debounceMs = 100,
            announceIntervalMs = 200,
        )
        return Device(env, settings, coordinator).also(devices::add)
    }

    @After
    fun tearDown() {
        devices.forEach { it.coordinator.stop() }
        scope.cancel()
    }

    private suspend fun waitUntil(what: String, condition: suspend () -> Boolean) {
        try {
            withTimeout(15_000) { while (!condition()) delay(25) }
        } catch (e: TimeoutCancellationException) {
            throw AssertionError("Timed out waiting for: $what\n" + devices.joinToString("\n") { it.coordinator.state.value.toString() })
        }
    }

    private suspend fun curriculumNames(e: TestEnvironment) = e.curricula.observeAll().first().map { it.name }

    private suspend fun doneOf(e: TestEnvironment, topic: String) =
        e.db.topicDao().get(topic)?.let { t ->
            e.db.topicProgressDao().getRowsByPhase(t.phaseId).first { it.topic.id == topic }.done
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

    @Test
    fun withoutAPassphraseNothingIsStarted() = runBlocking {
        val d = device(a, "dev-a", passphrase = null)

        d.coordinator.start()
        waitUntil("running") { d.coordinator.state.value.running }
        delay(300)

        assertFalse(d.coordinator.state.value.hasPassphrase)
        assertTrue(network.channels.isEmpty())
        assertTrue(d.coordinator.state.value.peers.isEmpty())
    }

    @Test
    fun devicesWithTheSamePassphraseFindEachOtherAndSyncOnTheirOwn() = runBlocking {
        val topic = seed(a)
        val first = device(a, "dev-a")
        val second = device(b, "dev-b")

        first.coordinator.start()
        second.coordinator.start()

        waitUntil("B receives A's data") { curriculumNames(b) == listOf("Kotlin") && doneOf(b, topic) == 4 }
        waitUntil("both list a synced peer") {
            first.coordinator.state.value.peers.singleOrNull()?.sync is PeerSync.Synced &&
                second.coordinator.state.value.peers.singleOrNull()?.sync is PeerSync.Synced
        }
        assertEquals(DeviceName.of("dev-b"), first.coordinator.state.value.peers.single().peer.label)
        assertEquals(DeviceName.of("dev-a"), second.coordinator.state.value.peers.single().peer.label)
        assertEquals(a.sync.snapshot().meta, b.sync.snapshot().meta)
    }

    @Test
    fun whicheverDeviceHoldsTheDataBothEndUpWithIt() = runBlocking {
        val topic = seed(b)

        device(a, "dev-a").coordinator.start()
        device(b, "dev-b").coordinator.start()

        waitUntil("A receives B's data") { curriculumNames(a) == listOf("Kotlin") && doneOf(a, topic) == 4 }
    }

    @Test
    fun aLocalChangeIsPushedToThePeersAndTheyCanAnswerInKind() = runBlocking {
        val topic = seed(a)
        device(a, "dev-a").coordinator.start()
        device(b, "dev-b").coordinator.start()
        waitUntil("first sync") { doneOf(b, topic) == 4 }

        b.now = 5_000
        b.progress.recordTopicProgress(topic, 7)
        waitUntil("A sees 7") { doneOf(a, topic) == 7 }

        a.now = 9_000
        a.progress.recordTopicProgress(topic, 6)
        waitUntil("B sees 6") { doneOf(b, topic) == 6 }
    }

    @Test
    fun aChangeMadeBeforeThePeerWasSeenIsPushedAsSoonAsItAppears() = runBlocking {
        val first = device(a, "dev-a")
        first.coordinator.start()
        waitUntil("A listening") { network.channels.size == 1 }
        val second = device(b, "dev-b") // the larger id: it does not start sessions just because a peer appears
        second.coordinator.start()
        waitUntil("B listening") { network.channels.size == 2 }
        val bChannel = network.channels[1]
        bChannel.deaf = true
        waitUntil("A knows B") { first.coordinator.state.value.peers.any { it.sync is PeerSync.Synced } }

        b.curricula.create("Piano")
        delay(500) // the debounced push finds nobody to push to
        assertEquals(emptyList<String>(), curriculumNames(a))
        bChannel.deaf = false

        waitUntil("A receives what B did while deaf") { curriculumNames(a) == listOf("Piano") }
    }

    @Test
    fun withAutoSyncOffNothingMovesUntilTheUserAsks() = runBlocking {
        val topic = seed(a)
        val first = device(a, "dev-a", autoSync = false)
        val second = device(b, "dev-b", autoSync = false)
        first.coordinator.start()
        second.coordinator.start()
        waitUntil("peers visible") {
            first.coordinator.state.value.peers.isNotEmpty() && second.coordinator.state.value.peers.isNotEmpty()
        }
        delay(500)
        assertEquals(emptyList<String>(), curriculumNames(b))

        first.coordinator.syncWith("dev-b")

        waitUntil("manual sync") { doneOf(b, topic) == 4 }
    }

    @Test
    fun theSyncAllButtonSyncsEveryVisiblePeer() = runBlocking {
        val topic = seed(a)
        val first = device(a, "dev-a", autoSync = false)
        val second = device(b, "dev-b", autoSync = false)
        first.coordinator.start()
        second.coordinator.start()
        waitUntil("peer visible") { second.coordinator.state.value.peers.isNotEmpty() }

        second.coordinator.syncAll()

        waitUntil("B pulls A's data") { doneOf(b, topic) == 4 }
    }

    @Test
    fun devicesWithDifferentPassphrasesNeverSeeEachOther() = runBlocking {
        seed(a)
        val first = device(a, "dev-a", passphrase = "one")
        val second = device(b, "dev-b", passphrase = "two")

        first.coordinator.start()
        second.coordinator.start()
        delay(1_000)

        assertTrue(first.coordinator.state.value.peers.isEmpty())
        assertTrue(second.coordinator.state.value.peers.isEmpty())
        assertEquals(emptyList<String>(), curriculumNames(b))
    }

    @Test
    fun enteringTheSamePassphraseJoinsTheGroup() = runBlocking {
        val topic = seed(a)
        device(a, "dev-a", passphrase = "shared").coordinator.start()
        val second = device(b, "dev-b", passphrase = "other")
        second.coordinator.start()
        delay(500)
        assertEquals(emptyList<String>(), curriculumNames(b))

        second.settings.setPassphrase("shared")

        waitUntil("B joins and syncs") { doneOf(b, topic) == 4 }
    }

    @Test
    fun stoppingEndsDiscoverySyncingAndTheListOfPeers() = runBlocking {
        val topic = seed(a)
        val first = device(a, "dev-a")
        val second = device(b, "dev-b")
        first.coordinator.start()
        second.coordinator.start()
        waitUntil("first sync") { doneOf(b, topic) == 4 }

        first.coordinator.stop()
        second.coordinator.stop()
        b.now = 5_000
        b.progress.recordTopicProgress(topic, 8)
        delay(800)

        assertEquals(4, doneOf(a, topic))
        assertFalse(first.coordinator.state.value.running)
        assertTrue(first.coordinator.state.value.peers.isEmpty())
        assertTrue(network.channels.all { it.closed })
    }

    @Test
    fun aPeerThatCannotBeReachedIsShownAsFailed() = runBlocking {
        val first = device(a, "dev-a")
        first.coordinator.start()
        waitUntil("listening") { network.channels.isNotEmpty() }
        val group = SyncCrypto("correct horse").groupTag

        // A phantom peer whose announced port nobody listens on; "zzz" sorts after "dev-a", so dev-a initiates.
        network.channels.single().hear(
            DiscoveryPacket(DiscoveryPacket.Type.ANNOUNCE, PeerInfo("zzz", "Ghost"), port = 1, group = group).encode(),
        )

        waitUntil("failure shown") {
            (first.coordinator.state.value.peers.singleOrNull()?.sync as? PeerSync.Failed)?.failure is SessionFailure.Network
        }
    }

    @Test
    fun aNetworkThatCannotBeOpenedIsReportedAndNothingCrashes() = runBlocking {
        val failing = SyncCoordinator(
            scope = scope,
            repository = a.sync,
            settings = device(a, "dev-a").settings,
            changes = a.tracker.changes,
            newChannel = { throw java.net.BindException("Address already in use") },
            debounceMs = 100,
            announceIntervalMs = 200,
        )

        failing.start()

        waitUntil("the problem is reported") { failing.state.value.networkError != null }
        assertTrue(failing.state.value.networkError!!.contains("Address already in use"))
        assertTrue(failing.state.value.running)
        failing.stop()
    }

    @Test
    fun refreshingTriesToOpenTheNetworkAgain() = runBlocking {
        var failing = true
        val settings = device(a, "dev-a").settings
        val coordinator = SyncCoordinator(
            scope = scope,
            repository = a.sync,
            settings = settings,
            changes = a.tracker.changes,
            newChannel = { if (failing) throw java.net.BindException("busy") else network.channel() },
            debounceMs = 100,
            announceIntervalMs = 200,
        )
        coordinator.start()
        waitUntil("the problem is reported") { coordinator.state.value.networkError != null }

        failing = false
        coordinator.refresh()

        waitUntil("the problem is cleared") { coordinator.state.value.networkError == null && network.channels.isNotEmpty() }
        coordinator.stop()
    }

    @Test
    fun refreshAsksTheNetworkWhoIsThere() = runBlocking {
        val first = device(a, "dev-a")
        first.coordinator.start()
        waitUntil("listening") { network.broadcasts.isNotEmpty() }

        first.coordinator.refresh()

        waitUntil("query sent") { network.broadcasts.any { it.type == DiscoveryPacket.Type.QUERY } }
    }

    @Test
    fun theStateTellsThisDevicesNameAndWhenItLastSynced() = runBlocking {
        val topic = seed(a)
        val first = device(a, "dev-a")
        device(b, "dev-b").coordinator.start()
        first.coordinator.start()

        waitUntil("synced") { doneOf(b, topic) == 4 && first.coordinator.state.value.lastSyncedAt != null }

        assertEquals(DeviceName.of("dev-a"), first.coordinator.state.value.deviceName)
        assertTrue(first.coordinator.state.value.autoSync)
        assertTrue(first.coordinator.state.value.hasPassphrase)
    }
}
