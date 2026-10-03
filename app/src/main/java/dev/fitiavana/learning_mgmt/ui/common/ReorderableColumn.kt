package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A list whose rows can be long-pressed and dragged to a new position; [onMove] is called on
 * release with 0-based positions. [itemContent] must apply the given modifier to its row.
 * With [enabled] false the rows cannot be dragged.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> String,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemContent: @Composable (item: T, dragging: Boolean, modifier: Modifier) -> Unit,
) {
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    val current by rememberUpdatedState(items)

    LazyColumn(modifier) {
        items(items, key = key) { item ->
            val id = key(item)
            val dragging = draggedId == id
            val dragModifier = if (enabled) {
                Modifier.pointerInput(id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { draggedId = id; dragOffset = 0f },
                        onDrag = { change, amount -> change.consume(); dragOffset += amount.y },
                        onDragEnd = {
                            val from = current.indexOfFirst { key(it) == id }
                            val to = dragTargetIndex(from, dragOffset, rowHeight, current.size)
                            draggedId = null
                            dragOffset = 0f
                            if (from >= 0 && to != from) onMove(from, to)
                        },
                        onDragCancel = { draggedId = null; dragOffset = 0f },
                    )
                }
            } else {
                Modifier
            }
            itemContent(
                item,
                dragging,
                Modifier
                    .onSizeChanged { rowHeight = it.height.toFloat() }
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (dragging) dragOffset else 0f
                        shadowElevation = if (dragging) 8.dp.toPx() else 0f
                    }
                    .then(dragModifier),
            )
        }
    }
}
