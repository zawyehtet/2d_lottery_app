package com.lucky.lucky2d.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky.lucky2d.logic.PatternAlgorithms
import com.lucky.lucky2d.model.BetItem
import com.lucky.lucky2d.model.RejectedAlertInfo
import com.lucky.lucky2d.ui.components.LuckyLogoCircle
import com.lucky.lucky2d.ui.theme.LuckyBg
import com.lucky.lucky2d.ui.theme.LuckyBorder
import com.lucky.lucky2d.ui.theme.LuckyCardSoft
import com.lucky.lucky2d.ui.theme.LuckyDanger
import com.lucky.lucky2d.ui.theme.LuckyGreen
import com.lucky.lucky2d.ui.theme.LuckyInputBorder
import com.lucky.lucky2d.ui.theme.LuckyKeyBg
import com.lucky.lucky2d.ui.theme.LuckyKeyBorder
import com.lucky.lucky2d.ui.theme.LuckyMessenger
import com.lucky.lucky2d.ui.theme.LuckyRowAlt
import com.lucky.lucky2d.ui.theme.LuckySuccess
import com.lucky.lucky2d.ui.theme.LuckyTextMuted
import com.lucky.lucky2d.ui.theme.LuckyTextPrimary
import com.lucky.lucky2d.ui.theme.LuckyTextSecondary
import com.lucky.lucky2d.ui.theme.LuckyViber
import com.lucky.lucky2d.utils.CleanAudio2D
import com.lucky.lucky2d.utils.ShareHelper
import com.lucky.lucky2d.viewmodel.LotteryViewModel

private enum class EntryField { NUMBER, PATTERN, AMOUNT }

private data class QuickShareData(
    val phoneNumber: String,
    val message: String
)

private val SINGLE_DIGIT_PATTERNS = setOf("ထိပ်", "နောက်", "အပါ", "ဘရိတ်")
private val AUTO_ADVANCE_PATTERNS = setOf("ပါဝါ", "ညီအစ်ကို", "အပူး", "စုံစုံ", "မမ", "စုံမ", "မစုံ", "နက္ခတ်")
private val CHIP_PATTERNS = listOf("ဒဲ့", "ပါဝါ", "ညီအစ်ကို", "ထိပ်", "နောက်", "အပူး", "စုံစုံ", "မမ", "စုံမ", "မစုံ", "ဘရိတ်")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberEntryScreen(
    viewModel: LotteryViewModel,
    onClose: () -> Unit,
    onOpenAgentVouchers: () -> Unit
) {
    val context = LocalContext.current

    var activeField by remember { mutableStateOf(EntryField.NUMBER) }
    var inputNumber by remember { mutableStateOf("") }
    var inputPattern by remember { mutableStateOf("ဒဲ့") }
    var inputAmount by remember { mutableStateOf("") }
    var isReverseOn by remember { mutableStateOf(false) }
    var shouldOverwriteAmount by remember { mutableStateOf(false) }
    var amountValidationError by remember { mutableStateOf(false) }

    // Dialog states
    var quickEntryMode by remember { mutableStateOf<Int?>(null) }
    var quickEntryText by remember { mutableStateOf("") }
    var showAgentPicker by remember { mutableStateOf(viewModel.entryAgent == null) }
    var showConfirmBetDialog by remember { mutableStateOf(false) }
    var showKwayDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var editNumInput by remember { mutableStateOf("") }
    var editAmtInput by remember { mutableStateOf("") }
    var rejectedAlert by remember { mutableStateOf<RejectedAlertInfo?>(null) }
    var quickShareData by remember { mutableStateOf<QuickShareData?>(null) }

    val listState = rememberLazyListState()
    val entryItems = viewModel.entryItems
    val totalAmount = remember(entryItems) { entryItems.sumOf { it.a } }

    LaunchedEffect(entryItems.size) {
        if (entryItems.isNotEmpty()) {
            listState.animateScrollToItem(entryItems.lastIndex)
        }
    }

    LaunchedEffect(inputAmount) {
        amountValidationError = if (inputAmount.isEmpty()) false
        else !PatternAlgorithms.validateBetAmount(inputAmount).isValid
    }

    fun advanceToAmount() {
        activeField = EntryField.AMOUNT
        shouldOverwriteAmount = inputAmount.isNotEmpty()
    }

    fun onKeypadNumPress(num: String) {
        if (!viewModel.hasRound) {
            viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        CleanAudio2D.playClick(context)

        when (activeField) {
            EntryField.NUMBER -> {
                if (inputPattern == "ခွေ") {
                    if (inputNumber.length < 9) {
                        inputNumber = (inputNumber + num).take(9)
                    }
                } else if (SINGLE_DIGIT_PATTERNS.contains(inputPattern)) {
                    inputNumber = num.take(1)
                    activeField = EntryField.PATTERN
                    shouldOverwriteAmount = false
                } else {
                    if (inputNumber.length < 2) {
                        val next = (inputNumber + num).take(2)
                        inputNumber = next
                        if (next.length == 2) {
                            activeField = EntryField.PATTERN
                            shouldOverwriteAmount = false
                        }
                    }
                }
            }
            EntryField.PATTERN -> {
                if (SINGLE_DIGIT_PATTERNS.contains(inputPattern) && inputNumber.isEmpty()) {
                    inputNumber = num.take(1)
                    advanceToAmount()
                } else if (inputPattern == "ခွေ" && inputNumber.length < 2) {
                    inputNumber = (inputNumber + num).take(9)
                    activeField = EntryField.NUMBER
                } else {
                    if (inputPattern.isEmpty()) inputPattern = "ဒဲ့"
                    inputAmount = num.filter { it.isDigit() }
                    activeField = EntryField.AMOUNT
                    shouldOverwriteAmount = false
                }
            }
            EntryField.AMOUNT -> {
                val allowed = num.filter { it.isDigit() || it == '/' }
                if (shouldOverwriteAmount) {
                    inputAmount = allowed
                    shouldOverwriteAmount = false
                } else {
                    inputAmount = if (allowed.contains('/') && inputAmount.contains('/')) {
                        inputAmount + allowed.replace("/", "")
                    } else {
                        inputAmount + allowed
                    }
                }
            }
        }
    }

    fun onKeypadSlashPress() {
        if (!viewModel.hasRound) {
            viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        CleanAudio2D.playClick(context)
        if (activeField == EntryField.NUMBER || activeField == EntryField.PATTERN) {
            if (inputPattern.isEmpty()) inputPattern = "ဒဲ့"
            advanceToAmount()
            if (inputAmount.isNotEmpty() && !inputAmount.contains('/')) {
                inputAmount += "/"
                shouldOverwriteAmount = false
            }
        } else {
            shouldOverwriteAmount = false
            if (!inputAmount.contains('/')) {
                inputAmount = if (inputAmount.isNotEmpty()) "$inputAmount/" else "/"
            }
        }
    }

    fun onPatternSelected(pattern: String) {
        if (!viewModel.hasRound) {
            viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        CleanAudio2D.playClick(context)
        inputPattern = pattern

        if (SINGLE_DIGIT_PATTERNS.contains(pattern) && inputNumber.length > 1) {
            inputNumber = inputNumber.take(1)
        }

        when {
            activeField == EntryField.PATTERN -> advanceToAmount()
            AUTO_ADVANCE_PATTERNS.contains(pattern) -> advanceToAmount()
            SINGLE_DIGIT_PATTERNS.contains(pattern) && inputNumber.length == 1 -> advanceToAmount()
            pattern == "ဒဲ့" && inputNumber.length == 2 -> advanceToAmount()
            else -> {
                activeField = EntryField.NUMBER
                shouldOverwriteAmount = false
            }
        }
    }

    fun onAdvanceFocusPress() {
        CleanAudio2D.playClick(context)
        when (activeField) {
            EntryField.NUMBER -> {
                activeField = EntryField.PATTERN
                shouldOverwriteAmount = false
            }
            EntryField.PATTERN -> {
                if (inputPattern.isEmpty()) inputPattern = "ဒဲ့"
                advanceToAmount()
            }
            EntryField.AMOUNT -> {
                activeField = EntryField.NUMBER
                shouldOverwriteAmount = false
            }
        }
    }

    fun handleAddBet(withDoublesForKway: Boolean = false) {
        CleanAudio2D.playClick(context)
        if (!viewModel.hasRound) {
            viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        if (viewModel.entryAgent == null) {
            viewModel.showToast("ထိုးသူ အရင်ရွေးပါ")
            showAgentPicker = true
            return
        }

        // Check full expression e.g. 58=3000/2000
        val fullInput = when {
            inputNumber.contains('=') -> inputNumber
            inputAmount.contains('=') -> inputAmount
            else -> null
        }
        if (fullInput != null) {
            val parsed = PatternAlgorithms.parseBetInput(fullInput)
            if (parsed.isNotEmpty()) {
                val betList = mutableListOf<BetItem>()
                val blockedFound = mutableListOf<String>()
                for (item in parsed) {
                    if (viewModel.blocked.contains(item.n)) {
                        blockedFound.add(item.n)
                    } else {
                        betList.add(BetItem(viewModel.nextId(), item.n, item.a))
                    }
                }
                val overLimits = if (betList.isNotEmpty()) viewModel.addEntryItems(betList) else emptyList()
                if (blockedFound.isNotEmpty() || overLimits.isNotEmpty()) {
                    rejectedAlert = RejectedAlertInfo(
                        blockedNumbers = blockedFound.distinct(),
                        overLimitEntries = overLimits,
                        brakeLimit = viewModel.settings.brakeLimit
                    )
                }
                if (betList.isNotEmpty()) {
                    inputNumber = ""
                    inputPattern = "ဒဲ့"
                    inputAmount = ""
                    isReverseOn = false
                    activeField = EntryField.NUMBER
                    shouldOverwriteAmount = false
                }
                return
            }
        }

        val amtValidation = PatternAlgorithms.validateBetAmount(inputAmount)
        if (!amtValidation.isValid) {
            amountValidationError = true
            viewModel.showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            activeField = EntryField.AMOUNT
            return
        }

        val isSplit = inputAmount.contains('/')
        val directAmount: Int
        val reverseAmount: Int
        if (isSplit) {
            val parts = inputAmount.split('/')
            directAmount = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
            reverseAmount = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        } else {
            directAmount = inputAmount.trim().toIntOrNull() ?: 0
            reverseAmount = 0
        }

        val effectivePattern = inputPattern.ifEmpty { "ဒဲ့" }
        when {
            effectivePattern == "ခွေ" -> {
                val distinct = inputNumber.filter { it.isDigit() }.toSet()
                if (distinct.size < 2) {
                    viewModel.showToast("ခွေရန် အနည်းဆုံး ဂဏန်း ၂ လုံး ရိုက်ပါ")
                    activeField = EntryField.NUMBER
                    return
                }
            }
            SINGLE_DIGIT_PATTERNS.contains(effectivePattern) -> {
                if (inputNumber.isEmpty()) {
                    viewModel.showToast("ဂဏန်း ၁ လုံး ရိုက်ထည့်ပါ")
                    activeField = EntryField.NUMBER
                    return
                }
            }
            effectivePattern == "ဒဲ့" -> {
                if (inputNumber.length != 2) {
                    viewModel.showToast("၂ လုံးဂဏန်း အပြည့်ရိုက်ပါ")
                    activeField = EntryField.NUMBER
                    return
                }
            }
        }

        val generated = PatternAlgorithms.generateNumbersForPattern(
            pattern = effectivePattern,
            inputNumber = inputNumber,
            withReverse = if (isSplit) false else isReverseOn,
            withDoublesForKway = withDoublesForKway
        )

        if (generated.isEmpty()) {
            viewModel.showToast("ဂဏန်း မထွက်ရှိပါ")
            return
        }

        val betList = mutableListOf<BetItem>()
        val blockedFound = mutableListOf<String>()

        if (isSplit) {
            for (num in generated) {
                val rev = PatternAlgorithms.reverse2D(num)
                if (rev == num) {
                    val comb = directAmount + reverseAmount
                    if (viewModel.blocked.contains(num)) blockedFound.add(num)
                    else betList.add(BetItem(viewModel.nextId(), num, comb))
                } else {
                    if (viewModel.blocked.contains(num)) blockedFound.add(num)
                    else betList.add(BetItem(viewModel.nextId(), num, directAmount))

                    if (reverseAmount > 0) {
                        if (viewModel.blocked.contains(rev)) blockedFound.add(rev)
                        else betList.add(BetItem(viewModel.nextId(), rev, reverseAmount))
                    }
                }
            }
        } else {
            for (num in generated) {
                if (viewModel.blocked.contains(num)) blockedFound.add(num)
                else betList.add(BetItem(viewModel.nextId(), num, directAmount))
            }
        }

        val overLimits = if (betList.isNotEmpty()) viewModel.addEntryItems(betList) else emptyList()
        if (blockedFound.isNotEmpty() || overLimits.isNotEmpty()) {
            rejectedAlert = RejectedAlertInfo(
                blockedNumbers = blockedFound.distinct(),
                overLimitEntries = overLimits,
                brakeLimit = viewModel.settings.brakeLimit
            )
        }

        if (betList.isNotEmpty()) {
            inputNumber = ""
            inputPattern = "ဒဲ့"
            isReverseOn = false
            activeField = EntryField.NUMBER
            shouldOverwriteAmount = false
        }
    }

    fun handleQuickEntrySubmit() {
        CleanAudio2D.playClick(context)
        if (quickEntryText.isBlank()) return
        val res = if (quickEntryMode == 1) {
            PatternAlgorithms.parseQuickEntryMode1(quickEntryText, viewModel.blocked)
        } else {
            PatternAlgorithms.parseQuickEntryMode2(quickEntryText, viewModel.blocked)
        }

        val newItems = res.items.map { BetItem(viewModel.nextId(), it.n, it.a) }
        val overLimits = if (newItems.isNotEmpty()) viewModel.addEntryItems(newItems) else emptyList()

        if (res.blockedSkipped.isNotEmpty() || overLimits.isNotEmpty()) {
            rejectedAlert = RejectedAlertInfo(
                blockedNumbers = res.blockedSkipped.distinct(),
                overLimitEntries = overLimits,
                brakeLimit = viewModel.settings.brakeLimit
            )
        }

        if (newItems.isEmpty() && res.blockedSkipped.isEmpty()) {
            viewModel.showToast("ထိုးမည့် ဂဏန်း မတွေ့ပါ")
            return
        }

        quickEntryMode = null
        quickEntryText = ""
        if (newItems.isNotEmpty()) {
            viewModel.showToast("${newItems.size} ကွက် ထည့်သွင်းပြီးပါပြီ")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        // Top Header
        Surface(color = LuckyGreen, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    LuckyLogoCircle(sizeDp = 32)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "နေ့စွဲ : ${viewModel.selectedDate} , အကြိမ် : ${if (viewModel.hasRound) viewModel.selectedSession else "အကြိမ်မရှိ"}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        CleanAudio2D.playClick(context)
                        onClose()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ပိတ်မည်",
                        tint = Color.White
                    )
                }
            }
        }

        // Sub-header: Agent Info Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ထိုးသူ : ${viewModel.entryAgent?.name ?: "ရွေးချယ်ပါ"}",
                fontSize = 14.sp,
                color = LuckyTextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        CleanAudio2D.playClick(context)
                        showAgentPicker = true
                    }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ဘောင်ချာများ ကြည့်ရန် နှိပ်ပါ",
                fontSize = 14.sp,
                color = LuckyGreen,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    CleanAudio2D.playClick(context)
                    val ag = viewModel.entryAgent
                    viewModel.openAgentVouchersFilter(ag?.id, ag?.name)
                    onOpenAgentVouchers()
                }
            )
        }
        HorizontalDivider(color = LuckyBorder)

        // Main Entered Bets Table
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCardSoft)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "စဉ်",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LuckyTextPrimary,
                    modifier = Modifier.width(52.dp)
                )
                Text(
                    text = "ဂဏန်း",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LuckyTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "ပမာဏ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LuckyTextPrimary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(96.dp)
                )
            }
            HorizontalDivider(color = LuckyBorder)

            if (entryItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ဂဏန်းနှင့် ပမာဏ ရိုက်ထည့်ပါ",
                        fontSize = 16.sp,
                        color = LuckyTextMuted
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(entryItems, key = { _, item -> item.id }) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 0) Color.White else LuckyRowAlt)
                                .combinedClickable(
                                    onClick = {
                                        CleanAudio2D.playClick(context)
                                        editingItemIndex = index
                                        editNumInput = item.n
                                        editAmtInput = item.a.toString()
                                    },
                                    onLongClick = {
                                        CleanAudio2D.playClick(context)
                                        editingItemIndex = index
                                        editNumInput = item.n
                                        editAmtInput = item.a.toString()
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 15.sp,
                                color = LuckyTextSecondary,
                                modifier = Modifier.width(52.dp)
                            )
                            Text(
                                text = item.n,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LuckyGreen,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.a}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = LuckyTextPrimary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(96.dp)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFEAF0EE))
                    }
                }
            }

            // Middle Summary & Action Bar
            HorizontalDivider(color = LuckyBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEF3F1))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        CleanAudio2D.playClick(context)
                        quickEntryMode = 1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("အမြန်ထိုး", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = "စုစုပေါင်း = $totalAmount (${entryItems.size} ကွက်)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = LuckyTextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        CleanAudio2D.playClick(context)
                        showConfirmBetDialog = true
                    },
                    enabled = entryItems.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuckySuccess,
                        disabledContainerColor = Color(0xFFDFE6E3),
                        disabledContentColor = LuckyTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("ထိုးရန်", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Pinned Bottom Custom Compose Keypad
        HorizontalDivider(color = LuckyBorder)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 3 Preview Cells: ဂဏန်း | ဒဲ့ | ပမာဏ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InputPreviewCell(
                    text = inputNumber.ifEmpty { "ဂဏန်း" },
                    isPlaceholder = inputNumber.isEmpty(),
                    isFocused = activeField == EntryField.NUMBER,
                    isError = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CleanAudio2D.playClick(context)
                        activeField = EntryField.NUMBER
                        shouldOverwriteAmount = false
                    }
                )
                InputPreviewCell(
                    text = "${inputPattern.ifEmpty { "ဒဲ့" }}${if (isReverseOn) " (R)" else ""}",
                    isPlaceholder = false,
                    isFocused = activeField == EntryField.PATTERN,
                    isError = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CleanAudio2D.playClick(context)
                        activeField = EntryField.PATTERN
                        shouldOverwriteAmount = false
                    }
                )
                InputPreviewCell(
                    text = inputAmount.ifEmpty { "ပမာဏ" },
                    isPlaceholder = inputAmount.isEmpty(),
                    isFocused = activeField == EntryField.AMOUNT,
                    isError = amountValidationError,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CleanAudio2D.playClick(context)
                        activeField = EntryField.AMOUNT
                        shouldOverwriteAmount = inputAmount.isNotEmpty()
                    }
                )
            }

            // Horizontal Pattern Chips Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CHIP_PATTERNS.forEach { p ->
                    val selected = inputPattern == p
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) LuckySuccess else LuckyGreen)
                            .clickable { onPatternSelected(p) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 5-Column Keypad Grid
            // Row 1: နက္ခတ် | 1 | 2 | 3 | R
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeypadActionButton(
                    label = "နက္ခတ်",
                    bg = if (inputPattern == "နက္ခတ်") LuckySuccess else LuckyGreen,
                    fontSize = 13,
                    modifier = Modifier.weight(1f),
                    onClick = { onPatternSelected("နက္ခတ်") }
                )
                KeypadDigitButton("1", Modifier.weight(1f)) { onKeypadNumPress("1") }
                KeypadDigitButton("2", Modifier.weight(1f)) { onKeypadNumPress("2") }
                KeypadDigitButton("3", Modifier.weight(1f)) { onKeypadNumPress("3") }
                KeypadActionButton(
                    label = "R",
                    bg = if (isReverseOn) LuckySuccess else LuckyGreen,
                    fontSize = 16,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CleanAudio2D.playClick(context)
                        isReverseOn = !isReverseOn
                    }
                )
            }

            // Row 2: ခွေ | 4 | 5 | 6 | /
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeypadActionButton(
                    label = "ခွေ",
                    bg = if (inputPattern == "ခွေ") LuckySuccess else LuckyGreen,
                    fontSize = 14,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onPatternSelected("ခွေ")
                        showKwayDialog = true
                    }
                )
                KeypadDigitButton("4", Modifier.weight(1f)) { onKeypadNumPress("4") }
                KeypadDigitButton("5", Modifier.weight(1f)) { onKeypadNumPress("5") }
                KeypadDigitButton("6", Modifier.weight(1f)) { onKeypadNumPress("6") }
                KeypadActionButton(
                    label = "/",
                    bg = LuckyGreen,
                    fontSize = 18,
                    modifier = Modifier.weight(1f),
                    onClick = { onKeypadSlashPress() }
                )
            }

            // Row 3: အပါ | 7 | 8 | 9 | ENTER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeypadActionButton(
                    label = "အပါ",
                    bg = if (inputPattern == "အပါ") LuckySuccess else LuckyGreen,
                    fontSize = 14,
                    modifier = Modifier.weight(1f),
                    onClick = { onPatternSelected("အပါ") }
                )
                KeypadDigitButton("7", Modifier.weight(1f)) { onKeypadNumPress("7") }
                KeypadDigitButton("8", Modifier.weight(1f)) { onKeypadNumPress("8") }
                KeypadDigitButton("9", Modifier.weight(1f)) { onKeypadNumPress("9") }
                KeypadActionButton(
                    label = "ENTER",
                    bg = LuckyGreen,
                    fontSize = 13,
                    modifier = Modifier.weight(1f),
                    onClick = { onAdvanceFocusPress() }
                )
            }

            // Row 4: ဖျက် | 0 | 00 | 000 | OK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeypadActionButton(
                    label = "ဖျက်",
                    bg = LuckyDanger,
                    fontSize = 15,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CleanAudio2D.playClick(context)
                        when (activeField) {
                            EntryField.NUMBER -> if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1)
                            EntryField.PATTERN -> {
                                inputPattern = "ဒဲ့"
                                isReverseOn = false
                            }
                            EntryField.AMOUNT -> if (inputAmount.isNotEmpty()) inputAmount = inputAmount.dropLast(1)
                        }
                    }
                )
                KeypadDigitButton("0", Modifier.weight(1f)) { onKeypadNumPress("0") }
                KeypadDigitButton("00", Modifier.weight(1f), fontSize = 17) { onKeypadNumPress("00") }
                KeypadDigitButton("000", Modifier.weight(1f), fontSize = 16) { onKeypadNumPress("000") }
                KeypadActionButton(
                    label = "OK",
                    bg = LuckySuccess,
                    fontSize = 16,
                    modifier = Modifier.weight(1f),
                    onClick = { handleAddBet(false) }
                )
            }
        }
    }

    // DIALOG 1: Quick Entry Modal (အမြန်ထိုး 1 & 2) with Paste support
    quickEntryMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { quickEntryMode = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = LuckyGreen)
                    Text(
                        text = "2D ဂဏန်းများ ( အမြန်ထိုး $mode )",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                CleanAudio2D.playClick(context)
                                quickEntryMode = 1
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == 1) LuckyGreen else LuckyCardSoft,
                                contentColor = if (mode == 1) Color.White else LuckyTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("အမြန်ထိုး 1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                CleanAudio2D.playClick(context)
                                quickEntryMode = 2
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == 2) LuckyGreen else LuckyCardSoft,
                                contentColor = if (mode == 2) Color.White else LuckyTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("အမြန်ထိုး 2", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = quickEntryText,
                        onValueChange = { quickEntryText = it },
                        placeholder = {
                            Text(
                                text = if (mode == 1) {
                                    "ကြေးတူရင်\n01.78=500\nကြေးတူ R ရင်\n01.78R500\nကြေးမတူရင်\n01.78=500/100"
                                } else {
                                    "5a500 (5 အပါ)\n6b600 (6 ဘရိတ်)\n1t500 (1 ထိပ်)\n1w500 (1 နောက်)\np3500 (ပါဝါ) / n7500 (နက္ခတ်)\ns500 (အပူး) / x200 (ညီကို)\n123k500 (ခွေ)"
                                },
                                fontSize = 12.sp,
                                color = LuckyTextMuted
                            )
                        },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = {
                            CleanAudio2D.playClick(context)
                            val clip = ShareHelper.readFromClipboard(context)
                            if (clip.isNotBlank()) {
                                quickEntryText = if (quickEntryText.isBlank()) clip else "$quickEntryText\n$clip"
                            } else {
                                viewModel.showToast("Clipboard ထဲတွင် စာသားမရှိပါ")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = LuckyGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("PASTE (Clipboard မှ ကူးထည့်မည်)", color = LuckyGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { handleQuickEntrySubmit() },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Text("ထည့်သွင်းမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { quickEntryMode = null }) {
                    Text("မလုပ်တော့ပါ", color = LuckyTextMuted)
                }
            }
        )
    }

    // DIALOG 2: Agent Picker
    if (showAgentPicker) {
        AlertDialog(
            onDismissRequest = { showAgentPicker = false },
            title = {
                Text("ထိုးသူ (ကော်မရှင်) ရွေးချယ်ပါ", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 16.sp)
            },
            text = {
                if (viewModel.agents.isEmpty()) {
                    Text("ကော်မရှင် မရှိသေးပါ။ ကော်မရှင် အရင်ထည့်ပါ။", fontSize = 13.sp, color = LuckyTextMuted)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        itemsIndexed(viewModel.agents) { idx, ag ->
                            val isSelected = viewModel.entryAgent?.id == ag.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) LuckyCardSoft else Color.Transparent)
                                    .clickable {
                                        CleanAudio2D.playClick(context)
                                        viewModel.selectEntryAgent(ag)
                                        showAgentPicker = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${ag.name}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) LuckyGreen else LuckyTextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "ကော် ${LotteryViewModel.formatRate(ag.rate)}% | အဆ ${ag.mult}",
                                    fontSize = 12.sp,
                                    color = LuckyTextMuted
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAgentPicker = false }) {
                    Text("ပိတ်မည်", color = LuckyGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // DIALOG 3: Kway Doubles Dialog
    if (showKwayDialog) {
        AlertDialog(
            onDismissRequest = { showKwayDialog = false },
            title = {
                Text(
                    text = "ခွေဂဏန်းများ အပူး ပါမည်လား?",
                    fontWeight = FontWeight.Bold,
                    color = LuckyGreen,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "( ဥပမာ: 123 ခွေရာတွင် 11, 22, 33 အပူးများ ထည့်သွင်းမည်လား )",
                    fontSize = 13.sp,
                    color = LuckyTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showKwayDialog = false
                        handleAddBet(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Text("အပူးပါမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showKwayDialog = false
                        handleAddBet(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyCardSoft, contentColor = LuckyTextPrimary)
                ) {
                    Text("အပူးမပါပါ", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // DIALOG 4: Rejected / Blocked / Over-Limit Numbers Alert Box
    rejectedAlert?.let { info ->
        AlertDialog(
            onDismissRequest = { rejectedAlert = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = LuckyDanger,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "မရဂဏန်းများ သတိပေးချက်",
                    fontWeight = FontWeight.Bold,
                    color = LuckyDanger,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (info.blockedNumbers.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "ပိတ်ထားသော မရဂဏန်းများ (ထိုး၍မရပါ) :",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LuckyDanger
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = info.blockedNumbers.joinToString(", "),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = LuckyTextPrimary
                                )
                            }
                        }
                    }
                    if (info.overLimitEntries.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "ဘရိတ် (${info.brakeLimit}) ကျော်လွန်နေသော ဂဏန်းများ :",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                                Spacer(Modifier.height(4.dp))
                                info.overLimitEntries.forEach { (num, total) ->
                                    Text(
                                        text = "$num = $total",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = LuckyTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { rejectedAlert = null },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen)
                ) {
                    Text("သိရှိပြီးပါပြီ", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // DIALOG 5: Confirm Bet Dialog ("ဘောင်ချာ အတည်ပြုချက်")
    if (showConfirmBetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmBetDialog = false },
            title = {
                Text(
                    text = "ဘောင်ချာ အတည်ပြုချက်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = LuckyGreen
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEEF3F1),
                        border = BorderStroke(1.dp, Color(0xFFD4DFDA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ထိုးသူ : ${viewModel.entryAgent?.name ?: "-"}",
                                fontSize = 15.sp,
                                color = LuckyTextPrimary
                            )
                            Text(
                                text = "စုစုပေါင်း : ${entryItems.size} ကွက်",
                                fontSize = 15.sp,
                                color = LuckyTextPrimary
                            )
                            Text(
                                text = "စုစုပေါင်း ကျသင့်ငွေ : $totalAmount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuckyGreen
                            )
                        }
                    }
                    Text(
                        text = "ဤစာရင်းအား ဘောင်ချာအဖြစ် အတည်ပြုထိုးမည် သေချာပါသလား?",
                        fontSize = 15.sp,
                        color = LuckyTextPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmBetDialog = false
                        val selectedAgent = viewModel.agents.firstOrNull { it.id == viewModel.entryAgent?.id }
                            ?: viewModel.entryAgent
                        val savedVoucher = viewModel.commitCurrentVoucher()
                        if (savedVoucher != null) {
                            val agentPhone = selectedAgent?.phoneNumber?.trim() ?: ""
                            if (agentPhone.isNotEmpty()) {
                                val slipTotalAmount = savedVoucher.items.sumOf { it.a }
                                quickShareData = QuickShareData(
                                    phoneNumber = agentPhone,
                                    message = "No.${savedVoucher.no}=$slipTotalAmount"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ထိုးမည်", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        CleanAudio2D.playClick(context)
                        showConfirmBetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("မထိုးပါ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        )
    }

    // DIALOG 6: Conditional Quick Share Dialog — displays ONLY "No.<latestSlipCount>=<slipTotalAmount>"
    quickShareData?.let { share ->
        AlertDialog(
            onDismissRequest = { quickShareData = null },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEEF3F1),
                        border = BorderStroke(1.dp, Color(0xFFD4DFDA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = share.message,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = LuckyGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                CleanAudio2D.playClick(context)
                                ShareHelper.shareSms(context, share.phoneNumber, share.message)
                                quickShareData = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckySuccess),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                CleanAudio2D.playClick(context)
                                ShareHelper.shareViber(context, share.phoneNumber, share.message)
                                quickShareData = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyViber),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Viber", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                CleanAudio2D.playClick(context)
                                ShareHelper.shareMessenger(context, share.phoneNumber, share.message)
                                quickShareData = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyMessenger),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Messenger", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = {
                            CleanAudio2D.playClick(context)
                            quickShareData = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = LuckyTextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("မလုပ်ပါ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {}
        )
    }

    // DIALOG 7: Edit / Delete Draft Bet Item
    editingItemIndex?.let { idx ->
        AlertDialog(
            onDismissRequest = { editingItemIndex = null },
            title = {
                Text("ဂဏန်း ပြင်ဆင်ရန် / ဖျက်ရန်", fontWeight = FontWeight.Bold, color = LuckyGreen, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editNumInput,
                        onValueChange = { editNumInput = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("ဂဏန်း (၂ လုံး)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmtInput,
                        onValueChange = { editAmtInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("ပမာဏ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = editAmtInput.toIntOrNull() ?: 0
                        if (editNumInput.length == 2 && amt > 0) {
                            if (viewModel.editEntryItem(idx, editNumInput, amt)) {
                                editingItemIndex = null
                            }
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
                            viewModel.deleteEntryItem(idx)
                            editingItemIndex = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ဖျက်မည်")
                    }
                    TextButton(onClick = { editingItemIndex = null }) {
                        Text("ပိတ်မည်", color = LuckyTextMuted)
                    }
                }
            }
        )
    }
}

@Composable
private fun InputPreviewCell(
    text: String,
    isPlaceholder: Boolean,
    isFocused: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        isError -> Color(0xFFFEF2F2)
        isFocused -> LuckyRowAlt
        else -> Color.White
    }
    val borderColor = when {
        isError -> LuckyDanger
        isFocused -> LuckyGreen
        else -> LuckyInputBorder
    }
    val borderWidth = if (isFocused || isError) 2.dp else 1.dp

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                isError -> LuckyDanger
                isPlaceholder -> LuckyTextMuted
                else -> LuckyTextPrimary
            },
            fontSize = 15.sp,
            fontWeight = if (isFocused || !isPlaceholder) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KeypadDigitButton(
    label: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 18,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LuckyKeyBg)
            .border(1.dp, LuckyKeyBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = LuckyTextPrimary,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun KeypadActionButton(
    label: String,
    bg: Color,
    fontSize: Int = 14,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
