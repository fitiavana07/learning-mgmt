package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.phases.Phase

/**
 * A phase's "Phase N · Name" title and rendered (collapsible) description, under an optional
 * [header] and above an optional [footer].
 */
@Composable
fun PhaseContent(
    phase: Phase,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        header()
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.phase_title, phase.number, phase.name),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))
        CollapsibleContent(contentModifier = Modifier.testTag("phase-description")) {
            MarkdownText(phase.description)
        }
        footer()
    }
}
