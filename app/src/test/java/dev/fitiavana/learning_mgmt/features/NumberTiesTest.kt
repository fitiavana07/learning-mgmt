package dev.fitiavana.learning_mgmt.features

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.topics.Topic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * After a sync two devices can each have added an item with the same number, so every ordered
 * query falls back to the id to give all devices the same order.
 */
@RunWith(RobolectricTestRunner::class)
class NumberTiesTest {
    @get:Rule
    val env = TestEnvironment()

    private suspend fun seed() {
        env.db.curriculumDao().insert(Curriculum("c1", "Kotlin", 1))
        env.db.phaseDao().insert(Phase("pb", "c1", 1, "B", ""))
        env.db.phaseDao().insert(Phase("pa", "c1", 1, "A", ""))
        env.db.topicDao().insert(Topic("tb", "pa", 1, "B", null, null))
        env.db.topicDao().insert(Topic("ta", "pa", 1, "A", null, null))
    }

    /** Stored numbers with gaps and duplicates, as a sync can leave them: 7, 7, 20 and 4, 4, 9. */
    private suspend fun seedGaps() {
        env.db.curriculumDao().insert(Curriculum("c1", "Kotlin", 1))
        env.db.phaseDao().insert(Phase("pb", "c1", 7, "B", ""))
        env.db.phaseDao().insert(Phase("pa", "c1", 7, "A", ""))
        env.db.phaseDao().insert(Phase("pc", "c1", 20, "C", ""))
        env.db.topicDao().insert(Topic("tb", "pa", 4, "B", null, null))
        env.db.topicDao().insert(Topic("ta", "pa", 4, "A", null, null))
        env.db.topicDao().insert(Topic("tc", "pa", 9, "C", null, null))
    }

    private val phasePlaces = listOf("pa" to 1, "pb" to 2, "pc" to 3)
    private val topicPlaces = listOf("ta" to 1, "tb" to 2, "tc" to 3)

    @Test
    fun phasesAreNumberedByTheirPlaceWhateverTheStoredNumbers() = runBlocking {
        seedGaps()

        assertEquals(phasePlaces, env.phases.observe("c1").first().map { it.id to it.number })
        assertEquals(phasePlaces, env.progress.observe("c1").first().map { it.phase.id to it.phase.number })
        assertEquals(phasePlaces, env.progress.observeAll().first().getValue("c1").map { it.phase.id to it.phase.number })
    }

    @Test
    fun topicsAreNumberedByTheirPlaceWhateverTheStoredNumbers() = runBlocking {
        seedGaps()

        assertEquals(topicPlaces, env.topics.observe("pa").first().map { it.id to it.number })
        assertEquals(topicPlaces, env.progress.observeTopics("pa").first().map { it.topic.id to it.topic.number })
    }

    @Test
    fun savingAnItemShownByItsPlaceKeepsItWhereItIs() = runBlocking {
        seedGaps()

        env.phases.update("pb", "B renamed", "")
        env.topics.update("tb", "B renamed", null, null)

        assertEquals(listOf("pa", "pb", "pc"), env.phases.observe("c1").first().map { it.id })
        assertEquals(listOf("ta", "tb", "tc"), env.topics.observe("pa").first().map { it.id })
    }

    @Test
    fun movingUsesThePlacesTheUserSees() = runBlocking {
        seedGaps()

        env.phases.move("c1", from = 0, to = 2)
        env.topics.move("pa", from = 2, to = 0)

        assertEquals(listOf("pb", "pc", "pa"), env.phases.observe("c1").first().map { it.id })
        assertEquals(listOf("tc", "ta", "tb"), env.topics.observe("pa").first().map { it.id })
        assertEquals(listOf(1, 2, 3), env.phases.observe("c1").first().map { it.number })
    }

    @Test
    fun curriculaWithTheSameNumberAreOrderedById() = runBlocking {
        env.db.curriculumDao().insert(Curriculum("cb", "B", 1))
        env.db.curriculumDao().insert(Curriculum("ca", "A", 1))

        assertEquals(listOf("ca", "cb"), env.curricula.observeAll().first().map { it.id })
    }

    @Test
    fun phasesWithTheSameNumberAreOrderedById() = runBlocking {
        seed()

        assertEquals(listOf("pa", "pb"), env.db.phaseDao().getByCurriculum("c1").map { it.id })
        assertEquals(listOf("pa", "pb"), env.db.phaseDao().observeByCurriculum("c1").first().map { it.id })
        assertEquals(listOf("pa", "pb"), env.db.phaseStatusDao().getRowsByCurriculum("c1").map { it.phase.id })
        assertEquals(listOf("pa", "pb"), env.db.phaseStatusDao().observeAllRows().first().map { it.phase.id })
    }

    @Test
    fun topicsWithTheSameNumberAreOrderedById() = runBlocking {
        seed()

        assertEquals(listOf("ta", "tb"), env.db.topicDao().getByPhase("pa").map { it.id })
        assertEquals(listOf("ta", "tb"), env.db.topicDao().observeByPhase("pa").first().map { it.id })
        assertEquals(listOf("ta", "tb"), env.db.topicProgressDao().getRowsByPhase("pa").map { it.topic.id })
    }
}
