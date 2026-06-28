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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitune.app.ui.navigation.OmniTuneMainScreen
import com.omnitune.app.ui.shell.OmniShellBackground
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.content.Context
import androidx.media3.exoplayer.offline.Download
import com.omnitune.app.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.MusicService
import com.omnitune.app.playback.PlayerConnection
import kotlinx.coroutines.flow.map
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.constants.DynamicThemeKey
import com.omnitune.app.utils.dataStore
import androidx.compose.runtime.collectAsState
import com.omnitune.app.ui.screens.SettingsScreen
import com.omnitune.app.utils.reportException
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTuneTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var database: MusicDatabase
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

    private fun startMusicServiceSafely() { try { startService(Intent(this, MusicService::class.java)) } catch (e: Exception) { reportException(e) } }
    private fun bindToMusicService() {
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            android.widget.Toast.makeText(this, "Notifications disabled. Media controls won't show in status bar.", android.widget.Toast.LENGTH_LONG).show()
        }
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
            val dynamicThemeFlow = remember(context) { context.dataStore.data.map { it[DynamicThemeKey] ?: false } }
            val dynamicTheme by dynamicThemeFlow.collectAsState(initial = false)
                
            OmniTuneTheme(dynamicColor = dynamicTheme) {
                CompositionLocalProvider(LocalPlayerConnection provides playerConnection) {
                    OmniShellBackground {
                        OmniTuneMainScreen(database = database)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startMusicServiceSafely()
        bindToMusicService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }
}

