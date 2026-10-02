package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization

/** Shared keyboard configuration for the app's text fields. */
object TextInput {
    val keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
}
