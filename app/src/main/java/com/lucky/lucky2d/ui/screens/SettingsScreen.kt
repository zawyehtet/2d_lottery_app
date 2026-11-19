package com.lucky.lucky2d.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky.lucky2d.ui.components.LuckyHeader
import com.lucky.lucky2d.ui.theme.LuckyBg
import com.lucky.lucky2d.ui.theme.LuckyBorder
import com.lucky.lucky2d.ui.theme.LuckyCardSoft
import com.lucky.lucky2d.ui.theme.LuckyDanger
import com.lucky.lucky2d.ui.theme.LuckyGreen
import com.lucky.lucky2d.ui.theme.LuckySuccess
import com.lucky.lucky2d.ui.theme.LuckyTextMuted
import com.lucky.lucky2d.ui.theme.LuckyTextPrimary
import com.lucky.lucky2d.utils.CleanAudio2D
import com.lucky.lucky2d.viewmodel.LotteryViewModel

private enum class SettingsModal { BLOCKED, SESSIONS, WINNING, PASSWORD, BRAKE, RESET }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: LotteryViewModel,
    onBack: () -> Unit,
    onOpenAgentSettlement: () -> Unit
) {
    val context = LocalContext.current
    var activeModal by remember { mutableStateOf<SettingsModal?>(null) }

    // Modal inputs
    var newBlockedNum by remember { mutableStateOf("") }
    var newSessionName by remember { mutableStateOf("") }
    var newSessionRate by remember { mutableStateOf("15") }
    var newSessionMult by remember { mutableStateOf("80") }
    var winNumInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var brakeInput by remember { mutableStateOf("") }
    var resetConfirmText by remember { mutableStateOf("") }

    val settings = viewModel.settings
    val blockedList = remember(viewModel.blocked) { viewModel.blocked.sorted() }
    val dotDate = LotteryViewModel.formatDotDate(viewModel.selectedDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        LuckyHeader(
            title = "ဆက်တင်",
            viewModel = viewModel,
            showBack = true,
            onBack = onBack,
            showSessionPill = false
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ပင်မ သတ်မှတ်ချက်များ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = LuckyGreen
            )

            // 1. Blocked Numbers
            SettingsItemCard(
                icon = Icons.Default.Block,
                iconTint = Color(0xFFD93025),
                iconBg = Color(0xFFFDECEC),
                title = "မရဂဏန်းများ",
                subtitle = if (blockedList.isEmpty()) "ပိတ်ထားသော ဂဏန်း မရှိသေးပါ" else blockedList.joinToString(", "),
                onClick = {
                    CleanAudio2D.playClick(context)
                    newBlockedNum = ""
                    activeModal = SettingsModal.BLOCKED
                }
            )

            // 2. Sessions
            SettingsItemCard(
                icon = Icons.Default.Schedule,
                iconTint = LuckyGreen,
                iconBg = Color(0xFFE9F0ED),
                title = "အကြိမ်ရေ",
                subtitle = viewModel.selectedSession.ifBlank { "အကြိမ်မရှိ" },
                onClick = {
                    CleanAudio2D.playClick(context)
                    newSessionName = ""
                    activeModal = SettingsModal.SESSIONS
                }
            )

            // 3. Winning Number & Pauk Tee Check
            SettingsItemCard(
                icon = Icons.Default.EmojiEvents,
                iconTint = Color(0xFFE67E00),
                iconBg = Color(0xFFFEF3E6),
                title = "ထွက်ဂဏန်းများ (ပေါက်သီး)",
                subtitle = if (viewModel.currentWinningNumber.isNotEmpty()) {
                    "ပေါက်သီး: ${viewModel.currentWinningNumber}"
                } else {
                    "ပေါက်သီး ထည့်သွင်းရန်နှင့် စစ်ဆေးရန်"
                },
                onClick = {
                    CleanAudio2D.playClick(context)
                    winNumInput = viewModel.currentWinningNumber
                    activeModal = SettingsModal.WINNING
                }
            )

            // 4. Password Lock & Change Password
            SettingsItemCard(
                icon = Icons.Default.Lock,
                iconTint = Color(0xFFE67E00),
                iconBg = Color(0xFFFEF3E6),
                title = "Password Lock & Change Password",
                subtitle = if (settings.passwordLockEnabled) "စကားဝှက် ဖွင့်ထားသည်" else "လုံခြုံရေး စကားဝှက် သတ်မှတ်ရန်",
                onClick = {
                    CleanAudio2D.playClick(context)
                    newPasswordInput = ""
                    confirmPasswordInput = ""
                    passwordError = null
                    activeModal = SettingsModal.PASSWORD
                }
            )

            // 5. Reset
            SettingsItemCard(
                icon = Icons.Default.Delete,
                iconTint = Color(0xFFD93025),
                iconBg = Color(0xFFFDECEC),
                title = "Reset",
                subtitle = "ဘောင်ချာနှင့် မှတ်တမ်းအားလုံး အသစ်ပြန်စရန်",
                onClick = {
                    CleanAudio2D.playClick(context)
                    resetConfirmText = ""
                    activeModal = SettingsModal.RESET
                }
            )

            // 6. Brake Limit
            SettingsItemCard(
                icon = Icons.Default.Speed,
                iconTint = LuckySuccess,
                iconBg = Color(0xFFEAF4EB),
                title = "ဘရိတ် သတ်မှတ်ချက်",
                subtitle = "ဘရိတ်: ${settings.brakeLimit}",
                onClick = {
                    CleanAudio2D.playClick(context)
                    brakeInput = settings.brakeLimit.toString()
                    activeModal = SettingsModal.BRAKE
                }
            )

            // 7. Sound Toggle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, LuckyBorder),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE9F0ED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = if (settings.soundEnabled) LuckyGreen else LuckyTextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text("အသံ (Sound)", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = LuckyTextPrimary)
                            Text(
                                text = if (settings.soundEnabled) "အသံဖွင့်ထားသည်" else "အသံပိတ်ထားသည်",
                                fontSize = 13.sp,
                                color = LuckyTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(soundEnabled = enabled) }
                            if (enabled) CleanAudio2D.playClick(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LuckyGreen
                        )
                    )
                }
            }
        }
    }

    // MODAL: Blocked Numbers
    if (activeModal == SettingsModal.BLOCKED) {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("မရဂဏန်းများ စီမံခြင်း", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newBlockedNum,
                            onValueChange = { newBlockedNum = it.filter { ch -> ch.isDigit() }.take(2) },
                            placeholder = { Text("ဥပမာ - 99") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newBlockedNum.isNotEmpty()) {
                                    viewModel.toggleBlockedNumber(newBlockedNum)
                                    CleanAudio2D.playClick(context)
                                    newBlockedNum = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("ပိတ်မည်", fontSize = 12.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LuckyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 220.dp)
                    ) {
                        if (blockedList.isEmpty()) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                                Text("ပိတ်ထားသော ဂဏန်း မရှိသေးပါ", fontSize = 12.sp, color = LuckyTextMuted)
                            }
                        } else {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                blockedList.forEach { num ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                        modifier = Modifier.clickable {
                                            CleanAudio2D.playClick(context)
                                            viewModel.toggleBlockedNumber(num)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(num, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = LuckyDanger, fontSize = 13.sp)
                                            Icon(Icons.Default.Close, contentDescription = null, tint = LuckyDanger, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeModal = null }) {
                    Text("ပိတ်မည်", color = LuckyGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // MODAL: Sessions Management
    if (activeModal == SettingsModal.SESSIONS) {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("အကြိမ်ရေ စီမံခြင်း", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newSessionName,
                        onValueChange = { newSessionName = it },
                        label = { Text("အကြိမ်အမည် (ဥပမာ - a, p)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newSessionRate,
                            onValueChange = { newSessionRate = it },
                            label = { Text("ကော် %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newSessionMult,
                            onValueChange = { newSessionMult = it.filter { ch -> ch.isDigit() } },
                            label = { Text("အဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            if (newSessionName.isNotBlank()) {
                                viewModel.addSession(
                                    newSessionName.trim(),
                                    newSessionRate.toDoubleOrNull() ?: 15.0,
                                    newSessionMult.toIntOrNull() ?: 80
                                )
                                newSessionName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ အကြိမ်အသစ် ထည့်သွင်းမည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider()

                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(viewModel.sessions, key = { it.id }) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${s.name} (${LotteryViewModel.formatRate(s.rate)}% • x${s.mult})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LuckyTextPrimary
                                )
                                IconButton(
                                    onClick = { viewModel.deleteSessionRecord(s.id, s.name) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = LuckyDanger, modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeModal = null }) {
                    Text("ပိတ်မည်", color = LuckyGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // MODAL: Winning Number & Pauk Tee Check
    if (activeModal == SettingsModal.WINNING) {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("ပေါက်ဂဏန်း ထည့်သွင်းမည်", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ရက်စွဲ: $dotDate (${viewModel.selectedSession.ifBlank { "အကြိမ်မရှိ" }})", fontSize = 13.sp, color = LuckyTextMuted)
                    OutlinedTextField(
                        value = winNumInput,
                        onValueChange = { winNumInput = it.filter { ch -> ch.isDigit() }.take(2) },
                        placeholder = { Text("00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            CleanAudio2D.playClick(context)
                            if (winNumInput.length == 2) {
                                viewModel.setWinningNumber(winNumInput)
                            }
                            activeModal = null
                            onOpenAgentSettlement()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyCardSoft, contentColor = LuckyGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ပေါက်သီး စစ်ဆေးမည် (ကိုယ်စားလှယ် စာရင်း)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (winNumInput.isNotEmpty()) {
                            viewModel.setWinningNumber(winNumInput)
                            activeModal = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Text("အတည်ပြုမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeModal = null }) {
                    Text("မလုပ်ပါ", color = LuckyTextMuted)
                }
            }
        )
    }

    // MODAL: Password Lock & Change Password
    if (activeModal == SettingsModal.PASSWORD) {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("Password Settings", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("App Password Lock ဖွင့်မည်", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = settings.passwordLockEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateSettings { it.copy(passwordLockEnabled = enabled) }
                                CleanAudio2D.playClick(context)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = LuckyGreen)
                        )
                    }
                    HorizontalDivider()
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = {
                            newPasswordInput = it
                            passwordError = null
                        },
                        label = { Text("စကားဝှက် အသစ်") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = {
                            confirmPasswordInput = it
                            passwordError = null
                        },
                        label = { Text("စကားဝှက် ထပ်မံအတည်ပြုပါ") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    passwordError?.let {
                        Text(it, color = LuckyDanger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newPasswordInput.trim()
                        if (trimmed.isEmpty()) {
                            passwordError = "စကားဝှက် အသစ် ရိုက်ထည့်ပါ"
                            return@Button
                        }
                        if (trimmed != confirmPasswordInput.trim()) {
                            passwordError = "စကားဝှက် နှစ်ခု မတူညီပါ"
                            return@Button
                        }
                        viewModel.updateSettings { it.copy(password = trimmed) }
                        CleanAudio2D.playSuccess(context)
                        viewModel.showToast("စကားဝှက် အသစ် ပြောင်းလဲပြီးပါပြီ")
                        activeModal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeModal = null }) {
                    Text("ပိတ်မည်", color = LuckyTextMuted)
                }
            }
        )
    }

    // MODAL: Brake Limit
    if (activeModal == SettingsModal.BRAKE) {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("ဘရိတ် သတ်မှတ်ချက်", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ဂဏန်းတစ်လုံးချင်းစီအတွက် မူလဘရိတ်", fontSize = 13.sp, color = LuckyTextMuted)
                    OutlinedTextField(
                        value = brakeInput,
                        onValueChange = { brakeInput = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = brakeInput.toIntOrNull() ?: 0
                        viewModel.updateSettings { it.copy(brakeLimit = limit) }
                        CleanAudio2D.playSuccess(context)
                        viewModel.showToast("ဘရိတ်သတ်မှတ်ချက် ပြင်ဆင်ပြီးပါပြီ")
                        activeModal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeModal = null }) {
                    Text("မလုပ်ပါ", color = LuckyTextMuted)
                }
            }
        )
    }

    // MODAL: Master Reset
    if (activeModal == SettingsModal.RESET) {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("ဒေတာများအားလုံး ဖျက်ဆီးမည်လား?", fontWeight = FontWeight.Bold, color = LuckyDanger, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ဤလုပ်ဆောင်ချက်သည် ဘောင်ချာများ၊ ကော်မရှင်များ၊ တင်ကွက်များနှင့် ရက်ချုပ်မှတ်တမ်းများ အားလုံးကို အပြီးတိုင် ဖျက်ဆီးပါမည်။",
                        fontSize = 13.sp,
                        color = LuckyTextMuted
                    )
                    Text("အတည်ပြုရန် reset ဟု ရိုက်ထည့်ပါ:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = resetConfirmText,
                        onValueChange = { resetConfirmText = it },
                        placeholder = { Text("reset") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        activeModal = null
                    },
                    enabled = resetConfirmText.trim().equals("reset", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                ) {
                    Text("ရှင်းလင်းမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeModal = null }) {
                    Text("မလုပ်ပါ", color = LuckyTextMuted)
                }
            }
        )
    }
}

@Composable
private fun SettingsItemCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LuckyBorder),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = LuckyTextPrimary)
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = LuckyTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = LuckyTextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
