package com.omnitune.app.utils

import android.content.pm.PackageManager
import com.omnitune.app.models.LogoVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconAliasRegistryTest {
    @Test
    fun `every logo variant resolves to a launcher alias`() {
        LogoVariant.entries.forEach { variant ->
            val suffix = LauncherIconAliasRegistry.aliasSuffixFor(variant)

            assertTrue(
                "Missing launcher alias for ${variant.name}",
                LauncherIconAliasRegistry.allAliasSuffixes.contains(suffix),
            )
        }
    }

    @Test
    fun `enabled states enable exactly one alias`() {
        LogoVariant.entries.forEach { variant ->
            val states = LauncherIconAliasRegistry.enabledStatesFor(variant)

            assertEquals(LauncherIconAliasRegistry.allAliasSuffixes.size, states.size)
            assertEquals(
                "Wrong enabled alias count for ${variant.name}",
                1,
                states.values.count { it == PackageManager.COMPONENT_ENABLED_STATE_ENABLED },
            )
            assertEquals(
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                states[LauncherIconAliasRegistry.aliasSuffixFor(variant)],
            )
        }
    }

    @Test
    fun `classic variants use the default launcher alias`() {
        assertEquals(
            LauncherIconAliasRegistry.DefaultAlias,
            LauncherIconAliasRegistry.aliasSuffixFor(LogoVariant.DEFAULT),
        )
        assertEquals(
            LauncherIconAliasRegistry.DefaultAlias,
            LauncherIconAliasRegistry.aliasSuffixFor(LogoVariant.CLASSIC),
        )
    }

    @Test
    fun `component class names use the app package prefix`() {
        assertEquals(
            "com.omnitune.app.launcher.PulseLauncherAlias",
            LauncherIconAliasRegistry.componentClassName(
                "com.omnitune.app",
                LauncherIconAliasRegistry.aliasSuffixFor(LogoVariant.PULSE),
            ),
        )
    }
}
