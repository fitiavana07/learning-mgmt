package dev.fitiavana.learning_mgmt.ui.common

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.progress.Status
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopicHandlersTest {
    @get:Rule
    val env = TestEnvironment()

    private val errors = mutableListOf<Throwable>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> errors += e })
    private lateinit var phase: String

    @Before
    fun setUp() = runBlocking {
        phase = env.phases.add(env.curricula.create("Spanish"), "Basics", "")
        env.topics.add(phase, "Greetings")
        env.topics.add(phase, "Chapter 1", total = 40)
        env.progress.start(phase)
    }

    @After
    fun tearDown() = scope.cancel()

    private suspend fun settle() = scope.coroutineContext[Job]!!.children.toList().joinAll()

    private suspend fun states() = env.progress.observeTopics(phase).first().map { it.status to it.done }

    @Test
    fun startCompleteAndRecordMoveTheTopicsForward() = runBlocking {
        val handlers = TopicHandlers.of(scope, env.progress)

        handlers.onStart("t1"); settle()
        handlers.onComplete("t1"); settle()
        handlers.onStart("t2"); settle()
        handlers.onRecord("t2", 12); settle()

        assertEquals(listOf(Status.COMPLETED to 0, Status.IN_PROGRESS to 12), states())
    }

    @Test
    fun aStaleTapIsIgnoredInsteadOfCrashing() = runBlocking {
        val handlers = TopicHandlers.of(scope, env.progress)

        handlers.onStart("t1"); settle()
        handlers.onStart("t1"); settle()
        handlers.onComplete("t2"); settle()

        assertEquals(emptyList<Throwable>(), errors)
        assertEquals(listOf(Status.IN_PROGRESS to 0, Status.NOT_STARTED to 0), states())
    }
}
