package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

private val rows = listOf(
    TopicRow("t1", 1, "Regular verbs", Status.COMPLETED, total = null, done = 0, unit = null),
    TopicRow("t2", 2, "Chapter 4", Status.IN_PROGRESS, total = 40, done = 12, unit = "pages"),
    TopicRow("t3", 3, "Exercise set", Status.NOT_STARTED, total = 25, done = 0, unit = null),
)

private class ManageTopicsStates : PreviewParameterProvider<ManageTopicsState> {
    override val values = sequenceOf(
        ManageTopicsState(phaseNumber = 2, phaseName = "Past Tenses", rows = rows),
        ManageTopicsState(phaseNumber = 2, phaseName = "Past Tenses", rows = rows, locked = true),
        ManageTopicsState(phaseNumber = 2, phaseName = "Past Tenses"),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ManageTopicsScreenPreview(@PreviewParameter(ManageTopicsStates::class) state: ManageTopicsState) {
    LearningmgmtTheme {
        ManageTopicsScreen(
            state = state,
            onAdd = {},
            onTopicClick = {},
            onDelete = {},
            onMove = { _, _ -> },
            onBack = {},
        )
    }
}
