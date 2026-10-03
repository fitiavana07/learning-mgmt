package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R

private val FADE_HEIGHT = 40.dp

/**
 * Shows [content] in full when it fits within [maxScreenFraction] of the screen height. A taller
 * content is collapsed to that height, with a fade and a "Show more" / "Show less" button.
 * [contentModifier] is applied to the (possibly clipped) content box.
 */
@Composable
fun CollapsibleContent(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    maxScreenFraction: Float = 0.2f,
    content: @Composable () -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * maxScreenFraction
    val maxHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var naturalHeightPx by remember { mutableIntStateOf(0) }
    val overflows = naturalHeightPx > maxHeightPx
    val collapsed = overflows && !expanded
    val fadeTo = MaterialTheme.colorScheme.background

    Column(modifier) {
        Box(
            contentModifier
                .fillMaxWidth()
                .then(if (collapsed) Modifier.heightIn(max = maxHeight) else Modifier)
                .clipToBounds()
                .drawWithContent {
                    drawContent()
                    if (collapsed) {
                        val fade = FADE_HEIGHT.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, fadeTo),
                                startY = size.height - fade,
                                endY = size.height,
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - fade),
                            size = androidx.compose.ui.geometry.Size(size.width, fade),
                        )
                    }
                },
        ) {
            // Measured without a height limit, so its size is the content's natural height.
            Box(
                Modifier
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .onSizeChanged { naturalHeightPx = it.height },
            ) {
                content()
            }
        }
        if (overflows) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.action_show_less else R.string.action_show_more))
            }
        }
    }
}
