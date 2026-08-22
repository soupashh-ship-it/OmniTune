package com.omnitune.app.ui.screens.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBehaviorRegistryTest {
    @Test
    fun everyDirectPreferenceBackedSettingsControlHasOneRegistryOwner() {
        val settingsDirectory = projectRoot().resolve("app/src/main/kotlin/com/omnitune/app/ui/screens/settings")
        val directPreferenceKeys = settingsDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "SettingsComponents.kt" }
            .flatMap { file ->
                Regex("rememberPreference\\((?:com\\.omnitune\\.app\\.constants\\.)?([A-Za-z][A-Za-z0-9_]*)")
                    .findAll(file.readText())
                    .map { it.groupValues[1] }
            }
            .toSet()
        val registered = SettingsBehaviorRegistry.entries.map { it.keyName }.toSet()

        assertEquals(
            "A visible preference control needs a registry entry with a runtime owner before it can ship.",
            directPreferenceKeys,
            registered.intersect(directPreferenceKeys),
        )
    }

    @Test
    fun registryHasNoConflictingOrUnownedSettings() {
        val entries = SettingsBehaviorRegistry.entries
        assertEquals(entries.size, entries.map { it.keyName }.toSet().size)
        assertTrue(entries.all { it.screen.isNotBlank() && it.defaultValue.isNotBlank() })
        assertTrue(entries.all { it.runtimeOwner.isNotBlank() && it.effect.isNotBlank() })
    }

    @Test
    fun everyNamedPreferenceKeyReferencedBySettingsIsClassified() {
        val settingsDirectory = projectRoot().resolve("app/src/main/kotlin/com/omnitune/app/ui/screens/settings")
        val referencedKeys = settingsDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "SettingsComponents.kt" }
            .flatMap { file ->
                Regex("\\b([A-Za-z][A-Za-z0-9_]*Key)\\b")
                    .findAll(file.readText())
                    .map { it.groupValues[1] }
            }
            .filterNot { it == "refreshKey" }
            .toSet()
        val registered = SettingsBehaviorRegistry.entries.map { it.keyName }.toSet()

        assertEquals(
            "A named Settings preference key must be behavior-classified before it can ship.",
            referencedKeys,
            registered.intersect(referencedKeys),
        )
    }

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
        .firstOrNull { it.resolve("settings.gradle.kts").isFile }
        ?: error("Could not locate the OmniTune project root")
}
