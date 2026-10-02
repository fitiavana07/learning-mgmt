package dev.fitiavana.learning_mgmt.ui.home.phases

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.PhaseWithStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

private const val DESCRIPTION = """
## Goals
- Master the **preterite** and *imperfect*
- [x] Read chapter 4
- [ ] Finish exercise set
"""

private fun phase(number: Int, name: String, status: Status) =
    PhaseWithStatus(Phase("p$number", "c", number, name, DESCRIPTION.trimIndent()), status)

private val phases = listOf(
    phase(1, "Basics", Status.COMPLETED),
    phase(2, "Past Tenses", Status.IN_PROGRESS),
    phase(3, "Subjunctive", Status.NOT_STARTED),
    phase(4, "Conditionals", Status.NOT_STARTED),
)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PhasesScreenPreview() {
    LearningmgmtTheme {
        PhasesScreen(
            state = PhasesUiState(loading = false, curriculumName = "Spanish A2", phases = phases),
            onPhaseClick = {},
            onBack = {},
        )
    }
}

private class PhaseViewStates : PreviewParameterProvider<PhaseViewState> {
    override val values = sequenceOf(
        PhaseViewState.Loaded(phases[1], PhaseAction.Complete),
        PhaseViewState.Loaded(phases[0], PhaseAction.None),
        PhaseViewState.Loaded(phases[2], PhaseAction.WaitForCompletion(phases[1].phase)),
        PhaseViewState.Loaded(phases[3], PhaseAction.WaitForStart(phases[2].phase)),
        PhaseViewState.NotFound,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PhaseViewScreenPreview(@PreviewParameter(PhaseViewStates::class) state: PhaseViewState) {
    LearningmgmtTheme {
        PhaseViewScreen(state = state, onStart = {}, onComplete = {}, onBack = {})
    }
}
