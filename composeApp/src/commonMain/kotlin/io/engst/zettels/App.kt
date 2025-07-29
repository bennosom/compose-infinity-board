package io.engst.zettels

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import kotlin.math.sqrt

@Composable
fun App() {
   MaterialTheme {
      WhiteboardDemo()
//        InfiniteWhiteboard()
   }
}

@Composable
fun WhiteboardDemo() {
   var items by remember {
      val size = 999
      val columns = sqrt(size.toFloat()).toInt() + 1
      mutableStateOf(
         List(size) {
            NoteItem(
               id = it.toString(),
               label = "Item $it",
               offset = Offset(
                  (it % columns) * 200f,
                  (it / columns) * 200f
               ).round(),
               size = IntSize(100, 100)
            )
         }
      )
   }

   Whiteboard(
      items = items,
      onMoveItem = { id, newOffset ->
         items = items.map { if (it.id == id) it.copy(offset = newOffset) else it }
      },
      modifier = Modifier.fillMaxSize()
   )
}
