/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.content.Context
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.MusicService
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.constants.DynamicSongColorsKey
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.constants.DarkModeKey
import com.omnitune.app.constants.UseSystemFontKey
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.reportException
import com.omnitune.app.ui.theme.OmniTuneTheme
import com.omnitune.app.ui.theme.DefaultThemeColor
import com.omnitune.app.ui.theme.extractThemeColor
import com.omnitune.app.ui.navigation.OmniTuneMainScreen
import com.omnitune.app.ui.shell.OmniShellBackground
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.request.ImageRequest
import coil3.SingletonImageLoader
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.compose.ui.graphics.toArgb
import timber.log.Timber
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var database: MusicDatabase
    @Inject lateinit var downloadUtil: com.omnitune.app.playback.DownloadUtil
    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
            val binder = service as? MusicService.MusicBinder ?: return
            if (playerConnection == null) {
                playerConnection = PlayerConnection(this@MainActivity, binder, database, lifecycleScope)
            }
        }
        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    private fun startMusicServiceSafely() {
        try { startService(Intent(this, MusicService::class.java)) } catch (e: Exception) { reportException(e) }
    }

    private fun bindToMusicService() {
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications disabled. Media controls won't show in status bar.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        startMusicServiceSafely()
        bindToMusicService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        val lastCrash = prefs.getString("last_crash", null)
        if (BuildConfig.DEBUG && lastCrash != null) {
            prefs.edit().remove("last_crash").apply()
            val scrollView = android.widget.ScrollView(this).apply {
                addView(android.widget.TextView(this@MainActivity).apply {
                    text = getString(R.string.debug_crash_occurred, lastCrash)
                    textSize = 12f
                    setPadding(32, 100, 32, 32)
                    setTextColor(android.graphics.Color.RED)
                    setTextIsSelectable(true)
                })
            }
            setContentView(scrollView)
            return
        } else if (lastCrash != null) {
            prefs.edit().remove("last_crash").apply()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current

            val dynamicSongColors by remember {
                context.dataStore.data.map { it[DynamicSongColorsKey] ?: true }
            }.collectAsStateWithLifecycle(initialValue = true)

            val pureBlack by remember {
                context.dataStore.data.map { it[PureBlackKey] ?: false }
            }.collectAsStateWithLifecycle(initialValue = false)

            val useSystemFont by remember {
                context.dataStore.data.map { it[UseSystemFontKey] ?: false }
            }.collectAsStateWithLifecycle(initialValue = false)

            val darkModePref by remember {
                context.dataStore.data.map { it[DarkModeKey] ?: "ON" }
            }.collectAsStateWithLifecycle(initialValue = "ON")

            val isDark = isSystemInDarkTheme()
            val darkTheme = remember(darkModePref, isDark) {
                when (darkModePref) {
                    "OFF" -> false
                    "AUTO" -> isDark
                    else -> true
                }
            }

            // Extract a restrained theme accent from current song artwork.
            var themeColor by remember { mutableStateOf(DefaultThemeColor) }

            LaunchedEffect(playerConnection, dynamicSongColors) {
                val pc = playerConnection
                if (!dynamicSongColors || pc == null) {
                    Timber.tag("OmniTuneColor").d("Dynamic song colors disabled or playerConnection is null, using default")
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }

                Timber.tag("OmniTuneColor").d("Starting to collect mediaMetadata flow")
                // Use direct flow.collectLatest (snapshotFlow doesn't track StateFlow.value)
                pc.mediaMetadata.collectLatest { metadata ->
                    Timber.tag("OmniTuneColor").d("Received metadata: ${metadata?.title ?: "null"}")
                    val thumbnailUrl = metadata?.thumbnailUrl
                    Timber.tag("OmniTuneColor").d("Thumbnail present: ${!thumbnailUrl.isNullOrBlank()}")
                    if (thumbnailUrl.isNullOrBlank()) {
                        themeColor = DefaultThemeColor
                        return@collectLatest
                    }
                    Timber.tag("OmniTuneColor").d("Loading artwork for dynamic colors")
                    // Run heavy work (image loading + palette extraction) on background thread
                    withContext(kotlinx.coroutines.Dispatchers.Default) {
                        try {
                            val imageLoader = SingletonImageLoader.get(context)
                            val request = ImageRequest.Builder(context)
                                .data(thumbnailUrl)
                                .allowHardware(false)
                                .build()
                            Timber.tag("OmniTuneColor").d("Executing Coil request...")
                            val result = imageLoader.execute(request)
                            val imageStatus = if (result.image != null) "success" else "null image"
                            Timber.tag("OmniTuneColor").d("Coil result: $imageStatus")
                            val bitmap = result.image?.toBitmap()
                            if (bitmap != null) {
                                Timber.tag("OmniTuneColor").d("Bitmap obtained, extracting theme color...")
                                val extractedColor = bitmap.extractThemeColor()
                                Timber.tag("OmniTuneColor").d("Extracted color: #${Integer.toHexString(extractedColor.toArgb())}")
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    themeColor = extractedColor
                                    Timber.tag("OmniTuneColor").d("themeColor updated")
                                }
                            } else {
                                Timber.tag("OmniTuneColor").d("Bitmap was null after loading")
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    themeColor = DefaultThemeColor
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag("OmniTuneColor").d(e, "Dynamic color extraction failed")
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                reportException(e)
                                themeColor = DefaultThemeColor
                            }
                        }
                    }
                }
            }

            OmniTuneTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                pureBlack = pureBlack,
                themeColor = themeColor,
                useSystemFont = useSystemFont,
            ) {
                val menuState = androidx.compose.runtime.remember { com.omnitune.app.ui.component.MenuState() }
                androidx.compose.runtime.CompositionLocalProvider(
                    com.omnitune.app.LocalPlayerConnection provides playerConnection,
                    com.omnitune.app.ui.component.LocalMenuState provides menuState,
                    LocalDatabase provides database,
                    LocalDownloadUtil provides downloadUtil,
                ) {
                    com.omnitune.app.ui.shell.OmniShellBackground {
                        com.omnitune.app.ui.navigation.OmniTuneMainScreen(database = database)
                        com.omnitune.app.ui.component.BottomSheetMenu(state = menuState)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try { unbindService(serviceConnection) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        playerConnection?.dispose()
        playerConnection = null
        super.onDestroy()
    }
}
