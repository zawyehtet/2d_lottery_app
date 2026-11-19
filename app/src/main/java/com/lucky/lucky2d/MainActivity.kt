package com.lucky.lucky2d

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import com.lucky.lucky2d.utils.CleanAudio2D
import java.io.ByteArrayInputStream

class MainActivity : Activity() {
    companion object {
        private const val ASSET_HOST = "appassets.androidplatform.net"
        private const val ASSET_BASE_URL = "https://$ASSET_HOST/index.html"
        private const val MAX_RENDERER_RECOVERIES = 3
    }

    private lateinit var rootContainer: FrameLayout
    private var webView: WebView? = null
    private var rendererRecoveryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CleanAudio2D initialization as the very first line
        CleanAudio2D.init(applicationContext)

        rootContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(rootContainer)
        mountWebView(useSoftwareLayer = false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun mountWebView(useSoftwareLayer: Boolean) {
        val wv = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            if (useSoftwareLayer) {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                mediaPlaybackRequiresUserGesture = false
            }
            addJavascriptInterface(AndroidShareBridge(this@MainActivity), "AndroidShareBridge")
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    if (uri.scheme.equals("https", ignoreCase = true) &&
                        uri.host.equals(ASSET_HOST, ignoreCase = true)
                    ) {
                        return loadAssetResponse(uri.path ?: "")
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return false
                    return handleExternalUrl(uri)
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url.isNullOrBlank()) return false
                    return handleExternalUrl(Uri.parse(url))
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        val crashedView = webView
                        if (crashedView != null) {
                            rootContainer.removeView(crashedView)
                            try {
                                crashedView.destroy()
                            } catch (_: Exception) {
                                // Ignore cleanup error on crashed renderer
                            }
                            webView = null
                        }
                        if (rendererRecoveryCount < MAX_RENDERER_RECOVERIES) {
                            rendererRecoveryCount++
                            mountWebView(useSoftwareLayer = true)
                        }
                    }
                    return true
                }
            }
        }

        webView = wv
        rootContainer.removeAllViews()
        rootContainer.addView(wv)
        wv.loadUrl(ASSET_BASE_URL)
    }

    private fun loadAssetResponse(rawPath: String): WebResourceResponse {
        val cleanPath = rawPath.trimStart('/').ifEmpty { "index.html" }
        val headers = mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Cache-Control" to "no-store, no-cache, must-revalidate"
        )
        return try {
            val mimeType = guessMimeType(cleanPath)
            val encoding = if (isTextMimeType(mimeType)) "UTF-8" else null
            val stream = assets.open(cleanPath)
            WebResourceResponse(
                mimeType,
                encoding,
                200,
                "OK",
                headers,
                stream
            )
        } catch (_: Exception) {
            if (!cleanPath.contains('.')) {
                try {
                    val fallbackStream = assets.open("index.html")
                    return WebResourceResponse(
                        "text/html",
                        "UTF-8",
                        200,
                        "OK",
                        headers,
                        fallbackStream
                    )
                } catch (_: Exception) {
                    // Fall through to 404
                }
            }
            WebResourceResponse(
                "text/plain",
                "UTF-8",
                404,
                "Not Found",
                headers,
                ByteArrayInputStream(ByteArray(0))
            )
        }
    }

    private fun isTextMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("text/") ||
            mimeType == "application/javascript" ||
            mimeType == "application/json" ||
            mimeType == "image/svg+xml"
    }

    private fun guessMimeType(path: String): String {
        return when (path.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> "text/html"
            "js", "mjs" -> "application/javascript"
            "css" -> "text/css"
            "json" -> "application/json"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }
    }

    private fun handleExternalUrl(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme == "file" || scheme == "about" || scheme == "data" || scheme == "javascript") {
            return false
        }
        val host = uri.host?.lowercase() ?: ""
        if ((scheme == "http" || scheme == "https") &&
            (host == ASSET_HOST || host == "localhost" || host == "127.0.0.1")
        ) {
            return false
        }

        return try {
            val intent = if (scheme == "intent") {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, uri)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "App ကို ဖွင့်၍မရပါ", Toast.LENGTH_SHORT).show()
            true
        } catch (_: Exception) {
            true
        }
    }

    inner class AndroidShareBridge(private val activity: Activity) {
        @JavascriptInterface
        fun shareSms(phone: String, text: String) {
            activity.runOnUiThread {
                val cleanPhone = phone.trim()
                val body = text.trim()
                try {
                    val smsUri = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
                    val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                        putExtra("sms_body", body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val fallback = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("sms:${Uri.encode(cleanPhone)}")
                        ).apply {
                            putExtra("sms_body", body)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        activity.startActivity(fallback)
                    } catch (_: Exception) {
                        openGenericShare(body, "SMS")
                    }
                }
            }
        }

        @JavascriptInterface
        fun shareViber(phone: String, text: String) {
            activity.runOnUiThread {
                val body = text.trim()
                copyToClipboard(body)
                try {
                    val viberIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, body)
                        setPackage("com.viber.voip")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(viberIntent)
                } catch (_: ActivityNotFoundException) {
                    try {
                        val uriIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("viber://forward?text=${Uri.encode(body)}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        activity.startActivity(uriIntent)
                    } catch (_: Exception) {
                        openGenericShare(body, "Viber")
                    }
                } catch (_: Exception) {
                    openGenericShare(body, "Viber")
                }
            }
        }

        @JavascriptInterface
        fun shareMessenger(phone: String, text: String) {
            activity.runOnUiThread {
                val body = text.trim()
                copyToClipboard(body)
                try {
                    val messengerIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, body)
                        setPackage("com.facebook.orca")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(messengerIntent)
                } catch (_: ActivityNotFoundException) {
                    try {
                        val liteIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, body)
                            setPackage("com.facebook.mlite")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        activity.startActivity(liteIntent)
                    } catch (_: ActivityNotFoundException) {
                        try {
                            val uriIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("fb-messenger://share/?link=${Uri.encode(body)}")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            activity.startActivity(uriIntent)
                        } catch (_: Exception) {
                            openGenericShare(body, "Messenger")
                        }
                    } catch (_: Exception) {
                        openGenericShare(body, "Messenger")
                    }
                } catch (_: Exception) {
                    openGenericShare(body, "Messenger")
                }
            }
        }

        private fun copyToClipboard(text: String) {
            try {
                val clipboard =
                    activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Lucky2D Slip", text))
            } catch (_: Exception) {
                // Ignore clipboard failure
            }
        }

        private fun openGenericShare(text: String, title: String) {
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                val chooser = Intent.createChooser(sendIntent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(chooser)
            } catch (_: Exception) {
                Toast.makeText(activity, "$title App မတွေ့ပါ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        val wv = webView
        if (wv != null) {
            rootContainer.removeView(wv)
            wv.destroy()
            webView = null
        }
        super.onDestroy()
    }
}
