package io.engst.zettels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MouseWheel(
   onScroll: (offset: Offset, delta: Offset) -> Unit,
   content: @Composable () -> Unit
) {
   Box(
      Modifier
         .fillMaxSize()
         .onPointerEvent(PointerEventType.Scroll) { pointerEvent ->
            pointerEvent.changes.forEach { change ->
               onScroll(change.position, change.scrollDelta)
            }
         }
   ) {
      content()
   }
}