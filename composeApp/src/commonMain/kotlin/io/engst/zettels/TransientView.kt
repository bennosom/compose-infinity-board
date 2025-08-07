package io.engst.zettels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun <T> TransientView(
   value: T,
   duration: Duration = 1.seconds,
   content: @Composable () -> Unit
) {
   var visible by remember { mutableStateOf(false) }

   LaunchedEffect(value) {
      visible = true
      delay(duration)
      visible = false
   }

   AnimatedVisibility(
      visible = visible,
      enter = fadeIn(),
      exit = fadeOut()
   ) {
      content()
   }
}