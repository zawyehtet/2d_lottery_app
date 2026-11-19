package com.lucky.lucky2d.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky.lucky2d.model.LayoffRecord
import com.lucky.lucky2d.ui.components.LuckyHeader
import com.lucky.lucky2d.ui.theme.LuckyBg
import com.lucky.lucky2d.ui.theme.LuckyBorder
import com.lucky.lucky2d.ui.theme.LuckyCardSoft
import com.lucky.lucky2d.ui.theme.LuckyDanger
import com.lucky.lucky2d.ui.theme.LuckyGreen
import com.lucky.lucky2d.ui.theme.LuckyTextMuted
import com.lucky.lucky2d.ui.theme.LuckyTextPrimary
import com.lucky.lucky2d.ui.theme.LuckyTextSecondary
import com.lucky.lucky2d.utils.CleanAudio2D
import com.lucky.lucky2d.utils.ShareHelper
import com.lucky.lucky2d.viewmodel.LotteryViewModel
import kotlinx.coroutines.delay

@Composable
fun LayoffHistoryScreen(
    viewModel: LotteryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedRecord by remember { mutableStateOf<LayoffRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<LayoffRecord?>(null) }
    var copiedId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(copiedId) {
        if (copiedId != null) {
            delay(2000)
            copiedId = null
        }
    }

    val sortedRecords = remember(viewModel.layoffs) {
        viewModel.layoffs.sortedByDescending { it.id }
    }

    fun copyLayoff(record: LayoffRecord) {
        CleanAudio2D.playClick(context)
        val sb = StringBuilder()
        sb.appendLine("Lucky2D တင်ကွက်စာရင်း")
        sb.appendLine("အမှတ်: No.${record.recordNo}")
        sb.appendLine("ရက်စွဲ: ${LotteryViewModel.formatDotDate(record.date)} (${record.session})")
        sb.appendLine("အချိန်: ${record.time}")
        sb.appendLine("-----------------------")
        record.items.forEach { sb.appendLine("${it.n}=${it.a}") }
        sb.appendLine("-----------------------")
        sb.append("စုစုပေါင်း: ${record.total}")
        ShareHelper.copyToClipboard(context, sb.toString(), "Lucky2D Layoff")
        copiedId = record.id
        viewModel.showToast("တင်ကွက်စာရင်း ကူးယူပြီးပါပြီ")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        LuckyHeader(
            title = "တင်ကွက် မှတ်တမ်းများ",
            viewModel = viewModel,
            showBack = true,
            onBack = onBack,
            showSessionPill = false
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, LuckyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("တင်ကွက်မှတ်တမ်း စာရင်း", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuckyTextPrimary)
                    Text("စုစုပေါင်း ${sortedRecords.size} ကြိမ်", fontSize = 13.sp, color = LuckyTextMuted)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, LuckyBorder),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyGreen)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("အမှတ်", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.7f))
                        Text("ရက်စွဲ / အချိန်", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.5f))
                        Text("ပမာဏ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        Text("လုပ်ဆောင်ချက်", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }

                    if (sortedRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("တင်ကွက်မှတ်တမ်း မရှိသေးပါ", fontSize = 13.sp, color = LuckyTextMuted)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(sortedRecords, key = { _, item -> item.id }) { idx, item ->
                                val isCopied = copiedId == item.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 1) LuckyCardSoft else Color.White)
                                        .clickable {
                                            CleanAudio2D.playClick(context)
                                            selectedRecord = item
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "No.${item.recordNo}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LuckyTextPrimary,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = "${LotteryViewModel.formatDotDate(item.date)} (${item.session})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LuckyTextPrimary
                                        )
                                        Text(text = item.time, fontSize = 10.sp, color = LuckyTextMuted)
                                    }
                                    Text(
                                        text = "${item.total}",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = LuckyDanger,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { copyLayoff(item) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "ကူးမည်",
                                                tint = LuckyGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                CleanAudio2D.playClick(context)
                                                recordToDelete = item
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "ဖျက်မည်",
                                                tint = LuckyDanger,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFEEF3F1))
                            }
                        }
                    }
                }
            }
        }
    }

    selectedRecord?.let { rec ->
        AlertDialog(
            onDismissRequest = { selectedRecord = null },
            title = {
                Column {
                    Text("တင်ကွက် အသေးစိတ် (No.${rec.recordNo})", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 16.sp)
                    Text("${LotteryViewModel.formatDotDate(rec.date)} (${rec.session})", fontSize = 12.sp, color = LuckyTextMuted)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        itemsIndexed(rec.items) { _, it ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(it.n, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${it.a}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = LuckyDanger, fontSize = 14.sp)
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("စုစုပေါင်း တင်ငွေ:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuckyDanger)
                        Text("${rec.total}", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = LuckyDanger)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { copyLayoff(rec) },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Text("စာသားကူးမည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecord = null }) {
                    Text("ပိတ်မည်", color = LuckyTextMuted)
                }
            }
        )
    }

    recordToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = {
                Text("တင်ကွက်မှတ်တမ်း ဖျက်မည်လား?", fontWeight = FontWeight.Bold, color = LuckyDanger)
            },
            text = {
                Text("ဤတင်ကွက်မှတ်တမ်း No.${rec.recordNo} (${rec.total}) အား ဖျက်ပါက လက်ခံကွက်များသို့ ပြန်လည်ပေါင်းထည့်သွားမည် ဖြစ်သည်။")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLayoffRecord(rec.id)
                        if (selectedRecord?.id == rec.id) selectedRecord = null
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                ) {
                    Text("ဖျက်မည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("မဖျက်ပါ", color = LuckyTextMuted)
                }
            }
        )
    }
}
