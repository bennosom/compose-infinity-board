package io.engst.zettels

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        Text(item.description, modifier = Modifier.padding(12.dp))
    }
}