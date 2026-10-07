package dev.fitiavana.learning_mgmt.features.backup

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: BackupDao

    private val curricula = listOf(Curriculum("c2", "Spanish"), Curriculum("c1", "Math"))
    private val phases = listOf(Phase("p2", "c2", 2, "Verbs", ""), Phase("p1", "c2", 1, "Basics", "# Intro"))
    private val phaseStatuses = listOf(PhaseStatus("p1", Status.IN_PROGRESS))
    private val topics = listOf(
        Topic("t2", "p1", 2, "Reading", 40, "pages"),
        Topic("t1", "p1", 1, "Greetings", null, null),
    )
    private val topicProgress = listOf(TopicProgress("t2", Status.IN_PROGRESS, 12))

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.backupDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertAll() {
        dao.insertCurricula(curricula)
        dao.insertPhases(phases)
        dao.insertTopics(topics)
        dao.insertPhaseStatuses(phaseStatuses)
        dao.insertTopicProgress(topicProgress)
    }

    @Test
    fun readsBackWhatWasInsertedInInsertionOrder() = runBlocking {
        insertAll()

        assertEquals(curricula, dao.curricula())
        assertEquals(phases, dao.phases())
        assertEquals(topics, dao.topics())
        assertEquals(phaseStatuses, dao.phaseStatuses())
        assertEquals(topicProgress, dao.topicProgress())
    }

    @Test
    fun deleteAllRemovesEveryTable() = runBlocking {
        insertAll()

        dao.deleteAll()

        assertEquals(emptyList<Curriculum>(), dao.curricula())
        assertEquals(emptyList<Phase>(), dao.phases())
        assertEquals(emptyList<Topic>(), dao.topics())
        assertEquals(emptyList<PhaseStatus>(), dao.phaseStatuses())
        assertEquals(emptyList<TopicProgress>(), dao.topicProgress())
    }
}
