package io.engst.zettels

import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

enum class IntUiTheme {
   Light,
   LightWithLightHeader,
   Dark,
   System;

   fun isDark(): Boolean = (if (this == System) fromSystemTheme(currentSystemTheme) else this) == Dark

   fun isLightHeader(): Boolean = this == LightWithLightHeader

   companion object {
      fun fromSystemTheme(systemTheme: SystemTheme): IntUiTheme =
         if (systemTheme == SystemTheme.LIGHT) Light else Dark
   }
}