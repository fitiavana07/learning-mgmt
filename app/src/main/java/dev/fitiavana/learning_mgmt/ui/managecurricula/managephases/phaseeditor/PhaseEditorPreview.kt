package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

private const val DESCRIPTION = "## Goals\n- Master the **preterite** and *imperfect*\n- [x] Read chapter 4\n- [ ] Finish exercise set"

private val editing = PhaseEditorState(
    loading = false,
    isNew = false,
    name = "Past Tenses",
    description = DESCRIPTION,
    initialName = "Past Tenses",
    initialDescription = DESCRIPTION,
)

private class EditorStates : PreviewParameterProvider<PhaseEditorState> {
    override val values = sequenceOf(
        editing,
        editing.copy(preview = true),
        PhaseEditorState(loading = false, nameError = true),
        PhaseEditorState(loading = false, notFound = true, isNew = false),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PhaseEditorScreenPreview(@PreviewParameter(EditorStates::class) state: PhaseEditorState) {
    LearningmgmtTheme {
        PhaseEditorScreen(
            state = state,
            onNameChange = {},
            onDescriptionChange = {},
            onPreviewChange = {},
            onSave = {},
            onClose = {},
        )
    }
}
