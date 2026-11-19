package com.lucky.lucky2d.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky.lucky2d.ui.components.LuckyDatePickerDialog
import com.lucky.lucky2d.ui.components.LuckyLogoCircle
import com.lucky.lucky2d.ui.theme.LuckyBg
import com.lucky.lucky2d.ui.theme.LuckyBorder
import com.lucky.lucky2d.ui.theme.LuckyGreen
import com.lucky.lucky2d.ui.theme.LuckyTextMuted
import com.lucky.lucky2d.ui.theme.LuckyTextPrimary
import com.lucky.lucky2d.utils.CleanAudio2D
import com.lucky.lucky2d.viewmodel.LotteryViewModel

@Composable
fun HomeScreen(
    viewModel: LotteryViewModel,
    onOpenAgents: () -> Unit,
    onOpenNumbers: () -> Unit,
    onOpenVouchers: () -> Unit,
    onOpenBrake: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showRoundMenu by remember { mutableStateOf(false) }

    val dotDate = LotteryViewModel.formatDotDate(viewModel.selectedDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        // Dark-green top bar
        Surface(
            color = LuckyGreen,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LuckyLogoCircle(sizeDp = 40)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Lucky2D",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Today date display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = LuckyGreen,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "ယနေ့ - $dotDate",
                    color = LuckyGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(28.dp))

            // 2x2 Dark-Green Grid Menu Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                HomeMenuCard(
                    title = "ကော်မရှင်",
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CleanAudio2D.playClick(context)
                        onOpenAgents()
                    }
                )
                HomeMenuCard(
                    title = "ဂဏန်းများ",
                    icon = Icons.Default.GridView,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CleanAudio2D.playClick(context)
                        onOpenNumbers()
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                HomeMenuCard(
                    title = "ဘောင်ချာ",
                    icon = Icons.Default.ReceiptLong,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CleanAudio2D.playClick(context)
                        viewModel.clearVoucherFilter()
                        onOpenVouchers()
                    }
                )
                HomeMenuCard(
                    title = "တင်ကွက်",
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CleanAudio2D.playClick(context)
                        onOpenBrake()
                    }
                )
            }

            Spacer(Modifier.height(26.dp))

            // Selection Controls Row directly below 4-Grid Cards: Date | Session (a/p) | Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Selector
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, LuckyBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(54.dp)
                        .clickable {
                            CleanAudio2D.playClick(context)
                            showDatePicker = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = LuckyGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ရက်စွဲ : $dotDate",
                            fontSize = 14.sp,
                            color = LuckyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Session Dropdown (a / p / custom)
                Box(modifier = Modifier.weight(0.95f)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, LuckyBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clickable {
                                CleanAudio2D.playClick(context)
                                showRoundMenu = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (viewModel.hasRound) viewModel.selectedSession else "အကြိမ်မရှိ",
                                fontSize = 15.sp,
                                color = LuckyTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "▼",
                                fontSize = 11.sp,
                                color = LuckyGreen
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showRoundMenu,
                        onDismissRequest = { showRoundMenu = false }
                    ) {
                        val rounds = viewModel.availableRounds
                        if (rounds.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("အကြိမ် မရှိသေးပါ", fontSize = 13.sp, color = Color.Gray) },
                                onClick = { showRoundMenu = false }
                            )
                        } else {
                            rounds.forEach { round ->
                                val isSel = round == viewModel.selectedSession
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = round,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) LuckyGreen else LuckyTextPrimary
                                        )
                                    },
                                    onClick = {
                                        CleanAudio2D.playClick(context)
                                        viewModel.setSession(round)
                                        showRoundMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Settings Gear Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, LuckyBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(54.dp)
                        .clickable {
                            CleanAudio2D.playClick(context)
                            onOpenSettings()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "ဆက်တင်",
                            tint = LuckyGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Footer Brand
            Text(
                text = "Power by Lcky2d3d ShweKhinMyay",
                fontSize = 14.sp,
                color = LuckyTextMuted
            )
        }
    }

    if (showDatePicker) {
        LuckyDatePickerDialog(
            currentDateIso = viewModel.selectedDate,
            onSelect = {
                viewModel.setDate(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun HomeMenuCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = LuckyGreen,
        shadowElevation = 6.dp,
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
