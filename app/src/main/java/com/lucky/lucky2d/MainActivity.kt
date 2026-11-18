package com.lucky.lucky2d

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.lucky.lucky2d.utils.CleanAudio2D
import java.io.IOException
import java.net.URLDecoder

class MainActivity : Activity() {

    private lateinit var webView: WebView

    companion object {
        private const val ASSET_HOST = "appassets.androidplatform.net"
        private const val START_URL = "https://$ASSET_HOST/index.html"
        private const val PKG_VIBER = "com.viber.voip"
        private const val PKG_MESSENGER = "com.facebook.orca"
    }

    inner class AndroidShareBridge(private val activity: Activity) {

        private fun copyToClipboard(text: String) {
            try {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Lucky2D", text))
            } catch (_: Exception) {
                // Ignore clipboard exceptions on restricted devices
            }
        }

        @JavascriptInterface
        fun shareSms(phone: String, text: String) {
            val cleanPhone = phone.trim()
            activity.runOnUiThread {
                try {
                    val sendToIntent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("smsto:${Uri.encode(cleanPhone)}")
                    ).apply {
                        putExtra("sms_body", text)
                    }
                    activity.startActivity(sendToIntent)
                } catch (_: ActivityNotFoundException) {
                    try {
                        val fallbackIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("sms:${Uri.encode(cleanPhone)}?body=${Uri.encode(text)}")
                        ).apply {
                            putExtra("sms_body", text)
                        }
                        activity.startActivity(fallbackIntent)
                    } catch (_: Exception) {
                        Toast.makeText(activity, "SMS App ကို ဖွင့်မရပါ", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(activity, "SMS App ကို ဖွင့်မရပါ", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun shareViber(text: String) {
            activity.runOnUiThread {
                copyToClipboard(text)
                try {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        setPackage(PKG_VIBER)
                    }
                    if (sendIntent.resolveActivity(activity.packageManager) != null) {
                        activity.startActivity(sendIntent)
                        return@runOnUiThread
                    }
                } catch (_: Exception) {
                    // Fallback to package launch below
                }

                try {
                    val launchIntent = activity.packageManager.getLaunchIntentForPackage(PKG_VIBER)
                    if (launchIntent != null) {
                        activity.startActivity(launchIntent)
                        return@runOnUiThread
                    }
                } catch (_: Exception) {
                    // Ignore and show toast below
                }

                Toast.makeText(activity, "Viber ထည့်သွင်းထားခြင်း မရှိပါ", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun shareMessenger(text: String) {
            activity.runOnUiThread {
                copyToClipboard(text)
                try {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        setPackage(PKG_MESSENGER)
                    }
                    if (sendIntent.resolveActivity(activity.packageManager) != null) {
                        activity.startActivity(sendIntent)
                        return@runOnUiThread
                    }
                } catch (_: Exception) {
                    // Fallback to package launch below
                }

                try {
                    val launchIntent = activity.packageManager.getLaunchIntentForPackage(PKG_MESSENGER)
                    if (launchIntent != null) {
                        activity.startActivity(launchIntent)
                        return@runOnUiThread
                    }
                } catch (_: Exception) {
                    // Ignore and show toast below
                }

                Toast.makeText(activity, "Messenger ထည့်သွင်းထားခြင်း မရှိပါ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CleanAudio2D initialization as the very first line
        CleanAudio2D.init(applicationContext)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.parseColor("#235347")
        }

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#F4F7F6"))
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }

        // Clear any stale cached HTML/JS/CSS before loading bundled assets
        webView.clearCache(true)
        webView.clearHistory()

        webView.addJavascriptInterface(AndroidShareBridge(this), "AndroidShare")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
                if (uri.host != ASSET_HOST) {
                    return super.shouldInterceptRequest(view, request)
                }

                var assetPath = (uri.path ?: "").removePrefix("/")
                if (assetPath.isEmpty()) {
                    assetPath = "index.html"
                }

                val stream = try {
                    assets.open(assetPath)
                } catch (_: IOException) {
                    try {
                        assets.open("public/$assetPath")
                    } catch (_: IOException) {
                        if (!assetPath.contains('.')) {
                            try {
                                assets.open("index.html")
                            } catch (_: IOException) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }

                if (stream == null) {
                    return super.shouldInterceptRequest(view, request)
                }

                val mimeType = resolveMimeType(assetPath)
                val encoding = if (isTextMime(mimeType)) "UTF-8" else null
                val headers = mapOf(
                    "Cache-Control" to "no-store, no-cache, must-revalidate, max-age=0",
                    "Pragma" to "no-cache",
                    "Expires" to "0"
                )
                return WebResourceResponse(mimeType, encoding, 200, "OK", headers, stream)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase() ?: return false

                if (scheme == "https" && uri.host == ASSET_HOST) {
                    return false
                }

                if (scheme in setOf("sms", "smsto", "viber", "fb-messenger", "tel", "mailto", "intent")) {
                    launchExternalUri(uri)
                    return true
                }

                if (scheme == "http" || scheme == "https") {
                    launchExternalUri(uri)
                    return true
                }

                return false
            }
        }

        setContentView(webView)
        webView.loadUrl(START_URL)
    }

    private fun launchExternalUri(uri: Uri) {
        val scheme = uri.scheme?.lowercase() ?: ""
        try {
            if (scheme == "intent") {
                val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                startActivity(intent)
                return
            }

            if (scheme == "sms" || scheme == "smsto") {
                val rawPart = uri.schemeSpecificPart ?: ""
                val phone = rawPart.substringBefore("?")
                val body = extractQueryParam(uri.encodedQuery, "body")

                val sendToUri = Uri.parse("smsto:${Uri.encode(phone)}")
                val sendToIntent = Intent(Intent.ACTION_SENDTO, sendToUri).apply {
                    if (!body.isNullOrEmpty()) {
                        putExtra("sms_body", body)
                    }
                }
                try {
                    startActivity(sendToIntent)
                    return
                } catch (_: ActivityNotFoundException) {
                    val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        if (!body.isNullOrEmpty()) {
                            putExtra("sms_body", body)
                        }
                    }
                    startActivity(viewIntent)
                    return
                }
            }

            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            val msg = when (scheme) {
                "viber" -> "Viber ထည့်သွင်းထားခြင်း မရှိပါ"
                "fb-messenger" -> "Messenger ထည့်သွင်းထားခြင်း မရှိပါ"
                else -> "သက်ဆိုင်ရာ App ကို ဖွင့်မရပါ"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "လင့်ခ် ဖွင့်ရာတွင် အဆင်မပြေပါ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractQueryParam(encodedQuery: String?, key: String): String? {
        if (encodedQuery.isNullOrEmpty()) return null
        for (pair in encodedQuery.split("&")) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val k = pair.substring(0, idx)
                if (k == key) {
                    return try {
                        URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                    } catch (_: Exception) {
                        pair.substring(idx + 1)
                    }
                }
            }
        }
        return null
    }

    private fun resolveMimeType(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".html") || lower.endsWith(".htm") -> "text/html"
            lower.endsWith(".js") || lower.endsWith(".mjs") -> "application/javascript"
            lower.endsWith(".css") -> "text/css"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".ico") -> "image/x-icon"
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".ogg") -> "audio/ogg"
            lower.endsWith(".wav") -> "audio/wav"
            lower.endsWith(".ttf") -> "font/ttf"
            lower.endsWith(".woff") -> "font/woff"
            lower.endsWith(".woff2") -> "font/woff2"
            else -> "application/octet-stream"
        }
    }

    private fun isTextMime(mime: String): Boolean {
        return mime.startsWith("text/") ||
            mime == "application/javascript" ||
            mime == "application/json" ||
            mime == "image/svg+xml"
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
        }
    }

    override fun onPause() {
        if (::webView.isInitialized) {
            webView.onPause()
        }
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
