package io.engst.zettels

// Absolut positionierender Container für sehr viele Kinder (z.B. 10_000)
// - O(n)-Messung, ungebundenes Messen der Kinder
// - Größe = Bounding-Box aller Kinder, innerhalb der vom Eltern-Constraint erlaubten Grenzen
// - Zwei Varianten der Positionsangabe: Dp (bequem) und Px (performanter)

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

@Composable
fun AbsoluteBox(
   modifier: Modifier = Modifier,
   content: @Composable AbsoluteLayoutScope.() -> Unit
) {
   Layout(
      content = { AbsoluteLayoutScopeInstance.content() },
      modifier = modifier,
      measurePolicy = { measurables, constraints ->
         // Kinder ungebunden messen
         val unbounded = Constraints(
            minWidth = 0, maxWidth = Constraints.Infinity,
            minHeight = 0, maxHeight = Constraints.Infinity
         )

         val count = measurables.size
         val xs = IntArray(count)
         val ys = IntArray(count)
         val placeables = arrayOfNulls<Placeable>(count)

         var maxRight = 0
         var maxBottom = 0

         var i = 0
         while (i < count) {
            val m: Measurable = measurables[i]
            val pd = (m.parentData as? AbsoluteParentData) ?: AbsoluteParentData(0, 0)
            val p = m.measure(unbounded)
            xs[i] = pd.x
            ys[i] = pd.y

            val right = pd.x + p.width
            val bottom = pd.y + p.height
            if (right > maxRight) maxRight = right
            if (bottom > maxBottom) maxBottom = bottom

            placeables[i] = p
            i++
         }

         val w = if (constraints.hasBoundedWidth)
            maxRight.coerceIn(constraints.minWidth, constraints.maxWidth)
         else
            maxRight.coerceAtLeast(constraints.minWidth)

         val h = if (constraints.hasBoundedHeight)
            maxBottom.coerceIn(constraints.minHeight, constraints.maxHeight)
         else
            maxBottom.coerceAtLeast(constraints.minHeight)

         layout(w, h) {
            var j = 0
            while (j < count) {
               // TODO: use item zindex to determine stacking order
               placeables[j]!!.placeRelative(xs[j], ys[j], zIndex = 0f)
               j++
            }
         }
      }
   )
}

@Stable
interface AbsoluteLayoutScope {
   fun Modifier.absolutePosition(x: Dp, y: Dp): Modifier
   fun Modifier.absolutePositionPx(x: Int, y: Int): Modifier
}

private object AbsoluteLayoutScopeInstance : AbsoluteLayoutScope {
   override fun Modifier.absolutePosition(x: Dp, y: Dp): Modifier =
      this.then(PositionDpParentDataModifier(x, y))

   override fun Modifier.absolutePositionPx(x: Int, y: Int): Modifier =
      this.then(PositionPxParentDataModifier(x, y))
}

@Immutable
private data class AbsoluteParentData(val x: Int, val y: Int)

private data class PositionDpParentDataModifier(
   val x: Dp,
   val y: Dp
) : ParentDataModifier {
   override fun Density.modifyParentData(parentData: Any?): Any {
      return AbsoluteParentData(x.roundToPx(), y.roundToPx())
   }
}

private data class PositionPxParentDataModifier(
   val x: Int,
   val y: Int
) : ParentDataModifier {
   override fun Density.modifyParentData(parentData: Any?): Any {
      return AbsoluteParentData(x, y)
   }
}

/* --- Beispielnutzung --- */
/*
@Composable
fun AbsoluteLayoutDemo() {
    val cols = 100
    val size = 24.dp

    AbsoluteLayout(Modifier) {
        // 10_000 Elemente in 100x100 Grid
        for (i in 0 until 10_000) {
            val x = (i % cols) * 40   // px-Variante ist günstiger, hier nur Dp zur Illustration
            val y = (i / cols) * 40
            androidx.compose.foundation.Box(
                modifier = Modifier
                    .positionPx(x, y)   // für maximale Performance: positionPx verwenden
                    .size(size)
            )
        }
    }
}
*/
