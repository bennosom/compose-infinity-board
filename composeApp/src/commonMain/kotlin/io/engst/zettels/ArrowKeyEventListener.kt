package io.engst.zettels

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.node.ModifierNodeElement

fun Modifier.onArrowKeyEvent(onTransform: (Offset) -> Unit) =
   this then KeyboardTransformElement(onTransform)

private data class KeyboardTransformElement(var onTransform: (Offset) -> Unit) :
   ModifierNodeElement<KeyboardTransformNode>() {
   override fun create(): KeyboardTransformNode = KeyboardTransformNode(onTransform)

   override fun update(node: KeyboardTransformNode) {
      node.onTransform = onTransform
   }
}

private class KeyboardTransformNode(
   var onTransform: (Offset) -> Unit
) : KeyInputModifierNode, Modifier.Node() {
   override fun onKeyEvent(event: KeyEvent): Boolean {
      if (event.type != KeyEventType.KeyDown) return false
      when (event.key) {
         Key.DirectionLeft -> onTransform(Offset(-10f, 0f))

         Key.DirectionUp -> onTransform(Offset(0f, -10f))

         Key.DirectionRight -> onTransform(Offset(10f, 0f))

         Key.DirectionDown -> onTransform(Offset(0f, 10f))

         else -> return false
      }
      return true
   }

   override fun onPreKeyEvent(event: KeyEvent): Boolean {
      return false
   }
}