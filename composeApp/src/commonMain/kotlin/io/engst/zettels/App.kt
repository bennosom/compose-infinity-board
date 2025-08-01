package io.engst.zettels

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun AppPreview() {
   App()
}

@Composable
fun App() {
   MaterialTheme {
      WhiteboardDemo()
   }
}

@Composable
fun WhiteboardDemo() {
   var items by remember {
      val initialItems = listOf(
         "Gurke",
         "Zitrone",
         "Steak",
         "Kartoffel",
         "Klopapier",
         "Waschmittel",
         "Brot",
         "Butter",
         "Wurst",
         "Apfel",
      ).mapIndexed { index, description ->
         NoteItem(
            id = index.toString(),  // unique identifier for each item
            description = description,
            offset = Offset(
               (index % 4) * 200f,
               (index / 4) * 200f
            ).round(),
            size = IntSize(100, 100)
         )
      }
      mutableStateOf(initialItems)
   }

   val scope = rememberCoroutineScope()

   Whiteboard(
      items = items,
      onMoveItem = { id, newOffset ->
         items = items.map { if (it.id == id) it.copy(offset = newOffset) else it }
      },
      onSmartArrange = {
         scope.launch {
            val suggestions = OpenAI.arrangeItems(items.map {
               Item(
                  id = it.id,
                  description = it.description,
                  position = Position(it.offset.x, it.offset.y),
                  size = Size(it.size.width, it.size.height)
               )
            })
            println("smart arranged items: $suggestions")

            items = items.map { item ->
               suggestions.find { it.id == item.id }?.let { suggestedItem ->
                  item.copy(
                     offset = IntOffset(
                        suggestedItem.position.x,
                        suggestedItem.position.y
                     )
                  )
               } ?: item
            }
         }
      },
      modifier = Modifier.fillMaxSize()
   )
}
