package com.lucky.lucky2d.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky.lucky2d.model.BetItem
import com.lucky.lucky2d.model.Voucher
import com.lucky.lucky2d.ui.components.LuckyHeader
import com.lucky.lucky2d.ui.components.RoundRequiredBanner
import com.lucky.lucky2d.ui.theme.LuckyBg
import com.lucky.lucky2d.ui.theme.LuckyBorder
import com.lucky.lucky2d.ui.theme.LuckyDanger
import com.lucky.lucky2d.ui.theme.LuckyGreen
import com.lucky.lucky2d.ui.theme.LuckyInputBorder
import com.lucky.lucky2d.ui.theme.LuckyRowAlt
import com.lucky.lucky2d.ui.theme.LuckyTextMuted
import com.lucky.lucky2d.ui.theme.LuckyTextPrimary
import com.lucky.lucky2d.ui.theme.LuckyTextSecondary
import com.lucky.lucky2d.utils.CleanAudio2D
import com.lucky.lucky2d.utils.ShareHelper
import com.lucky.lucky2d.viewmodel.LotteryViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VouchersScreen(
    viewModel: LotteryViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var copiedVoucherId by remember { mutableStateOf<Long?>(null) }
    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }
    var betItemToEdit by remember { mutableStateOf<BetItem?>(null) }
    var editNum by remember { mutableStateOf("") }
    var editAmt by remember { mutableStateOf("") }

    LaunchedEffect(copiedVoucherId) {
        if (copiedVoucherId != null) {
            delay(2000)
            copiedVoucherId = null
        }
    }

    val dotDate = LotteryViewModel.formatDotDate(viewModel.selectedDate)

    // Vouchers sorted Newest First (activeVouchers is already sorted by no DESC, id DESC)
    val filteredVouchers = remember(
        viewModel.activeVouchers,
        viewModel.voucherFilterAgent,
        viewModel.voucherFilterAgentId,
        searchQuery
    ) {
        val q = searchQuery.trim().lowercase()
        viewModel.activeVouchers.filter { v ->
            val matchesAgentFilter = if (viewModel.voucherFilterAgent == "ALL") true
            else (v.agent == viewModel.voucherFilterAgent || v.agentId == viewModel.voucherFilterAgentId)

            val matchesSearch = if (q.isEmpty()) true else {
                v.no.toString().contains(q) ||
                    v.agent.lowercase().contains(q) ||
                    v.items.any { it.n.contains(q) }
            }
            matchesAgentFilter && matchesSearch
        }
    }

    val totalVoucherAmount = remember(filteredVouchers) {
        filteredVouchers.sumOf { v -> v.items.sumOf { it.a } }
    }

    val headerTitle = if (viewModel.voucherFilterAgent != "ALL") {
        "${viewModel.voucherFilterAgent} ၏ ဘောင်ချာ"
    } else {
        "ဘောင်ချာ"
    }

    fun copyVoucherText(v: Voucher) {
        CleanAudio2D.playClick(context)
        val ag = viewModel.agents.firstOrNull { it.id == v.agentId || it.name == v.agent }
        val rate = ag?.rate ?: 15.0
        val total = v.items.sumOf { it.a }
        val comm = (total * (rate / 100.0)).roundToInt()
        val net = total - comm

        val sb = StringBuilder()
        sb.appendLine("Lucky2D ဘောင်ချာ")
        sb.appendLine("အမှတ်: No.${v.no}")
        sb.appendLine("ကိုယ်စားလှယ်: ${v.agent}")
        sb.appendLine("ရက်စွဲ: $dotDate (${viewModel.selectedSession})")
        sb.appendLine("-------------------")
        v.items.forEach { item ->
            sb.appendLine("${item.n} = ${item.a}")
        }
        sb.appendLine("-------------------")
        sb.appendLine("စုစုပေါင်း: $total")
        sb.appendLine("ကော်မရှင် (${LotteryViewModel.formatRate(rate)}%): $comm")
        sb.append("ကျန်ငွေ: $net")

        ShareHelper.copyToClipboard(context, sb.toString(), "Lucky2D Voucher")
        copiedVoucherId = v.id
        viewModel.showToast("ဘောင်ချာ စာသားကူးယူပြီးပါပြီ")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        LuckyHeader(
            title = headerTitle,
            viewModel = viewModel,
            showBack = true,
            onBack = onBack
        )

        RoundRequiredBanner(viewModel = viewModel, onOpenSettings = onOpenSettings)

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ဘောင်ချာ ရှာရန်", color = LuckyTextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LuckyTextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = LuckyTextMuted)
                    }
                }
            },
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

        if (viewModel.voucherFilterAgent != "ALL") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ထိုးသူ: ${viewModel.voucherFilterAgent}", fontSize = 12.sp, color = LuckyTextSecondary)
                Text(
                    text = "အားလုံးကြည့်ရန်",
                    fontSize = 12.sp,
                    color = LuckyGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        CleanAudio2D.playClick(context)
                        viewModel.clearVoucherFilter()
                    }
                )
            }
        }

        // Vouchers List (Newest First)
        if (filteredVouchers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, LuckyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = LuckyTextMuted, modifier = Modifier.size(34.dp))
                        Text("ဘောင်ချာမှတ်တမ်း မရှိသေးပါ", fontSize = 14.sp, color = LuckyTextMuted)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredVouchers, key = { it.id }) { voucher ->
                    val isCopied = copiedVoucherId == voucher.id
                    val total = voucher.items.sumOf { it.a }
                    val initial = voucher.agent.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, LuckyBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Dark-Green Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LuckyGreen)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "အမှတ် ${voucher.no}",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "အချိန် : ${voucher.time}",
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 12.sp
                                )
                            }

                            // Agent Info Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(LuckyGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(initial, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "အိုင်ဒီ : ${voucher.agentId} , အမည် : ${voucher.agent}",
                                        fontSize = 14.sp,
                                        color = LuckyTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${voucher.items.size} ကွက်",
                                    fontSize = 14.sp,
                                    color = LuckyTextSecondary
                                )
                            }
                            HorizontalDivider(color = Color(0xFFE5ECE9))

                            // Bet Items Table
                            voucher.items.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 0) Color.White else LuckyRowAlt)
                                        .combinedClickable(
                                            onClick = {
                                                CleanAudio2D.playClick(context)
                                                betItemToEdit = item
                                                editNum = item.n
                                                editAmt = item.a.toString()
                                            },
                                            onLongClick = {
                                                CleanAudio2D.playClick(context)
                                                betItemToEdit = item
                                                editNum = item.n
                                                editAmt = item.a.toString()
                                            }
                                        )
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        fontSize = 14.sp,
                                        color = LuckyTextSecondary,
                                        modifier = Modifier.width(48.dp)
                                    )
                                    Text(
                                        text = item.n,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LuckyGreen,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${item.a}",
                                        fontSize = 15.sp,
                                        color = LuckyTextPrimary,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(92.dp)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFEEF3F1))
                            }

                            // Footer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF6FAF8))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "စုစုပေါင်း : $total",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LuckyGreen
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { copyVoucherText(voucher) }) {
                                        Icon(
                                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "ကူးယူမည်",
                                            tint = if (isCopied) LuckyGreen else LuckyTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            CleanAudio2D.playClick(context)
                                            voucherToDelete = voucher
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "ဖျက်မည်",
                                            tint = LuckyDanger,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Dark-Green Bottom Total Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LuckyGreen,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("စုစုပေါင်း:", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("$totalVoucherAmount", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Edit Single Bet Item Dialog
    betItemToEdit?.let { item ->
        AlertDialog(
            onDismissRequest = { betItemToEdit = null },
            title = {
                Text("ဂဏန်း ပြင်ဆင်/ဖျက်မည်", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editNum,
                        onValueChange = { editNum = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("ဂဏန်း (၂ လုံး)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmt,
                        onValueChange = { editAmt = it.filter { ch -> ch.isDigit() } },
                        label = { Text("ငွေပမာဏ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = editNum.trim().padStart(2, '0')
                        val amt = editAmt.toIntOrNull() ?: 0
                        if (num.length == 2 && amt > 0) {
                            viewModel.editVoucherNumber(item.id, num, amt)
                            betItemToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Text("သိမ်းမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            viewModel.deleteVoucherNumber(item.id)
                            betItemToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                    ) {
                        Text("ဖျက်မည်")
                    }
                    TextButton(onClick = { betItemToEdit = null }) {
                        Text("ပိတ်မည်", color = LuckyTextMuted)
                    }
                }
            }
        )
    }

    // Delete Voucher Confirmation Dialog
    voucherToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { voucherToDelete = null },
            title = {
                Text("ဘောင်ချာ ဖျက်ရန် သေချာပါသလား?", fontWeight = FontWeight.Bold, color = LuckyDanger)
            },
            text = {
                Text("ဘောင်ချာ အမှတ် ${v.no} (${v.agent}) အား ဖျက်ပါက ထိုးထားသော ဂဏန်းအားလုံး စာရင်းမှ ဖယ်ရှားသွားမည် ဖြစ်သည်။")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVoucher(v.id)
                        voucherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                ) {
                    Text("ဖျက်မည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { voucherToDelete = null }) {
                    Text("မဖျက်ပါ", color = LuckyTextMuted)
                }
            }
        )
    }
}
