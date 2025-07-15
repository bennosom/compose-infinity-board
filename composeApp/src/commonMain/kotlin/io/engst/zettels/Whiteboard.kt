package io.engst.zettels

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun Whiteboard(
    items: List<NoteItem>,
    onMoveItem: (id: String, newOffset: IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val itemBounds by remember(items) { mutableStateOf(getItemBounds(items)) }

    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        println("bx $density")
    }

    // Gesture state
    var isDraggingNote by remember { mutableStateOf(false) }
    var draggedNoteId by remember { mutableStateOf<String?>(null) }
    var draggedNoteOffset by remember { mutableStateOf(IntOffset.Zero) }

    fun zoomOverview() {
        val padding = 100f
        val contentWidth = itemBounds.width + padding
        val contentHeight = itemBounds.height + padding
        val fitScale = if (contentWidth == 0f || contentHeight == 0f) 1f else min(
            boardSize.width / contentWidth,
            boardSize.height / contentHeight
        ).coerceIn(0.01f, 1f)
        scale = fitScale
        val itemBoundsCenter = itemBounds.center
        offset = Offset(
            x = (boardSize.width / 2f - itemBoundsCenter.x + padding / 2) * fitScale,
            y = (boardSize.height / 2f - itemBoundsCenter.y + padding / 2) * fitScale
        ).round()
    }

    fun mapToBoard(position: Offset): IntOffset =
        ((position + offset.toOffset() * scale) / scale).round()

    fun findNoteAtPosition(position: Offset): NoteItem? {
        val boardPos = mapToBoard(position)
        val fixedPos = with(density) {
            IntOffset(boardPos.x.toDp().value.roundToInt(), boardPos.y.toDp().value.roundToInt())
        }
        println("bx findNoteAtPosition: $position mapped to $boardPos and fixed to $fixedPos")
        return items.lastOrNull { item ->
            fixedPos.x in item.offset.x..(item.offset.x + item.size.width) &&
                    fixedPos.y in item.offset.y..(item.offset.y + item.size.height)
        }
    }

    Surface(modifier = modifier) {
        val scope = rememberCoroutineScope()
        val viewConfig = LocalViewConfiguration.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { boardSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Wait for first pointer down
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        var pointerCount = 1
                        var initialNote: NoteItem? = null
                        var isTransforming = false
                        var longPressJob: Job? = null
                        var totalMove = Offset.Zero

                        // Check if first pointer is over a note
                        initialNote = findNoteAtPosition(firstDown.position)
                        println("bx hit testing at ${firstDown.position}: $initialNote")

                        // Start long press timer if over a note
                        if (initialNote != null) {
                            longPressJob = scope.launch {
                                delay(viewConfig.longPressTimeoutMillis)
                                println("bx Long press detected on note ${initialNote.id}")
                                if (pointerCount == 1 && !isTransforming) {
                                    isDraggingNote = true
                                    draggedNoteId = initialNote.id
                                    draggedNoteOffset = initialNote.offset
                                }
                            }
                        }

                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val currentPointers = event.changes.filter { it.pressed }
                            pointerCount = currentPointers.size

                            when {
                                pointerCount >= 2 -> {
                                    // Multi-touch: handle zoom/pan
                                    longPressJob?.cancel()

                                    if (!isTransforming) {
                                        isTransforming = true
                                        // Stop any note dragging
                                        if (isDraggingNote) {
                                            isDraggingNote = false
                                            draggedNoteId = null
                                        }
                                    }

                                    if (currentPointers.size >= 2) {
                                        val pointer1 = currentPointers[0]
                                        val pointer2 = currentPointers[1]

                                        // Calculate zoom
                                        val currentDistance =
                                            (pointer1.position - pointer2.position).getDistance()
                                        val previousDistance =
                                            (pointer1.previousPosition - pointer2.previousPosition).getDistance()

                                        if (previousDistance > 0f) {
                                            val zoom = currentDistance / previousDistance
                                            val centroid =
                                                ((pointer1.position + pointer2.position) / 2f).round()

                                            val newScale = (scale * zoom).coerceIn(0.05f, 5f)
                                            offset =
                                                (offset + centroid / scale) - (centroid / newScale)
                                            scale = newScale
                                        }

                                        // Calculate pan
                                        val centroid = (pointer1.position + pointer2.position) / 2f
                                        val previousCentroid =
                                            (pointer1.previousPosition + pointer2.previousPosition) / 2f
                                        val pan = centroid - previousCentroid

                                        offset -= (pan / scale).round()
                                    }
                                }

                                pointerCount == 1 && !isTransforming -> {
                                    // Single touch: handle note drag or board pan
                                    val pointer = currentPointers[0]
                                    val change = pointer.position - pointer.previousPosition

                                    if (isDraggingNote && draggedNoteId != null) {
                                        // Continue dragging note
                                        val dpChange = with(density) {
                                            IntOffset(
                                                change.x.toDp().value.roundToInt(),
                                                change.y.toDp().value.roundToInt()
                                            )
                                        }
                                        draggedNoteOffset += (dpChange / scale)
                                        onMoveItem(draggedNoteId!!, draggedNoteOffset)
                                        pointer.consume()
                                    } else {
                                        totalMove += change
                                        // Cancel long-press if user starts panning the board
                                        if (totalMove.getDistance() > viewConfig.touchSlop) {
                                            longPressJob?.cancel()
                                        }

                                        // Pan the board
                                        offset -= (change / scale).round()
                                        pointer.consume()
                                    }
                                }
                            }
                        } while (pointerCount > 0)

                        // Reset state when all pointers are up
                        longPressJob?.cancel()
                        isDraggingNote = false
                        draggedNoteId = null
                        isTransforming = false
                        totalMove = Offset.Zero
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
                    .graphicsLayer {
                        translationX = -offset.x * scale
                        translationY = -offset.y * scale
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                items.forEach { item ->
                    println("bx composing $item")
                    NoteView(
                        item = item,
                        modifier = Modifier
                            .absoluteOffset {
                                IntOffset(
                                    item.offset.x.dp.toPx().roundToInt(),
                                    item.offset.y.dp.toPx().roundToInt()
                                )
                            }
                            .size(item.size.width.dp, item.size.height.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (scale != 1f) {
                    AssistChip(
                        onClick = { scale = 1f },
                        label = { Text("${(scale * 100).toInt()}%") },
                        colors = SuggestionChipDefaults.suggestionChipColors().copy(
                            containerColor = MaterialTheme.colorScheme.surface,
                            trailingIconContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Zoom 100%")
                        }
                    )
                }
                AssistChip(
                    onClick = { zoomOverview() },
                    leadingIcon = {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    },
                    label = {
                        Text("Overview")
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors().copy(
                        containerColor = MaterialTheme.colorScheme.surface,
                        trailingIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                )
            }
        }
    }
}

fun getItemBounds(items: List<NoteItem>): IntRect {
    if (items.isEmpty()) return IntRect.Zero
    val minX = items.minBy { it.offset.x }
    val minY = items.minBy { it.offset.y }
    val maxX = items.maxBy { it.offset.x + it.size.width }
    val maxY = items.maxBy { it.offset.y + it.size.height }
    return IntRect(
        left = minX.offset.x,
        top = minY.offset.y,
        right = maxX.offset.x + maxX.size.width,
        bottom = maxY.offset.y + maxY.size.height
    )
}
