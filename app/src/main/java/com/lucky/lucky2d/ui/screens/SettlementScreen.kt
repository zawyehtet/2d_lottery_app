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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.lucky.lucky2d.model.SettledRecord
import com.lucky.lucky2d.ui.components.LuckyHeader
import com.lucky.lucky2d.ui.theme.LuckyBg
import com.lucky.lucky2d.ui.theme.LuckyBorder
import com.lucky.lucky2d.ui.theme.LuckyCardSoft
import com.lucky.lucky2d.ui.theme.LuckyDanger
import com.lucky.lucky2d.ui.theme.LuckyGreen
import com.lucky.lucky2d.ui.theme.LuckySuccess
import com.lucky.lucky2d.ui.theme.LuckyTextMuted
import com.lucky.lucky2d.ui.theme.LuckyTextPrimary
import com.lucky.lucky2d.ui.theme.LuckyTextSecondary
import com.lucky.lucky2d.utils.CleanAudio2D
import com.lucky.lucky2d.utils.ShareHelper
import com.lucky.lucky2d.viewmodel.LotteryViewModel

@Composable
fun SettlementScreen(
    viewModel: LotteryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = rounds, 1 = daily
    var selectedRecord by remember { mutableStateOf<SettledRecord?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val settled = viewModel.settled
    val dailySettlements = viewModel.dailySettlements

    val totalRoundsRetained = remember(settled) { settled.sumOf { it.totalRetained } }
    val totalRoundsPayout = remember(settled) { settled.sumOf { it.payout } }
    val totalRoundsProfitLoss = remember(settled) { settled.sumOf { it.profitLoss } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        LuckyHeader(
            title = "ရက်ချုပ် မှတ်တမ်းများ",
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Action Header Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, LuckyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ရက်ချုပ် စာရင်းပေါင်းချုပ်", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuckyTextPrimary)
                        Text("စုစုပေါင်း ${settled.size} ကြိမ်", fontSize = 12.sp, color = LuckyTextMuted)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                CleanAudio2D.playClick(context)
                                ShareHelper.copyToClipboard(context, viewModel.buildDailySettlementsTextSummary(), "Lucky2D Summary")
                                viewModel.showToast("စာရင်းချုပ် အကျဉ်း ကူးယူပြီးပါပြီ")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyCardSoft, contentColor = LuckyGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ကူးမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                CleanAudio2D.playClick(context)
                                ShareHelper.openGenericShare(context, viewModel.buildDailySettlementsTextSummary(), "Lucky2D ရက်ချုပ်")
                            },
                            enabled = settled.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyCardSoft, contentColor = LuckyGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                CleanAudio2D.playClick(context)
                                showClearAllDialog = true
                            },
                            enabled = settled.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = LuckyDanger),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("အားလုံးဖျက်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { activeTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 0) Color.White else Color.Transparent,
                        contentColor = if (activeTab == 0) LuckyGreen else LuckyTextSecondary
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("အပိုင်းအလိုက် (${settled.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { activeTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 1) Color.White else Color.Transparent,
                        contentColor = if (activeTab == 1) LuckyGreen else LuckyTextSecondary
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ရက်အလိုက် (${dailySettlements.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeTab == 0) {
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
                                .padding(horizontal = 12.dp, vertical = 11.dp)
                        ) {
                            Text("စဉ်", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
                            Text("ရက်စွဲ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
                            Text("အပိုင်း", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                            Text("အမြတ်/အရှုံး", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                        }

                        if (settled.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("ရက်ချုပ်မှတ်တမ်း မရှိသေးပါ", fontSize = 13.sp, color = LuckyTextMuted)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(settled, key = { _, it -> it.id }) { idx, item ->
                                    val isProfit = item.profitLoss >= 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (idx % 2 == 1) LuckyCardSoft else Color.White)
                                            .clickable {
                                                CleanAudio2D.playClick(context)
                                                selectedRecord = item
                                            }
                                            .padding(horizontal = 12.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${idx + 1}", fontSize = 12.sp, color = LuckyTextMuted, modifier = Modifier.width(36.dp))
                                        Text(LotteryViewModel.formatDotDate(item.date), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LuckyTextPrimary, modifier = Modifier.weight(1.2f))
                                        Text(item.session, fontSize = 12.sp, color = LuckyTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                                        Text(
                                            text = "${if (isProfit) "+" else ""}${item.profitLoss}",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isProfit) LuckySuccess else LuckyDanger,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1.1f)
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFFEEF3F1))
                                }
                            }
                        }

                        // Bottom Summary Bar
                        HorizontalDivider(color = LuckyBorder)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LuckyCardSoft)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("လက်ခံငွေပေါင်း: $totalRoundsRetained", fontSize = 12.sp, color = LuckyTextPrimary)
                                Text("လျော်ကြေးပေါင်း: $totalRoundsPayout", fontSize = 12.sp, color = LuckyDanger)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("အသားတင် အမြတ်ငွေ :", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LuckyTextPrimary)
                                Text(
                                    text = "${if (totalRoundsProfitLoss >= 0) "+" else ""}$totalRoundsProfitLoss",
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalRoundsProfitLoss >= 0) LuckySuccess else LuckyDanger
                                )
                            }
                        }
                    }
                }
            } else {
                // Daily Settlements List
                if (dailySettlements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ရက်အလိုက် စာရင်းချုပ် မရှိသေးပါ", fontSize = 13.sp, color = LuckyTextMuted)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(dailySettlements, key = { it.date }) { ds ->
                            val isProfit = ds.profitOrLoss >= 0
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, LuckyBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${LotteryViewModel.formatDotDate(ds.date)} (${ds.roundCount} ကြိမ်)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = LuckyGreen
                                        )
                                        Text(
                                            text = ds.winningNumbersSummary,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = LuckyDanger
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        SummaryMiniBox("ရောင်းရငွေ", "${ds.totalSales}", LuckyTextPrimary, Modifier.weight(1f))
                                        SummaryMiniBox("ကော်မရှင်", "${ds.totalCommission}", Color(0xFFB45309), Modifier.weight(1f))
                                        SummaryMiniBox("ပေါက်ကြေး", "${ds.winningPayout}", LuckyDanger, Modifier.weight(1f))
                                        SummaryMiniBox(
                                            if (isProfit) "အမြတ်" else "အရှုံး",
                                            "${if (isProfit) "+" else ""}${ds.profitOrLoss}",
                                            if (isProfit) LuckySuccess else LuckyDanger,
                                            Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedRecord?.let { rec ->
        val isProfit = rec.profitLoss >= 0
        AlertDialog(
            onDismissRequest = { selectedRecord = null },
            title = {
                Column {
                    Text("ရက်ချုပ် စာရင်းအသေးစိတ်", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 17.sp)
                    Text("${LotteryViewModel.formatDotDate(rec.date)} (${rec.session})", fontSize = 12.sp, color = LuckyTextMuted)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("လက်ခံငွေ", "${rec.totalRetained}", LuckyGreen)
                    DetailRow("ကော်မရှင်", "${rec.commission}", Color(0xFFB45309))
                    DetailRow("ကျန်ငွေ", "${rec.retainedNet}", LuckyTextPrimary)
                    DetailRow("ပေါက်ဂဏန်း", rec.winningNumber.ifBlank { "-" }, LuckyDanger)
                    DetailRow("ပေါက်ကြေးလျော်ငွေ", "${rec.payout}", LuckyDanger)
                    HorizontalDivider()
                    DetailRow(
                        if (isProfit) "အမြတ်ငွေ" else "အရှုံးငွေ",
                        "${if (isProfit) "+" else ""}${rec.profitLoss}",
                        if (isProfit) LuckySuccess else LuckyDanger
                    )
                    Text("အချိန်: ${rec.timestamp}", fontSize = 11.sp, color = LuckyTextMuted)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRecord = null }) {
                    Text("ပိတ်မည်", color = LuckyGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.deleteSettledRecord(rec.id)
                        selectedRecord = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                ) {
                    Text("ဖျက်မည်")
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("မှတ်တမ်းအားလုံး ဖျက်မည်လား?", fontWeight = FontWeight.Bold, color = LuckyDanger) },
            text = { Text("သိမ်းဆည်းထားသော ရက်ချုပ်မှတ်တမ်းများအားလုံး ပျက်ပြယ်သွားမည်ဖြစ်ပြီး ပြန်ယူ၍မရနိုင်ပါ။") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearSettledLedger()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                ) {
                    Text("အားလုံးဖျက်မည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("မဖျက်ပါ", color = LuckyTextMuted)
                }
            }
        )
    }
}

@Composable
private fun SummaryMiniBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = LuckyTextMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = LuckyTextSecondary)
        Text(value, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
