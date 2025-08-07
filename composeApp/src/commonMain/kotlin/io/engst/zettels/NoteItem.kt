package io.engst.zettels

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize

@Immutable
data class NoteItem(
   val id: String,
   val description: String,
   val offset: DpOffset,
   val size: DpSize = DpSize.Zero
)