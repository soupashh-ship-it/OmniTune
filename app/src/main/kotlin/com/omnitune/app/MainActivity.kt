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
import android.util.Log
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.MusicService
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.constants.DynamicThemeKey
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.constants.DarkModeKey
import com.omnitune.app.constants.UseSystemFontKey
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.reportException
import com.omnitune.app.ui.theme.OmniTuneTheme
import com.omnitune.app.ui.theme.DefaultThemeColor
import com.omnitune.app.ui.theme.extractThemeColor
import com.omnitune.app.ui.theme.boostSaturation
import com.omnitune.app.ui.navigation.OmniTuneMainScreen
import com.omnitune.app.ui.shell.OmniShellBackground
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.BitmapImage
import coil3.request.ImageRequest
import coil3.SingletonImageLoader
import coil3.request.allowHardware
import androidx.compose.ui.graphics.toArgb
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var database: MusicDatabase
    @Inject lateinit var downloadUtil: com.omnitune.app.playback.DownloadUtil
    @Inject lateinit var syncUtils: com.omnitune.app.utils.SyncUtils
    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
            val binder = service as? MusicService.MusicBinder ?: return
            if (playerConnection == null) {
                playerConnection = PlayerConnection(this@MainActivity, binder, database, lifecycleScope)
            }
        }
        override fun onServiceDisconnected(name: android.content.ComponentName?) {
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
            prefs.edit().remove("last_crash").commit()
            val scrollView = android.widget.ScrollView(this).apply {
                addView(android.widget.TextView(this@MainActivity).apply {
                    text = "CRASH OCCURRED:\n\n$lastCrash"
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

            val dynamicTheme by remember {
                context.dataStore.data.map { it[DynamicThemeKey] ?: false }
            }.collectAsState(initial = false)

            val pureBlack by remember {
                context.dataStore.data.map { it[PureBlackKey] ?: false }
            }.collectAsState(initial = false)

            val useSystemFont by remember {
                context.dataStore.data.map { it[UseSystemFontKey] ?: false }
            }.collectAsState(initial = false)

            val darkModePref by remember {
                context.dataStore.data.map { it[DarkModeKey] ?: "ON" }
            }.collectAsState(initial = "ON")

            val isDark = isSystemInDarkTheme()
            val darkTheme = remember(darkModePref, isDark) {
                when (darkModePref) {
                    "OFF" -> false
                    "AUTO" -> isDark
                    else -> true
                }
            }

            // Extract theme color from current song artwork via Coil (Velune-style)
            var themeColor by remember { mutableStateOf(DefaultThemeColor) }

            LaunchedEffect(playerConnection, dynamicTheme) {
                val pc = playerConnection
                if (pc == null) {
                    Log.d("OmniTuneColor", "playerConnection is null, using default")
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }

                Log.d("OmniTuneColor", "Starting to collect mediaMetadata flow")
                // Use direct flow.collectLatest (snapshotFlow doesn't track StateFlow.value)
                pc.mediaMetadata.collectLatest { metadata ->
                    Log.d("OmniTuneColor", "Received metadata: " + (metadata?.title ?: "null"))
                    val thumbnailUrl = metadata?.thumbnailUrl
                    Log.d("OmniTuneColor", "Thumbnail URL: " + (thumbnailUrl ?: "null/blank"))
                    if (thumbnailUrl.isNullOrBlank()) {
                        themeColor = DefaultThemeColor
                        return@collectLatest
                    }
                    Log.d("OmniTuneColor", "Loading image from: " + thumbnailUrl)
                    // Run heavy work (image loading + palette extraction) on background thread
                    withContext(kotlinx.coroutines.Dispatchers.Default) {
                        try {
                            val imageLoader = SingletonImageLoader.get(context)
                            val request = ImageRequest.Builder(context)
                                .data(thumbnailUrl)
                                .allowHardware(false)
                                .build()
                            Log.d("OmniTuneColor", "Executing Coil request...")
                            val result = imageLoader.execute(request)
                            Log.d("OmniTuneColor", "Coil result: " + result.image?.let { "success" } ?: "null image")
                            val bitmap = (result.image as? BitmapImage)?.bitmap
                            if (bitmap != null) {
                                Log.d("OmniTuneColor", "Bitmap obtained, extracting theme color...")
                                val extractedColor = bitmap.extractThemeColor()
                                // Boost saturation for more exciting/vibrant colors (Velune-style)
                                val boostedColor = extractedColor.boostSaturation()
                                Log.d("OmniTuneColor", "Extracted color: #" + Integer.toHexString(extractedColor.toArgb()) + " boosted: #" + Integer.toHexString(boostedColor.toArgb()))
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    themeColor = boostedColor
                                    Log.d("OmniTuneColor", "themeColor updated!")
                                }
                            } else {
                                Log.d("OmniTuneColor", "Bitmap was null after loading")
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    themeColor = DefaultThemeColor
                                }
                            }
                        } catch (e: Exception) {
                            Log.d("OmniTuneColor", "Exception: " + e.message)
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
                dynamicColor = dynamicTheme,
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
                    LocalSyncUtils provides syncUtils
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
}
