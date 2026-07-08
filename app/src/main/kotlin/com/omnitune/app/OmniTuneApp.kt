/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import androidx.datastore.preferences.core.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade
import com.omnitune.app.constants.ContentCountryKey
import com.omnitune.app.constants.ContentLanguageKey
import com.omnitune.app.constants.CustomThemeColorKey
import com.omnitune.app.constants.DataSyncIdKey
import com.omnitune.app.constants.InnerTubeCookieKey
import com.omnitune.app.constants.LastFMSessionKey
import com.omnitune.app.constants.MaxImageCacheSizeKey
import com.omnitune.app.constants.PoTokenGvsKey
import com.omnitune.app.constants.PoTokenKey
import com.omnitune.app.constants.PoTokenPlayerKey
import com.omnitune.app.constants.ProxyEnabledKey
import com.omnitune.app.constants.ProxyTypeKey
import com.omnitune.app.constants.ProxyUrlKey
import com.omnitune.app.constants.RandomThemeOnStartupKey
import com.omnitune.app.constants.SYSTEM_DEFAULT
import com.omnitune.app.constants.SmartTrimmerKey
import com.omnitune.app.constants.StreamBypassProxyKey
import com.omnitune.app.constants.UseLoginForBrowse
import com.omnitune.app.constants.VisitorDataKey
import com.omnitune.app.backup.OfflineDownloadArchive
import com.omnitune.app.extensions.toInetSocketAddress
import com.omnitune.app.extensions.toEnum
import com.omnitune.kugou.KuGou
import com.omnitune.lastfm.LastFM
import com.omnitune.app.ui.player.CanvasArtworkPlaybackCache
import com.omnitune.app.ui.screens.settings.ThemePalettes
import com.omnitune.app.ui.theme.ThemeSeedPalette
import com.omnitune.app.ui.theme.ThemeSeedPaletteCodec
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.reportException
import com.omnitune.app.utils.PreferenceStore
import com.omnitune.app.utils.GlobalLogTree
import com.omnitune.app.utils.forgetAccount
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.YouTubeLocale
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import okio.Path.Companion.toPath
import java.io.PrintWriter
import java.io.StringWriter
import java.net.Proxy
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

@HiltAndroidApp
class OmniTuneApp : Application(), SingletonImageLoader.Factory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var isInitialized = false

    private val didRunImageCacheTrim = AtomicBoolean(false)

    private fun currentProcessName(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            val pid = Process.myPid()
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager?.runningAppProcesses
                ?.firstOrNull { it.pid == pid }
                ?.processName
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (currentProcessName()?.endsWith(":crash") == true) {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
            return
        }

        PreferenceStore.start(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            try {
                Timber.plant(GlobalLogTree())
            } catch (_: Exception) {}
        }

        if (OfflineDownloadArchive.applyPending(this)) {
            Timber.i("Applied pending offline download restore")
        }

        initializeCriticalSync()
        initializeDeferredAsync()
    }

    private fun initializeCriticalSync() {
        CanvasArtworkPlaybackCache.init(this)

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "")

        YouTube.locale = YouTubeLocale(
            gl = locale.country.takeIf { it in CountryCodeToName } ?: "US",
            hl = locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )

        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        LastFM.initialize(
            apiKey = BuildConfig.LASTFM_API_KEY,
            secret = BuildConfig.LASTFM_SECRET
        )
    }

    private fun initializeDeferredAsync() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val prefs = dataStore.data.first()

                prefs[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }?.let { country ->
                    YouTube.locale = YouTube.locale.copy(gl = country)
                }

                prefs[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }?.let { lang ->
                    YouTube.locale = YouTube.locale.copy(hl = lang)
                }

                LastFM.sessionKey = prefs[LastFMSessionKey]

                if (prefs[ProxyEnabledKey] == true) {
                    try {
                        YouTube.proxy = Proxy(
                            prefs[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                            prefs[ProxyUrlKey]!!.toInetSocketAddress()
                        )
                    } catch (e: Exception) {
                        reportException(e)
                    }
                    YouTube.streamBypassProxy = prefs[StreamBypassProxyKey] == true
                }

                if (prefs[UseLoginForBrowse] != false) {
                    YouTube.useLoginForBrowse = true
                }

                // Apply random theme on startup if enabled
                if (prefs[RandomThemeOnStartupKey] == true) {
                    val randomPalette = ThemePalettes.generateRandomPalette()
                    val seedPalette = ThemeSeedPalette(
                        primary = randomPalette.primary,
                        secondary = randomPalette.secondary,
                        tertiary = randomPalette.tertiary,
                        neutral = randomPalette.neutral
                    )
                    val encodedPalette = ThemeSeedPaletteCodec.encodeForPreference(seedPalette, "Random")
                    dataStore.edit { settings ->
                        settings[CustomThemeColorKey] = encodedPalette
                    }
                }

                isInitialized = true
            } catch (e: Exception) {
                Timber.e(e, "Error during deferred initialization")
                reportException(e)
            }
        }

        // Observe visitor data changes
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" }
                        ?: YouTube.visitorData().onFailure {
                            reportException(it)
                        }.getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }

        // Global crash handler
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    val sw = StringWriter()
                    val pw = PrintWriter(sw)
                    throwable.printStackTrace(pw)
                    val stack = sw.toString()
                    
                    // Save to SharedPreferences so we can read it on next launch
                    val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("last_crash", stack).commit()
                    
                    // Write to external files dir as fallback
                    val crashFile = java.io.File(getExternalFilesDir(null), "crash.txt")
                    crashFile.writeText("CRASH LOG:\n$stack")

                } catch (e: Exception) {
                    // Ignore
                } finally {
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(thread, throwable)
                    } else {
                        Process.killProcess(Process.myPid())
                        exitProcess(2)
                    }
                }
            }
        } catch (e: Exception) {
            reportException(e)
        }

        // Observe data sync ID
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.let {
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                }
        }

        // Observe cookie changes
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        Timber.e("Could not parse cookie. Clearing existing cookie. %s", e.message)
                        forgetAccount(this@OmniTuneApp)
                    }
                }
        }

        // Observe PoToken changes
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[PoTokenKey] }
                .distinctUntilChanged()
                .collect { token ->
                    YouTube.poToken = token?.takeIf { it.isNotBlank() }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[PoTokenGvsKey] }
                .distinctUntilChanged()
                .collect { token ->
                    YouTube.poTokenGvs = token?.takeIf { it.isNotBlank() }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[PoTokenPlayerKey] }
                .distinctUntilChanged()
                .collect { token ->
                    YouTube.poTokenPlayer = token?.takeIf { it.isNotBlank() }
                }
        }

        // Observe Last.fm session changes
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[LastFMSessionKey] }
                .distinctUntilChanged()
                .collect { sessionKey ->
                    LastFM.sessionKey = sessionKey
                }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val smartTrimmer = PreferenceStore.get(SmartTrimmerKey) ?: false
        val imageCacheConfig = resolveImageDiskCacheConfig(PreferenceStore.get(MaxImageCacheSizeKey))

        val diskCache = DiskCache.Builder()
            .directory(cacheDir.resolve("coil").absolutePath.toPath())
            .maxSizeBytes(imageCacheConfig.maxSizeBytes)
            .build()

        if (smartTrimmer && imageCacheConfig.policy == CachePolicy.ENABLED &&
            didRunImageCacheTrim.compareAndSet(false, true)
        ) {
            applicationScope.launch(Dispatchers.IO) { trimImageDiskCache(diskCache) }
        }

        return ImageLoader.Builder(this)
            .crossfade(true)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .diskCache(diskCache)
            .diskCachePolicy(imageCacheConfig.policy)
            .build()
    }

    private fun trimImageDiskCache(diskCache: DiskCache) {
        try {
            val limitBytes = diskCache.maxSize
            if (limitBytes <= 0L || limitBytes == Long.MAX_VALUE) return

            val dir = java.io.File(diskCache.directory.toString())
            if (!dir.exists()) return

            val files = dir.walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.lastModified() }
                .toList()

            var currentSize = files.sumOf { it.length() }
            if (currentSize <= limitBytes) return

            for (file in files) {
                if (currentSize <= limitBytes) break
                currentSize -= file.length()
                file.delete()
            }
        } catch (e: Exception) {
            reportException(e)
        }
    }

    private data class ImageCacheConfig(
        val maxSizeBytes: Long,
        val policy: CachePolicy
    )

    private fun resolveImageDiskCacheConfig(maxSizeKey: Int?): ImageCacheConfig {
        val sizeMb = when (maxSizeKey) {
            null -> 128
            0 -> 64
            1 -> 128
            2 -> 256
            3 -> 512
            else -> 128
        }
        return ImageCacheConfig(
            maxSizeBytes = sizeMb * 1024L * 1024L,
            policy = if (sizeMb == 0) CachePolicy.DISABLED else CachePolicy.ENABLED
        )
    }

    companion object {
        lateinit var instance: OmniTuneApp
            private set

        const val COUNTRY_CODES = "US,GB,CA,AU,DE,FR,JP,KR,BR,IN,RU,MX,IT,ES,NL,SE,NO,DK,FI,PL,TR,AR,ZA,EG,NG,KE,IL,AE,SA,TH,VN,ID,PH,MY,SG,HK,TW,CN"
        const val LANGUAGE_CODES = "en,es,fr,de,ja,ko,pt,ru,zh,ar,hi,bn,pa,ta,te,mr,gu,kn,ml,th,vi,id,ms,ja,ko,zh-TW"

        val CountryCodeToName: Set<String> = COUNTRY_CODES.split(",").toSet()
        val LanguageCodeToName: Set<String> = LANGUAGE_CODES.split(",").toSet()
    }
}
