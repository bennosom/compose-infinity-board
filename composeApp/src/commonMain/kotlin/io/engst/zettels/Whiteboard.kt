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
import androidx.compose.runtime.key
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

// Spatial index for efficient hit testing
class SpatialIndex {
    private val gridSize = 500
    private val grid = mutableMapOf<Pair<Int, Int>, MutableSet<String>>()
    private val itemPositions = mutableMapOf<String, NoteItem>()

    fun rebuild(items: List<NoteItem>) {
        grid.clear()
        itemPositions.clear()
        items.forEach { insert(it) }
    }

    fun insert(item: NoteItem) {
        itemPositions[item.id] = item
        val cells = getCells(item)
        cells.forEach { cell ->
            grid.getOrPut(cell) { mutableSetOf() }.add(item.id)
        }
    }

    fun updateItem(item: NoteItem) {
        // Remove from old cells
        val oldItem = itemPositions[item.id]
        if (oldItem != null) {
            val oldCells = getCells(oldItem)
            oldCells.forEach { cell ->
                grid[cell]?.remove(item.id)
            }
        }

        // Add to new cells
        insert(item)
    }

    fun queryIds(point: IntOffset): List<String> {
        val cell = Pair(point.x / gridSize, point.y / gridSize)
        return grid[cell]?.toList() ?: emptyList()
    }

    private fun getCells(item: NoteItem): List<Pair<Int, Int>> {
        val startX = item.offset.x / gridSize
        val endX = (item.offset.x + item.size.width) / gridSize
        val startY = item.offset.y / gridSize
        val endY = (item.offset.y + item.size.height) / gridSize

        val cells = mutableListOf<Pair<Int, Int>>()
        for (x in startX..endX) {
            for (y in startY..endY) {
                cells.add(Pair(x, y))
            }
        }
        return cells
    }
}

const val BOARD_ZOOM_MIN = 0.05f
const val BOARD_ZOOM_MAX = 5f

@Composable
fun Whiteboard(
    items: List<NoteItem>,
    onMoveItem: (id: String, newOffset: IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var boardOffset by remember { mutableStateOf(IntOffset.Zero) }
    var boardScale by remember { mutableStateOf(1f) }

    var dragItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }

    val itemBounds = remember(items) {
        if (items.isEmpty()) IntRect.Zero
        else {
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var maxY = Int.MIN_VALUE

            items.forEach { item ->
                minX = minOf(minX, item.offset.x)
                minY = minOf(minY, item.offset.y)
                maxX = maxOf(maxX, item.offset.x + item.size.width)
                maxY = maxOf(maxY, item.offset.y + item.size.height)
            }

            IntRect(minX, minY, maxX, maxY)
        }
    }

    val spatialIndex = remember { SpatialIndex() }
    LaunchedEffect(items) {
        spatialIndex.rebuild(items)
    }

    val visibleItems = remember(items, boardOffset, boardScale, boardSize) {
        items
        // TODO: enable viewport culling
//        if (items.isEmpty()) return@remember emptyList<NoteItem>()
//
//        val visibleLeft = (-offset.x / scale).toInt()
//        val visibleTop = (-offset.y / scale).toInt()
//        val visibleRight = visibleLeft + (boardSize.width / scale).toInt()
//        val visibleBottom = visibleTop + (boardSize.height / scale).toInt()
//
//        items.filter { item ->
//            item.offset.x < visibleRight &&
//                    item.offset.x + item.size.width > visibleLeft &&
//                    item.offset.y < visibleBottom &&
//                    item.offset.y + item.size.height > visibleTop
//        }
    }

    fun zoomOverview() {
        if (itemBounds.isEmpty || boardSize == IntSize.Zero) return

        val padding = 100f
        val contentWidth = itemBounds.width.toFloat()
        val contentHeight = itemBounds.height.toFloat()

        // Calculate scale to fit content with padding
        val scaleX = (boardSize.width - padding * 2) / contentWidth
        val scaleY = (boardSize.height - padding * 2) / contentHeight
        val fitScale = min(scaleX, scaleY).coerceIn(BOARD_ZOOM_MIN, BOARD_ZOOM_MAX)

        // Calculate offset to center the content
        val scaledContentWidth = contentWidth * fitScale
        val scaledContentHeight = contentHeight * fitScale

        val centerX = boardSize.width / 2f
        val centerY = boardSize.height / 2f

        val contentCenterX = itemBounds.left + contentWidth / 2f
        val contentCenterY = itemBounds.top + contentHeight / 2f

        boardScale = fitScale
        boardOffset = IntOffset(
            x = (centerX - contentCenterX * fitScale).roundToInt(),
            y = (centerY - contentCenterY * fitScale).roundToInt()
        )
    }

    fun findItemAtPosition(items: List<NoteItem>, position: Offset): NoteItem? {
        val mappedPosition = ((position + boardOffset.toOffset() * boardScale) / boardScale).round()
        val fixedPosition = with(density) {
            IntOffset(
                mappedPosition.x.toDp().value.roundToInt(),
                mappedPosition.y.toDp().value.roundToInt()
            )
        }

        val candidateIds = spatialIndex.queryIds(fixedPosition)
        return items.filter { it.id in candidateIds }.lastOrNull { item ->
            fixedPosition.x in item.offset.x..(item.offset.x + item.size.width) &&
                    fixedPosition.y in item.offset.y..(item.offset.y + item.size.height)
        }
    }

    fun getItemOffset(item: NoteItem): IntOffset {
        return if (dragItemId == item.id) {
            dragOffset
        } else {
            item.offset
        }
    }

    Surface(modifier = modifier) {
        val viewConfig = LocalViewConfiguration.current
        val scope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { boardSize = it }
                .pointerInput(items) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        var pointerCount = 1
                        var touchedItem: NoteItem? = null
                        var isTransforming = false
                        var longPressJob: Job? = null
                        var totalMove = Offset.Zero

                        touchedItem = findItemAtPosition(items, firstDown.position)
                        if (touchedItem != null) {
                            longPressJob = scope.launch {
                                delay(viewConfig.longPressTimeoutMillis)
                                if (pointerCount == 1 && !isTransforming) {
                                    dragItemId = touchedItem.id
                                    dragOffset = touchedItem.offset
                                }
                            }
                        }

                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val currentPointers = event.changes.filter { it.pressed }
                            pointerCount = currentPointers.size

                            when {
                                pointerCount >= 2 -> {
                                    longPressJob?.cancel()

                                    // End drag if multi-touch starts
                                    if (dragItemId != null) {
                                        onMoveItem(dragItemId!!, dragOffset)
                                        dragItemId = null
                                    }

                                    if (!isTransforming) {
                                        isTransforming = true
                                    }

                                    if (currentPointers.size >= 2) {
                                        val pointer1 = currentPointers[0]
                                        val pointer2 = currentPointers[1]

                                        val currentDistance =
                                            (pointer1.position - pointer2.position).getDistance()
                                        val previousDistance =
                                            (pointer1.previousPosition - pointer2.previousPosition).getDistance()

                                        if (previousDistance > 0f) {
                                            val zoom = currentDistance / previousDistance
                                            val centroid =
                                                ((pointer1.position + pointer2.position) / 2f).round()

                                            val newScale = (boardScale * zoom).coerceIn(
                                                BOARD_ZOOM_MIN,
                                                BOARD_ZOOM_MAX
                                            )
                                            boardOffset =
                                                (boardOffset + centroid / boardScale) - (centroid / newScale)
                                            boardScale = newScale
                                        }

                                        val centroid = (pointer1.position + pointer2.position) / 2f
                                        val previousCentroid =
                                            (pointer1.previousPosition + pointer2.previousPosition) / 2f
                                        val pan = centroid - previousCentroid

                                        boardOffset -= (pan / boardScale).round()
                                    }
                                }

                                pointerCount == 1 && !isTransforming -> {
                                    val pointer = currentPointers[0]
                                    val change = pointer.position - pointer.previousPosition

                                    if (dragItemId != null) {
                                        // Update temporary offset during drag
                                        val dpChange = with(density) {
                                            IntOffset(
                                                change.x.toDp().value.roundToInt(),
                                                change.y.toDp().value.roundToInt()
                                            )
                                        }
                                        dragOffset += (dpChange / boardScale)
                                        pointer.consume()
                                    } else {
                                        totalMove += change
                                        if (totalMove.getDistance() > viewConfig.touchSlop) {
                                            longPressJob?.cancel()
                                        }
                                        boardOffset -= (change / boardScale).round()
                                        pointer.consume()
                                    }
                                }
                            }
                        } while (pointerCount > 0)

                        // When drag ends, apply final position
                        if (dragItemId != null) {
                            onMoveItem(dragItemId!!, dragOffset)
                        }

                        longPressJob?.cancel()
                        dragItemId = null
                        dragOffset = IntOffset.Zero
                        totalMove = Offset.Zero
                        isTransforming = false
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
                    .graphicsLayer {
                        translationX = -boardOffset.x * boardScale
                        translationY = -boardOffset.y * boardScale
                        scaleX = boardScale
                        scaleY = boardScale
                    }
            ) {
                visibleItems.forEach { item ->
                    key(item.id) {
                        val effectiveOffset = getItemOffset(item)
                        NoteView(
                            item = item,
                            modifier = Modifier
                                .absoluteOffset {
                                    IntOffset(
                                        effectiveOffset.x.dp.toPx().roundToInt(),
                                        effectiveOffset.y.dp.toPx().roundToInt()
                                    )
                                }
                                .size(item.size.width.dp, item.size.height.dp)
                        )
                    }
                }
            }

            // UI controls
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visible: ${visibleItems.size}/${items.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (boardScale != 1f) {
                    AssistChip(
                        onClick = { boardScale = 1f },
                        label = { Text("${(boardScale * 100).toInt()}%") },
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
                    label = { Text("Overview") },
                    colors = SuggestionChipDefaults.suggestionChipColors().copy(
                        containerColor = MaterialTheme.colorScheme.surface,
                        trailingIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                )
            }
        }
    }
}