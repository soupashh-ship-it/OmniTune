package com.omnitune.app

import com.omnitune.app.utils.LauncherIconAliasRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidManifestContractTest {
    private val manifestText: String
        get() = File("src/main/AndroidManifest.xml").readText()

    private val backupRulesText: String
        get() = File("src/main/res/xml/backup_rules.xml").readText()

    private val dataExtractionRulesText: String
        get() = File("src/main/res/xml/data_extraction_rules.xml").readText()

    @Test
    fun mainActivityDeclaresPictureInPictureSupport() {
        assertTrue(manifestText.contains("""android:name=".MainActivity""""))
        assertTrue(manifestText.contains("""android:supportsPictureInPicture="true""""))
        assertTrue(manifestText.contains("""android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize""""))
    }

    @Test
    fun pipReceiverIsDeclaredButNotExported() {
        assertTrue(manifestText.contains("""android:name=".pip.PipActionReceiver""""))
        assertTrue(manifestText.contains("""android:exported="false""""))
        assertFalse(manifestText.contains("com.omnitune.app.action.PLAY_PAUSE"))
    }

    @Test
    fun mediaServiceKeepsMedia3LibraryAction() {
        val mediaServiceBlock = Regex(
            """<service\s+[^>]*android:name="\.playback\.MusicService"[\s\S]*?</service>""",
        ).find(manifestText)?.value.orEmpty()

        assertTrue(mediaServiceBlock.contains("""android:exported="true""""))
        assertTrue(mediaServiceBlock.contains("""androidx.media3.session.MediaLibraryService"""))
        assertTrue(mediaServiceBlock.contains("""android.media.browse.MediaBrowserService"""))
        assertTrue(mediaServiceBlock.contains("""android.media.action.MEDIA_PLAY_FROM_SEARCH"""))
        assertFalse(mediaServiceBlock.contains("""androidx.media3.session.MediaSessionService"""))
    }

    @Test
    fun manifestDeclaresAndroidAutoMediaSupport() {
        assertTrue(manifestText.contains("""android:name="com.google.android.gms.car.application""""))
        assertTrue(manifestText.contains("""android:resource="@xml/automotive_app_desc""""))
    }

    @Test
    fun mainActivityDeclaresNarrowYouTubeViewFilters() {
        assertTrue(manifestText.contains("""android:name="android.intent.action.VIEW""""))
        assertTrue(manifestText.contains("""android:name="android.intent.category.BROWSABLE""""))
        assertTrue(manifestText.contains("""android:scheme="https""""))
        assertEquals(
            5,
            Regex("""<intent-filter android:autoVerify="false">[\s\S]*?android:scheme="https"""")
                .findAll(manifestText)
                .count(),
        )
        assertTrue(manifestText.contains("""android:host="youtube.com""""))
        assertTrue(manifestText.contains("""android:host="www.youtube.com""""))
        assertTrue(manifestText.contains("""android:host="m.youtube.com""""))
        assertTrue(manifestText.contains("""android:host="music.youtube.com""""))
        assertTrue(manifestText.contains("""android:host="youtu.be""""))
        assertFalse(manifestText.contains("""android:host="*.youtube.com""""))
        assertFalse(manifestText.contains("""android:scheme="http""""))
    }

    @Test
    fun mainActivityDeclaresAudioOpenWithFilters() {
        assertTrue(manifestText.contains("""android:mimeType="audio/*""""))
        assertTrue(manifestText.contains("""android:scheme="content""""))
        assertTrue(manifestText.contains("""android:scheme="file""""))
    }

    @Test
    fun packageVisibilityIsNarrowAndBootPermissionIsNotDeclared() {
        assertTrue(manifestText.contains("""<queries>"""))
        assertTrue(manifestText.contains("""android:scheme="upi""""))
        assertFalse(manifestText.contains("""android.permission.QUERY_ALL_PACKAGES"""))
        assertFalse(manifestText.contains("""android.permission.RECEIVE_BOOT_COMPLETED"""))
    }

    @Test
    fun launcherAliasesOwnMainLauncherEntry() {
        val mainActivityBlock = Regex(
            """<activity\s+[^>]*android:name="\.MainActivity"[\s\S]*?</activity>""",
        ).find(manifestText)?.value.orEmpty()

        assertFalse(mainActivityBlock.contains("""android.intent.action.MAIN"""))
        assertFalse(mainActivityBlock.contains("""android.intent.category.LAUNCHER"""))

        LauncherIconAliasRegistry.allAliasSuffixes.forEach { suffix ->
            assertTrue(
                "Missing launcher alias $suffix",
                manifestText.contains("""android:name="$suffix""""),
            )
        }
        assertEquals(
            LauncherIconAliasRegistry.allAliasSuffixes.size,
            Regex("""android:name="android.intent.action.MAIN"""").findAll(manifestText).count(),
        )
        assertTrue(manifestText.contains("""android:name=".launcher.DefaultLauncherAlias""""))
        assertTrue(manifestText.contains("""android:enabled="true""""))
        assertTrue(manifestText.contains("""android:icon="@mipmap/ic_launcher_pulse""""))
        assertTrue(manifestText.contains("""android:icon="@mipmap/ic_launcher_resonance""""))
        assertTrue(manifestText.contains("""android:icon="@mipmap/ic_launcher_aether""""))
    }

    @Test
    fun backupRulesDefensivelyExcludeSecretStores() {
        listOf(backupRulesText, dataExtractionRulesText).forEach { rules ->
            assertTrue(rules.contains("""domain="database""""))
            assertTrue(rules.contains("""domain="file""""))
            assertTrue(rules.contains("""path="datastore/""""))
            assertTrue(rules.contains("""domain="sharedpref""""))
            assertTrue(rules.contains("""domain="root""""))
            assertTrue(rules.contains("""path="app_webview/""""))
        }
    }
}
