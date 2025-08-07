package io.engst.zettels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MouseWheel(
   onScroll: (offset: Offset, delta: Offset, ctrlPressed: Boolean) -> Unit,
   content: @Composable () -> Unit
) {
   Box(
      Modifier
         .fillMaxSize()
         .onPointerEvent(PointerEventType.Scroll) { pointerEvent ->
            val ctrlPressed = pointerEvent.keyboardModifiers.isCtrlPressed
            pointerEvent.changes.forEach { change ->
               onScroll(change.position, change.scrollDelta, ctrlPressed)
            }
         }
   ) {
      content()
   }
}
