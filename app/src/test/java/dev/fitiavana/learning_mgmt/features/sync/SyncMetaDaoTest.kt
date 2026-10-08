package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncMetaDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SyncMetaDao

    private val topic = RecordKey(SyncKind.TOPIC, "t1")
    private val phase = RecordKey(SyncKind.PHASE, "p1")

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.syncMetaDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun startsEmptyWithHighestTimeZero() = runBlocking {
        assertEquals(emptyList<SyncMetaRow>(), dao.all())
        assertEquals(0L, dao.highestTime())
    }

    @Test
    fun rowsRoundTripThroughTheirKeyAndMeta() = runBlocking {
        val meta = Meta(Stamp(42, "dev"), deleted = true)

        dao.upsert(listOf(SyncMetaRow.of(topic, meta)))

        val row = dao.all().single()
        assertEquals(topic, row.key())
        assertEquals(meta, row.meta())
    }

    @Test
    fun upsertReplacesTheRowWithTheSameKey() = runBlocking {
        dao.upsert(listOf(SyncMetaRow.of(topic, Meta(Stamp(1, "a"), false))))
        dao.upsert(listOf(SyncMetaRow.of(topic, Meta(Stamp(2, "b"), true))))

        assertEquals(mapOf(topic to Meta(Stamp(2, "b"), true)), dao.all().associate { it.key() to it.meta() })
    }

    @Test
    fun theSameIdInDifferentKindsAreDifferentRecords() = runBlocking {
        dao.upsert(
            listOf(
                SyncMetaRow.of(RecordKey(SyncKind.PHASE, "x"), Meta(Stamp(1, "a"), false)),
                SyncMetaRow.of(RecordKey(SyncKind.TOPIC, "x"), Meta(Stamp(2, "a"), false)),
            ),
        )

        assertEquals(2, dao.all().size)
    }

    @Test
    fun highestTimeIsTheLargestStampTime() = runBlocking {
        dao.upsert(
            listOf(
                SyncMetaRow.of(topic, Meta(Stamp(5, "a"), false)),
                SyncMetaRow.of(phase, Meta(Stamp(9, "a"), true)),
            ),
        )

        assertEquals(9L, dao.highestTime())
    }

    @Test
    fun deleteAllEmptiesTheTable() = runBlocking {
        dao.upsert(listOf(SyncMetaRow.of(topic, Meta(Stamp(5, "a"), false))))

        dao.deleteAll()

        assertEquals(emptyList<SyncMetaRow>(), dao.all())
    }
}
