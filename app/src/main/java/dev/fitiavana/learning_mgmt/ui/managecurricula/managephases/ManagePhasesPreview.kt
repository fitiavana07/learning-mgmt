package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

private class ManagePhasesStates : PreviewParameterProvider<ManagePhasesState> {
    override val values = sequenceOf(
        ManagePhasesState(
            curriculumName = "Spanish A2",
            rows = listOf(
                PhaseRow("p1", 1, "Basics", Status.COMPLETED),
                PhaseRow("p2", 2, "Past Tenses", Status.IN_PROGRESS),
                PhaseRow("p3", 3, "Subjunctive", Status.NOT_STARTED),
                PhaseRow("p4", 4, "Conditionals", Status.NOT_STARTED),
            ),
        ),
        ManagePhasesState(curriculumName = "Spanish A2"),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ManagePhasesScreenPreview(@PreviewParameter(ManagePhasesStates::class) state: ManagePhasesState) {
    LearningmgmtTheme {
        ManagePhasesScreen(
            state = state,
            onAdd = {},
            onPhaseClick = {},
            onDelete = {},
            onMove = { _, _ -> },
            onBack = {},
        )
    }
}
