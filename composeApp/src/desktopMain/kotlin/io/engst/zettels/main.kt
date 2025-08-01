package io.engst.zettels

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.application
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.intui.window.styling.dark
import org.jetbrains.jewel.intui.window.styling.light
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.styling.TitleBarStyle

@OptIn(ExperimentalLayoutApi::class)
fun main() = application {
   val textStyle = JewelTheme.createDefaultTextStyle()
   val themeDefinition =
      if (isSystemInDarkTheme()) {
         JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle)
      } else {
         JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle)
      }

   IntUiTheme(
      theme = themeDefinition,
      styling = ComponentStyling.default().decoratedWindow(
         titleBarStyle = if (isSystemInDarkTheme()) {
            TitleBarStyle.dark()
         } else {
            TitleBarStyle.light()
         }
      ),
   ) {
      DecoratedWindow(
         onCloseRequest = ::exitApplication,
         title = "Whiteboard",
         onKeyEvent = { keyEvent ->
            if (!keyEvent.isAltPressed || keyEvent.type != KeyEventType.KeyDown) return@DecoratedWindow false
            when (keyEvent.key) {
               else -> false
            }
         },
      ) {
         TitleBarView()
         MouseWheel(
            onScroll = { offset, delta ->
               println("onScroll: offset=$offset delta=$delta")
            }
         ) {
            App()
         }
      }
   }
}
