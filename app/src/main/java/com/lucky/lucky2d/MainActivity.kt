package com.lucky.lucky2d

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.lucky.lucky2d.utils.CleanAudio2D

class MainActivity : Activity() {
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CleanAudio2D initialization as the very first line
        CleanAudio2D.init(applicationContext)

        val wv = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
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
            clearCache(true)
            addJavascriptInterface(AndroidShareBridge(this@MainActivity), "AndroidShareBridge")
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
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
            }
        }

        webView = wv
        setContentView(wv)
        wv.loadUrl("file:///android_asset/index.html")
    }

    private fun handleExternalUrl(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme == "file" || scheme == "about" || scheme == "data" || scheme == "javascript") {
            return false
        }
        val host = uri.host?.lowercase() ?: ""
        if ((scheme == "http" || scheme == "https") && (host == "localhost" || host == "127.0.0.1")) {
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
        fun shareSms(phoneNumber: String, message: String) {
            activity.runOnUiThread {
                val cleanPhone = phoneNumber.trim()
                val text = message.trim()
                try {
                    val smsUri = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
                    val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                        putExtra("sms_body", text)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${Uri.encode(cleanPhone)}")).apply {
                            putExtra("sms_body", text)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        activity.startActivity(fallback)
                    } catch (_: Exception) {
                        openGenericShare(text, "SMS")
                    }
                }
            }
        }

        @JavascriptInterface
        fun shareViber(phoneNumber: String, message: String) {
            activity.runOnUiThread {
                val text = message.trim()
                copyToClipboard(text)
                try {
                    val viberIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        setPackage("com.viber.voip")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(viberIntent)
                } catch (_: ActivityNotFoundException) {
                    try {
                        val uriIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("viber://forward?text=${Uri.encode(text)}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        activity.startActivity(uriIntent)
                    } catch (_: Exception) {
                        openGenericShare(text, "Viber")
                    }
                } catch (_: Exception) {
                    openGenericShare(text, "Viber")
                }
            }
        }

        @JavascriptInterface
        fun shareMessenger(phoneNumber: String, message: String) {
            activity.runOnUiThread {
                val text = message.trim()
                copyToClipboard(text)
                try {
                    val messengerIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        setPackage("com.facebook.orca")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(messengerIntent)
                } catch (_: ActivityNotFoundException) {
                    try {
                        val liteIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            setPackage("com.facebook.mlite")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        activity.startActivity(liteIntent)
                    } catch (_: ActivityNotFoundException) {
                        try {
                            val uriIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("fb-messenger://share/?link=${Uri.encode(text)}")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            activity.startActivity(uriIntent)
                        } catch (_: Exception) {
                            openGenericShare(text, "Messenger")
                        }
                    } catch (_: Exception) {
                        openGenericShare(text, "Messenger")
                    }
                } catch (_: Exception) {
                    openGenericShare(text, "Messenger")
                }
            }
        }

        private fun copyToClipboard(text: String) {
            try {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
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
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
