package io.engst.zettels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp

@Composable
fun NoteView(
   item: NoteItem,
   onCursorChanged: (Rect) -> Unit,
   modifier: Modifier = Modifier,
   showElevated: Boolean = false,
) {
   Surface(
      modifier = modifier,
      shape = RoundedCornerShape(6.dp),
      tonalElevation = 0.dp,
      shadowElevation = if (showElevated) 4.dp else 1.dp
   ) {
      var text by remember { mutableStateOf(item.description) }

      Box {
         var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

         BasicTextField(
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.padding(12.dp)
               .onFocusChanged {
//                  println("${item.description} onFocusChanged: hasFocus=${it.hasFocus} isFocused=${it.isFocused}")
               }
               .onFocusedBoundsChanged {
//                  println("${item.description} onFocusBoundsChanged: boundsInRoot=${it?.boundsInRoot()}")
               }
               .onGloballyPositioned { coords ->
                  // Sobald wir das Layout haben, holen wir uns den Cursor-Offset
                  layoutResult?.let { result ->
                     val cursorRect = result.getCursorRect(text.length)
                     val windowOffset = coords.localToWindow(cursorRect.topLeft)
//                     println("${item.description} CursorPos x=${windowOffset.x}, y=${windowOffset.y}")
                  }
               },
            onTextLayout = {
               layoutResult = it

               val cursorRect = it.getCursorRect(text.length)
//               println("${item.description} cursorRect=${cursorRect}")

               // TODO: map cursorRect to item-relative coords
//               onCursorChanged(cursorRect)
            },
            value = text,
            onValueChange = { text = it },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
         )
      }
   }
}