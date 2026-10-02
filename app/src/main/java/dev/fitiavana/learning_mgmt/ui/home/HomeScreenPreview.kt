package dev.fitiavana.learning_mgmt.ui.home

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

private val sampleDescription = """
    ## Goals
    - Master the **preterite** and *imperfect*
    - [x] Read chapter 4
    - [ ] Finish exercise set

    See the [course page](https://example.com) for `examples`.
""".trimIndent()

private fun samplePhase(number: Int, name: String) = Phase("p$number", "c", number, name, sampleDescription)

private class HomeStates : PreviewParameterProvider<HomeUiState> {
    override val values = sequenceOf(
        HomeUiState("Spanish A2", HomeContent.InProgress(samplePhase(2, "Past Tenses"))),
        HomeUiState("Spanish A2", HomeContent.ReadyToStart(samplePhase(1, "Basics"))),
        HomeUiState("Spanish A2", HomeContent.AllCompleted),
        HomeUiState("Spanish A2", HomeContent.NoPhases),
        HomeUiState(null, HomeContent.NoCurricula),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview(@PreviewParameter(HomeStates::class) state: HomeUiState) {
    LearningmgmtTheme {
        HomeScreen(state = state, onStart = {}, onComplete = {}, onManageCurricula = {}, onOpenMenu = {}, onShowPhases = {})
    }
}
