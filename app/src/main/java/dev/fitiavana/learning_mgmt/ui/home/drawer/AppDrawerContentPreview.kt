package dev.fitiavana.learning_mgmt.ui.home.drawer

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.CurriculumProgress
import dev.fitiavana.learning_mgmt.features.progress.CurriculumProgress.Summary
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

private fun phase(number: Int, name: String) = Phase("p$number", "c", number, name, "")

private val sampleItems = listOf(
    DrawerItem("a", "Spanish A2", CurriculumProgress(2, 8, Summary.InProgress(phase(2, "Past Tenses"))), selected = true),
    DrawerItem("b", "Kotlin", CurriculumProgress(4, 6, Summary.Next(phase(5, "Coroutines"))), selected = false),
    DrawerItem("c", "Piano", CurriculumProgress(0, 5, Summary.NotStarted), selected = false),
    DrawerItem("d", "Chess", CurriculumProgress(3, 3, Summary.AllCompleted), selected = false),
    DrawerItem("e", "Empty curriculum", CurriculumProgress(0, 0, Summary.NoPhases), selected = false),
)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppDrawerContentPreview() {
    LearningmgmtTheme {
        AppDrawerContent(items = sampleItems, onSelect = {}, onManageCurricula = {})
    }
}
