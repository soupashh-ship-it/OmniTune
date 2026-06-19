/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitune.app.R
import com.omnitune.app.playback.EqualizerBand
import com.omnitune.app.playback.EqualizerPresets
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    onApplyBands: (List<EqualizerBand>) -> Unit,
    initialBands: List<EqualizerBand> = EqualizerPresets.FLAT.bands,
) {
    var bands by remember { mutableStateOf(initialBands) }
    var selectedPreset by remember { mutableStateOf(EqualizerPresets.FLAT.name) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(OmniColors.GlassSurface)
            ) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = OmniColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Equalizer", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OmniColors.TextPrimary)
        }

        // Preset chips
        Text("Presets", fontSize = 13.sp, color = OmniColors.TextPrimary.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EqualizerPresets.all) { preset ->
                val isSelected = preset.name == selectedPreset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) OmniColors.Primary else OmniColors.GlassSurface)
                        .clickable {
                            selectedPreset = preset.name
                            bands = preset.bands
                            onApplyBands(preset.bands)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(preset.name, fontSize = 13.sp, color = if (isSelected) Color.White else OmniColors.TextPrimary)
                }
            }
        }

        // Band sliders
        Text("Custom", fontSize = 13.sp, color = OmniColors.TextPrimary.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        bands.forEachIndexed { i, band ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (band.centerFrequencyHz >= 1000) "${band.centerFrequencyHz / 1000}kHz" else "${band.centerFrequencyHz}Hz",
                    fontSize = 12.sp,
                    color = OmniColors.TextPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.width(40.dp)
                )
                Slider(
                    value = band.gainDb,
                    onValueChange = { newGain ->
                        bands = bands.toMutableList().also { it[i] = band.copy(gainDb = newGain) }
                        selectedPreset = "Custom"
                        onApplyBands(bands)
                    },
                    valueRange = -15f..15f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = OmniColors.Primary,
                        activeTrackColor = OmniColors.Primary,
                        inactiveTrackColor = OmniColors.GlassSurface
                    )
                )
                Text(
                    text = "${if (band.gainDb >= 0) "+" else ""}${"%.1f".format(band.gainDb)}dB",
                    fontSize = 12.sp,
                    color = OmniColors.TextPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.width(48.dp)
                )
            }
        }
    }
}
