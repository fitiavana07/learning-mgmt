package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.ui.text.input.KeyboardCapitalization
import org.junit.Assert.assertEquals
import org.junit.Test

class TextInputTest {
    @Test
    fun keyboardStartsEachSentenceInUppercase() {
        assertEquals(KeyboardCapitalization.Sentences, TextInput.keyboardOptions.capitalization)
    }
}
