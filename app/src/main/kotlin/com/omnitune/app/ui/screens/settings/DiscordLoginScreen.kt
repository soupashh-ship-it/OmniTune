/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * OmniTune Discord login
 */

package com.omnitune.app.ui.screens.settings

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
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
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.constants.DiscordTokenKey
import com.omnitune.app.constants.EnableDiscordRPCKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.PreferenceStore
import com.omnitune.app.utils.dataStore

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    var isCompleting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Discord Login",
                        fontWeight = FontWeight.Bold,
                        color = OmniColors.TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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

                    // Clear cookies
                    android.webkit.CookieManager.getInstance().removeAllCookies(null)

                    // JavaScript interface for token retrieval
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onRetrieveToken(token: String) {
                            Handler(Looper.getMainLooper()).post {
                                if (!isCompleting && token.isNotBlank()) {
                                    isCompleting = true
                                    PreferenceStore.launchEdit(context.dataStore) {
                                        this[DiscordTokenKey] = token
                                        this[EnableDiscordRPCKey] = true
                                    }
                                    playerConnection?.restartDiscordPresence()
                                    onLoggedIn()
                                }
                            }
                        }
                    }, "Android")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript(TOKEN_EXTRACTION_SCRIPT, null)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onJsAlert(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: android.webkit.JsResult?,
                        ): Boolean {
                            if (message?.startsWith("DCTOKEN:") == true) {
                                val token = message.removePrefix("DCTOKEN:")
                                Handler(Looper.getMainLooper()).post {
                                    if (!isCompleting && token.isNotBlank()) {
                                        isCompleting = true
                                        PreferenceStore.launchEdit(context.dataStore) {
                                            this[DiscordTokenKey] = token
                                            this[EnableDiscordRPCKey] = true
                                        }
                                        playerConnection?.restartDiscordPresence()
                                        onLoggedIn()
                                    }
                                }
                                result?.confirm()
                                return true
                            }
                            return super.onJsAlert(view, url, message, result)
                        }
                    }

                    loadUrl("https://discord.com/login")
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

private const val TOKEN_EXTRACTION_SCRIPT = """
(function() {
    try {
        var iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        document.body.appendChild(iframe);
        
        var token = null;
        var scripts = document.getElementsByTagName('script');
        for (var i = 0; i < scripts.length; i++) {
            var src = scripts[i].src || '';
            if (src.includes('discord') || src.includes('webpack')) {
                try {
                    var webpackChunk = window.webpackChunkdiscord_app;
                    if (webpackChunk) {
                        webpackChunk.push([[Math.random()], {}, function(r) {
                            for (var m in r.c) {
                                if (r.c[m].exports) {
                                    var mod = r.c[m].exports;
                                    if (mod.default && mod.default.getToken) {
                                        token = mod.default.getToken();
                                    }
                                }
                            }
                        }]);
                    }
                } catch(e) {}
            }
        }
        
        if (token) {
            Android.onRetrieveToken(token);
        }
        
        // Fallback: try localStorage
        try {
            var localToken = localStorage.getItem('token');
            if (localToken) {
                Android.onRetrieveToken(localToken);
            }
        } catch(e) {}
        
        document.body.removeChild(iframe);
    } catch(e) {
        console.error('Token extraction failed:', e);
    }
})();
"""
