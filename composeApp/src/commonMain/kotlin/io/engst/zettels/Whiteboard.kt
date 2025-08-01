package io.engst.zettels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

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
   onSmartArrange: () -> Unit,
   modifier: Modifier = Modifier
) {
   val density = LocalDensity.current

   var canvasSize by remember { mutableStateOf(IntSize.Zero) }
   var canvasOffset by remember { mutableStateOf(Offset.Zero) }
   var canvasZoom by remember { mutableStateOf(1f) }

   var dragItemId by remember { mutableStateOf<String?>(null) }
   var dragItemOffset by remember { mutableStateOf(Offset.Zero) }

   val globalBounds = remember(items) {
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
      }.also {
         println("globalBounds: $it")
      }
   }

   val spatialIndex = remember { SpatialIndex() }
   LaunchedEffect(items) {
      spatialIndex.rebuild(items)
   }

   val visibleItems = remember(items, canvasOffset, canvasZoom, canvasSize) {
      items
//      if (items.isEmpty()) return@remember emptyList()
//
//      with(density) {
//         val left = (-canvasOffset.x / canvasZoom).toDp().value
//         val top = (-canvasOffset.y / canvasZoom).toDp().value
//         val right = left + (canvasSize.width / canvasZoom).toDp().value
//         val bottom = top + (canvasSize.height / canvasZoom).toDp().value
//
//         println("viewport culling: $left, $top, $right, $bottom")
//
//         items.fastFilter { item ->
//            item.offset.x < right && item.offset.x + item.size.width > left &&
//              item.offset.y < bottom && item.offset.y + item.size.height > top
//         }
//      }
   }

   /**
    * Zooms the canvas to fit the entire content within the viewport.
    */
   fun zoomOverview() {
      if (globalBounds.isEmpty || canvasSize == IntSize.Zero) return

      val padding: Float
      val globalSize: Size
      val globalCenter: Offset
      with(density) {
         padding = 16.dp.toPx()
         globalSize = Size(
            width = globalBounds.width.toFloat().dp.toPx(),
            height = globalBounds.height.toFloat().dp.toPx()
         )
         globalCenter = Offset(
            x = globalBounds.left.toFloat().dp.toPx() + globalSize.width / 2,
            y = globalBounds.top.toFloat().dp.toPx() + globalSize.height / 2
         )
      }

      val scaleX = (canvasSize.width - padding * 2) / globalSize.width
      val scaleY = (canvasSize.height - padding * 2) / globalSize.height
      val overviewZoom = min(scaleX, scaleY).coerceIn(BOARD_ZOOM_MIN, BOARD_ZOOM_MAX)
      canvasZoom = overviewZoom

      val canvasCenter = canvasSize.toSize().center
      canvasOffset = Offset(
         x = canvasCenter.x - globalCenter.x * canvasZoom,
         y = canvasCenter.y - globalCenter.y * canvasZoom
      )
   }

   /**
    * Maps touch input coordinates from the canvas space to the global coordinate space.
    * Global coordinates are in Dp, while the canvas space is in pixels.
    */
   fun mapTouchInput(pointer: Offset): Offset {
      return with(density) {
         ((pointer - canvasOffset) / canvasZoom).let { globalPx ->
            Offset(
               globalPx.x.toDp().value,
               globalPx.y.toDp().value
            ).also {
               println("mapTouchInput: $pointer to $it")
            }
         }
      }
   }

   /**
    * Finds the item at the given global coordinates using the spatial index.
    * Returns the last item that contains the point, or null if no item is found.
    */
   fun findItem(items: List<NoteItem>, global: IntOffset): NoteItem? {
      val candidateIds = spatialIndex.queryIds(global)
      return items.filter { it.id in candidateIds }.lastOrNull { item ->
         global.x in item.offset.x..(item.offset.x + item.size.width) &&
           global.y in item.offset.y..(item.offset.y + item.size.height)
      }
   }

   Surface(modifier = modifier) {
      val viewConfig = LocalViewConfiguration.current
      val scope = rememberCoroutineScope()

      Box(
         modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(items) {
               awaitEachGesture {
                  val firstDown = awaitFirstDown(requireUnconsumed = false)
                  println("pointerInput: start at ${firstDown.position}")
                  var pointerCount: Int
                  var longPressJob: Job? = null
                  var totalChange = Offset.Zero

                  val global = mapTouchInput(firstDown.position).round()
                  val touchedItem = findItem(items, global)
                  if (touchedItem != null) {
                     println("pointerInput: found $touchedItem")
                     longPressJob = scope.launch {
                        delay(viewConfig.longPressTimeoutMillis)
                        println("pointerInput: long press detected")
                        dragItemId = touchedItem.id
                        dragItemOffset = touchedItem.offset.toOffset()
                     }
                  }

                  do {
                     val event = awaitPointerEvent()
                     pointerCount =
                        event.changes.filter { it.pressed }.size// drag the item// pan the board

                     // Finish item drag if multi-touch starts
                     if (pointerCount >= 2) {
                        longPressJob?.cancel()

                        // Finish item drag if multi-touch starts
                        if (dragItemId != null) {
                           onMoveItem(dragItemId!!, dragItemOffset.round())
                           dragItemId = null
                        }

                        val centroid = event.calculateCentroid()
                        val newZoom =
                           (event.calculateZoom() * canvasZoom).coerceIn(0.05f, 5f)
                        val scaleChange = newZoom / canvasZoom
                        canvasOffset =
                           (canvasOffset - centroid) * scaleChange + centroid + event.calculatePan()
                        canvasZoom = newZoom
                     } else if (pointerCount == 1) {
                        val panChange = event.calculatePan()

                        if (dragItemId == null) {
                           // pan the board
                           totalChange += panChange
                           if (totalChange.getDistance() > viewConfig.touchSlop) {
                              longPressJob?.cancel()
                           }
                           canvasOffset += panChange
                        } else {
                           // drag the item
                           val dpChange = with(density) {
                              Offset(
                                 panChange.x.toDp().value,
                                 panChange.y.toDp().value
                              )
                           }
                           dragItemOffset += (dpChange / canvasZoom)
                        }
                     }
                  } while (pointerCount > 0)
                  longPressJob?.cancel()
                  println("pointerInput: finished")

                  // When drag ends, apply final position
                  if (dragItemId != null) {
                     onMoveItem(dragItemId!!, dragItemOffset.round())
                  }

                  dragItemId = null
                  dragItemOffset = Offset.Zero
               }
            }
      ) {
         // infinite board
         Box(
            modifier = Modifier
               .fillMaxSize()
               .graphicsLayer {
                  transformOrigin = TransformOrigin(0f, 0f)
                  scaleX = canvasZoom
                  scaleY = canvasZoom
                  translationX = canvasOffset.x
                  translationY = canvasOffset.y
               }
         ) {
            AbsoluteBox {
               visibleItems.forEach { item ->
                  val isDragging = dragItemId == item.id
                  val offset = if (dragItemId == item.id) dragItemOffset else item.offset.toOffset()
                  NoteView(
                     item = item,
                     modifier = Modifier
                        .absolutePosition(offset.x.dp, offset.y.dp)
                        .size(item.size.width.dp, item.size.height.dp)
                        .then(
                           if (isDragging) Modifier.zIndex(Float.MAX_VALUE) else Modifier
                        ),
                     showElevated = isDragging
                  )
               }
            }
         }

         // control overlay
         Row(
            modifier = Modifier
               .align(Alignment.TopEnd)
               .safeDrawingPadding()
               .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
         ) {
            IconButton(onClick = onSmartArrange) {
               Icon(Icons.Default.SmartToy, contentDescription = "Smart Arrange")
            }

            if (canvasZoom != 1f) {
               AssistChip(
                  onClick = { canvasZoom = 1f },
                  label = { Text("${(canvasZoom * 100).toInt()}%") },
                  colors = SuggestionChipDefaults.suggestionChipColors().copy(
                     containerColor = MaterialTheme.colorScheme.surface,
                     trailingIconContentColor = MaterialTheme.colorScheme.onSurface,

                  ),
                  border = AssistChipDefaults.assistChipBorder(true, borderColor = MaterialTheme.colorScheme.surfaceVariant),
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
               border = AssistChipDefaults.assistChipBorder(true, borderColor = MaterialTheme.colorScheme.surfaceVariant),
            )
         }
      }
   }
}