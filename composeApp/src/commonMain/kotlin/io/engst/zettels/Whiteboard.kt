package io.engst.zettels

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Whiteboard(
   state: WhiteboardState,
   onUserTransform: (scale: Float, offset: Offset) -> Unit,
   onFinalTransform: (scale: Float, offset: Offset) -> Unit,
   onItemMoved: (id: String, newOffset: Offset) -> Unit,
   onBoundsChanged: (IntRect) -> Unit,
   modifier: Modifier = Modifier
) {
   val density = LocalDensity.current
   val spatialIndex = remember { SpatialIndex() }

   var dragScale by remember(state) { mutableStateOf(0f) }
   var dragOffset by remember(state) { mutableStateOf(Offset.Zero) }

   var dragItemId by remember { mutableStateOf<String?>(null) }
   var dragItemOffset by remember { mutableStateOf(Offset.Zero) }

   /**
    * Maps the given point from viewport space to the Board space.
    */
   fun mapToBoardSpace(point: Offset): Offset {
      return ((point - state.offset) / state.scale).also {
         println("mapToBoardSpace: $point -> $it")
      }
   }

   /**
    * Query targets at the given point in Board space using the spatial index.
    */
   fun findTouchTarget(point: IntOffset): TouchTarget? {
      val targets = spatialIndex.query(point)
         .also { println("findTouchTarget: candidates for $point: $it") }

      // TODO: only take the one with hightest z-index
      return targets.lastOrNull()
   }

   Surface(
      modifier = modifier,
      color = MaterialTheme.colorScheme.surfaceContainer
   ) {
      val scope = rememberCoroutineScope()
      val viewConfig = LocalViewConfiguration.current
      val focusRequester = remember { FocusRequester() }

      Box(
         modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .fillMaxSize()
            .onArrowKeyEvent { onFinalTransform(dragScale, it) }
            .pointerInput(state) {
               awaitEachGesture {
                  var pointerCount: Int
                  var longPressJob: Job? = null
                  var panTotal = Offset.Zero

                  // detect drags even if one or more fingers are actually touching items
                  val firstDown = awaitFirstDown(pass = PointerEventPass.Initial)
                  val pointAtBoard = mapToBoardSpace(firstDown.position)
                  val target = findTouchTarget(pointAtBoard.round())
                  if (target != null) {
                     println("pointerInput: touching $target")
                     longPressJob = scope.launch {
                        delay(viewConfig.longPressTimeoutMillis)
                        println("pointerInput: long press detected")
                        dragItemId = target.first
                        dragItemOffset = target.second.topLeft.toOffset()
                     }
                  }

                  println("pointerInput: start at ${firstDown.position}")
                  focusRequester.requestFocus() // steel focus from any focused item
                  onUserTransform(dragScale, dragOffset)

                  do {
                     val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                     pointerCount = event.changes.filter { it.pressed }.size

                     if (pointerCount >= 2) {
                        longPressJob?.cancel()

                        // finish item drag if multi-touch starts
                        if (dragItemId != null) {
                           onItemMoved(dragItemId!!, dragItemOffset)
                           dragItemId = null
                        }

                        val oldScale = state.scale + dragScale
                        val newScale = (event.calculateZoom() * oldScale).coerceIn(0.05f, 5f)
                        dragScale = newScale - state.scale

                        val pivot = event.calculateCentroid()
                        val oldOffset = state.offset + dragOffset
                        val newOffset =
                           pivot - (pivot - oldOffset) * (newScale / oldScale) + event.calculatePan()
                        dragOffset = newOffset - state.offset

                        event.changes.forEach { it.consume() }
                     } else if (pointerCount == 1) {
                        val panDelta = event.calculatePan()

                        if (dragItemId == null) { // pan the board
                           panTotal += panDelta
                           if (panTotal.getDistance() > viewConfig.touchSlop) {
                              longPressJob?.cancel()
                           }

                           dragOffset += panDelta
                        } else { // drag the item
                           val scale = state.scale + dragScale
                           dragItemOffset += panDelta / scale
                        }

                        event.changes.forEach { it.consume() }
                     }

                     onUserTransform(dragScale, dragOffset)
                  } while (pointerCount > 0)

                  longPressJob?.cancel()
                  println("pointerInput: finished")

                  // When drag ends, apply final position
                  if (dragItemId != null) {
                     onItemMoved(dragItemId!!, dragItemOffset)
                  }

                  onFinalTransform(dragScale, dragOffset)

                  dragScale = 0f
                  dragOffset = Offset.Zero
                  dragItemId = null
                  dragItemOffset = Offset.Zero
               }
            }
      ) {
         AbsoluteBox(
            modifier = Modifier
               .graphicsLayer {
                  transformOrigin = TransformOrigin(0f, 0f)
                  scaleX = state.scale + dragScale
                  scaleY = state.scale + dragScale
                  translationX = state.offset.x + dragOffset.x
                  translationY = state.offset.y + dragOffset.y
               }
               .border(Dp.Hairline, Color.Magenta),
            onMeasured = { layoutBounds, childBounds ->
               onBoundsChanged(layoutBounds)
               spatialIndex.rebuild(state.items.mapIndexed { index, item ->
                  Pair(item.id, childBounds[index])
               })
            }
         ) {
            state.items.forEach { item ->
               val isDragging = dragItemId == item.id

               NoteView(
                  item = item,
                  modifier = Modifier
                     .absolutePositionAndSize(
                        offset = if (dragItemId == item.id) {
                           with(density) {
                              DpOffset(
                                 dragItemOffset.x.toDp(),
                                 dragItemOffset.y.toDp()
                              )
                           }
                        } else {
                           item.offset
                        },
                        size = item.size
                     )
                     .then(
                        if (isDragging) Modifier.zIndex(Float.MAX_VALUE) else Modifier
                     ),
                  showElevated = isDragging,
                  onCursorChanged = { rect ->
//                        println("noteItem/cursorChange: $rect")
//                        val cursorGlobal = item.offset + rect.topLeft.round()
//                        zoomToCursor(cursorGlobal)
                  }
               )
            }
         }
      }

      LaunchedEffect(Unit) {
         focusRequester.requestFocus() // enable key event listener
      }
   }
}