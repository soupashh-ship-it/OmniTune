package com.omnitune.app.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.omnitune.app.R
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

private const val YTM_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var isCompleting by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { navController.popBackStack() }) {
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
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val currentUrl = url ?: return

                            if (isCompleting) return

                            // Check if we landed on YouTube Music after login
                            if (currentUrl.contains("music.youtube.com") ||
                                (currentUrl.contains("youtube.com") && !currentUrl.contains("accounts.google.com"))) {

                                val cookieManager = CookieManager.getInstance()
                                val cookies = cookieManager.getCookie("https://music.youtube.com")

                                if (cookies != null && cookies.contains("SAPISID")) {
                                    isCompleting = true
                                    Timber.d("LoginScreen: YouTube Music login successful, cookies captured")

                                    Handler(Looper.getMainLooper()).post {
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
                                        navController.popBackStack()
                                    }
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
