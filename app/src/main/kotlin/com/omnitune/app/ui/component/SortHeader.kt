package com.omnitune.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> SortHeader(
    sortType: T,
    sortDescending: Boolean,
    onSortTypeChange: (T) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
    sortTypeText: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.clickable { onSortDescendingChange(!sortDescending) }) {
        Text(text = sortTypeText(sortType), modifier = Modifier.padding(16.dp))
    }
}
