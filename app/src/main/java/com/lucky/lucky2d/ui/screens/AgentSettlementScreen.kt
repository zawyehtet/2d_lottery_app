package com.lucky.lucky2d.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky.lucky2d.model.Agent2DSettlement
import com.lucky.lucky2d.ui.components.LuckyHeader
import com.lucky.lucky2d.ui.components.RoundRequiredBanner
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
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun AgentSettlementScreen(
    viewModel: LotteryViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var copiedAgentId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(copiedAgentId) {
        if (copiedAgentId != null) {
            delay(2000)
            copiedAgentId = null
        }
    }

    val dotDate = LotteryViewModel.formatDotDate(viewModel.selectedDate)
    val winNum = viewModel.currentWinningNumber
    val settlements = remember(
        viewModel.vouchers,
        viewModel.agents,
        viewModel.selectedDate,
        viewModel.selectedSession,
        winNum
    ) {
        viewModel.getAgent2DSettlements(winNum)
    }

    val totalSales = remember(settlements) { settlements.sumOf { it.totalSales.toLong() } }
    val totalCommission = remember(settlements) { settlements.sumOf { it.commissionAmount.toLong() } }
    val totalPayout = remember(settlements) { settlements.sumOf { it.payoutAmount } }
    val totalMasterNet = remember(settlements) { settlements.sumOf { it.netBalance } }

    fun copyAgentReport(a: Agent2DSettlement) {
        CleanAudio2D.playClick(context)
        val isGain = a.netBalance >= 0
        val sb = StringBuilder()
        sb.appendLine("Lucky2D ကိုယ်စားလှယ် စာရင်းရှင်းတမ်း")
        sb.appendLine("ကိုယ်စားလှယ်: ${a.agentName}")
        sb.appendLine("ရက်စွဲ: $dotDate (${viewModel.selectedSession})")
        sb.appendLine("ပေါက်ဂဏန်း: ${winNum.ifBlank { "မထွက်သေး" }}")
        sb.appendLine("-----------------------")
        sb.appendLine("ရောင်းရငွေ: ${a.totalSales}")
        sb.appendLine("ကော်မရှင် (${LotteryViewModel.formatRate(a.commissionRate)}%): ${a.commissionAmount}")
        sb.appendLine("ကျန်ငွေ: ${a.netSales}")
        sb.appendLine("ပေါက်ငွေ: ${if (a.winningBetAmount > 0) "${a.winningBetAmount} (x${a.multiplier})" else "0"}")
        sb.appendLine("ပေါက်ကြေး: ${a.payoutAmount}")
        sb.appendLine("-----------------------")
        sb.append(
            if (isGain) "ဒိုင်မှ ရရန်ကျန်ငွေ: ${a.netBalance}"
            else "ကိုယ်စားလှယ်သို့ ပေးရန်ငွေ: ${abs(a.netBalance)}"
        )
        ShareHelper.copyToClipboard(context, sb.toString(), "Agent Settlement")
        copiedAgentId = a.agentId
        viewModel.showToast("စာရင်းရှင်းတမ်း ကူးယူပြီးပါပြီ")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        LuckyHeader(
            title = "ပေါက်သီး စာရင်းရှင်းတမ်း",
            viewModel = viewModel,
            showBack = true,
            onBack = onBack
        )

        RoundRequiredBanner(viewModel = viewModel, onOpenSettings = onOpenSettings)

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Top Winning Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, LuckyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(26.dp))
                            }
                            Column {
                                Text("ထွက်ဂဏန်း (ပေါက်သီး)", fontSize = 12.sp, color = LuckyTextMuted)
                                Text(
                                    text = winNum.ifBlank { "မသတ်မှတ်ရသေး" },
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LuckyDanger
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ရက်စွဲ", fontSize = 11.sp, color = LuckyTextMuted)
                            Text(dotDate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LuckyTextPrimary)
                        }
                    }
                }
            }

            // Aggregate Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AggregateCard("ရောင်းရငွေ", "$totalSales", LuckyGreen, Modifier.weight(1f))
                    AggregateCard("ကော်မရှင်", "$totalCommission", Color(0xFFB45309), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AggregateCard("ပေါက်ကြေး", "$totalPayout", LuckyDanger, Modifier.weight(1f))
                    AggregateCard(
                        if (totalMasterNet >= 0) "ဒိုင် အမြတ်" else "ဒိုင် အရှုံး",
                        "${if (totalMasterNet >= 0) "+" else ""}$totalMasterNet",
                        if (totalMasterNet >= 0) LuckySuccess else LuckyDanger,
                        Modifier.weight(1f)
                    )
                }
            }

            if (settlements.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, LuckyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = LuckyTextMuted, modifier = Modifier.size(32.dp))
                            Text("ထိုးထားသော ကိုယ်စားလှယ် စာရင်းမရှိသေးပါ", fontSize = 13.sp, color = LuckyTextMuted)
                        }
                    }
                }
            } else {
                items(settlements, key = { it.agentId }) { item ->
                    val isGain = item.netBalance >= 0
                    val isCopied = copiedAgentId == item.agentId

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, LuckyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(LuckyCardSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.agentName.take(1).uppercase(),
                                            color = LuckyGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(item.agentName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuckyTextPrimary)
                                        Text(
                                            text = "ကော်: ${LotteryViewModel.formatRate(item.commissionRate)}% • အဆ: x${item.multiplier}",
                                            fontSize = 11.sp,
                                            color = LuckyTextMuted
                                        )
                                    }
                                }

                                Button(
                                    onClick = { copyAgentReport(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuckyCardSoft, contentColor = LuckyGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isCopied) "ကူးပြီး" else "ကူးမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricCol("ရောင်းရငွေ", "${item.totalSales}", LuckyTextPrimary, Modifier.weight(1f))
                                MetricCol("ကော်မရှင်", "${item.commissionAmount}", Color(0xFFB45309), Modifier.weight(1f))
                                MetricCol("ကျန်ငွေ", "${item.netSales}", LuckyGreen, Modifier.weight(1f))
                                MetricCol("ပေါက်ကြေး", "${item.payoutAmount}", LuckyDanger, Modifier.weight(1f))
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isGain) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, if (isGain) Color(0xFFA7F3D0) else Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isGain) "ဒိုင်သို့ ပေးသွင်းရန်ငွေ :" else "ဒိုင်မှ ပြန်လျော်ရန်ငွေ :",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isGain) LuckyGreen else LuckyDanger
                                    )
                                    Text(
                                        text = "${abs(item.netBalance)}",
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGain) LuckyGreen else LuckyDanger
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

@Composable
private fun AggregateCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LuckyBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = LuckyTextMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun MetricCol(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = LuckyTextMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = color)
    }
}
