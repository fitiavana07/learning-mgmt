package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown

/** Renders GitHub-flavoured markdown (headings, emphasis, lists, links, code, task lists). */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    Markdown(content = markdown, modifier = modifier.fillMaxWidth())
}
