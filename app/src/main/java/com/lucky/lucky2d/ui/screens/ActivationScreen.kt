package com.lucky.lucky2d.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky.lucky2d.R
import com.lucky.lucky2d.security.LicenseManager
import com.lucky.lucky2d.ui.theme.LuckyBg
import com.lucky.lucky2d.ui.theme.LuckyBorder
import com.lucky.lucky2d.ui.theme.LuckyDanger
import com.lucky.lucky2d.ui.theme.LuckyGold
import com.lucky.lucky2d.ui.theme.LuckyGreen
import com.lucky.lucky2d.ui.theme.LuckyGreenDark
import com.lucky.lucky2d.ui.theme.LuckySuccess
import com.lucky.lucky2d.ui.theme.LuckyTextMuted
import com.lucky.lucky2d.ui.theme.LuckyTextPrimary
import com.lucky.lucky2d.utils.CleanAudio2D
import com.lucky.lucky2d.utils.ShareHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen(onActivated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deviceId = remember { LicenseManager.getDeviceId(context) }

    var rawKey by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, LuckyBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyGreen)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(LuckyGreenDark)
                            .border(2.dp, LuckyGold, RoundedCornerShape(16.dp))
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_lucky2d_logo),
                            contentDescription = "Lucky2D",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(13.dp))
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Lucky2D",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ဆော့ဝဲလ် အသက်သွင်းခြင်း",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "တစ်ကြိမ်သာ အသက်သွင်းရန် လိုအပ်ပါသည်",
                        color = Color(0xFFA7F3D0),
                        fontSize = 12.sp
                    )
                }

                // Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Device ID Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = LuckyGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "သင့်စက်အမှတ် (Device ID)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuckyTextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = deviceId,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = LuckyGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        CleanAudio2D.playClick(context)
                                        ShareHelper.copyToClipboard(context, deviceId, "Device ID")
                                        copied = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (copied) "ကူးပြီးပြီ" else "ကူးမည်",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "သင့်စက်အမှတ်ကို Admin ထံပေးပို့ပြီး အသက်သွင်းကုဒ် ရယူပါ",
                            fontSize = 12.sp,
                            color = LuckyTextMuted
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Key Input Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = LuckyGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "အသက်သွင်းကုဒ် ထည့်သွင်းရန်",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LuckyTextPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        CleanAudio2D.playClick(context)
                                        val pasted = ShareHelper.readFromClipboard(context)
                                        if (pasted.isNotBlank()) {
                                            rawKey = LicenseManager.normalizeKeyInput(pasted)
                                            errorMessage = null
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = LuckyGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "PASTE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LuckyGreen
                                )
                            }
                        }

                        val formattedKey = LicenseManager.formatKeyWithDashes(rawKey)

                        OutlinedTextField(
                            value = formattedKey,
                            onValueChange = { input ->
                                rawKey = LicenseManager.normalizeKeyInput(input)
                                errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    text = "XXXX-XXXX-XXXX",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            isError = errorMessage != null,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                letterSpacing = 2.sp,
                                color = if (errorMessage != null) LuckyDanger else LuckyTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuckyGreen,
                                unfocusedBorderColor = LuckyGreen,
                                errorBorderColor = LuckyDanger,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = LuckyDanger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Activate Button (NO Key Generator button)
                    Button(
                        onClick = {
                            val normalized = LicenseManager.normalizeKeyInput(rawKey)
                            if (normalized.length < 12) {
                                CleanAudio2D.playClick(context)
                                errorMessage = "သော့ကုဒ် မှားယွင်းနေပါသည်၊ မှန်ကန်သော သော့ကုဒ် ထည့်သွင်းပါ"
                                return@Button
                            }
                            val ok = LicenseManager.verifyAndSaveLicense(context, normalized)
                            if (ok) {
                                CleanAudio2D.playSuccess(context)
                                showSuccessDialog = true
                                scope.launch {
                                    delay(1100)
                                    showSuccessDialog = false
                                    onActivated()
                                }
                            } else {
                                CleanAudio2D.playClick(context)
                                errorMessage = "သော့ကုဒ် မှားယွင်းနေပါသည်၊ မှန်ကန်သော သော့ကုဒ် ထည့်သွင်းပါ"
                            }
                        },
                        enabled = rawKey.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckyGreen,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "စစ်ဆေးပြီး စတင်အသုံးပြုမည်",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Power by Lcky2d3d ShweKhinMyay",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LuckyTextMuted
                    )
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = LuckySuccess,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "အသက်သွင်းမှု အောင်မြင်ပါသည်",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = "သင့်စက်အတွက် လိုင်စင်အတည်ပြုပြီးပါပြီ။ မင်္ဂလာရှိသော နေ့ရက်ဖြစ်ပါစေ။",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
