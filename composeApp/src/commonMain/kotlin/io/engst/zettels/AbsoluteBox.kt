package io.engst.zettels

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlin.time.measureTime

/**
 * Layout that has no own bounds and only min-constraints for its children.
 * All children are placed and sized absolutely by Modifier.absolutePositionAndSize
 *
 * @param onMeasured Callback after children have been measured (everything in pixels)
 */
@Composable
fun AbsoluteBox(
   modifier: Modifier = Modifier,
   onMeasured: (layoutBounds: IntRect, childBounds: Array<IntRect>) -> Unit,
   content: @Composable AbsoluteBoxScope.() -> Unit
) {
   Layout(
      content = { AbsoluteBoxScopeInstance.content() },
      modifier = modifier,
      measurePolicy = { measurables, constraints ->
         val childConstraints = Constraints(
            minWidth = 50.dp.roundToPx(),
            maxWidth = Constraints.Infinity,
            minHeight = 50.dp.roundToPx(),
            maxHeight = Constraints.Infinity
         )
         println("Measuring: ${measurables.size} with constraints $childConstraints")

         val count = measurables.size
         val childBounds = arrayOfNulls<IntRect>(count)
         val placeables = arrayOfNulls<Placeable>(count)

         var i = 0
         var maxLeft = 0
         var maxTop = 0
         var maxRight = 0
         var maxBottom = 0
         var measurable: Measurable
         var placeable: Placeable
         var parentData: DpRect
         var rect: IntRect

         measureTime {
            while (i < count) {
               measurable = measurables[i]

               placeable = measurable.measure(childConstraints)
               placeables[i] = placeable

               parentData = measurable.parentData as? DpRect ?: run {
                  println("Warning: parentData is null for measurable $i")
                  DpRect(0.dp, 0.dp, 0.dp, 0.dp)
               }
               rect = IntRect(
                  left = parentData.left.roundToPx(),
                  top = parentData.top.roundToPx(),
                  right = parentData.left.roundToPx() + placeable.measuredWidth,
                  bottom = parentData.top.roundToPx() + placeable.measuredHeight
               )
               childBounds[i] = rect

               if (rect.left < maxLeft) maxLeft = rect.left
               if (rect.top < maxTop) maxTop = rect.top
               if (rect.right > maxRight) maxRight = rect.right
               if (rect.bottom > maxBottom) maxBottom = rect.bottom

               i++
            }
         }.also {
            println("AbsoluteBox: measure pass took ${it.inWholeMilliseconds}ms")
         }

         val layoutBounds = IntRect(maxLeft, maxTop, maxRight, maxBottom)

         measureTime {
            onMeasured(layoutBounds, childBounds as Array<IntRect>)
         }.also {
            println("AbsoluteBox: onMeasure callback took ${it.inWholeMilliseconds}ms")
         }

         /**
          * Actually using `layoutBounds.width, layoutBounds.height` as layout size would be
          * correct, but if the content is larger than its parent Compose will center it inside
          * its parent. Therefore we need to size it with given max-constraints.
          */
         layout(constraints.maxWidth, constraints.maxHeight) {
            measureTime {
               var j = 0
               while (j < count) {
                  // TODO: use item zindex to determine stacking order
                  // TODO: check placeWithLayer
                  placeables[j]!!.place(
                     x = childBounds[j]!!.left,
                     y = childBounds[j]!!.top,
                     zIndex = 0f
                  )
                  j++
               }
            }.also {
               println("AbsoluteBox: layout pass took ${it.inWholeMilliseconds}ms")
            }
         }
      }
   )
}

@Stable
interface AbsoluteBoxScope {
   fun Modifier.absolutePositionAndSize(offset: DpOffset, size: DpSize = DpSize.Zero): Modifier
}

private object AbsoluteBoxScopeInstance : AbsoluteBoxScope {
   override fun Modifier.absolutePositionAndSize(offset: DpOffset, size: DpSize): Modifier =
      this
         .size(size)
         .then(AbsoluteBoxParentDataModifier(offset, size))
}

private data class AbsoluteBoxParentDataModifier(
   val offset: DpOffset,
   val size: DpSize
) : ParentDataModifier {
   override fun Density.modifyParentData(parentData: Any?): Any {
      return DpRect(offset, size)
   }
}
