package io.engst.zettels

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

data class NoteItem(val id: String, val label: String, val offset: IntOffset, val size: IntSize)