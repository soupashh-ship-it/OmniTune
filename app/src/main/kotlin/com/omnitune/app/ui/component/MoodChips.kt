package com.omnitune.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.omnitune.app.ui.screens.MoodChipsSection as OmniMoodChipsSection

@Composable
fun MoodChipsSection(
    selectedMood: String?,
    onMoodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OmniMoodChipsSection(
        selectedMood = selectedMood,
        onMoodSelected = onMoodSelected,
        modifier = modifier
    )
}
