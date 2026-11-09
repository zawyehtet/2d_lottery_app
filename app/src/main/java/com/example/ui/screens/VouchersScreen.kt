@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BetItem
import com.example.data.toAsciiDigits
import com.example.data.BetItemEntity
import com.example.data.Voucher
import com.example.data.VoucherEntity
import com.example.ui.Lottery2DViewModel
import com.example.ui.MainViewModel
import com.example.ui.components.AppHeader
import com.example.ui.components.AppOutlinedTextField
import com.example.ui.components.RoundRequiredHelperBanner
import com.example.ui.components.SearchPill
import com.example.ui.components.ShareHelper
import com.example.ui.theme.*

/**
 * Standard Agent Voucher Receipt Builder ("ကော်မရှင်တစ်ဦးချင်းစီ၏ ဘောင်ချာ")
 */
fun buildAgentVoucherReceipt(voucher: Voucher): String {
    val builder = StringBuilder()
    builder.append("----------------------------\n")
    builder.append("🍀 Lucky2D - အရောင်းဘောင်ချာ\n")
    builder.append("----------------------------\n")
    builder.append("ဘောင်ချာ အမှတ် : No. ").append(voucher.no).append("\n")
    builder.append("ထိုးသူ/ကိုယ်စားလှယ် : ").append(voucher.agent).append("\n")
    builder.append("ရက်စွဲ : ").append(voucher.date).append(" (").append(voucher.session).append(")\n")
    builder.append("အချိန် : ").append(voucher.formattedDateTime).append("\n")
    builder.append("----------------------------\n")
    voucher.items.forEach { bet ->
        builder.append(bet.number).append(" = ").append(bet.amount).append("\n")
    }
    builder.append("----------------------------\n")
    builder.append("စုစုပေါင်း = ").append(voucher.totalAmount).append("\n")
    builder.append("----------------------------")
    return builder.toString()
}

/**
 * Standardized Voucher Copy Utility for both 2D and 3D.
 * Strictly guarantees Header, Body, and Footer are concatenated.
 */
fun copyVoucherWithHeaderAndFooter(
    context: Context,
    voucherNo: Int,
    sessionOrRound: String, // "31.AM" for 2D or "23" for 3D
    items: List<BetItemEntity>, // List of bets with .number and .amount
    totalAmount: Long
) {
    val builder = StringBuilder()

    // 1. MANDATORY HEADER: No. [အမှတ်စဉ်]=[အကြိမ်]
    builder.append("No. ").append(voucherNo).append("=").append(sessionOrRound.trim()).append("\n")

    // 2. ITEM LINES: [ဂဏန်း]=[ငွေ]
    items.forEach { bet ->
        builder.append(bet.number.trim()).append("=").append(bet.amount).append("\n")
    }

    // 3. MANDATORY FOOTER: Blank line divider + စုစုပေါင်း=[ငွေပမာဏ စုစုပေါင်း]
    builder.append("\nစုစုပေါင်း=").append(totalAmount)

    val finalClipboardText = builder.toString()

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Voucher No. $voucherNo", finalClipboardText)
    clipboard.setPrimaryClip(clip)

    Toast.makeText(context, "No. $voucherNo တင်ကွက်စာရင်း ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
}

/**
 * Overload accepting domain Voucher model
 */
fun copyVoucherWithHeaderAndFooter(
    context: Context,
    voucher: Voucher
) {
    copyVoucherWithHeaderAndFooter(
        context = context,
        voucherNo = voucher.no,
        sessionOrRound = voucher.session,
        items = voucher.items,
        totalAmount = voucher.totalAmount
    )
}

fun copy2DVoucherToClipboard(
    context: Context,
    voucherNo: Int,
    sessionName: String,
    bets: List<BetItemEntity>,
    totalAmount: Long
) {
    copyVoucherWithHeaderAndFooter(context, voucherNo, sessionName, bets, totalAmount)
}

fun copy2DVoucherToClipboard(
    context: Context,
    voucher: Voucher
) {
    copyVoucherWithHeaderAndFooter(context, voucher)
}

fun formatVoucherCopyText(voucher: Voucher): String {
    val builder = StringBuilder()
    builder.append("No. ").append(voucher.no).append("=").append(voucher.session).append("\n")
    voucher.items.forEach { bet ->
        builder.append(bet.number).append("=").append(bet.amount).append("\n")
    }
    builder.append("\nစုစုပေါင်း=").append(voucher.totalAmount)
    return builder.toString()
}

/**
 * Main Voucher History Screen ("ဘောင်ချာများ / တင်ပြီးမှတ်တမ်း")
 * Features:
 * 1. Full-height LazyColumn smoothly utilizing vertical space (Bottom summary footer removed).
 * 2. Pure Myanmar long-press bet action dialog with side-by-side "ပြင်ဆင်မည်" and "ဖျက်မည်" buttons.
 * 3. Edit bet dialog allowing updates to BOTH 2D Number and Bet Amount with side-by-side "မလုပ်တော့ပါ" and "သိမ်းဆည်းမည်".
 */
@Composable
fun VouchersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedSession by viewModel.selectedSession.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val sessionVouchers by viewModel.vouchersState.collectAsStateWithLifecycle()
    val allVouchers by viewModel.vouchers.collectAsStateWithLifecycle()

    val wins by viewModel.wins.collectAsStateWithLifecycle()
    val settled by viewModel.settled.collectAsStateWithLifecycle()
    val availableRounds = remember(sessions, selectedDate, allVouchers, sessionVouchers, wins, settled) {
        viewModel.getAvailableRoundsForDate(selectedDate)
    }
    val cleanDate = selectedDate.toAsciiDigits().trim()
    val cleanSession = selectedSession.trim()

    // Real-time reactive session vouchers from Room Flow; fallback to allVouchers if needed
    val activeVouchers = remember(sessionVouchers, allVouchers, cleanDate, cleanSession) {
        if (sessionVouchers.isNotEmpty()) {
            sessionVouchers
        } else {
            allVouchers.filter {
                it.date.toAsciiDigits().trim() == cleanDate && it.session.trim() == cleanSession
            }
        }
    }
    val hasVouchersForSession = activeVouchers.isNotEmpty()
    val hasRound = cleanSession.isNotBlank() && (sessions.any { it.name.trim() == cleanSession } || availableRounds.any { it.trim() == cleanSession } || hasVouchersForSession)

    val filterAgentName by viewModel.selectedVoucherFilterAgent.collectAsStateWithLifecycle()
    val filterAgentId by viewModel.selectedVoucherFilterAgentId.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val isFilteredByAgent = filterAgentId != null || (filterAgentName != "ALL" && filterAgentName.isNotBlank())
    val headerTitle = if (isFilteredByAgent) {
        "$filterAgentName ၏ ဘောင်ချာများ"
    } else {
        stringResource(R.string.vouchers_screen_title)
    }

    BackHandler {
        viewModel.clearVoucherFilter()
        viewModel.goBack()
    }

    // Filter and sort vouchers oldest first with robust date/session normalization
    val filteredVouchers = remember(activeVouchers, filterAgentName, filterAgentId, searchQuery, hasRound, hasVouchersForSession) {
        if (!hasRound && !hasVouchersForSession) {
            emptyList()
        } else {
            activeVouchers.filter { v ->
                val matchesAgent = when {
                    filterAgentId != null -> v.agentId == filterAgentId || (filterAgentName != "ALL" && v.agent == filterAgentName)
                    filterAgentName != "ALL" -> v.agent == filterAgentName
                    else -> true
                }
                matchesAgent &&
                    (searchQuery.isBlank() ||
                        v.agent.contains(searchQuery, ignoreCase = true) ||
                        v.no.toString() == searchQuery.trim() ||
                        v.items.any { it.n == searchQuery.trim() })
            }.sortedWith(compareBy<Voucher> { it.no }.thenBy { it.id })
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = headerTitle,
                selectedSession = selectedSession,
                sessions = sessions,
                onSessionSelected = { viewModel.setSession(it) },
                onBack = {
                    viewModel.clearVoucherFilter()
                    viewModel.goBack()
                },
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setDate(it) },
                historicalRounds = availableRounds
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp)
            ) {
                RoundRequiredHelperBanner(visible = !hasRound)

                if (hasRound) {
                    SearchPill(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        label = stringResource(R.string.label_search_voucher),
                        enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .testTag("voucher_search_input")
                    )
                }

                if (filteredVouchers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("vouchers_empty_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ဘောင်ချာ မရှိသေးပါ",
                            color = LuckyTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    // Full vertical height LazyColumn with bottom padding for smooth scrolling
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                    ) {
                        items(filteredVouchers, key = { it.id }) { voucher ->
                            VoucherCardItem(
                                voucher = voucher,
                                betsList = voucher.items,
                                onUpdateBet = { betItem, newNum, newAmt ->
                                    viewModel.editNumber(betItem.id, newNum, newAmt)
                                },
                                onDeleteSingleBet = { betItem ->
                                    viewModel.deleteNumber(betItem.id)
                                },
                                onDeleteEntireVoucher = { v ->
                                    viewModel.deleteVoucher(v.id)
                                },
                                onCopyVoucher = {
                                    val receiptText = buildAgentVoucherReceipt(voucher)
                                    ShareHelper.copyToClipboard(context, receiptText, "Voucher No. ${voucher.no}")
                                    Toast.makeText(context, "ဘောင်ချာ No. ${voucher.no} ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * VoucherCardItem strictly based on the reference layout specification:
 * 1. Dark Green Header Strip (#0C4E3E) without '+' button, long-press to delete voucher
 * 2. Agent Sub-Header with circular avatar badge + name + bet count badge
 * 3. Bet Numbers Vertical Table: Index | 2D Number (#0F766E) | Bet Amount (#1E293B)
 *    Long-press row triggers Action Dialog: Side-by-side "ပြင်ဆင်မည်" (Left) and "ဖျက်မည်" (Right)
 * 4. Card Footer Bar: Total Amount in green (#15803D) + Copy, Share, Print action icons
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoucherCardItem(
    voucher: Voucher,
    betsList: List<BetItemEntity>,
    onUpdateBet: (BetItemEntity, String, Long) -> Unit,
    onDeleteSingleBet: (BetItemEntity) -> Unit,
    onDeleteEntireVoucher: (Voucher) -> Unit,
    onCopyVoucher: () -> Unit
) {
    var showDeleteVoucherDialog by remember { mutableStateOf(false) }
    var selectedBetForAction by remember { mutableStateOf<BetItemEntity?>(null) }
    var isEditAmountDialogOpen by remember { mutableStateOf(false) }
    var editNumberInput by remember { mutableStateOf("") }
    var editAmountInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("voucher_card_item_${voucher.no}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // 1. Dark Green Header Strip (NO '+' Button, Long-Press to Delete Voucher)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { /* Optional tap */ },
                        onLongClick = { showDeleteVoucherDialog = true } // Long-press triggers voucher deletion
                    )
                    .testTag("voucher_header_strip_${voucher.no}"),
                color = Color(0xFF0C4E3E),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "အမှတ် ${voucher.voucherNo}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "အချိန် : ${voucher.formattedDateTime}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 2. Agent Sub-Header: Avatar + Name + Bet Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0xFF0C4E3E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = voucher.agentName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "အိုင်ဒီ : ${voucher.agentId} , အမည် : ${voucher.agentName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                Text(
                    text = "${betsList.size} ကွက်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // 3. Vertical Table: Index | 2D Number | Bet Amount (No inline buttons)
            Column(modifier = Modifier.fillMaxWidth()) {
                betsList.forEachIndexed { index, bet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { /* Normal tap */ },
                                onLongClick = { selectedBetForAction = bet } // Long press individual bet row
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("voucher_bet_row_${bet.number}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Index (e.g., "1.")
                        Text(
                            text = "${index + 1}.",
                            fontSize = 13.5.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.weight(0.6f),
                            textAlign = TextAlign.Start
                        )
                        // 2D Number (e.g., "12")
                        Text(
                            text = bet.number,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = Color(0xFF0F766E),
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        // Amount (e.g., "500")
                        Text(
                            text = "${bet.amount}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            // 4. Footer Bar: Total Amount & Action Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "စုစုပေါင်း : ${voucher.totalAmount}",
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF475569),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onCopyVoucher() }
                            .testTag("copy_icon_${voucher.no}")
                    )
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF475569),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                val receipt = buildAgentVoucherReceipt(voucher)
                                ShareHelper.shareText(context, receipt)
                            }
                            .testTag("share_icon_${voucher.no}")
                    )
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print",
                        tint = Color(0xFF475569),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                val receipt = buildAgentVoucherReceipt(voucher)
                                ShareHelper.printText(context, receipt)
                            }
                            .testTag("print_icon_${voucher.no}")
                    )
                }
            }
        }
    }

    // DIALOG 1: Entire Voucher Deletion Dialog (Header Long-Press)
    if (showDeleteVoucherDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteVoucherDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = "ဘောင်ချာ ဖျက်ရန်",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB91C1C),
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "ဘောင်ချာကို ဖျက်မည်လား?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteVoucherDialog = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("မဖျက်ပါ", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                    Button(
                        onClick = {
                            showDeleteVoucherDialog = false
                            onDeleteEntireVoucher(voucher)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("ဖျက်မည်", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = null
        )
    }

    // DIALOG 2: Single Bet Item Long-Press Action (Pure Myanmar Text, Side-by-Side Horizontal Buttons)
    selectedBetForAction?.let { betItem ->
        AlertDialog(
            onDismissRequest = { selectedBetForAction = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = "ဂဏန်း ${betItem.number} စီမံရန်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Text(
                    text = "လက်ရှိ ထိုးငွေပမာဏ : ${betItem.amount}",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left: "ပြင်ဆင်မည်"
                    Button(
                        onClick = {
                            editNumberInput = betItem.number
                            editAmountInput = betItem.amount.toString()
                            isEditAmountDialogOpen = true
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("ပြင်ဆင်မည်", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    // Right: "ဖျက်မည်"
                    Button(
                        onClick = {
                            onDeleteSingleBet(betItem)
                            selectedBetForAction = null
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("ဖျက်မည်", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = null
        )
    }

    // DIALOG 3: Edit Bet Dialog (Updates BOTH 2D Number and Bet Amount, Side-by-Side "မလုပ်တော့ပါ" and "သိမ်းဆည်းမည်")
    if (isEditAmountDialogOpen && selectedBetForAction != null) {
        AlertDialog(
            onDismissRequest = { isEditAmountDialogOpen = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = "ဂဏန်း ပြင်ဆင်ရန်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0C4E3E)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppOutlinedTextField(
                        value = editNumberInput,
                        onValueChange = { editNumberInput = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("ဂဏန်း") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppOutlinedTextField(
                        value = editAmountInput,
                        onValueChange = { editAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("ငွေပမာဏ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left: "မလုပ်တော့ပါ"
                    OutlinedButton(
                        onClick = { isEditAmountDialogOpen = false },
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("မလုပ်တော့ပါ", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                    // Right: "သိမ်းဆည်းမည်"
                    Button(
                        onClick = {
                            val cleanNum = editNumberInput.trim()
                            val newAmt = editAmountInput.toLongOrNull() ?: 0L
                            if (cleanNum.length != 2) {
                                Toast.makeText(context, "ဂဏန်း ၂ လုံး ဖြစ်ရပါမည်", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newAmt <= 0L) {
                                Toast.makeText(context, "ငွေပမာဏ မှားယွင်းနေပါသည်", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onUpdateBet(selectedBetForAction!!, cleanNum, newAmt)
                            isEditAmountDialogOpen = false
                            selectedBetForAction = null
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C4E3E))
                    ) {
                        Text("သိမ်းဆည်းမည်", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = null
        )
    }
}

/**
 * Overload for Voucher with onUpdateBetAmount
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoucherCardItem(
    voucher: Voucher,
    betsList: List<BetItemEntity>,
    onUpdateBetAmount: (BetItemEntity, Long) -> Unit,
    onDeleteSingleBet: (BetItemEntity) -> Unit,
    onDeleteEntireVoucher: (Voucher) -> Unit,
    onCopyVoucher: () -> Unit
) {
    VoucherCardItem(
        voucher = voucher,
        betsList = betsList,
        onUpdateBet = { bet, _, newAmt -> onUpdateBetAmount(bet, newAmt) },
        onDeleteSingleBet = onDeleteSingleBet,
        onDeleteEntireVoucher = onDeleteEntireVoucher,
        onCopyVoucher = onCopyVoucher
    )
}

/**
 * Overload for VoucherEntity
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoucherCardItem(
    voucher: VoucherEntity,
    betsList: List<BetItemEntity>,
    onUpdateBetAmount: (BetItemEntity, Long) -> Unit,
    onDeleteSingleBet: (BetItemEntity) -> Unit,
    onDeleteEntireVoucher: (VoucherEntity) -> Unit,
    onCopyVoucher: () -> Unit
) {
    VoucherCardItem(
        voucher = voucher.toDomainVoucher(),
        betsList = betsList,
        onUpdateBet = { bet, _, newAmt -> onUpdateBetAmount(bet, newAmt) },
        onDeleteSingleBet = onDeleteSingleBet,
        onDeleteEntireVoucher = { onDeleteEntireVoucher(voucher) },
        onCopyVoucher = onCopyVoucher
    )
}

/**
 * Standard VoucherHistoryTableScreen2D implementation for backward compatibility with tests
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherHistoryTableScreen2D(
    vouchers: List<VoucherEntity>,
    viewModel: Lottery2DViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("တင်ပြီးမှတ်တမ်း", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF235347))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F9FA))
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // TABLE HEADER: စဉ် | အကြိမ် | တင်ကွက်ငွေပမာဏ | ကူးမည်
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF235347))
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("စဉ်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                        Text("အကြိမ်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                        Text("တင်ကွက်ငွေပမာဏ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
                        Text("ကူးမည်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                    }

                    if (vouchers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("တင်ပြီးမှတ်တမ်း မရှိသေးပါ", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(vouchers, key = { it.voucherNo }) { voucher ->
                                val bets by viewModel.getBetsForVoucher(voucher.voucherNo).collectAsState(initial = emptyList())

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            copy2DVoucherToClipboard(context, voucher.voucherNo, voucher.sessionName, bets, voucher.totalAmount)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(0.9f), contentAlignment = Alignment.Center) {
                                        Surface(color = Color(0xFFFFEB3B), shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = "No. ${voucher.voucherNo}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = voucher.sessionName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF235347),
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "${voucher.totalAmount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF111111),
                                        modifier = Modifier.weight(1.4f),
                                        textAlign = TextAlign.Center
                                    )

                                    Box(modifier = Modifier.weight(0.7f), contentAlignment = Alignment.Center) {
                                        IconButton(
                                            onClick = {
                                                copy2DVoucherToClipboard(context, voucher.voucherNo, voucher.sessionName, bets, voucher.totalAmount)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Voucher",
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
        }
    }
}
