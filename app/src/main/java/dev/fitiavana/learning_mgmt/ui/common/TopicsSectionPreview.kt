package dev.fitiavana.learning_mgmt.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicWithProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

private fun topic(number: Int, name: String, status: Status, total: Int? = null, done: Int = 0, unit: String? = null) =
    TopicWithProgress(Topic("t$number", "p", number, name, total, unit), status, done)

private class TopicLists : PreviewParameterProvider<List<TopicWithProgress>> {
    override val values = sequenceOf(
        listOf(
            topic(1, "Regular verbs", Status.COMPLETED),
            topic(2, "Chapter 4", Status.IN_PROGRESS, total = 40, done = 12, unit = "pages"),
            topic(3, "Exercise set", Status.NOT_STARTED, total = 25),
            topic(4, "Irregular verbs", Status.NOT_STARTED),
        ),
        listOf(topic(1, "Regular verbs", Status.NOT_STARTED), topic(2, "Irregular verbs", Status.NOT_STARTED)),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TopicsSectionPreview(@PreviewParameter(TopicLists::class) topics: List<TopicWithProgress>) {
    LearningmgmtTheme {
        TopicsSection(topics, editable = true, handlers = TopicHandlers.None, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun TopicsSectionReadOnlyPreview(@PreviewParameter(TopicLists::class) topics: List<TopicWithProgress>) {
    LearningmgmtTheme {
        TopicsSection(topics, editable = false, handlers = TopicHandlers.None, modifier = Modifier.padding(16.dp))
    }
}
