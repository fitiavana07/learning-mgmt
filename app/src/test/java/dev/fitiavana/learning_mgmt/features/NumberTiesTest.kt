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
