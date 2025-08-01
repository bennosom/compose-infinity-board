package io.engst.zettels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NoteView(
   item: NoteItem,
   modifier: Modifier = Modifier,
   showElevated: Boolean = false,
) {
   Surface(
      shape = RoundedCornerShape(6.dp),
      shadowElevation = if (showElevated) 4.dp else 1.dp,
      tonalElevation = 1.dp,
      modifier = modifier
   ) {
      var text by remember { mutableStateOf(item.description) }
      Column {
         BasicTextField(
            modifier = Modifier.padding(12.dp),
            value = text,
            onValueChange = { text = it }
         )
      }
   }
}