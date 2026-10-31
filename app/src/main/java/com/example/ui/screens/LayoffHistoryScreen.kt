@file:OptIn(ExperimentalFoundationApi::class)
package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.BetItemEntity
import com.example.data.LayoffItem
import com.example.data.LayoffRecord
import com.example.data.PassBetRecordEntity
import com.example.data.toAsciiDigits
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.copyPassBetsRecordToClipboard

/**
 * Screen: "တင်ကွက်များ -> တင်ကွက်မှတ်တမ်း" (Layoff & Pass Bets Export Screen)
 * Enforces:
 * 1. 4-column Table UI: [ စဉ် ] (0.9f) | [ အကြိမ် ] (1.2f) | [ တင်ကွက်ငွေပမာဏ ] (1.4f) | [ ကူးမည် ] (0.7f)
 * 2. Oldest-First Ascending sequence (No. 1, No. 2, ...)
 * 3. Strict clipboard export format:
 *    No. [အမှတ်စဉ်]=[အကြိမ်ရေ]
 *    [ဂဏန်း]=[ငွေ]
 *    ...
 *
 *    စုစုပေါင်း=[ငွေပမာဏ စုစုပေါင်း]
 */
@Composable
fun LayoffHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val layoffs by viewModel.layoffs.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val vouchers by viewModel.vouchers.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val settled by viewModel.settled.collectAsState()
    val availableRounds = remember(sessions, selectedDate, vouchers, wins, settled) {
        viewModel.getAvailableRoundsForDate(selectedDate)
    }
    val hasRound = selectedSession.isNotBlank() && (sessions.any { it.name == selectedSession } || availableRounds.contains(selectedSession))

    var selectedDetailRecord by remember { mutableStateOf<LayoffRecord?>(null) }
    var batchToDelete by remember { mutableStateOf<LayoffRecord?>(null) }
    var itemActionTarget by remember { mutableStateOf<Pair<Int, LayoffItem>?>(null) }
    var itemToEdit by remember { mutableStateOf<Pair<Int, LayoffItem>?>(null) }
    var itemToDelete by remember { mutableStateOf<Pair<Int, LayoffItem>?>(null) }

    LaunchedEffect(layoffs) {
        selectedDetailRecord?.let { current ->
            selectedDetailRecord = layoffs.firstOrNull { it.id == current.id }
        }
    }

    // Enforce strictly Oldest-First sequence (No. 1, No. 2, No. 3...)
    val filtered = remember(layoffs, selectedDate, selectedSession, hasRound) {
        if (!hasRound) {
            emptyList()
        } else {
            layoffs.filter {
                it.date.toAsciiDigits() == selectedDate.toAsciiDigits() && it.session == selectedSession
            }.sortedWith(compareBy<LayoffRecord> { it.recordNo }.thenBy { it.id })
        }
    }
    val grandTotal = filtered.sumOf { it.total }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "တင်ကွက်မှတ်တမ်း",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) },
            onBack = { viewModel.goBack() },
            selectedDate = selectedDate,
            onDateSelected = { viewModel.setDate(it) },
            historicalRounds = availableRounds,
            actions = {
                Button(
                    onClick = { viewModel.exportCurrentSessionLedgerToFile(context, launchShare = true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LuckyHeader
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("export_layoff_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Export",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoundRequiredHelperBanner(visible = !hasRound)

            // 4-Column Table Card: [ စဉ် ] | [ အကြိမ် ] | [ တင်ကွက်ငွေပမာဏ ] | [ ကူးမည် ]
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, LuckyBorder, RoundedCornerShape(8.dp)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // TABLE HEADER: Deep Green (#235347)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF235347))
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                            .testTag("pass_bet_table_header"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "စဉ်",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(0.9f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "အကြိမ်",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "တင်ကွက်ငွေပမာဏ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1.4f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ကူးမည်",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("layoff_empty_state"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "တင်ကွက်မှတ်တမ်း မရှိသေးပါ",
                                color = LuckyTextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // TABLE BODY: Oldest-First (Strictly No. 1, No. 2, ...)
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(filtered, key = { _, r -> r.id }) { idx, record ->
                                val displayNo = if (record.recordNo > 0) record.recordNo else (idx + 1)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                copyPassBetsRecordToClipboard(
                                                    context = context,
                                                    record = record,
                                                    recordNo = displayNo
                                                )
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedDetailRecord = record
                                            }
                                        )
                                        .padding(vertical = 10.dp, horizontal = 8.dp)
                                        .testTag("pass_bet_row_$displayNo"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. စဉ် (High-contrast yellow pill badge)
                                    Box(
                                        modifier = Modifier.weight(0.9f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            color = Color(0xFFFFEB3B),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "No. $displayNo",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // 2. အကြိမ် (Active session string)
                                    Text(
                                        text = record.session,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF235347),
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.Center
                                    )

                                    // 3. တင်ကွက်ငွေပမာဏ (Pure integer total amount)
                                    Text(
                                        text = "${record.total}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF111111),
                                        modifier = Modifier.weight(1.4f),
                                        textAlign = TextAlign.Center
                                    )

                                    // 4. ကူးမည် Action Button
                                    Box(
                                        modifier = Modifier.weight(0.7f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                copyPassBetsRecordToClipboard(
                                                    context = context,
                                                    record = record,
                                                    recordNo = displayNo
                                                )
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("pass_bet_copy_btn_$displayNo")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Pass Bet",
                                                tint = Color(0xFF235347),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            }
                        }
                    }
                }
            }

            // Summary Footer
            if (filtered.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, LuckyBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "တင်ကွက် (${filtered.size}) ခု",
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "စုစုပေါင်း: $grandTotal",
                        color = Color(0xFF235347),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Delete Batch Confirm Dialog
        batchToDelete?.let { record ->
            DeleteConfirmDialog(
                title = "ဖျက်ရန် သေချာပါသလား?",
                message = "တင်ကွက်မှတ်တမ်း (အချိန်: ${record.time} ၊ ပမာဏ: ${fmt(record.total)}) ကို ဖျက်ရန် သေချာပါသလား?",
                onDismiss = { batchToDelete = null },
                onConfirm = {
                    viewModel.deleteLayoffRecord(record.id)
                    batchToDelete = null
                }
            )
        }

        // Detail dialog when long-pressing a row
        selectedDetailRecord?.let { record ->
            val displayNo = if (record.recordNo > 0) record.recordNo else 1
            AlertDialog(
                onDismissRequest = { selectedDetailRecord = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "တင်ကွက် No. $displayNo အသေးစိတ်",
                            color = Color(0xFF235347),
                            fontSize = 16.sp,
                            lineHeight = 1.5.em,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(onClick = {
                                copyPassBetsRecordToClipboard(context, record, displayNo)
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "ကူးယူရန်", tint = Color(0xFF235347))
                            }
                            IconButton(onClick = {
                                val text = buildLayoffDetailText(record, displayNo)
                                ShareHelper.shareText(context, text)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "မျှဝေရန်", tint = Color(0xFF235347))
                            }
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        Text("အကြိမ် : ${record.session} | အချိန် : ${record.time}", color = LuckyTextSecondary, fontSize = 12.sp, lineHeight = 1.5.em)
                        Text("ဘရိတ်သတ်မှတ်ချက် : ${fmt(record.limit)}", color = LuckyTextSecondary, fontSize = 12.sp, lineHeight = 1.5.em)
                        Text("စုစုပေါင်း : ${record.total} (${record.items.size} လုံး)", color = Color(0xFF235347), fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 1.5.em)

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = LuckyBorder)

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            itemsIndexed(record.items) { i, itm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                viewModel.onEditActionStarted()
                                                itemActionTarget = Pair(i, itm)
                                            }
                                        )
                                        .padding(vertical = 8.dp)
                                        .testTag("layoff_detail_row_$i"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${i + 1}. ${itm.n}", color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(fmt(itm.a), color = LuckyDanger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                HorizontalDivider(color = LuckyBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            copyPassBetsRecordToClipboard(context, record, displayNo)
                            selectedDetailRecord = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF235347))
                    ) {
                        Text("ကူးမည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            batchToDelete = record
                            selectedDetailRecord = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = LuckyDanger)
                    ) {
                        Text("ဖျက်မည်", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Long-press Action Sheet / Dialog for item in record
        itemActionTarget?.let { (idx, itm) ->
            RecordActionMenuDialog(
                title = "${itm.n} = ${fmt(itm.a)}",
                onDismiss = { itemActionTarget = null },
                onEdit = {
                    itemToEdit = Pair(idx, itm)
                },
                onDelete = {
                    itemToDelete = Pair(idx, itm)
                }
            )
        }

        // Delete Confirmation Dialog for a single Layoff Item
        itemToDelete?.let { (idx, itm) ->
            DeleteConfirmDialog(
                itemName = "${itm.n} = ${fmt(itm.a)}",
                onDismiss = { itemToDelete = null },
                onConfirm = {
                    selectedDetailRecord?.let { rec ->
                        viewModel.deleteLayoffItem(rec.id, idx)
                        selectedDetailRecord = viewModel.layoffs.value.firstOrNull { it.id == rec.id }
                    }
                    itemToDelete = null
                }
            )
        }

        // Edit Layoff Item Dialog
        itemToEdit?.let { (idx, itm) ->
            val savedNumStr = itm.n
            val savedAmtStr = if (itm.a > 0L) itm.a.toString() else ""
            var newNumberStr by rememberSaveable(idx, itm.n, itm.a) { mutableStateOf(savedNumStr) }
            var newAmountStr by rememberSaveable(idx, itm.n, itm.a) { mutableStateOf(savedAmtStr) }

            AlertDialog(
                onDismissRequest = { itemToEdit = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Text(
                        text = "တင်ကွက် ပြင်ဆင်ရန်",
                        color = LuckyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 1.5.em,
                        modifier = Modifier.testTag("edit_layoff_dialog_title")
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppOutlinedTextField(
                            value = newNumberStr,
                            onValueChange = { newNumberStr = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("ဂဏန်း", lineHeight = 1.5.em) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .soundOnInputFocus()
                                .testTag("edit_layoff_number_input")
                        )
                        AppOutlinedTextField(
                            value = newAmountStr,
                            onValueChange = { newAmountStr = it.filter { c -> c.isDigit() } },
                            label = { Text("ငွေပမာဏ", lineHeight = 1.5.em) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .soundOnInputFocus()
                                .testTag("edit_layoff_amount_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanNum = newNumberStr.trim().filter { it.isDigit() }
                            val amt = MainViewModel.parseAmountOrZero(newAmountStr)
                            if (cleanNum.length !in 2..3) {
                                viewModel.showToast("ဂဏန်း မှားယွင်းနေပါသည်")
                                return@Button
                            }
                            if (amt <= 0L) {
                                viewModel.showToast("ပမာဏ မှားယွင်းနေပါသည်")
                                return@Button
                            }
                            selectedDetailRecord?.let { rec ->
                                if (viewModel.editLayoffItem(rec.id, idx, cleanNum, amt)) {
                                    selectedDetailRecord = viewModel.layoffs.value.firstOrNull { it.id == rec.id }
                                    itemToEdit = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF235347), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_edit_layoff_btn")
                    ) {
                        Text(
                            text = "သိမ်းဆည်းမည်",
                            fontWeight = FontWeight.Bold,
                            lineHeight = 1.5.em
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { itemToEdit = null },
                        modifier = Modifier.testTag("cancel_edit_layoff_btn")
                    ) {
                        Text(
                            text = "မလုပ်တော့ပါ",
                            color = LuckyTextSecondary,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 1.5.em
                        )
                    }
                }
            )
        }
    }
}

private fun buildLayoffDetailText(record: LayoffRecord, recordNo: Int): String {
    val builder = StringBuilder()
    builder.append("No. ").append(recordNo).append("=").append(record.session.trim()).append("\n")
    record.items.forEach { itm ->
        builder.append(itm.n).append("=").append(itm.a).append("\n")
    }
    builder.append("\nစုစုပေါင်း=").append(record.total)
    return builder.toString()
}
