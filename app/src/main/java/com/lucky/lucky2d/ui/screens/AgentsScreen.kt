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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky.lucky2d.model.Agent
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgentsScreen(
    viewModel: LotteryViewModel,
    onBack: () -> Unit,
    onOpenNumberEntry: (Agent) -> Unit,
    onOpenAgentVouchers: (Agent) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingAgent by remember { mutableStateOf<Agent?>(null) }
    var longPressedAgent by remember { mutableStateOf<Agent?>(null) }
    var agentToDelete by remember { mutableStateOf<Agent?>(null) }

    // Form state
    var nameInput by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("15") }
    var multInput by remember { mutableStateOf("80") }
    var phoneInput by remember { mutableStateOf("") }

    val filteredAgents = remember(viewModel.agents, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) viewModel.agents
        else viewModel.agents.filter {
            it.name.lowercase().contains(q) || it.phoneNumber.lowercase().contains(q)
        }
    }

    fun openAdd() {
        CleanAudio2D.playClick(context)
        editingAgent = null
        nameInput = ""
        rateInput = "15"
        multInput = "80"
        phoneInput = ""
        showAddEditDialog = true
    }

    fun openEdit(agent: Agent) {
        CleanAudio2D.playClick(context)
        editingAgent = agent
        nameInput = agent.name
        rateInput = LotteryViewModel.formatRate(agent.rate)
        multInput = agent.mult.toString()
        phoneInput = agent.phoneNumber
        showAddEditDialog = true
    }

    Scaffold(
        containerColor = LuckyBg,
        topBar = {
            LuckyHeader(
                title = "ကော်မရှင်",
                viewModel = viewModel,
                showBack = true,
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openAdd() },
                shape = CircleShape,
                containerColor = LuckyGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .size(58.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "ကော်မရှင်အသစ်",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            RoundRequiredBanner(viewModel = viewModel, onOpenSettings = onOpenSettings)

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ကော်မရှင် ရှာရန်", color = LuckyTextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = LuckyTextPrimary
                    )
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
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )

            if (filteredAgents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, LuckyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(LuckyCardSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = LuckyGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text("ကော်မရှင် မရှိသေးပါ", color = LuckyTextMuted, fontSize = 14.sp)
                            TextButton(onClick = { openAdd() }) {
                                Text(
                                    "+ ကော်မရှင်အသစ် ထည့်သွင်းပါ",
                                    color = LuckyGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredAgents, key = { _, ag -> ag.id }) { index, agent ->
                        val summary = viewModel.getAgentSummaryData(agent)
                        val rateText = LotteryViewModel.formatRate(agent.rate)
                        val trimmedPhone = agent.phoneNumber.trim()

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, LuckyBorder),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        CleanAudio2D.playClick(context)
                                        longPressedAgent = agent
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Dark-Green Card Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LuckyGreen)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = LuckyGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = agent.name,
                                                color = Color.White,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "ကော်: $rateText% | အဆ: ${agent.mult}",
                                                color = Color.White.copy(alpha = 0.92f),
                                                fontSize = 12.sp
                                            )
                                            if (trimmedPhone.isNotEmpty()) {
                                                Text(
                                                    text = "📞 $trimmedPhone",
                                                    color = Color(0xFFD1FAE5),
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // White Pill "+ ထိုးရန်" button
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.clickable {
                                            CleanAudio2D.playClick(context)
                                            onOpenNumberEntry(agent)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = LuckyGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "ထိုးရန်",
                                                color = LuckyGreen,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // White Card Body
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "စုစုပေါင်းထိုးကြေး",
                                                fontSize = 12.sp,
                                                color = LuckyTextSecondary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${summary.totalBet}",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LuckyTextPrimary
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ကော်မရှင် ($rateText%)",
                                                fontSize = 12.sp,
                                                color = LuckyTextSecondary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${summary.commission}",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LuckyTextPrimary
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ကော်နုတ်ပြီး",
                                                fontSize = 12.sp,
                                                color = LuckyTextSecondary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${summary.netAmount}",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LuckyGreen
                                            )
                                        }
                                    }

                                    if (summary.payout > 0L) {
                                        Spacer(Modifier.height(10.dp))
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.EmojiEvents,
                                                    contentDescription = null,
                                                    tint = LuckyDanger,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "ပေါက်ကြေး",
                                                    color = LuckyDanger,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "${summary.payout}",
                                                color = LuckyDanger,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Spacer(Modifier.height(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = LuckyCardSoft,
                                        border = BorderStroke(1.dp, Color(0xFFCBD8D2)),
                                        modifier = Modifier.clickable {
                                            CleanAudio2D.playClick(context)
                                            onOpenAgentVouchers(agent)
                                        }
                                    ) {
                                        Text(
                                            text = "ဘောင်ချာ = ${summary.voucherCount}",
                                            color = LuckyGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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

    // Long-Press Edit & Delete Action Dialog
    longPressedAgent?.let { ag ->
        AlertDialog(
            onDismissRequest = { longPressedAgent = null },
            title = {
                Text(
                    text = ag.name,
                    fontWeight = FontWeight.Bold,
                    color = LuckyGreen
                )
            },
            text = {
                Text(
                    text = "ကော်မရှင် အချက်အလက်ကို ပြင်ဆင်ရန် သို့မဟုတ် ဖျက်ရန် ရွေးချယ်ပါ။",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        longPressedAgent = null
                        openEdit(ag)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ပြင်ဆင်မည်")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { longPressedAgent = null }) {
                        Text("ပိတ်မည်", color = LuckyTextMuted)
                    }
                    Button(
                        onClick = {
                            longPressedAgent = null
                            agentToDelete = ag
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ဖျက်မည်")
                    }
                }
            }
        )
    }

    // Add / Edit Agent Dialog
    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (editingAgent != null) "ကော်မရှင် အချက်အလက်ပြင်ဆင်မည်" else "ကော်မရှင် အသစ်ထည့်သွင်းမည်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = LuckyGreen
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("ကော်မရှင် အမည် *") },
                        placeholder = { Text("ဥပမာ - ကိုကျော်") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("ကော်မရှင် ရာခိုင်နှုန်း (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = multInput,
                        onValueChange = { multInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("ပေါက်ကြေး အမြှောက်နှုန်း (ဆ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("ဖုန်းနံပါတ် (Optional)") },
                        placeholder = { Text("09xxxxxxxxx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = nameInput.trim()
                        if (trimmedName.isEmpty()) return@Button
                        val rate = rateInput.toDoubleOrNull() ?: 0.0
                        val mult = multInput.toIntOrNull() ?: 80
                        val phone = phoneInput.trim()
                        val currentEdit = editingAgent
                        if (currentEdit != null) {
                            viewModel.updateAgent(
                                currentEdit.copy(
                                    name = trimmedName,
                                    rate = rate,
                                    mult = mult,
                                    phoneNumber = phone
                                )
                            )
                        } else {
                            viewModel.addAgent(trimmedName, rate, mult, phone)
                        }
                        showAddEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("မလုပ်တော့ပါ", color = LuckyTextMuted)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    agentToDelete?.let { ag ->
        AlertDialog(
            onDismissRequest = { agentToDelete = null },
            title = {
                Text("ဖျက်ရန် သေချာပါသလား?", fontWeight = FontWeight.Bold, color = LuckyDanger)
            },
            text = {
                Text("ကော်မရှင် '${ag.name}' အား ဖျက်မည် ဖြစ်သည်။")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAgent(ag.id)
                        agentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                ) {
                    Text("ဖျက်မည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { agentToDelete = null }) {
                    Text("မဖျက်ပါ", color = LuckyTextMuted)
                }
            }
        )
    }
}
