package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics.topiceditor

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

private class EditorStates : PreviewParameterProvider<TopicEditorState> {
    override val values = sequenceOf(
        TopicEditorState(loading = false),
        TopicEditorState(
            loading = false,
            isNew = false,
            name = "Chapter 4",
            quantified = true,
            total = "40",
            unit = "pages",
            initialName = "Chapter 4",
            initialQuantified = true,
            initialTotal = "40",
            initialUnit = "pages",
        ),
        TopicEditorState(loading = false, quantified = true, nameError = true, totalError = true),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TopicEditorScreenPreview(@PreviewParameter(EditorStates::class) state: TopicEditorState) {
    LearningmgmtTheme {
        TopicEditorScreen(
            state = state,
            onNameChange = {},
            onQuantifiedChange = {},
            onTotalChange = {},
            onUnitChange = {},
            onSave = {},
            onClose = {},
        )
    }
}
