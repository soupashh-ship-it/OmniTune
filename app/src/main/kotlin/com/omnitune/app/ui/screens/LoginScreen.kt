package com.omnitune.app.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.omnitune.app.R
import com.omnitune.app.auth.LoginWebViewPolicy
import com.omnitune.app.constants.AccountChannelHandleKey
import com.omnitune.app.constants.AccountEmailKey
import com.omnitune.app.constants.AccountNameKey
import com.omnitune.app.constants.InnerTubeCookieKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.PreferenceStore
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

private const val YTM_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val screenActive = remember { AtomicBoolean(true) }
    val completionStarted = remember { AtomicBoolean(false) }

    fun closeLogin() {
        screenActive.set(false)
        webViewRef.value?.stopLoading()
        navController.popBackStack()
    }

    DisposableEffect(Unit) {
        onDispose {
            screenActive.set(false)
            completionStarted.set(true)
            webViewRef.value?.run {
                stopLoading()
                webViewClient = WebViewClient()
                removeAllViews()
                destroy()
            }
            webViewRef.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sign in to YouTube Music",
                        fontWeight = FontWeight.Bold,
                        color = OmniColors.TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::closeLogin) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = OmniColors.TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = OmniColors.TextPrimary,
                ),
            )
        },
        containerColor = OmniColors.OmniBackgroundBase,
    ) { padding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef.value = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val targetUrl = request?.url?.toString()
                            val allowed = LoginWebViewPolicy.isAllowedNavigation(targetUrl)
                            if (!allowed) {
                                Timber.w("LoginScreen: blocked unexpected WebView navigation during sign-in")
                            }
                            return !allowed
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val currentUrl = url ?: return

                            if (!screenActive.get() || completionStarted.get()) return

                            val cookieManager = CookieManager.getInstance()
                            val cookies = cookieManager.collectYouTubeCookies()
                            if (LoginWebViewPolicy.shouldCompleteLogin(currentUrl, cookies) &&
                                completionStarted.compareAndSet(false, true)
                            ) {
                                Timber.d("LoginScreen: YouTube Music login successful, cookies captured")

                                Handler(Looper.getMainLooper()).post {
                                    if (!screenActive.get()) return@post
                                    PreferenceStore.launchEdit(context.dataStore) {
                                        this[InnerTubeCookieKey] = SecurePreferenceCipher.encrypt(cookies)
                                    }
                                    com.omnitune.innertube.YouTube.cookie = cookies
                                    CoroutineScope(Dispatchers.IO).launch {
                                        com.omnitune.innertube.YouTube.accountInfo().getOrNull()?.let { account ->
                                            PreferenceStore.launchEdit(context.dataStore) {
                                                this[AccountNameKey] = account.name
                                                this[AccountEmailKey] = account.email.orEmpty()
                                                this[AccountChannelHandleKey] = account.channelHandle.orEmpty()
                                            }
                                        }
                                    }
                                    closeLogin()
                                }
                            }
                        }
                    }

                    loadUrl(YTM_LOGIN_URL)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

private fun CookieManager.collectYouTubeCookies(): String =
    listOf(
        "https://music.youtube.com",
        "https://www.youtube.com",
        "https://youtube.com",
    ).mapNotNull { url -> getCookie(url)?.takeIf { it.isNotBlank() } }
        .joinToString("; ")
