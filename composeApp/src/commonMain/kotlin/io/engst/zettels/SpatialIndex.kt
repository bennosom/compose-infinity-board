package io.engst.zettels

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect

typealias Cell = Pair<Int, Int>
typealias TouchTarget = Pair<String, IntRect>

// Spatial index for efficient hit testing
class SpatialIndex {
   companion object {
      private const val gridSize = 1000
   }

   private val grid = mutableMapOf<Cell, MutableSet<TouchTarget>>()

   fun query(point: IntOffset): List<TouchTarget> {
      val cell: Cell = Pair(point.x / gridSize, point.y / gridSize)
      return (grid[cell]?.toList() ?: emptyList()).filter { (_, rect) -> rect.contains(point) }
   }

   fun rebuild(targets: List<TouchTarget>) {
      grid.clear()
      targets.forEach { insert(it) }
   }

   private fun insert(target: TouchTarget) {
      val (_, rect) = target
      getCells(rect).forEach { cell ->
         grid.getOrPut(cell) { mutableSetOf() }.add(target)
      }
   }

   private fun getCells(rect: IntRect): List<Cell> {
      val left = rect.left / gridSize
      val top = rect.top / gridSize
      val right = rect.right / gridSize
      val bottom = rect.bottom / gridSize

      return buildList {
         for (x in left..right) {
            for (y in top..bottom) {
               add(Pair(x, y))
            }
         }
      }
   }
}