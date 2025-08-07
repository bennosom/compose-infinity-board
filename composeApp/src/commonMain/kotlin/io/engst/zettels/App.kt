package io.engst.zettels

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun AppPreview() {
   App()
}

@Composable
fun App(
   stateHolder: WhiteBoardStateHolder = WhiteBoardStateHolder(),
   isDarkMode: Boolean = isSystemInDarkTheme()
) {
   val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
   MaterialTheme(colorScheme = colorScheme) {

      val density = LocalDensity.current
      val state by stateHolder.state.collectAsStateWithLifecycle()
      val zoomToFit by stateHolder.zoomToFit.collectAsStateWithLifecycle()
      var userScale by remember(state) { mutableStateOf(0f) }
      var userOffset by remember(state) { mutableStateOf(Offset.Zero) }
      var viewportSize by remember { mutableStateOf(IntSize.Zero) }
      var boardBounds by remember { mutableStateOf(IntRect.Zero) }

      LaunchedEffect(viewportSize, boardBounds, zoomToFit) {
         if (zoomToFit) {
            stateHolder.zoomToFit(
               viewportSize = viewportSize,
               boardBounds = boardBounds,
               padding = with(density) { 16.dp.roundToPx() }
            )
         }
      }

      Box(Modifier.fillMaxSize()) {
         Whiteboard(
            state = state,
            onUserTransform = { scale, offset ->
               userScale = scale
               userOffset = offset
            },
            onFinalTransform = { scale, offset ->
               stateHolder.transform(
                  scale = state.scale + scale,
                  offset = state.offset + offset
               )
            },
            onItemMoved = { id, offsetPx ->
               val offset = with(density) { DpOffset(offsetPx.x.toDp(), offsetPx.y.toDp()) }
               stateHolder.moveItem(id, offset)
            },
            onBoundsChanged = { boardBounds = it },
            modifier = Modifier
               .fillMaxSize()
               .onSizeChanged { viewportSize = it }
         )

         ControlOverlay(
            state = state,
            userScale = userScale,
            onZoomReset = {
               stateHolder.zoomReset(
                  viewportSize = viewportSize,
                  boardSize = boardBounds.size
               )
            },
            onZoomToFit = {
               stateHolder.zoomToFit(
                  viewportSize = viewportSize,
                  boardBounds = boardBounds,
                  padding = with(density) { 16.dp.roundToPx() }
               )
            },
            onSmartArrange = { stateHolder.smartArrange() },
            modifier = Modifier.fillMaxSize()
         )

         val density = LocalDensity.current
         Text(
            """
               density=${density.density}
               viewport=$viewportSize 
               boardBounds=$boardBounds
               scale=${state.scale} / $userScale
               offset=${state.offset} / $userOffset
            """.trimIndent(),
            modifier = Modifier.align(Alignment.BottomStart).safeDrawingPadding().padding(6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
         )
      }
   }
}
