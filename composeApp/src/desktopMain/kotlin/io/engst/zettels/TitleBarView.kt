package io.engst.zettels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Dropdown
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.window.DecoratedWindowScope
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.newFullscreenControls

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalLayoutApi
@Composable
internal fun DecoratedWindowScope.TitleBarView() {

   var theme: IntUiThemes by remember { mutableStateOf(IntUiThemes.Light) }
   val projectColor: Color = if (theme.isLightHeader()) {
      Color(0xFFF5D4C1)
   } else {
      Color(0xFF654B40)
   }
   var current by remember { mutableStateOf(0) }

   TitleBar(Modifier.newFullscreenControls(), gradientStartColor = projectColor) {
      Row(Modifier.align(Alignment.Start)) {
         Dropdown(
            Modifier.height(30.dp),
            menuContent = {
               (0..5).forEach {
                  selectableItem(
                     selected = current == it,
                     onClick = { current = it },
                  ) {
                     Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                     ) {
                        Icon(Icons.Default.Star, null)
                        Text("Entry $it")
                     }
                  }
               }
            },
         ) {
            Row(
               horizontalArrangement = Arrangement.spacedBy(3.dp),
               verticalAlignment = Alignment.CenterVertically,
            ) {
               Row(
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalAlignment = Alignment.CenterVertically,
               ) {
                  Icon(Icons.Default.Star, null)
                  Text("Entry ${it.state}")
               }
            }
         }
      }

      Text(title)

      Row(Modifier.align(Alignment.End)) {
         Tooltip(
            tooltip = {
               when (theme) {
                  IntUiThemes.Light -> Text("Switch to light theme with light header")
                  IntUiThemes.LightWithLightHeader -> Text("Switch to dark theme")
                  IntUiThemes.Dark,
                  IntUiThemes.System -> Text("Switch to light theme")
               }
            }
         ) {
            IconButton(
               {
                  theme =
                     when (theme) {
                        IntUiThemes.Light -> IntUiThemes.LightWithLightHeader
                        IntUiThemes.LightWithLightHeader -> IntUiThemes.Dark
                        IntUiThemes.Dark,
                        IntUiThemes.System -> IntUiThemes.Light
                     }
               },
               Modifier.size(40.dp).padding(5.dp),
            ) {
               when (theme) {
                  IntUiThemes.Light ->
                     Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = "Light"
                     )

                  IntUiThemes.LightWithLightHeader ->
                     Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Light with light header"
                     )

                  IntUiThemes.Dark ->
                     Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "Dark"
                     )

                  IntUiThemes.System ->
                     Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = "System"
                     )
               }
            }
         }
      }
   }
}