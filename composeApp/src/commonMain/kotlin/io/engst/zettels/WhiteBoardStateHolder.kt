package io.engst.zettels

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

@Immutable
data class WhiteboardState(
   val items: List<NoteItem>,
   val scale: Float = 1f,
   val offset: Offset = Offset.Zero
)

class WhiteBoardStateHolder() {

   companion object {
      const val BOARD_SCALE_MIN = 0.05f
      const val BOARD_SCALE_MAX = 5f
   }

   private val scope = CoroutineScope(SupervisorJob())

   private val initialItems = listOf(
      "Gurke",
      "Zitrone",
      "Apfel",
      "Käse",
      "Wurst",
      "Milch",
      "Eier",
      "Nudeln",
      "Mehl",
      "Paprika",
      "Olivenöl",
      "Gewürze",
      "Bananen",
      "Kekse",
      "Steak"
   ).mapIndexed { index, description ->
      NoteItem(
         id = index.toString(),  // unique identifier for each item
         description = description,
         offset = DpOffset(
            ((index % 5) * 200f - 100).dp,
            ((index / 5) * 200f - 100).dp
         ),
         size = if (index < 6) DpSize(100.dp, 100.dp) else DpSize.Zero
      )
   }
   private val _state = MutableStateFlow(WhiteboardState(initialItems))
   val state: StateFlow<WhiteboardState> = _state.asStateFlow()

   private val _fitToZoom = MutableStateFlow(true)
   val zoomToFit: StateFlow<Boolean> = _fitToZoom.asStateFlow()

   fun transform(scale: Float? = null, offset: Offset? = null) {
      _fitToZoom.update { false }
      _state.update {
         if (scale == null && offset == null) {
            it
         } else {
            println("transform: $scale, $offset")
            it.copy(
               scale = scale ?: it.scale,
               offset = offset ?: it.offset,
            )
         }
      }
   }

   /**
    * Transforms the board to its original size, but keeps the viewport center stable.
    */
   fun zoomReset(viewportSize: IntSize, boardSize: IntSize) {
      println("zoomReset")

      _fitToZoom.update { false }

      if (boardSize == IntSize.Zero || viewportSize == IntSize.Zero) return

      val viewportCenter = viewportSize.center.toOffset()
      val currentScale = _state.value.scale
      val currentOffset = _state.value.offset

      // Find the board-space point at the center of the viewport before reset
      val boardCenterBefore = (viewportCenter - currentOffset) / currentScale

      // After reset, scale is 1, so offset should place the same board point at the center
      val newOffset = viewportCenter - boardCenterBefore * 1f

      transformInternal(scale = 1f, offset = newOffset)
   }

   /**
    * Transforms the board so that its content fits inside the viewport.
    */
   fun zoomToFit(viewportSize: IntSize, boardBounds: IntRect, padding: Int) {
      println("zoomToFit")

      _fitToZoom.update { true }

      if (boardBounds == IntRect.Zero || viewportSize == IntSize.Zero) return

      val oldSize = boardBounds.size.toSize()
      val newSize = IntSize(viewportSize.width - padding * 2, viewportSize.height - padding * 2)

      val scaleToFit = min(newSize.width / oldSize.width, newSize.height / oldSize.height)
         .coerceIn(BOARD_SCALE_MIN, BOARD_SCALE_MAX)

      val scaledSize = oldSize * scaleToFit
      val offsetToFit = Offset(
         x = padding + (newSize.width - scaledSize.width) / 2 - boardBounds.left * scaleToFit,
         y = padding + (newSize.height - scaledSize.height) / 2 - boardBounds.top * scaleToFit
      )

      transformInternal(scale = scaleToFit, offset = offsetToFit)
   }

   /**
    * Follow the given cursor
    *
    * @param cursor Cursors offset in global coords
    * TODO: fix me
    */
   fun zoomToCursor(cursor: IntOffset) {
      println("zoomToCursor: $cursor")

      val oldScale = state.value.scale
      val newScale = 1f
      val scaleChange = newScale / oldScale

      val oldOffset = state.value.offset
      val position = (cursor.toOffset() - state.value.offset) / state.value.scale

      _fitToZoom.update { false }
      transformInternal(
         scale = newScale,
         offset = (oldOffset - position) * scaleChange + position
      )
   }

   fun moveItem(id: String, offset: DpOffset) {
      println("moveItem: $id, $offset")

      _state.update {
         it.copy(items = it.items.map { if (it.id == id) it.copy(offset = offset) else it })
      }
   }

   fun smartArrange() {
      scope.launch {
         val suggestions = OpenAI.arrangeItems(_state.value.items.map {
            Item(
               id = it.id,
               description = it.description,
               position = Position(it.offset.x.value.roundToInt(), it.offset.y.value.roundToInt()),
            )
         })
         println("smart arranged items: $suggestions")

         _state.update {
            it.copy(items = it.items.map { item ->
               suggestions.find { it.id == item.id }?.let { suggestedItem ->
                  item.copy(
                     offset = DpOffset(
                        suggestedItem.position.x.dp,
                        suggestedItem.position.y.dp
                     )
                  )
               } ?: item
            })
         }
      }
   }

   private fun transformInternal(scale: Float? = null, offset: Offset? = null) {
      _state.update {
         if (scale == null && offset == null) {
            it
         } else {
            it.copy(
               scale = scale ?: it.scale,
               offset = offset ?: it.offset,
            )
         }
      }
   }
}