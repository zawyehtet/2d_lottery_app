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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky.lucky2d.ui.components.LuckyHeader
import com.lucky.lucky2d.ui.components.RoundRequiredBanner
import com.lucky.lucky2d.ui.theme.LuckyBg
import com.lucky.lucky2d.ui.theme.LuckyBorder
import com.lucky.lucky2d.ui.theme.LuckyCardSoft
import com.lucky.lucky2d.ui.theme.LuckyDanger
import com.lucky.lucky2d.ui.theme.LuckyGreen
import com.lucky.lucky2d.ui.theme.LuckyInputBorder
import com.lucky.lucky2d.ui.theme.LuckyOrange
import com.lucky.lucky2d.ui.theme.LuckySuccess
import com.lucky.lucky2d.ui.theme.LuckyTextMuted
import com.lucky.lucky2d.ui.theme.LuckyTextPrimary
import com.lucky.lucky2d.ui.theme.LuckyTextSecondary
import com.lucky.lucky2d.utils.CleanAudio2D
import com.lucky.lucky2d.utils.ShareHelper
import com.lucky.lucky2d.viewmodel.LotteryViewModel
import kotlin.math.min

private data class ExcessItem(val n: String, val excess: Int)

@Composable
fun BrakeScreen(
    viewModel: LotteryViewModel,
    onBack: () -> Unit,
    onOpenSettlement: () -> Unit,
    onOpenLayoffHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var showLayoffModal by remember { mutableStateOf(false) }
    var layoffAll by remember { mutableStateOf(true) }

    val currentLimit = viewModel.currentBrakeInput.toIntOrNull() ?: 0
    val currentWin = viewModel.currentWinningNumber

    val all100Sorted = remember(
        viewModel.vouchers,
        viewModel.layoffs,
        viewModel.selectedDate,
        viewModel.selectedSession,
        currentWin
    ) {
        val all = viewModel.getAll100Numbers()
        viewModel.sort2DNumbersWithWinning(all, currentWin.ifBlank { null })
    }

    val allExcessItems = remember(all100Sorted, currentLimit) {
        if (currentLimit <= 0) emptyList()
        else all100Sorted
            .filter { it.netAmount > currentLimit }
            .map { ExcessItem(it.n, it.netAmount - currentLimit) }
            .sortedWith(compareByDescending<ExcessItem> { it.excess }.thenBy { it.n })
    }

    val modalExcessItems = remember(allExcessItems, layoffAll) {
        if (layoffAll) allExcessItems else allExcessItems.take(30)
    }

    val totalRetainedDisplay = remember(all100Sorted, currentLimit) {
        all100Sorted.sumOf { item ->
            if (currentLimit > 0) min(item.netAmount, currentLimit) else item.netAmount
        }
    }

    val totalExcessDisplay = remember(allExcessItems) {
        allExcessItems.sumOf { it.excess }
    }

    val totalModalExcess = remember(modalExcessItems) {
        modalExcessItems.sumOf { it.excess }
    }

    fun handleExport() {
        CleanAudio2D.playClick(context)
        val sb = StringBuilder()
        sb.appendLine("Lucky2D တင်ကွက် (${viewModel.selectedDate} - ${viewModel.selectedSession.ifBlank { "အကြိမ်မရှိ" }})")
        sb.appendLine("ဘရိတ်: $currentLimit")
        if (allExcessItems.isNotEmpty()) {
            sb.appendLine("--- တင်ကွက် ---")
            allExcessItems.forEach { sb.appendLine("${it.n}=${it.excess}") }
            sb.append("စုစုပေါင်း = $totalExcessDisplay")
        } else {
            val active = all100Sorted.filter { it.netAmount > 0 }
            sb.appendLine("--- လက်ခံကွက် ---")
            active.forEach { sb.appendLine("${it.n}=${it.netAmount}") }
            sb.append("စုစုပေါင်း = $totalRetainedDisplay")
        }
        ShareHelper.copyToClipboard(context, sb.toString(), "Lucky2D Layoff Export")
        viewModel.showToast("Export စာသား ကူးယူပြီးပါပြီ")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        LuckyHeader(
            title = "တင်ကွက်",
            viewModel = viewModel,
            showBack = true,
            onBack = onBack,
            actions = {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.clickable { handleExport() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export",
                            tint = LuckyTextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuckyTextPrimary)
                    }
                }
            }
        )

        RoundRequiredBanner(viewModel = viewModel, onOpenSettings = onOpenSettings)

        // Brake Limit Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ဘရိတ် :",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = LuckyGreen
            )
            OutlinedTextField(
                value = viewModel.currentBrakeInput,
                onValueChange = { viewModel.updateBrakeInput(it) },
                placeholder = { Text("0", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LuckyTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = LuckyGreen,
                    unfocusedBorderColor = LuckyInputBorder
                ),
                modifier = Modifier
                    .width(126.dp)
                    .height(50.dp)
            )
        }

        // Main 2-Column Split Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, LuckyBorder),
            shadowElevation = 2.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Orange Split Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyOrange)
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "လက်ခံကွက် (${all100Sorted.size})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.height(18.dp)
                    )
                    Text(
                        text = "တင်ကွက် (${allExcessItems.size})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Dark-Green Sub-Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyGreen)
                        .padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ဂဏန်းများ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("ပမာဏ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    VerticalDivider(
                        color = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.height(16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ဂဏန်းများ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("ပမာဏ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Scrollable 2-Column Content
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Left: Retained 100 Numbers
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        itemsIndexed(all100Sorted, key = { _, it -> it.n }) { idx, item ->
                            val kept = if (currentLimit > 0) min(item.netAmount, currentLimit) else item.netAmount
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 1) LuckyCardSoft else Color.White)
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.n, fontSize = 15.sp, color = LuckyTextPrimary)
                                Text("$kept", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LuckyTextPrimary)
                            }
                            HorizontalDivider(color = Color(0xFFEEF3F1))
                        }
                    }

                    VerticalDivider(color = LuckyBorder)

                    // Right: Excess / Layoff Numbers
                    if (allExcessItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(14.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text("ဘရိတ်ကျော် မရှိပါ", fontSize = 14.sp, color = LuckyTextMuted)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            itemsIndexed(allExcessItems, key = { _, it -> it.n }) { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 1) Color(0xFFFDF4F4) else Color.White)
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.n, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LuckyTextPrimary)
                                    Text("${item.excess}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LuckyDanger)
                                }
                                HorizontalDivider(color = Color(0xFFEEF3F1))
                            }
                        }
                    }
                }

                // Bottom Split Footer
                HorizontalDivider(color = LuckyBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyCardSoft)
                ) {
                    // Left Footer
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "စုစုပေါင်း = $totalRetainedDisplay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LuckyTextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveBrakeLimit()
                                    viewModel.lockAndSaveBrakeSettlement()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("သိမ်းမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    CleanAudio2D.playClick(context)
                                    onOpenSettlement()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckySuccess),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ရက်ချုပ်", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    VerticalDivider(color = LuckyBorder, modifier = Modifier.height(82.dp))

                    // Right Footer
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "စုစုပေါင်း = $totalExcessDisplay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LuckyDanger,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    CleanAudio2D.playClick(context)
                                    showLayoffModal = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ တင်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    CleanAudio2D.playClick(context)
                                    onOpenLayoffHistory()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("မှတ်တမ်းများ", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    // Layoff Confirmation Dialog
    if (showLayoffModal) {
        AlertDialog(
            onDismissRequest = { showLayoffModal = false },
            title = {
                Text("တင်ကွက် ထုတ်ယူရန် စစ်ဆေးခြင်း", fontWeight = FontWeight.Bold, color = LuckyDanger, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ဘရိတ်သတ်မှတ်ချက် $currentLimit ကျော်လွန်သော ဂဏန်းများကို တင်ထုတ်မည်။",
                        fontSize = 12.sp,
                        color = LuckyTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { layoffAll = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (layoffAll) LuckyGreen else LuckyCardSoft,
                                contentColor = if (layoffAll) Color.White else LuckyTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("အားလုံးတင်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { layoffAll = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!layoffAll) LuckyGreen else LuckyCardSoft,
                                contentColor = if (!layoffAll) Color.White else LuckyTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("အကွက် ၃၀ တင်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LuckyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (modalExcessItems.isEmpty()) {
                            Text(
                                text = "ဘရိတ်ကျော်သော ဂဏန်း မရှိပါ",
                                fontSize = 12.sp,
                                color = LuckyTextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                                itemsIndexed(modalExcessItems) { _, it ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(it.n, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${it.excess}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = LuckyDanger, fontSize = 13.sp)
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("စုစုပေါင်း တင်မည့်ငွေ (${modalExcessItems.size} ကွက်):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LuckyDanger)
                        Text("$totalModalExcess", fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = LuckyDanger)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.executeLayoff(layoffAll)
                        showLayoffModal = false
                    },
                    enabled = modalExcessItems.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                ) {
                    Text("တင်ကွက် အတည်ပြုမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLayoffModal = false }) {
                    Text("မလုပ်တော့ပါ", color = LuckyTextMuted)
                }
            }
        )
    }
}
