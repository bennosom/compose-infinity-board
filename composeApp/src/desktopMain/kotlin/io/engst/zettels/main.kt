package io.engst.zettels

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

   var theme: IntUiTheme by remember { mutableStateOf(IntUiTheme.System) }
   val textStyle = JewelTheme.createDefaultTextStyle()
   val themeDefinition =
      if (theme.isDark()) {
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
      )
   ) {
      DecoratedWindow(
         onCloseRequest = ::exitApplication,
         title = "Whiteboard",
      ) {
         TitleBarView(
            theme = theme,
            onThemeChange = {
               theme = it
            }
         )

         val stateHolder = remember { WhiteBoardStateHolder() }
         MouseWheel(
            onScroll = { position, change, ctrlPressed ->
               println("onScroll: position=$position change=$change, ctrl=$ctrlPressed")
               if (ctrlPressed) {
                  val oldScale = stateHolder.state.value.scale
                  val newScale = (oldScale - change.y * 0.1f).coerceIn(0.05f, 5f)
                  if (newScale == oldScale) return@MouseWheel

                  val scaleChange = newScale / oldScale

                  val oldOffset = stateHolder.state.value.offset
                  stateHolder.transform(
                     scale = newScale,
                     offset = (oldOffset - position) * scaleChange + position + change
                  )
               } else {
                  val oldOffset = stateHolder.state.value.offset
                  stateHolder.transform(
                     offset = oldOffset.copy(
                        x = oldOffset.x - change.x * 100f,
                        y = oldOffset.y - change.y * 100f
                     )
                  )
               }
            }
         ) {
            App(stateHolder = stateHolder, isDarkMode = theme.isDark())
         }
      }
   }
}
