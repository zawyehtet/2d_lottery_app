package com.lucky.lucky2d.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.lucky.lucky2d.ui.theme.LuckyTextMuted
import com.lucky.lucky2d.ui.theme.LuckyTextPrimary
import com.lucky.lucky2d.ui.theme.LuckyTextSecondary
import com.lucky.lucky2d.utils.CleanAudio2D
import com.lucky.lucky2d.viewmodel.LotteryViewModel

@Composable
fun NumbersScreen(
    viewModel: LotteryViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedNumber by remember { mutableStateOf<String?>(null) }

    val dotDate = LotteryViewModel.formatDotDate(viewModel.selectedDate)
    val currentWin = viewModel.currentWinningNumber

    val sortedNumbers = remember(
        viewModel.vouchers,
        viewModel.layoffs,
        viewModel.selectedDate,
        viewModel.selectedSession,
        currentWin
    ) {
        val all = viewModel.getAll100Numbers()
        viewModel.sort2DNumbersWithWinning(all, currentWin.ifBlank { null })
    }

    val filteredNumbers = remember(sortedNumbers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) sortedNumbers else sortedNumbers.filter { it.n.contains(q) }
    }

    val totalBetsAmount = remember(sortedNumbers) {
        sortedNumbers.sumOf { it.totalBet }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        LuckyHeader(
            title = "ဂဏန်းများ",
            viewModel = viewModel,
            showBack = true,
            onBack = onBack
        )

        RoundRequiredBanner(viewModel = viewModel, onOpenSettings = onOpenSettings)

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it.filter { ch -> ch.isDigit() }.take(2) },
            placeholder = { Text("ဂဏန်း ရှာရန်", color = LuckyTextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LuckyTextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = LuckyTextMuted)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = LuckyGreen,
                unfocusedBorderColor = LuckyInputBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // 3-Column Numbers Table Card
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
                // Dark-Green Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyGreen)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "အမှတ်စဉ်",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(68.dp)
                    )
                    Text(
                        text = "ဂဏန်း",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "ပမာဏ",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp)
                    )
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(filteredNumbers, key = { _, item -> item.n }) { index, item ->
                        val isWinning = currentWin.isNotEmpty() && item.n == currentWin
                        val rowBg = when {
                            isWinning -> Color(0xFFFEF3C7)
                            index % 2 == 1 -> LuckyCardSoft
                            else -> Color.White
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .clickable {
                                    CleanAudio2D.playClick(context)
                                    selectedNumber = item.n
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 15.sp,
                                color = LuckyTextPrimary,
                                modifier = Modifier.width(68.dp)
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.n,
                                    fontSize = 18.sp,
                                    fontWeight = if (isWinning) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isWinning) LuckyDanger else LuckyGreen
                                )
                                if (isWinning) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = LuckyDanger
                                    ) {
                                        Text(
                                            text = "ပေါက်သီး",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${item.totalBet}",
                                fontSize = 16.sp,
                                fontWeight = if (isWinning) FontWeight.Bold else FontWeight.Medium,
                                color = if (isWinning) LuckyDanger else LuckyTextPrimary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(96.dp)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFEEF3F1))
                    }
                }

                // Bottom Total Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyGreen)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("စုစုပေါင်း:", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("$totalBetsAmount", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Agent Breakdown Modal
    selectedNumber?.let { num ->
        val breakdown = viewModel.getNumberAgentBreakdown(num)
        AlertDialog(
            onDismissRequest = { selectedNumber = null },
            title = {
                Column {
                    Text("ဂဏန်း $num ၏ အသေးစိတ်", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 17.sp)
                    Text("$dotDate (${viewModel.selectedSession.ifBlank { "အကြိမ်မရှိ" }})", fontSize = 12.sp, color = LuckyTextMuted)
                }
            },
            text = {
                if (breakdown.isEmpty()) {
                    Text("ဤဂဏန်းအတွက် ထိုးကြေး မရှိပါ", fontSize = 13.sp, color = LuckyTextMuted)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LuckyCardSoft)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("အမည်", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LuckyTextSecondary)
                                Text("ပမာဏ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LuckyTextSecondary)
                            }
                        }
                        itemsIndexed(breakdown) { _, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.agentName, fontSize = 14.sp, color = LuckyTextPrimary)
                                Text(
                                    text = "${item.betOnNumber}",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = LuckyGreen
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedNumber = null }) {
                    Text("ပိတ်မည်", color = LuckyGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
