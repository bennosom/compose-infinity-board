package io.engst.zettels

// LazyAbsoluteLayout auf Basis von foundation LazyLayout
// - misst/platziert nur sichtbare Items (Viewport + Overscan)
// - Positionen in Pixel (IntOffset) für maximale Performance
// - Größe des Inhalts = Bounding‑Box aller Items
// Hinweis: LazyLayout ist @ExperimentalFoundationApi

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyAbsoluteLayout(
   modifier: Modifier = Modifier,
   itemCount: Int,
   key: (Int) -> Any = { it },
   positionPx: (Int) -> IntOffset,     // absolute Position in px
   viewportOrigin: IntOffset,          // aktueller Scroll/Pan‑Offset in px
   viewportSize: IntSize,              // sichtbare Fläche in px
   overscanPx: Int = 256,
   onContentSizeChanged: ((IntSize) -> Unit)? = null,
   item: @Composable (Int) -> Unit
) {
   // sehr leichter Size‑Cache (verbessert Culling ohne messen)
   val widthCache = remember(itemCount) { IntArray(itemCount) { 1 } }
   val heightCache = remember(itemCount) { IntArray(itemCount) { 1 } }
   var cachedW by remember { mutableIntStateOf(0) }
   var cachedH by remember { mutableIntStateOf(0) }

   LazyLayout(
      modifier = modifier,
      itemProvider = {
         object : LazyLayoutItemProvider {
            override val itemCount: Int get() = itemCount

            override fun getKey(index: Int): Any = key(index)

            @Composable
            override fun Item(index: Int, key: Any) {
               return item(index)
            }
         }
      },
      measurePolicy = { constraints ->
         // Viewport + Overscan
         val vx = viewportOrigin.x
         val vy = viewportOrigin.y
         val vw = viewportSize.width
         val vh = viewportSize.height
         val left   = vx - overscanPx
         val top    = vy - overscanPx
         val right  = vx + vw + overscanPx
         val bottom = vy + vh + overscanPx

         // Kandidaten (nur Indizes, die Box schneiden)
         val visibleIdx = mutableListOf<Int>()
         var contentMaxRight = 0
         var contentMaxBottom = 0

         var i = 0
         while (i < itemCount) {
            val p = positionPx(i)
            val w = widthCache[i]
            val h = heightCache[i]
            if (p.x < right && p.y < bottom && p.x + w > left && p.y + h > top) {
               visibleIdx.add(i)
            }
            val r = p.x + w
            val b = p.y + h
            if (r > contentMaxRight) contentMaxRight = r
            if (b > contentMaxBottom) contentMaxBottom = b
            i++
         }

         val unbounded = Constraints(
            minWidth = 0, maxWidth = Constraints.Infinity,
            minHeight = 0, maxHeight = Constraints.Infinity
         )

         // Messen nur sichtbarer Items
         val measured = ArrayList<Measured>(visibleIdx.size)
         var k = 0
         while (k < visibleIdx.size) {
            val index = visibleIdx[k]
            val placeable = measure(index, unbounded) // LazyLayout‑Messung
//            val w = placeable.width
//            val h = placeable.height
//            if (w != widthCache[index])  widthCache[index] = w
//            if (h != heightCache[index]) heightCache[index] = h
//
//            val pos = positionPx(index)
//            val r = pos.x + w
//            val b = pos.y + h
//            if (r > contentMaxRight) contentMaxRight = r
//            if (b > contentMaxBottom) contentMaxBottom = b
//
//            measured.add(Measured(index, pos, placeable))
            k++
         }

         if (contentMaxRight != cachedW || contentMaxBottom != cachedH) {
            cachedW = contentMaxRight
            cachedH = contentMaxBottom
            onContentSizeChanged?.invoke(IntSize(cachedW, cachedH))
         }

         // Layoutgröße = Viewport (Container/Scroll bestimmt sichtbaren Bereich)
         val lw = constraints.constrainWidth(viewportSize.width)
         val lh = constraints.constrainHeight(viewportSize.height)

         layout(lw, lh) {
            var m = 0
            while (m < measured.size) {
               val it = measured[m]
               // relativ zum Viewport platzieren
               it.placeable.placeRelative(it.pos.x - vx, it.pos.y - vy)
               m++
            }
         }
      }
   )
}

private data class Measured(
   val index: Int,
   val pos: IntOffset,
   val placeable: Placeable
)

/* --- Minimalbeispiel --- */
/*
@Composable
fun LazyAbsoluteDemo() {
    val cols = 200
    val count = 40_000
    val itemSizePx = 24
    val gap = 8
    val pos: (Int) -> IntOffset = { i ->
        val x = (i % cols) * (itemSizePx + gap)
        val y = (i / cols) * (itemSizePx + gap)
        IntOffset(x, y)
    }

    var origin by remember { mutableStateOf(IntOffset.Zero) }
    var content by remember { mutableStateOf(IntSize.Zero) }
    val viewport = IntSize(1080, 1920)

    Box(
        Modifier
            .size(viewport.width.dp, viewport.height.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val nx = (origin.x - drag.x.toInt()).coerceIn(0, maxOf(0, content.width - viewport.width))
                    val ny = (origin.y - drag.y.toInt()).coerceIn(0, maxOf(0, content.height - viewport.height))
                    origin = IntOffset(nx, ny)
                }
            }
    ) {
        LazyAbsoluteLayout(
            itemCount = count,
            key = { it },
            positionPx = pos,
            viewportOrigin = origin,
            viewportSize = viewport,
            overscanPx = 256,
            onContentSizeChanged = { content = it }
        ) { i ->
            Box(
                Modifier
                    .size(24.dp)
                    .background(if ((i and 1) == 0) Color.Gray else Color.LightGray)
            )
        }
    }
}
*/
