package dev.fitiavana.learning_mgmt

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundObserverTest {
    private val owner = object : LifecycleOwner {
        override val lifecycle: Lifecycle get() = registry
    }
    private val registry: LifecycleRegistry = LifecycleRegistry.createUnsafe(owner)
    private val calls = mutableListOf<String>()

    private fun observe() = registry.addObserver(ForegroundObserver({ calls += "foreground" }, { calls += "background" }))

    @Test
    fun runsWhileTheScreenIsVisibleAndStopsWhenItIsNot() {
        observe()

        registry.currentState = Lifecycle.State.STARTED
        assertEquals(listOf("foreground"), calls)

        registry.currentState = Lifecycle.State.CREATED
        assertEquals(listOf("foreground", "background"), calls)
    }

    @Test
    fun comingBackStartsAgain() {
        observe()

        registry.currentState = Lifecycle.State.RESUMED
        registry.currentState = Lifecycle.State.CREATED
        registry.currentState = Lifecycle.State.RESUMED

        assertEquals(listOf("foreground", "background", "foreground"), calls)
    }

    @Test
    fun nothingHappensBeforeTheFirstStart() {
        observe()

        registry.currentState = Lifecycle.State.CREATED

        assertEquals(emptyList<String>(), calls)
    }
}
