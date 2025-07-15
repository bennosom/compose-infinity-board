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
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 1.dp,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Text(item.id, modifier = Modifier.padding(12.dp))
    }
}