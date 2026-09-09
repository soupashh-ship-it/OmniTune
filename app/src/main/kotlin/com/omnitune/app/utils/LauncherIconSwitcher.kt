/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.omnitune.app.models.LogoVariant

object LauncherIconAliasRegistry {
    const val DefaultAlias = ".launcher.DefaultLauncherAlias"

    private val variantAliases = mapOf(
        LogoVariant.DEFAULT to DefaultAlias,
        LogoVariant.CLASSIC to DefaultAlias,
        LogoVariant.PULSE to ".launcher.PulseLauncherAlias",
        LogoVariant.PULSE_APP_ICON to ".launcher.PulseAppIconLauncherAlias",
        LogoVariant.PULSE_MONO to ".launcher.PulseMonoLauncherAlias",
        LogoVariant.PULSE_LIGHT to ".launcher.PulseLightLauncherAlias",
        LogoVariant.PULSE_TONE to ".launcher.PulseToneLauncherAlias",
        LogoVariant.RESONANCE to ".launcher.ResonanceLauncherAlias",
        LogoVariant.RESONANCE_APP_ICON to ".launcher.ResonanceAppIconLauncherAlias",
        LogoVariant.RESONANCE_MONO to ".launcher.ResonanceMonoLauncherAlias",
        LogoVariant.RESONANCE_LIGHT to ".launcher.ResonanceLightLauncherAlias",
        LogoVariant.RESONANCE_TONE to ".launcher.ResonanceToneLauncherAlias",
        LogoVariant.AETHER to ".launcher.AetherLauncherAlias",
        LogoVariant.AETHER_APP_ICON to ".launcher.AetherAppIconLauncherAlias",
        LogoVariant.AETHER_MONO to ".launcher.AetherMonoLauncherAlias",
        LogoVariant.AETHER_LIGHT to ".launcher.AetherLightLauncherAlias",
        LogoVariant.AETHER_TONE to ".launcher.AetherToneLauncherAlias",
    )

    val allAliasSuffixes: List<String> = variantAliases.values.distinct()

    fun aliasSuffixFor(variant: LogoVariant): String =
        variantAliases[variant] ?: DefaultAlias

    fun componentClassName(packageName: String, aliasSuffix: String): String =
        "$packageName$aliasSuffix"

    fun enabledStatesFor(selected: LogoVariant): Map<String, Int> {
        val selectedAlias = aliasSuffixFor(selected)
        return allAliasSuffixes.associateWith { alias ->
            if (alias == selectedAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        }
    }
}

class LauncherIconSwitcher(
    private val context: Context,
) {
    fun apply(variant: LogoVariant) {
        val packageManager = context.packageManager
        LauncherIconAliasRegistry.enabledStatesFor(variant).forEach { (aliasSuffix, state) ->
            packageManager.setComponentEnabledSetting(
                ComponentName(
                    context.packageName,
                    LauncherIconAliasRegistry.componentClassName(context.packageName, aliasSuffix),
                ),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
