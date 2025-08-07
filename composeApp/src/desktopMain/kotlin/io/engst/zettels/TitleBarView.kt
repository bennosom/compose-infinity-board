package io.engst.zettels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.ThemeDefinition
import org.jetbrains.jewel.ui.component.Dropdown
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.window.DecoratedWindowScope
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.defaultTitleBarStyle
import org.jetbrains.jewel.window.newFullscreenControls
import org.jetbrains.jewel.window.styling.LocalTitleBarStyle

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalLayoutApi
@Composable
internal fun DecoratedWindowScope.TitleBarView(
   theme: IntUiTheme,
   onThemeChange: (IntUiTheme) -> Unit
) {
   TitleBar(
      modifier = Modifier.newFullscreenControls(),
      style = JewelTheme.defaultTitleBarStyle
   ) {
      IconButton(
         onClick = { },
         modifier = Modifier.size(40.dp).padding(5.dp).align(Alignment.Start),
      ) {
         Icon(Icons.Default.Menu, null, tint = LocalTitleBarStyle.current.colors.content)
      }

      Row(
         horizontalArrangement = Arrangement.spacedBy(4.dp),
         verticalAlignment = Alignment.CenterVertically
      ) {
         Text(title, Modifier.padding(end = 4.dp))

         Text("/")

         var current by remember { mutableStateOf(0) }
         Dropdown(
            menuContent = {
               (0..5).forEach {
                  selectableItem(
                     selected = current == it,
                     onClick = { current = it },
                  ) {
                     Text("Board $it")
                  }
               }
            },
         ) {
            Text("Projects")
         }

         Text("/")

         var current2 by remember { mutableStateOf(0) }
         Dropdown(
            menuContent = {
               (0..5).forEach {
                  selectableItem(
                     selected = current2 == it,
                     onClick = { current2 = it },
                  ) {
                     Text("Board $it")
                  }
               }
            },
         ) {
            Text("TopSecret")
         }
      }

      Row(Modifier.align(Alignment.End)) {
         Tooltip(
            tooltip = {
               when (theme) {
                  IntUiTheme.Light -> Text("Switch to light theme with light header")
                  IntUiTheme.LightWithLightHeader -> Text("Switch to dark theme")
                  IntUiTheme.Dark,
                  IntUiTheme.System -> Text("Switch to light theme")
               }
            }
         ) {
            IconButton(
               {
                  val nextTheme = when (theme) {
                     IntUiTheme.Light -> IntUiTheme.LightWithLightHeader
                     IntUiTheme.LightWithLightHeader -> IntUiTheme.Dark
                     IntUiTheme.Dark,
                     IntUiTheme.System -> IntUiTheme.Light
                  }
                  onThemeChange(nextTheme)
               },
               Modifier.size(40.dp).padding(5.dp),
            ) {
               when (theme) {
                  IntUiTheme.Light ->
                     Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = "Light",
                        tint = LocalTitleBarStyle.current.colors.content
                     )

                  IntUiTheme.LightWithLightHeader ->
                     Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Light with light header",
                        tint = LocalTitleBarStyle.current.colors.content
                     )

                  IntUiTheme.Dark ->
                     Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "Dark",
                        tint = LocalTitleBarStyle.current.colors.content
                     )

                  IntUiTheme.System ->
                     Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = "System",
                        tint = LocalTitleBarStyle.current.colors.content
                     )
               }
            }
         }
      }
   }
}