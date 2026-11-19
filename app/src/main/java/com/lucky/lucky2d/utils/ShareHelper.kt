package com.lucky.lucky2d.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ShareHelper {

    fun copyToClipboard(context: Context, text: String, label: String = "Lucky2D") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        } catch (_: Exception) {
            // Ignore clipboard errors
        }
    }

    fun readFromClipboard(context: Context): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).coerceToText(context)?.toString() ?: ""
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun shareSms(context: Context, phone: String, text: String) {
        val cleanPhone = phone.trim()
        val body = text.trim()
        try {
            val smsUri = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
            val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("sms:${Uri.encode(cleanPhone)}")
                ).apply {
                    putExtra("sms_body", body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
                openGenericShare(context, body, "SMS")
            }
        }
    }

    fun shareViber(context: Context, phone: String, text: String) {
        val body = text.trim()
        copyToClipboard(context, body, "Lucky2D Slip")
        try {
            val viberIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
                setPackage("com.viber.voip")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viberIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                val uriIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("viber://forward?text=${Uri.encode(body)}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(uriIntent)
            } catch (_: Exception) {
                openGenericShare(context, body, "Viber")
            }
        } catch (_: Exception) {
            openGenericShare(context, body, "Viber")
        }
    }

    fun shareMessenger(context: Context, phone: String, text: String) {
        val body = text.trim()
        copyToClipboard(context, body, "Lucky2D Slip")
        try {
            val messengerIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
                setPackage("com.facebook.orca")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(messengerIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                val liteIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, body)
                    setPackage("com.facebook.mlite")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(liteIntent)
            } catch (_: ActivityNotFoundException) {
                try {
                    val uriIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("fb-messenger://share/?link=${Uri.encode(body)}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(uriIntent)
                } catch (_: Exception) {
                    openGenericShare(context, body, "Messenger")
                }
            } catch (_: Exception) {
                openGenericShare(context, body, "Messenger")
            }
        } catch (_: Exception) {
            openGenericShare(context, body, "Messenger")
        }
    }

    fun openGenericShare(context: Context, text: String, title: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "$title App မတွေ့ပါ", Toast.LENGTH_SHORT).show()
        }
    }
}
