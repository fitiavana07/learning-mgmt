package dev.fitiavana.learning_mgmt.features.selection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchSelectionTest {
    private val ids = listOf("a", "b", "c")

    @Test
    fun keepsStoredIdWhenItStillExists() {
        assertEquals("b", LaunchSelection.resolve(storedId = "b", availableIds = ids))
    }

    @Test
    fun fallsBackToFirstWhenStoredIdWasDeleted() {
        assertEquals("a", LaunchSelection.resolve(storedId = "gone", availableIds = ids))
    }

    @Test
    fun fallsBackToFirstWhenNothingWasStored() {
        assertEquals("a", LaunchSelection.resolve(storedId = null, availableIds = ids))
    }

    @Test
    fun returnsNullWhenThereAreNoCurricula() {
        assertNull(LaunchSelection.resolve(storedId = "a", availableIds = emptyList()))
    }
}
