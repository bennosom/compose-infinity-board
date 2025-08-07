package io.engst.zettels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ControlOverlay(
   state: WhiteboardState,
   userScale: Float,
   onZoomToFit: () -> Unit,
   onZoomReset: () -> Unit,
   onSmartArrange: () -> Unit,
   modifier: Modifier = Modifier
) {
   CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
      Box(modifier = modifier.safeDrawingPadding().padding(6.dp)) {
         // top controls
         Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
         ) {
            IconButton(onClick = onSmartArrange) {
               Icon(Icons.Default.SmartToy, contentDescription = "Arrange")
            }

            Spacer(Modifier.weight(1f))

            val scale = state.scale + userScale
            TransientView(scale) {
               Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            }

            AssistChip(
               onClick = {
                  when {
                     scale == 1f -> onZoomToFit()
                     else -> onZoomReset()
                  }
               },
               label = {
                  when {
                     scale == 1f -> Text("Zoom To Fit")
                     else -> Text("Zoom Reset")
                  }
               },
               colors = SuggestionChipDefaults.suggestionChipColors().copy(
                  containerColor = MaterialTheme.colorScheme.surface,
                  trailingIconContentColor = MaterialTheme.colorScheme.onSurface
               ),
               border = AssistChipDefaults.assistChipBorder(
                  true,
                  borderColor = MaterialTheme.colorScheme.surfaceVariant
               ),
            )
         }
      }
   }
}