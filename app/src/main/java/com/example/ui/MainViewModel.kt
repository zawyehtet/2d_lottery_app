package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen {
    HOME,
    AGENTS,
    NUMBER_ENTRY,
    VOUCHERS,
    NUMBERS,
    BRAKE,
    SETTLEMENT,
    SETTINGS,
    LAYOFF_HISTORY,
    AGENT_SETTLEMENT
}

data class NumberSummary(
    val n: String,
    val entryCount: Int = 0,
    val totalBet: Long,
    val layoffAmount: Long,
    val netAmount: Long
)

data class NumberRowItem2D(
    val number: String, // "00" to "99"
    val totalAmount: Long
)

data class NumberLedgerState2D(
    val number: String,
    val totalSales: Long,
    val totalPassed: Long,
    val netKept: Long = totalSales - totalPassed // Signed: Supports negative numbers!
)

data class NumberAgentBreakdownItem(
    val agentId: Int,
    val agentName: String,
    val rate: Double,
    val mult: Double,
    val entryCount: Int,
    val betOnNumber: Long,
    val totalAgentSales: Long,
    val commissionOnNumber: Long,
    val totalSalesCommission: Long,
    val winningPayout: Long
)

data class RoundSettlementSummary(
    val date: String,
    val session: String,
    val winningNumber: String,
    val totalBet: Long,
    val commission: Long,
    val netAmount: Long,
    val payout: Long,
    val profitLoss: Long
)

data class Agent2DSettlementItem(
    val agent: Agent,
    val voucherCount: Int,
    val totalBet: Long,
    val commission: Long,
    val netAmount: Long,
    val winBetAmount: Long,
    val payout: Long,
    val profitLoss: Long
)

data class Agent2DSettlement(
    val agentId: Int,
    val agentName: String,
    val commissionRate: Double,
    val multiplier: Double,
    val totalSales: Long,
    val commissionAmount: Long,
    val netSales: Long,
    val winningBetAmount: Long,
    val payoutAmount: Long,
    val netBalance: Long // Net Sales - Payout
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        fun sort2DNumbersWithWinning(
            allNumbers: List<NumberSummary>,
            winningNum: String?
        ): List<NumberSummary> {
            val cleanWin = winningNum?.trim()?.toAsciiDigits()
            val hasValidWin = !cleanWin.isNullOrEmpty() && cleanWin.length == 2 && cleanWin.all { it.isDigit() }

            val winItem = if (hasValidWin) {
                allNumbers.firstOrNull { it.n == cleanWin } ?: NumberSummary(cleanWin!!, 0, 0L, 0L, 0L)
            } else null

            val pool = if (hasValidWin) {
                allNumbers.filter { it.n != cleanWin }
            } else {
                allNumbers
            }

            // Tier 2: Active placed bets (totalBet > 0), sorted strictly by Bet Amount DESCENDING (အများမှ အနည်း)
            // If equal, tie-break numerically ASCENDING (ငယ်စဉ်ကြီးလိုက်: e.g., "05" before "24")
            val activeBets = pool
                .filter { it.totalBet > 0L }
                .sortedWith(
                    compareByDescending<NumberSummary> { it.totalBet }
                        .thenBy { it.n }
                )

            // Tier 3: Unplaced bets (totalBet == 0), positioned below active bets, sorted strictly in numerical order ASCENDING (00 to 99)
            val zeroBets = pool
                .filter { it.totalBet <= 0L }
                .sortedBy { it.n }

            return if (winItem != null) {
                listOf(winItem) + activeBets + zeroBets
            } else {
                activeBets + zeroBets
            }
        }

        fun parseAmountOrZero(input: String): Long {
            val trimmed = input.trim().toAsciiDigits()
            if (trimmed.contains("/")) {
                val parts = trimmed.split("/")
                val p1 = parts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                val p2 = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                return p1 + p2
            }
            return trimmed.toLongOrNull() ?: 0L
        }
        fun parseDoubleOrZero(input: String): Double = input.trim().toAsciiDigits().toDoubleOrNull() ?: 0.0

        fun validateBetAmountInput(rawInput: String?): BetAmountValidationResult = validateBetAmount(rawInput)
        fun validateBetAmountValue(amount: Long): BetAmountValidationResult = validateBetAmount(amount)

        fun shouldShowSaveButton(currentInput: String, savedValue: String): Boolean {
            return currentInput.isNotBlank() && currentInput.trim() != savedValue.trim()
        }

        fun shouldShowRoundSaveButton(roundNameInput: String): Boolean {
            return roundNameInput.isNotBlank()
        }

        fun formatWinningHeaderLine(
            winningNumber: String?,
            amount: Long,
            useThousandsSeparator: Boolean = false
        ): String {
            val clean = winningNumber?.trim().orEmpty()
            val numPart = if (clean.isEmpty()) {
                "-"
            } else {
                val parsed = clean.toIntOrNull()
                if (parsed != null && parsed in 0..99) "%02d".format(Locale.US, parsed) else clean
            }
            val safeAmount = if (numPart == "-") 0L else amount.coerceAtLeast(0L)
            val amtPart = if (useThousandsSeparator) {
                java.text.NumberFormat.getNumberInstance(Locale.US).format(safeAmount)
            } else {
                safeAmount.toString()
            }
            return "ပေါက်သီး $numPart=$amtPart"
        }

        fun formatDailyTotalResultText(profitLoss: Long): String {
            return when {
                profitLoss > 0L -> "အမြတ် +${java.text.NumberFormat.getNumberInstance(Locale.US).format(profitLoss)}"
                profitLoss < 0L -> "အရှုံး -${java.text.NumberFormat.getNumberInstance(Locale.US).format(kotlin.math.abs(profitLoss))}"
                else -> "0"
            }
        }
    }

    private val repository = StorageRepository(application)
    val soundManager: SoundManager = SoundManager

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.US)
    private val dotDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.US)
    private val timeFormatter = SimpleDateFormat("yyyy.MM.dd/HH:mm:ss", Locale.US)

    val todayDate: String = dateFormatter.format(Date()).toAsciiDigits()
    val todayDisplayDate: String = displayDateFormatter.format(Date()).toAsciiDigits()
    val todayDotDate: String = dotDateFormatter.format(Date()).toAsciiDigits()

    private val _selectedDate = MutableStateFlow(todayDate)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedSession = MutableStateFlow("")
    val selectedSession: StateFlow<String> = _selectedSession.asStateFlow()
    val selectedRound: StateFlow<String> = _selectedSession.asStateFlow()

    private val customRoundsByDate = mutableMapOf<String, MutableSet<String>>()
    private val _availableRoundsForSelectedDate = MutableStateFlow<List<String>>(emptyList())
    val availableRoundsForSelectedDate: StateFlow<List<String>> = _availableRoundsForSelectedDate.asStateFlow()

    val currentDrawFilter: Flow<Pair<String, String>> = combine(_selectedDate, _selectedSession) { date, round ->
        Pair(date.toAsciiDigits(), round)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeAgents: StateFlow<List<AgentEntity>> = combine(_selectedDate, _selectedSession) { date, round ->
        Pair(date.toAsciiDigits(), round)
    }.flatMapLatest { (date, round) ->
        repository.getAgentsByDateAndRound(date, round)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeVouchers: StateFlow<List<VoucherEntity>> = currentDrawFilter
        .flatMapLatest { (date, round) ->
            repository.getVouchersByDateAndRound(date, round)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeWinningNumber: StateFlow<WinningNumberEntity?> = currentDrawFilter
        .flatMapLatest { (date, round) ->
            repository.getWinningNumber(date, round)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSettlement: StateFlow<SettlementEntity?> = currentDrawFilter
        .flatMapLatest { (date, round) ->
            repository.getSettlement(date, round)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Date-Wide Aggregated Daily Settlement (ရက်ချုပ်) scoped strictly to selectedDate (independent of selectedRound)
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeDailySettlement: StateFlow<DailySettlementEntity?> = _selectedDate
        .flatMapLatest { date ->
            repository.getDailySettlementByDate(date.toAsciiDigits())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val historicalRoundsForSelectedDate: StateFlow<List<String>> = _selectedDate
        .flatMapLatest { date ->
            repository.getRoundsByDate(date.toAsciiDigits())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Navigation back stack for sub-screens
    private val screenHistory = mutableListOf(Screen.HOME)

    // Data lists
    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _vouchers = MutableStateFlow<List<Voucher>>(emptyList())
    val vouchers: StateFlow<List<Voucher>> = _vouchers.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val vouchersState: StateFlow<List<Voucher>> = combine(_selectedDate, _selectedSession) { date, session ->
        Pair(date.toAsciiDigits().trim(), session.trim())
    }.flatMapLatest { (date, session) ->
        if (date.isBlank() || session.isBlank()) {
            flowOf(emptyList())
        } else {
            repository.getVouchersForSessionFlow(date, session)
        }
    }.flowOn(Dispatchers.IO)
     .stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5000),
         initialValue = emptyList()
     )

    private val _layoffs = MutableStateFlow<List<LayoffRecord>>(emptyList())
    val layoffs: StateFlow<List<LayoffRecord>> = _layoffs.asStateFlow()

    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    private val _wins = MutableStateFlow<List<WinRecord>>(emptyList())
    val wins: StateFlow<List<WinRecord>> = _wins.asStateFlow()

    private val _settled = MutableStateFlow<List<SettledRecord>>(emptyList())
    val settled: StateFlow<List<SettledRecord>> = _settled.asStateFlow()

    private val _dailySettlements = MutableStateFlow<List<DailySettlementEntity>>(emptyList())
    val dailySettlements: StateFlow<List<DailySettlementEntity>> = _dailySettlements.asStateFlow()

    val currentDateDailySettlement: StateFlow<DailySettlementEntity?> = combine(_dailySettlements, _selectedDate) { list, date ->
        val asciiDate = date.toAsciiDigits()
        list.firstOrNull { it.date.toAsciiDigits() == asciiDate }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val full2DNumbersList: StateFlow<List<NumberRowItem2D>> = combine(
        _selectedDate,
        _selectedSession,
        _vouchers,
        _layoffs
    ) { date, session, vouchersList, layoffsList ->
        val cleanDate = date.trim().toAsciiDigits()
        val sessionVouchers = vouchersList.filter {
            it.date.toAsciiDigits() == cleanDate && it.session == session
        }
        val sessionLayoffs = layoffsList.filter {
            it.date.toAsciiDigits() == cleanDate && it.session == session
        }
        val salesMap = (0..99).associate { i ->
            String.format(Locale.US, "%02d", i) to 0L
        }.toMutableMap()
        val passedMap = (0..99).associate { i ->
            String.format(Locale.US, "%02d", i) to 0L
        }.toMutableMap()

        sessionVouchers.forEach { voucher ->
            voucher.items.forEach { betItem ->
                val num = betItem.n.toAsciiDigits().padStart(2, '0').takeLast(2)
                if (salesMap.containsKey(num)) {
                    salesMap[num] = (salesMap[num] ?: 0L) + betItem.a
                }
            }
        }
        sessionLayoffs.forEach { layoff ->
            layoff.items.forEach { betItem ->
                val num = betItem.n.toAsciiDigits().padStart(2, '0').takeLast(2)
                if (passedMap.containsKey(num)) {
                    passedMap[num] = (passedMap[num] ?: 0L) + betItem.a
                }
            }
        }

        salesMap.entries.map { entry ->
            val num = entry.key
            val sales = entry.value
            val passed = passedMap[num] ?: 0L
            val netKept = sales - passed // Signed: supports negative balance
            NumberRowItem2D(number = num, totalAmount = netKept)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        (0..99).map { NumberRowItem2D(String.format(Locale.US, "%02d", it), 0L) }
    )

    fun getNumberLedgerStateFlow(
        date: String = _selectedDate.value,
        sessionName: String = _selectedSession.value
    ): Flow<Map<String, NumberLedgerState2D>> {
        return combine(
            _vouchers,
            _layoffs
        ) { vouchersList, layoffsList ->
            val cleanDate = date.trim().toAsciiDigits()
            val sessionVouchers = vouchersList.filter {
                it.date.toAsciiDigits() == cleanDate && it.session == sessionName
            }
            val sessionLayoffs = layoffsList.filter {
                it.date.toAsciiDigits() == cleanDate && it.session == sessionName
            }

            val salesMap = mutableMapOf<String, Long>()
            sessionVouchers.forEach { v ->
                v.items.forEach { itm ->
                    val num = itm.n.toAsciiDigits().padStart(2, '0').takeLast(2)
                    salesMap[num] = (salesMap[num] ?: 0L) + itm.a
                }
            }

            val passedMap = mutableMapOf<String, Long>()
            sessionLayoffs.forEach { l ->
                l.items.forEach { itm ->
                    val num = itm.n.toAsciiDigits().padStart(2, '0').takeLast(2)
                    passedMap[num] = (passedMap[num] ?: 0L) + itm.a
                }
            }

            val all2DKeys = (0..99).map { String.format(Locale.US, "%02d", it) }
            all2DKeys.associateWith { num ->
                val sales = salesMap[num] ?: 0L
                val passed = passedMap[num] ?: 0L
                NumberLedgerState2D(
                    number = num,
                    totalSales = sales,
                    totalPassed = passed,
                    netKept = sales - passed
                )
            }
        }
    }

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Number Entry state
    private val _entryAgent = MutableStateFlow<Agent?>(null)
    val entryAgent: StateFlow<Agent?> = _entryAgent.asStateFlow()

    private val _entryItems = MutableStateFlow<List<BetItem>>(emptyList())
    val entryItems: StateFlow<List<BetItem>> = _entryItems.asStateFlow()

    val showConfirmBetDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val entryTotalAmount: Long
        get() = _entryItems.value.sumOf { it.a }

    // Brake screen limit state (starts empty, synchronized with Settings)
    private val _currentBrakeInput = MutableStateFlow("")
    val currentBrakeInput: StateFlow<String> = _currentBrakeInput.asStateFlow()

    private val _savedBrakeLimitText = MutableStateFlow("")
    val savedBrakeLimitText: StateFlow<String> = _savedBrakeLimitText.asStateFlow()

    // Flow A: active winning number passed to 2D စာရင်းချုပ်
    private val _activeWinningNumberFor2DSummary = MutableStateFlow<String?>(null)
    val activeWinningNumberFor2DSummary: StateFlow<String?> = _activeWinningNumberFor2DSummary.asStateFlow()

    // Flow B: daily record currently being edited on HomeScreen (ပင်မစာမျက်နှာ)
    private val _editingSettledRecord = MutableStateFlow<SettledRecord?>(null)
    val editingSettledRecord: StateFlow<SettledRecord?> = _editingSettledRecord.asStateFlow()

    // Toast event message
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun getHistoricalRoundsForDate(date: String = _selectedDate.value): List<String> {
        val cleanDate = date.trim().toAsciiDigits()
        val voucherRounds = _vouchers.value.filter { it.date.toAsciiDigits() == cleanDate }.map { it.session }
        val winRounds = _wins.value.filter { it.date.toAsciiDigits() == cleanDate }.map { it.session }
        val layoffRounds = _layoffs.value.filter { it.date.toAsciiDigits() == cleanDate }.map { it.session }
        val settledRounds = _settled.value.filter { it.date.toAsciiDigits() == cleanDate }.map { it.session }
        val dbRounds = repository.getDistinctHistoricalRoundsByDateSync(cleanDate)
        return (voucherRounds + winRounds + layoffRounds + settledRounds + dbRounds)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun getAvailableRoundsForDate(date: String = _selectedDate.value): List<String> {
        val cleanDate = date.trim().toAsciiDigits()
        val configured = _sessions.value.map { it.name.trim() }.filter { it.isNotEmpty() }
        val historical = getHistoricalRoundsForDate(cleanDate)
        val custom = customRoundsByDate[cleanDate]?.toList().orEmpty()
        return (configured + historical + custom).distinct()
    }

    private fun refreshAvailableRounds() {
        _availableRoundsForSelectedDate.value = getAvailableRoundsForDate(_selectedDate.value)
    }

    fun hasHistoricalOrCustomRound(date: String = _selectedDate.value, round: String = _selectedSession.value): Boolean {
        val cleanDate = date.trim().toAsciiDigits()
        val cleanRound = round.trim()
        if (cleanRound.isEmpty()) return false
        return customRoundsByDate[cleanDate]?.contains(cleanRound) == true ||
            getHistoricalRoundsForDate(cleanDate).contains(cleanRound)
    }

    val hasActiveRound: Boolean
        get() = _selectedSession.value.isNotBlank() && (
            _sessions.value.any { it.name == _selectedSession.value } ||
                hasHistoricalOrCustomRound(_selectedDate.value, _selectedSession.value)
            )

    fun formatDisplayDate(dateStr: String = _selectedDate.value): String {
        val cleanDateStr = dateStr.trim().toAsciiDigits()
        return try {
            val parsed = dateFormatter.parse(cleanDateStr)
            if (parsed != null) displayDateFormatter.format(parsed).toAsciiDigits() else cleanDateStr
        } catch (_: Exception) {
            cleanDateStr
        }
    }

    fun formatDotDate(dateStr: String = _selectedDate.value): String {
        val cleanDateStr = dateStr.trim().toAsciiDigits()
        return try {
            val parsed = dateFormatter.parse(cleanDateStr)
            if (parsed != null) dotDateFormatter.format(parsed).toAsciiDigits() else cleanDateStr
        } catch (_: Exception) {
            cleanDateStr
        }
    }

    val isResultInputEnabled: Boolean
        get() = hasActiveRound

    val isCommissionSetForEntry: Boolean
        get() = _entryAgent.value != null

    // Guard against rapid duplicate delete taps for the same voucherId / numberId / settledRecordId
    private val deletedVoucherIds = mutableSetOf<Long>()
    private val deletedNumberIds = mutableSetOf<Long>()
    private val deletedSettledIds = mutableSetOf<Long>()
    private val deletedRoundKeys = mutableSetOf<String>()
    private val manuallyEditedRecordIds = mutableSetOf<Long>()
    private var nextSettledIdCounter = 1L

    init {
        soundManager.init(application)
        loadAllData()
    }

    private fun refreshActiveAgentsForCurrentDraw(resetDraftOnSessionChange: Boolean = false) {
        val currentSessionAgents = repository.loadAgentsForDateAndRound(_selectedDate.value, _selectedSession.value)
        _agents.value = currentSessionAgents
        if (resetDraftOnSessionChange) {
            _entryItems.value = emptyList()
            showConfirmBetDialog.value = false
            val currentEntryAg = _entryAgent.value
            if (currentEntryAg != null) {
                _entryAgent.value = currentSessionAgents.firstOrNull {
                    it.id == currentEntryAg.id && it.name == currentEntryAg.name
                }
            }
        }
    }

    fun loadAllData() {
        deletedVoucherIds.clear()
        deletedNumberIds.clear()
        deletedSettledIds.clear()
        deletedRoundKeys.clear()
        manuallyEditedRecordIds.clear()
        customRoundsByDate.clear()
        showConfirmBetDialog.value = false
        val activeDate = _selectedDate.value.toAsciiDigits()
        val loadedSessions = repository.loadSessionsForDate(activeDate)
        _sessions.value = loadedSessions

        repository.loadAgents()
        _vouchers.value = repository.loadVouchers()
        _layoffs.value = repository.loadLayoffs()
        _blocked.value = repository.loadBlocked()
        _wins.value = repository.loadWins()
        _settled.value = repository.loadSettled()
        _dailySettlements.value = repository.loadAllDailySettlements()

        val histRoundsForDate = getHistoricalRoundsForDate(activeDate)
        val savedSession = repository.getLastActiveSession()
        val allKnownSessions = repository.loadSessions()
        if (savedSession.isNotEmpty() && (loadedSessions.any { it.name == savedSession } || savedSession in histRoundsForDate || allKnownSessions.any { it.name == savedSession } || loadedSessions.isEmpty())) {
            _selectedSession.value = savedSession
        } else if (loadedSessions.isNotEmpty()) {
            if (_selectedSession.value.isEmpty() ||
                (!loadedSessions.any { it.name == _selectedSession.value } && _selectedSession.value !in histRoundsForDate)
            ) {
                _selectedSession.value = loadedSessions.first().name
            }
        } else if (histRoundsForDate.isNotEmpty()) {
            if (_selectedSession.value !in histRoundsForDate) {
                _selectedSession.value = histRoundsForDate.first()
            }
        } else if (savedSession.isNotEmpty()) {
            _selectedSession.value = savedSession
        } else {
            _selectedSession.value = ""
        }
        refreshActiveAgentsForCurrentDraw(resetDraftOnSessionChange = false)
        refreshAvailableRounds()

        val s = repository.loadSettings()
        _settings.value = s
        soundManager.isSoundEnabled = s.soundEnabled
        val initialBrakeStr = if (s.brakeLimit > 0L) s.brakeLimit.toString() else ""
        _currentBrakeInput.value = initialBrakeStr
        _savedBrakeLimitText.value = initialBrakeStr
    }

    fun onInputFocusOrTap() {
        soundManager.playClick()
    }

    fun onEditActionStarted() {
        soundManager.playEdit()
    }

    fun setSoundEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(soundEnabled = enabled)
        _settings.value = updated
        soundManager.isSoundEnabled = enabled
        repository.saveSettings(updated)
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    fun setSession(session: String) {
        val trimmed = session.trim()
        val changed = _selectedSession.value != trimmed
        if (trimmed.isEmpty()) {
            _selectedSession.value = ""
        } else {
            customRoundsByDate.getOrPut(_selectedDate.value) { mutableSetOf() }.add(trimmed)
            _selectedSession.value = trimmed
            repository.saveLastActiveSession(trimmed)
        }
        refreshActiveAgentsForCurrentDraw(resetDraftOnSessionChange = changed)
        refreshAvailableRounds()
    }

    fun setRound(round: String) = setSession(round)

    fun setDate(date: String) {
        val cleanDate = date.trim().toAsciiDigits()
        if (cleanDate.isEmpty()) return
        val changed = _selectedDate.value != cleanDate
        _selectedDate.value = cleanDate

        val dateSessions = repository.loadSessionsForDate(cleanDate)
        _sessions.value = dateSessions
        val histRounds = getHistoricalRoundsForDate(cleanDate)
        val savedSession = repository.getLastActiveSession()

        if (savedSession.isNotEmpty() && (dateSessions.any { it.name == savedSession } || savedSession in histRounds)) {
            _selectedSession.value = savedSession
        } else if (dateSessions.isNotEmpty()) {
            if (_selectedSession.value.isEmpty() || !dateSessions.any { it.name == _selectedSession.value }) {
                _selectedSession.value = dateSessions.first().name
            }
        } else if (histRounds.isNotEmpty()) {
            if (_selectedSession.value.isEmpty() || _selectedSession.value !in histRounds) {
                _selectedSession.value = histRounds.first()
            }
        } else {
            _selectedSession.value = ""
        }
        refreshActiveAgentsForCurrentDraw(resetDraftOnSessionChange = changed)
        refreshAvailableRounds()
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value == screen) return
        val existingIndex = screenHistory.indexOf(screen)
        if (existingIndex >= 0) {
            while (screenHistory.size > existingIndex + 1) {
                screenHistory.removeAt(screenHistory.lastIndex)
            }
        } else {
            screenHistory.add(screen)
        }
        _currentScreen.value = screen
    }

    private val _selectedVoucherFilterAgent = MutableStateFlow("ALL")
    val selectedVoucherFilterAgent: StateFlow<String> = _selectedVoucherFilterAgent.asStateFlow()

    private val _selectedVoucherFilterAgentId = MutableStateFlow<Int?>(null)
    val selectedVoucherFilterAgentId: StateFlow<Int?> = _selectedVoucherFilterAgentId.asStateFlow()

    fun setVoucherFilterAgent(agentName: String) {
        _selectedVoucherFilterAgent.value = agentName
        if (agentName == "ALL") {
            _selectedVoucherFilterAgentId.value = null
        }
    }

    fun openAgentVouchers(agentName: String) {
        _selectedVoucherFilterAgent.value = agentName
        _selectedVoucherFilterAgentId.value = _agents.value.firstOrNull { it.name == agentName }?.id
        navigateTo(Screen.VOUCHERS)
    }

    fun openAgentVouchers(agentId: Int?, agentName: String?) {
        _selectedVoucherFilterAgentId.value = agentId
        _selectedVoucherFilterAgent.value = agentName ?: (_agents.value.firstOrNull { it.id == agentId }?.name ?: "ALL")
        navigateTo(Screen.VOUCHERS)
    }

    fun clearVoucherFilter() {
        _selectedVoucherFilterAgent.value = "ALL"
        _selectedVoucherFilterAgentId.value = null
    }

    fun getBackStack(): List<Screen> = screenHistory.toList()

    fun openNumbersFromWinningNumber() {
        navigateTo(Screen.NUMBERS)
    }

    fun open2DSummaryFromWinningNumber(
        winningNumber: String = getWinningNumberForCurrentSession(),
        session: String = _selectedSession.value
    ) {
        if (session.isNotBlank()) {
            setSession(session)
        }
        val cleanWin = winningNumber.trim()
        if (cleanWin.isNotEmpty()) {
            _activeWinningNumberFor2DSummary.value = cleanWin
        } else {
            _activeWinningNumberFor2DSummary.value = getWinningNumberForCurrentSession().ifEmpty { null }
        }
        navigateTo(Screen.AGENT_SETTLEMENT)
    }

    fun goBack(): Boolean {
        if (_editingSettledRecord.value != null && _currentScreen.value == Screen.HOME) {
            _editingSettledRecord.value = null
        }
        if (screenHistory.size > 1) {
            screenHistory.removeAt(screenHistory.lastIndex)
            _currentScreen.value = screenHistory.last()
            return true
        } else if (_currentScreen.value != Screen.HOME) {
            _currentScreen.value = Screen.HOME
            screenHistory.clear()
            screenHistory.add(Screen.HOME)
            return true
        }
        return false
    }

    // Agent Management (Strictly Scoped to selectedDate and selectedRound)
    fun addAgent(name: String, rate: Double, mult: Double) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val date = _selectedDate.value
        val round = _selectedSession.value
        val sessionAgents = repository.loadAgentsForDateAndRound(date, round)
        val nextId = (sessionAgents.maxOfOrNull { it.id } ?: 0) + 1
        val newAgent = Agent(
            id = nextId,
            name = cleanName,
            rate = rate,
            mult = mult,
            date = date,
            roundNumber = round
        )
        val updated = repository.insertOrUpdateAgentForSession(date, round, newAgent)
        _agents.value = updated
        refreshAvailableRounds()
        soundManager.playSave()
        showToast("ကော်မရှင် အသစ် ထည့်သွင်းပြီးပါပြီ")
    }

    fun updateAgent(agent: Agent) {
        val cleanName = agent.name.trim()
        if (cleanName.isEmpty()) return
        val date = _selectedDate.value
        val round = _selectedSession.value
        val normalized = agent.copy(
            name = cleanName,
            date = date,
            roundNumber = round
        )
        val updated = repository.insertOrUpdateAgentForSession(date, round, normalized)
        _agents.value = updated
        if (_entryAgent.value?.id == normalized.id) {
            _entryAgent.value = normalized
        }
        soundManager.playEdit()
        showToast("ပြင်ဆင်ပြီးပါပြီ")
    }

    fun deleteAgent(agentId: Int) {
        val date = _selectedDate.value.toAsciiDigits().trim()
        val round = _selectedSession.value.trim()
        val targetAgent = _agents.value.find { it.id == agentId } ?: return
        val updated = repository.deleteAgentBySessionSync(date, round, agentId)
        _agents.value = updated
        if (_entryAgent.value?.id == agentId) {
            _entryAgent.value = null
        }
        // Scope deletion of vouchers & bets ONLY to current date and session:
        val updatedVouchers = repository.deleteVouchersByAgentAndSessionSync(date, round, agentId, targetAgent.name)
        _vouchers.value = updatedVouchers
        refreshAvailableRounds()

        viewModelScope.launch {
            repository.agentDao.deleteAgentBySession(date, round, agentId)
        }
        soundManager.playDelete()
        showToast("ဖျက်ပြီးပါပြီ")
    }

    fun clearAgentsForCurrentSession() {
        val date = _selectedDate.value
        val round = _selectedSession.value
        repository.clearAgentsForSessionSync(date, round)
        _agents.value = emptyList()
        _entryAgent.value = null
        viewModelScope.launch {
            repository.agentDao.clearAgentsForSession(date, round)
        }
    }

    // Start Number Entry for an Agent
    fun startNumberEntry(agent: Agent) {
        _entryAgent.value = agent
        _entryItems.value = emptyList()
        showConfirmBetDialog.value = false
        navigateTo(Screen.NUMBER_ENTRY)
    }

    fun setEntryAgent(agent: Agent?) {
        _entryAgent.value = agent
    }

    fun validateBetAmountInput(rawInput: String?): BetAmountValidationResult = validateBetAmount(rawInput)

    fun validateBetAmountValue(amount: Long): BetAmountValidationResult = validateBetAmount(amount)

    // Number Entry Draft manipulations
    fun addEntryItem(item: BetItem): BetAmountValidationResult {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return BetAmountValidationResult.Empty
        }
        if (!isCommissionSetForEntry) return BetAmountValidationResult.Empty
        val cleanNum = item.n.trim().toAsciiDigits()
        if (cleanNum.length != 2 || !cleanNum.all { it.isDigit() }) return BetAmountValidationResult.Empty
        val amountValidation = validateBetAmount(item.a)
        if (!amountValidation.isValid) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return amountValidation
        }
        if (_blocked.value.contains(cleanNum)) {
            showToast("$cleanNum သည် မရဂဏန်း ဖြစ်ပါသည်")
            return BetAmountValidationResult.Empty
        }
        _entryItems.value = _entryItems.value + item.copy(n = cleanNum)
        soundManager.playSave()
        return BetAmountValidationResult.Valid
    }

    fun addEntryItems(items: List<BetItem>): BetAmountValidationResult {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return BetAmountValidationResult.Empty
        }
        if (!isCommissionSetForEntry) return BetAmountValidationResult.Empty
        if (items.isEmpty()) return BetAmountValidationResult.Empty
        val invalidResult = items.firstNotNullOfOrNull { item ->
            val v = validateBetAmount(item.a)
            if (!v.isValid) v else null
        }
        if (invalidResult != null) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return invalidResult
        }
        val valid = items.map { it.copy(n = it.n.trim().toAsciiDigits()) }.filter { item ->
            val cleanNum = item.n
            val blocked = _blocked.value.contains(cleanNum)
            cleanNum.length == 2 && cleanNum.all { it.isDigit() } && !blocked && validateBetAmount(item.a).isValid
        }
        if (valid.isEmpty()) return BetAmountValidationResult.Empty
        _entryItems.value = _entryItems.value + valid
        soundManager.playSave()
        return BetAmountValidationResult.Valid
    }

    fun removeLastEntryItem() {
        // Guarded: keypad "ဖျက်" must never delete confirmed betting rows from the table.
        // Confirmed rows can only be deleted via long-press / deleteEntryItem(index).
    }

    fun editEntryItem(index: Int, newNumber: String, newAmount: Long): Boolean {
        if (!isCommissionSetForEntry) return false
        val cleanNum = newNumber.trim().toAsciiDigits()
        if (cleanNum.length != 2 || !cleanNum.all { it.isDigit() }) {
            return false
        }
        val amountValidation = validateBetAmount(newAmount)
        if (!amountValidation.isValid) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return false
        }
        if (_blocked.value.contains(cleanNum)) {
            showToast("$cleanNum သည် မရဂဏန်း ဖြစ်ပါသည်")
            return false
        }
        if (index in _entryItems.value.indices) {
            val updated = _entryItems.value.toMutableList()
            updated[index] = updated[index].copy(n = cleanNum, a = newAmount)
            _entryItems.value = updated
            soundManager.playEdit()
            return true
        }
        return false
    }

    fun deleteEntryItem(index: Int): Boolean {
        if (!isCommissionSetForEntry) return false
        if (index in _entryItems.value.indices) {
            val updated = _entryItems.value.toMutableList()
            updated.removeAt(index)
            _entryItems.value = updated
            soundManager.playDelete()
            return true
        }
        return false
    }

    fun clearEntryItems() {
        _entryItems.value = emptyList()
    }

    fun requestBetSubmissionConfirmation(): Boolean {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return false
        }
        if (_entryAgent.value == null) {
            showToast("ထိုးသူ ရွေးချယ်ပါ")
            return false
        }
        if (_entryItems.value.isEmpty()) {
            showToast("ထိုးမည့် ဂဏန်းများ ထည့်သွင်းပါ")
            return false
        }
        if (_entryItems.value.any { !validateBetAmount(it.a).isValid }) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return false
        }
        showConfirmBetDialog.value = true
        return true
    }

    fun dismissConfirmBetDialog() {
        showConfirmBetDialog.value = false
    }

    fun confirmAndCommitVoucher(): Boolean {
        showConfirmBetDialog.value = false
        return commitCurrentVoucher()
    }

    // Stay on Bet Input View after submitting voucher (do NOT redirect to Vouchers page)
    // Also require an active round before saving voucher
    fun commitCurrentVoucher(): Boolean {
        showConfirmBetDialog.value = false
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return false
        }
        val currentAgent = _entryAgent.value
        if (currentAgent == null) {
            showToast("ထိုးသူ ရွေးချယ်ပါ")
            return false
        }
        val items = _entryItems.value
        if (items.isEmpty()) {
            showToast("ထိုးမည့် ဂဏန်းများ ထည့်သွင်းပါ")
            return false
        }
        if (items.any { !validateBetAmount(it.a).isValid }) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return false
        }

        val cleanDate = _selectedDate.value.toAsciiDigits()
        val sessionVouchers = _vouchers.value.filter {
            it.date.toAsciiDigits() == cleanDate && it.session == _selectedSession.value
        }
        val nextNo = (sessionVouchers.maxOfOrNull { it.no } ?: 0) + 1
        val newVoucher = Voucher(
            id = System.currentTimeMillis(),
            no = nextNo,
            agent = currentAgent.name,
            agentId = currentAgent.id,
            date = cleanDate,
            session = _selectedSession.value,
            time = timeFormatter.format(Date()).toAsciiDigits(),
            items = items.map { it.copy(n = it.n.toAsciiDigits()) }
        )

        val updated = (_vouchers.value + newVoucher).sortedWith(compareBy<Voucher> { it.no }.thenBy { it.id })
        _vouchers.value = updated
        repository.insertVoucher(newVoucher)
        repository.saveVouchers(updated)
        refreshAvailableRounds()

        _entryItems.value = emptyList()
        soundManager.playSave()
        showToast("ဘောင်ချာ (No.$nextNo) သိမ်းဆည်းပြီးပါပြီ")
        return true
    }

    /**
     * Deletes an entire Voucher by [voucherId] along with all its associated bet items (CASCADE),
     * updating voucher counts and recalculating grand totals immediately.
     */
    fun deleteVoucher(voucherId: Long): Boolean {
        if (!deletedVoucherIds.add(voucherId)) {
            return false
        }
        if (_vouchers.value.none { it.id == voucherId }) {
            deletedVoucherIds.remove(voucherId)
            return false
        }
        val updated = _vouchers.value.filter { it.id != voucherId }
        _vouchers.value = updated
        repository.deleteVoucherById(voucherId) ?: repository.saveVouchers(updated)
        refreshAvailableRounds()
        soundManager.playDelete()
        showToast("ဘောင်ချာ ဖျက်ပြီးပါပြီ")
        return true
    }

    // Single-number edit and delete in a Voucher by unique numberId (never deletes the Voucher object)
    fun editNumber(numberId: Long, newNumber: String, newAmount: Long): Boolean {
        val cleanNum = newNumber.trim().toAsciiDigits()
        if (cleanNum.length != 2 || !cleanNum.all { it.isDigit() }) return false
        val amountValidation = validateBetAmount(newAmount)
        if (!amountValidation.isValid) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return false
        }
        if (_blocked.value.contains(cleanNum)) {
            showToast("$cleanNum သည် မရဂဏန်း ဖြစ်ပါသည်")
            return false
        }
        var changed = false
        val updated = _vouchers.value.map { v ->
            if (v.items.any { it.id == numberId }) {
                changed = true
                v.copy(
                    items = v.items.map { item ->
                        if (item.id == numberId) item.copy(n = cleanNum, a = newAmount) else item
                    }
                )
            } else {
                v
            }
        }
        if (!changed) return false
        _vouchers.value = updated
        repository.updateNumberById(numberId, cleanNum, newAmount) ?: repository.saveVouchers(updated)
        soundManager.playEdit()
        showToast("ဘောင်ချာ ပြင်ဆင်ပြီးပါပြီ")
        return true
    }

    fun deleteNumber(numberId: Long): Boolean {
        if (!deletedNumberIds.add(numberId)) {
            return false
        }
        var changed = false
        val updated = _vouchers.value.map { v ->
            if (v.items.any { it.id == numberId }) {
                changed = true
                v.copy(items = v.items.filter { it.id != numberId })
            } else {
                v
            }
        }
        if (!changed) {
            deletedNumberIds.remove(numberId)
            return false
        }
        _vouchers.value = updated
        repository.deleteNumberById(numberId) ?: repository.saveVouchers(updated)
        soundManager.playDelete()
        showToast("ဘောင်ချာစာကြောင်း ဖျက်ပြီးပါပြီ")
        return true
    }

    fun addNumberToVoucher(voucherId: Long, number: String, amount: Long): Boolean {
        val cleanNum = number.trim().toAsciiDigits()
        if (cleanNum.length != 2 || !cleanNum.all { it.isDigit() }) return false
        val amountValidation = validateBetAmount(amount)
        if (!amountValidation.isValid) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return false
        }
        if (_blocked.value.contains(cleanNum)) {
            showToast("$cleanNum သည် မရဂဏန်း ဖြစ်ပါသည်")
            return false
        }
        val newItem = BetItem(n = cleanNum, a = amount)
        var changed = false
        val updated = _vouchers.value.map { v ->
            if (v.id == voucherId) {
                changed = true
                v.copy(items = v.items + newItem)
            } else {
                v
            }
        }
        if (!changed) return false
        _vouchers.value = updated
        repository.addNumberToVoucher(voucherId, newItem) ?: repository.saveVouchers(updated)
        soundManager.playSave()
        showToast("ဂဏန်း ထည့်သွင်းပြီးပါပြီ")
        return true
    }

    // Aggregations for Numbers Screen and Brake Screen
    fun getFilteredVouchers(date: String = _selectedDate.value, session: String = _selectedSession.value): List<Voucher> {
        val cleanDate = date.trim().toAsciiDigits()
        val cleanSession = session.trim()
        if (cleanDate.isBlank() || cleanSession.isBlank()) return emptyList()
        return _vouchers.value.filter {
            it.date.toAsciiDigits() == cleanDate && it.session == cleanSession
        }.sortedWith(compareBy<Voucher> { it.no }.thenBy { it.id })
    }

    fun getBetsForVoucher(voucherNo: Int): Flow<List<BetItem>> = _vouchers.map { list ->
        val cleanDate = _selectedDate.value.toAsciiDigits().trim()
        val sess = _selectedSession.value.trim()
        val v = list.firstOrNull { it.date.toAsciiDigits().trim() == cleanDate && it.session.trim() == sess && it.no == voucherNo }
        v?.items ?: emptyList()
    }

    fun getVouchersBySessionOldestFirst(date: String = _selectedDate.value, session: String = _selectedSession.value): Flow<List<Voucher>> = _vouchers.map { list ->
        val cleanDate = date.toAsciiDigits().trim()
        val cleanSession = session.trim()
        list.filter {
            it.date.toAsciiDigits().trim() == cleanDate && it.session.trim() == cleanSession
        }.sortedBy { it.no }
    }

    fun getFilteredLayoffs(date: String = _selectedDate.value, session: String = _selectedSession.value): List<LayoffRecord> {
        val cleanDate = date.trim().toAsciiDigits()
        val cleanSession = session.trim()
        if (cleanDate.isBlank() || cleanSession.isBlank()) return emptyList()
        return _layoffs.value.filter {
            it.date.toAsciiDigits() == cleanDate && it.session == cleanSession
        }.sortedWith(compareBy<LayoffRecord> { it.recordNo }.thenBy { it.id })
    }

    fun getPassBetRecordsOldestFirst(session: String = _selectedSession.value, date: String = _selectedDate.value): Flow<List<PassBetRecordEntity>> = flow {
        val list = getFilteredLayoffs(date, session).map { PassBetRecordEntity.fromDomainLayoffRecord(it) }
        emit(list)
    }


    // Total Bet aggregated per number (00 to 99) with entry count (အကြိမ်ရေ)
    fun getAggregatedNumbers(includeZeroBets: Boolean = false): List<NumberSummary> {
        val vouchers = getFilteredVouchers()
        val layoffs = getFilteredLayoffs()

        val betMap = mutableMapOf<String, Long>()
        val countMap = mutableMapOf<String, Int>()
        vouchers.forEach { v ->
            v.items.forEach { item ->
                val nKey = item.n.toAsciiDigits()
                betMap[nKey] = (betMap[nKey] ?: 0L) + item.a
                countMap[nKey] = (countMap[nKey] ?: 0) + 1
            }
        }

        val layoffMap = mutableMapOf<String, Long>()
        layoffs.forEach { l ->
            l.items.forEach { item ->
                val nKey = item.n.toAsciiDigits()
                layoffMap[nKey] = (layoffMap[nKey] ?: 0L) + item.a
            }
        }

        val allNumbers = mutableListOf<NumberSummary>()
        for (i in 0..99) {
            val numStr = "%02d".format(Locale.US, i)
            val total = betMap[numStr] ?: 0L
            val count = countMap[numStr] ?: 0
            val layoff = layoffMap[numStr] ?: 0L
            val net = total - layoff // Signed: Supports negative numbers when passed bets exceed sales
            if (includeZeroBets || total > 0 || layoff > 0) {
                allNumbers.add(NumberSummary(numStr, count, total, layoff, net))
            }
        }

        return if (includeZeroBets) {
            allNumbers
        } else {
            allNumbers.sortedWith(compareByDescending<NumberSummary> { it.netAmount }.thenBy { it.n })
        }
    }

    fun getAll100Numbers(): List<NumberSummary> = getAggregatedNumbers(includeZeroBets = true)

    fun sort2DNumbersWithWinning(
        allNumbers: List<NumberSummary>,
        winningNum: String?
    ): List<NumberSummary> = Companion.sort2DNumbersWithWinning(allNumbers, winningNum)

    fun getFull2DNumbersList(): List<NumberRowItem2D> {
        return getAll100Numbers().map {
            NumberRowItem2D(number = it.n, totalAmount = it.netAmount)
        }
    }

    // Breakdown of each sales agent's performance for a specific 2D number
    fun getNumberAgentBreakdown(number: String): List<NumberAgentBreakdownItem> {
        val vouchers = getFilteredVouchers()
        val sess = _sessions.value.firstOrNull { it.name == _selectedSession.value }
        val defaultMult = sess?.mult ?: 0.0
        val defaultRate = sess?.rate ?: 0.0

        val result = mutableListOf<NumberAgentBreakdownItem>()
        val processedAgentIds = mutableSetOf<Int>()

        for (agent in _agents.value) {
            processedAgentIds.add(agent.id)
            val agentVouchers = vouchers.filter { it.agentId == agent.id || it.agent == agent.name }
            var betOnNum = 0L
            var countOnNum = 0
            var totalSales = 0L

            agentVouchers.forEach { v ->
                v.items.forEach { item ->
                    totalSales += item.a
                    if (item.n == number) {
                        betOnNum += item.a
                        countOnNum += 1
                    }
                }
            }

            val effRate = if (agent.rate > 0.0) agent.rate else defaultRate
            val effMult = if (agent.mult > 0.0) agent.mult else defaultMult
            val commOnNum = Math.round(betOnNum.toDouble() * (effRate / 100.0))
            val totalComm = Math.round(totalSales.toDouble() * (effRate / 100.0))
            val payout = Math.round(betOnNum.toDouble() * effMult)

            if (betOnNum > 0L || totalSales > 0L || _agents.value.isNotEmpty()) {
                result.add(
                    NumberAgentBreakdownItem(
                        agentId = agent.id,
                        agentName = agent.name,
                        rate = effRate,
                        mult = effMult,
                        entryCount = countOnNum,
                        betOnNumber = betOnNum,
                        totalAgentSales = totalSales,
                        commissionOnNumber = commOnNum,
                        totalSalesCommission = totalComm,
                        winningPayout = payout
                    )
                )
            }
        }

        val extraVouchers = vouchers.filter { it.agentId !in processedAgentIds }
        extraVouchers.groupBy { it.agentId to it.agent }.forEach { (pair, vList) ->
            val (agId, agName) = pair
            var betOnNum = 0L
            var countOnNum = 0
            var totalSales = 0L
            vList.forEach { v ->
                v.items.forEach { item ->
                    totalSales += item.a
                    if (item.n == number) {
                        betOnNum += item.a
                        countOnNum += 1
                    }
                }
            }
            val commOnNum = Math.round(betOnNum.toDouble() * (defaultRate / 100.0))
            val totalComm = Math.round(totalSales.toDouble() * (defaultRate / 100.0))
            val payout = Math.round(betOnNum.toDouble() * defaultMult)
            result.add(
                NumberAgentBreakdownItem(
                    agentId = agId,
                    agentName = agName,
                    rate = defaultRate,
                    mult = defaultMult,
                    entryCount = countOnNum,
                    betOnNumber = betOnNum,
                    totalAgentSales = totalSales,
                    commissionOnNumber = commOnNum,
                    totalSalesCommission = totalComm,
                    winningPayout = payout
                )
            )
        }

        return result.sortedWith(
            compareByDescending<NumberAgentBreakdownItem> { it.betOnNumber }
                .thenByDescending { it.totalAgentSales }
                .thenBy { it.agentId }
        )
    }

    // Total winning payout amount for a given winning number on NumbersScreen
    fun getTotalWinningPayoutForNumber(winNum: String): Long {
        if (winNum.isBlank()) return 0L
        val breakdown = getNumberAgentBreakdown(winNum)
        val agentPayoutSum = breakdown.sumOf { it.winningPayout }
        if (agentPayoutSum > 0L) return agentPayoutSum
        val calc = calculateSettlement(winNum)
        return calc.payout
    }

    // Total retained amount for today's session
    fun getTotalRetainedMoney(): Long {
        val summaries = getAggregatedNumbers()
        return summaries.sumOf { it.netAmount }
    }

    // Layoff / Brake calculations — Two-way synchronized single source of truth
    fun updateBrakeLimit(limitStr: String) {
        val clean = limitStr.toAsciiDigits().filter { it.isDigit() }
        _currentBrakeInput.value = clean
        val limit = parseAmountOrZero(clean)
        val updated = _settings.value.copy(brakeLimit = limit)
        _settings.value = updated
        repository.saveSettings(updated)
    }

    fun saveBrakeLimit() {
        val raw = _currentBrakeInput.value.trim().toAsciiDigits()
        val limit = parseAmountOrZero(raw)
        val normalizedText = if (limit > 0L) limit.toString() else if (raw.isNotEmpty()) "0" else ""
        _currentBrakeInput.value = normalizedText
        _savedBrakeLimitText.value = normalizedText
        val updated = _settings.value.copy(brakeLimit = limit)
        _settings.value = updated
        repository.saveSettings(updated)
        soundManager.playSave()
        showToast("ဘရိတ်သတ်မှတ်ချက် (${limit}) သိမ်းဆည်းပြီးပါပြီ")
    }

    fun getEffectiveBrakeLimit(): Long {
        return if (_currentBrakeInput.value.isNotBlank()) {
            parseAmountOrZero(_currentBrakeInput.value)
        } else {
            _settings.value.brakeLimit.coerceAtLeast(0L)
        }
    }

    // Layoff execution (ALL or 30 rows). When brake == 0, 100% of bets overflow to တင်ကွက်.
    fun executeLayoff(isAll: Boolean) {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        val limit = getEffectiveBrakeLimit().coerceAtLeast(0L)
        val summaries = getAggregatedNumbers()
        val candidateItems = summaries.filter { it.netAmount > limit }

        val targetItems = if (isAll) candidateItems else candidateItems.take(30)
        if (targetItems.isEmpty()) {
            showToast("တင်ကွက်တင်ရန် ဘရိတ်ထက်ကျော်လွန်သော ဂဏန်းမရှိပါ")
            return
        }

        val layoffItems = targetItems.map { summary ->
            LayoffItem(
                n = summary.n,
                a = summary.netAmount - limit
            )
        }

        val totalLayoff = layoffItems.sumOf { it.a }
        val cleanDate = _selectedDate.value.toAsciiDigits()
        val currentSess = _selectedSession.value
        val existingForSession = _layoffs.value.filter {
            it.date.toAsciiDigits() == cleanDate && it.session == currentSess
        }
        val nextRecordNo = (existingForSession.maxOfOrNull { it.recordNo } ?: existingForSession.size) + 1

        val record = LayoffRecord(
            id = System.currentTimeMillis(),
            date = cleanDate,
            session = currentSess,
            time = timeFormatter.format(Date()).toAsciiDigits(),
            limit = limit,
            items = layoffItems,
            total = totalLayoff,
            recordNo = nextRecordNo
        )

        val updated = listOf(record) + _layoffs.value
        _layoffs.value = updated
        repository.saveLayoffs(updated)
        refreshAvailableRounds()
        soundManager.playSave()
        showToast("တင်ကွက် (${layoffItems.size} ခု ၊ $totalLayoff) တင်ပြီးပါပြီ")
    }

    // Layoff Item & Batch Management
    fun deleteLayoffRecord(recordId: Long) {
        if (_layoffs.value.none { it.id == recordId }) return
        val updated = _layoffs.value.filter { it.id != recordId }
        _layoffs.value = updated
        repository.saveLayoffs(updated)
        soundManager.playDelete()
        showToast("တင်ကွက်မှတ်တမ်း ဖျက်ပြီးပါပြီ")
    }

    fun deleteLayoffItem(recordId: Long, itemIndex: Int) {
        var changed = false
        val updated = _layoffs.value.mapNotNull { rec ->
            if (rec.id == recordId) {
                val newItems = rec.items.toMutableList()
                if (itemIndex in newItems.indices) {
                    newItems.removeAt(itemIndex)
                    changed = true
                }
                if (newItems.isEmpty()) {
                    null
                } else {
                    rec.copy(items = newItems, total = newItems.sumOf { it.a })
                }
            } else rec
        }
        if (!changed) return
        _layoffs.value = updated
        repository.saveLayoffs(updated)
        soundManager.playDelete()
        showToast("တင်ကွက် ဖျက်ပြီးပါပြီ")
    }

    fun editLayoffItem(recordId: Long, itemIndex: Int, newAmount: Long) {
        val rec = _layoffs.value.firstOrNull { it.id == recordId } ?: return
        val currentNum = rec.items.getOrNull(itemIndex)?.n ?: return
        editLayoffItem(recordId, itemIndex, currentNum, newAmount)
    }

    fun editLayoffItem(recordId: Long, itemIndex: Int, newNumber: String, newAmount: Long): Boolean {
        val cleanNum = newNumber.trim().toAsciiDigits().filter { it.isDigit() }
        if (cleanNum.length !in 2..3 || newAmount <= 0L) return false
        val normalizedNum = if (cleanNum.length == 2) {
            "%02d".format(Locale.US, cleanNum.toInt())
        } else {
            cleanNum
        }
        var changed = false
        val updated = _layoffs.value.map { rec ->
            if (rec.id == recordId) {
                val newItems = rec.items.toMutableList()
                if (itemIndex in newItems.indices) {
                    newItems[itemIndex] = newItems[itemIndex].copy(n = normalizedNum, a = newAmount)
                    changed = true
                }
                rec.copy(items = newItems, total = newItems.sumOf { it.a })
            } else rec
        }
        if (!changed) return false
        _layoffs.value = updated
        repository.saveLayoffs(updated)
        soundManager.playEdit()
        showToast("တင်ကွက် ပြင်ဆင်ပြီးပါပြီ")
        return true
    }

    fun updatePassedBetItem2D(
        recordId: Long,
        betId: Long = 0L,
        targetNumber: String,
        newAmount: Long
    ) {
        val rec = _layoffs.value.firstOrNull { it.id == recordId } ?: return
        val targetAscii = targetNumber.trim().toAsciiDigits()
        val itemIndex = rec.items.indexOfFirst { it.n.toAsciiDigits() == targetAscii }
        if (itemIndex >= 0) {
            editLayoffItem(recordId, itemIndex, targetNumber, newAmount)
        }
    }

    // Settlement
    fun getWinningNumberForCurrentSession(): String {
        if (!hasActiveRound) return ""
        val cleanDate = _selectedDate.value.toAsciiDigits()
        return _wins.value.firstOrNull {
            it.date.toAsciiDigits() == cleanDate && it.session == _selectedSession.value
        }?.n?.toAsciiDigits() ?: ""
    }

    // Reuses the existing getAggregatedNumbers() calculation to get the combined total amount (လက်ခံကွက် + တင်ကွက်) bet on the winning number
    fun getWinningNumberBetAmountForCurrentSession(winNumOverride: String? = null): Long {
        val rawWin = (winNumOverride?.trim() ?: getWinningNumberForCurrentSession()).toAsciiDigits()
        if (rawWin.isEmpty()) return 0L
        val parsed = rawWin.toIntOrNull()
        val normalizedWin = if (parsed != null && parsed in 0..99) "%02d".format(Locale.US, parsed) else rawWin
        val summary = getAggregatedNumbers().firstOrNull { it.n == normalizedWin }
        return summary?.totalBet ?: 0L
    }

    /**
     * Returns the amount kept within the configured brake limit (မိမိလက်ခံသော/ဘရိတ်အတွင်း ပေါက်သီးပမာဏ)
     * for the active winning number on the တင်ကွက်များ (Brake) screen.
     * When brake limit is 0, retained amount is 0 (dealer retains zero risk).
     */
    fun getKeptWinningAmountForBrake(winNumOverride: String? = null, brakeLimitOverride: Long? = null): Long {
        val rawWin = (winNumOverride?.trim() ?: getWinningNumberForCurrentSession()).toAsciiDigits()
        if (rawWin.isEmpty()) return 0L
        val parsed = rawWin.toIntOrNull()
        val normalizedWin = if (parsed != null && parsed in 0..99) "%02d".format(Locale.US, parsed) else rawWin
        val summary = getAggregatedNumbers().firstOrNull { it.n == normalizedWin } ?: return 0L
        val activeBrakeLimit = (brakeLimitOverride ?: getEffectiveBrakeLimit()).coerceAtLeast(0L)
        return minOf(summary.netAmount, activeBrakeLimit)
    }

    fun getFormattedWinningHeaderForCurrentSession(
        winNumOverride: String? = null,
        useThousandsSeparator: Boolean = false
    ): String {
        val rawWin = (winNumOverride?.trim() ?: getWinningNumberForCurrentSession()).toAsciiDigits()
        val amount = getWinningNumberBetAmountForCurrentSession(rawWin)
        return formatWinningHeaderLine(rawWin, amount, useThousandsSeparator)
    }

    fun hasSavedWinningNumberForCurrentSession(): Boolean {
        val win = getWinningNumberForCurrentSession().toAsciiDigits()
        return win.length == 2 && win.all { it.isDigit() }
    }

    fun setWinningNumber(n: String) {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        val clean = n.trim().toAsciiDigits()
        if (clean.isEmpty()) {
            clearWinningNumberForCurrentSession()
            return
        }
        if (clean.length != 2 || !clean.all { it.isDigit() }) return
        val cleanDate = _selectedDate.value.toAsciiDigits()
        val currentSess = _selectedSession.value
        val filtered = _wins.value.filterNot {
            it.date.toAsciiDigits() == cleanDate && it.session == currentSess
        }
        val updated = listOf(WinRecord(cleanDate, currentSess, clean)) + filtered
        _wins.value = updated
        _activeWinningNumberFor2DSummary.value = clean
        repository.saveWins(updated)

        // Lock winning number in Session2D
        repository.session2DDao.updateWinningNumberByDateAndNameSync(cleanDate, currentSess, clean, closed = true)
        _sessions.value = _sessions.value.map {
            if (it.name == currentSess) it.copy(winningNumber = clean, isClosed = true) else it
        }

        refreshAvailableRounds()
        soundManager.playSave()
        showToast("ထွက်ဂဏန်း ($clean) သိမ်းဆည်းပြီးပါပြီ")
    }

    fun clearWinningNumberForCurrentSession() {
        val currentDate = _selectedDate.value.toAsciiDigits()
        val currentSess = _selectedSession.value
        if (_wins.value.none { it.date.toAsciiDigits() == currentDate && it.session == currentSess }) {
            _activeWinningNumberFor2DSummary.value = null
            return
        }
        val updated = _wins.value.filterNot { it.date.toAsciiDigits() == currentDate && it.session == currentSess }
        _wins.value = updated
        _activeWinningNumberFor2DSummary.value = null
        repository.saveWins(updated)

        repository.session2DDao.updateWinningNumberByDateAndNameSync(currentDate, currentSess, null, closed = false)
        _sessions.value = _sessions.value.map {
            if (it.name == currentSess) it.copy(winningNumber = null, isClosed = false) else it
        }
    }

    fun updateWinRecord(oldRecord: WinRecord, newRecord: WinRecord) {
        val cleanN = newRecord.n.trim().toAsciiDigits()
        if (cleanN.isEmpty()) return
        val normalizedNew = newRecord.copy(date = newRecord.date.toAsciiDigits(), n = cleanN)
        val updated = _wins.value.map {
            if (it.date.toAsciiDigits() == oldRecord.date.toAsciiDigits() && it.session == oldRecord.session) normalizedNew else it
        }
        _wins.value = updated
        if (normalizedNew.date == _selectedDate.value.toAsciiDigits() && normalizedNew.session == _selectedSession.value) {
            _activeWinningNumberFor2DSummary.value = normalizedNew.n
        }
        repository.saveWins(updated)
        soundManager.playEdit()
        showToast("ထွက်ဂဏန်း ပြင်ဆင်ပြီးပါပြီ")
    }

    fun deleteWinRecord(record: WinRecord) {
        val recDate = record.date.toAsciiDigits()
        if (_wins.value.none { it.date.toAsciiDigits() == recDate && it.session == record.session }) return
        val updated = _wins.value.filterNot { it.date.toAsciiDigits() == recDate && it.session == record.session }
        _wins.value = updated
        if (recDate == _selectedDate.value.toAsciiDigits() && record.session == _selectedSession.value) {
            _activeWinningNumberFor2DSummary.value = null
        }
        repository.saveWins(updated)
        soundManager.playDelete()
        showToast("ထွက်ဂဏန်း ဖျက်ပြီးပါပြီ")
    }

    // Calculate Settlement for a specific date, session, and winning number
    // Net Profit/Loss = Total Bet Amount - (Total Commission + Total Payout)
    fun calculateSettlementForSession(
        date: String = _selectedDate.value,
        session: String = _selectedSession.value,
        winningNum: String
    ): SettledCalculation {
        val vouchers = getFilteredVouchers(date, session)
        val layoffs = getFilteredLayoffs(date, session)

        val totalPerNum = mutableMapOf<String, Long>()
        vouchers.forEach { v ->
            v.items.forEach { item ->
                totalPerNum[item.n] = (totalPerNum[item.n] ?: 0L) + item.a
            }
        }

        val layoffPerNum = mutableMapOf<String, Long>()
        layoffs.forEach { l ->
            l.items.forEach { item ->
                layoffPerNum[item.n] = (layoffPerNum[item.n] ?: 0L) + item.a
            }
        }

        val isExplicitZeroBrake = _currentBrakeInput.value.trim() == "0"
        val activeBrakeLimit = getEffectiveBrakeLimit()

        val retainedPerNum = mutableMapOf<String, Long>()
        for (i in 0..99) {
            val numStr = "%02d".format(Locale.US, i)
            val tot = totalPerNum[numStr] ?: 0L
            val lay = layoffPerNum[numStr] ?: 0L
            val netHeld = tot - lay // Signed: Never clamped to 0
            retainedPerNum[numStr] = when {
                isExplicitZeroBrake -> if (netHeld < 0L) netHeld else 0L
                activeBrakeLimit > 0L -> minOf(netHeld, activeBrakeLimit)
                else -> netHeld
            }
        }

        val totalRetained = retainedPerNum.values.sum()

        val sess = _sessions.value.firstOrNull { it.name == session }
        val sessionAgents = if (date == _selectedDate.value && session == _selectedSession.value && _agents.value.isNotEmpty()) {
            _agents.value
        } else {
            repository.loadAgentsForDateAndRound(date, session)
        }
        val fallbackAgents = if (sessionAgents.isEmpty()) repository.loadAgents() else emptyList()
        val effectiveAgents = if (sessionAgents.isNotEmpty()) sessionAgents else fallbackAgents

        val defaultRate = sess?.rate ?: 0.0
        val defaultMult = if ((sess?.mult ?: 0.0) > 0.0) {
            sess!!.mult
        } else {
            effectiveAgents.firstOrNull { it.mult > 0.0 }?.mult ?: 0.0
        }

        val agentMap = effectiveAgents.associateBy { it.id }
        val fallbackMap = fallbackAgents.associateBy { it.id }
        var totalCommission = 0.0
        var totalPayout = 0.0

        val vouchersByAgent = vouchers.groupBy { it.agentId }
        for ((agentId, agentVouchers) in vouchersByAgent) {
            val ag = agentMap[agentId]
                ?: effectiveAgents.firstOrNull { it.name == agentVouchers.firstOrNull()?.agent }
                ?: fallbackMap[agentId]
                ?: fallbackAgents.firstOrNull { it.name == agentVouchers.firstOrNull()?.agent }
            val rate = if ((ag?.rate ?: 0.0) > 0.0) ag!!.rate else defaultRate
            val mult = if ((ag?.mult ?: 0.0) > 0.0) ag!!.mult else defaultMult
            var agentCommissionBase = 0.0
            var agentWinRetainedBase = 0.0
            agentVouchers.forEach { v ->
                v.items.forEach { item ->
                    val tot = totalPerNum[item.n] ?: 0L
                    if (tot > 0L) {
                        val ret = retainedPerNum[item.n] ?: 0L
                        val retPositive = ret.coerceAtLeast(0L)
                        val scaledComm = item.a.toDouble() * (retPositive.toDouble() / tot.toDouble())
                        agentCommissionBase += scaledComm
                        if (winningNum.isNotEmpty() && item.n == winningNum) {
                            val scaledWin = item.a.toDouble() * (ret.toDouble() / tot.toDouble())
                            agentWinRetainedBase += scaledWin
                        }
                    }
                }
            }
            totalCommission += agentCommissionBase * (rate / 100.0)
            if (winningNum.isNotEmpty() && mult > 0.0) {
                totalPayout += agentWinRetainedBase * mult
            }
        }

        val commLong = Math.round(totalCommission)
        val retainedNet = totalRetained - commLong

        val winningRetained = if (winningNum.isNotEmpty()) (retainedPerNum[winningNum] ?: 0L) else 0L
        val payout = if (winningNum.isNotEmpty()) {
            if (winningRetained < 0L) {
                // Negative balance on kept bets: house collected external hedging profit!
                // Net Payout = NetKept(num) * Multiplier (e.g., -3000 * 80 = -240,000)
                Math.round(winningRetained.toDouble() * defaultMult)
            } else if (totalPayout != 0.0) {
                Math.round(totalPayout)
            } else if (defaultMult > 0.0) {
                Math.round(winningRetained.toDouble() * defaultMult)
            } else 0L
        } else 0L

        // Net Profit/Loss = Total Retained Bet Amount - (Total Commission + Total Payout)
        val profitLoss = totalRetained - (commLong + payout)

        return SettledCalculation(
            totalRetained = totalRetained,
            commission = commLong,
            retainedNet = retainedNet,
            multiplier = defaultMult,
            winningNumber = winningNum,
            winningRetained = winningRetained,
            payout = payout,
            profitLoss = profitLoss
        )
    }

    fun calculateSettlement(winningNum: String): SettledCalculation {
        return calculateSettlementForSession(
            date = _selectedDate.value,
            session = _selectedSession.value,
            winningNum = winningNum
        )
    }

    // Round-by-round (အကြိမ်ရေအလိုက်) net profit/loss calculation for selected date
    fun getRoundSummariesForSelectedDate(): List<RoundSettlementSummary> {
        val date = _selectedDate.value.toAsciiDigits()
        val sessionNames = (_sessions.value.map { it.name } +
            _vouchers.value.filter { it.date.toAsciiDigits() == date }.map { it.session } +
            _wins.value.filter { it.date.toAsciiDigits() == date }.map { it.session })
            .filter { it.isNotBlank() }
            .distinct()

        return sessionNames.map { sessName ->
            val winNum = _wins.value.firstOrNull { it.date.toAsciiDigits() == date && it.session == sessName }?.n?.toAsciiDigits() ?: ""
            val calc = calculateSettlementForSession(date = date, session = sessName, winningNum = winNum)
            RoundSettlementSummary(
                date = date,
                session = sessName,
                winningNumber = winNum,
                totalBet = calc.totalRetained,
                commission = calc.commission,
                netAmount = calc.retainedNet,
                payout = calc.payout,
                profitLoss = calc.profitLoss
            )
        }
    }

    // Agent calculation excluding layoff amounts for agent card
    fun getAgentSummaryData(agent: Agent, winNum: String): AgentSummaryData {
        val vouchers = getFilteredVouchers().filter { it.agentId == agent.id || it.agent == agent.name }
        val allVouchers = getFilteredVouchers()
        val layoffs = getFilteredLayoffs()

        val totalPerNum = mutableMapOf<String, Long>()
        allVouchers.forEach { v ->
            v.items.forEach { item ->
                totalPerNum[item.n] = (totalPerNum[item.n] ?: 0L) + item.a
            }
        }

        val layoffPerNum = mutableMapOf<String, Long>()
        layoffs.forEach { l ->
            l.items.forEach { item ->
                layoffPerNum[item.n] = (layoffPerNum[item.n] ?: 0L) + item.a
            }
        }

        val retainedPerNum = mutableMapOf<String, Long>()
        for (i in 0..99) {
            val numStr = "%02d".format(Locale.US, i)
            val tot = totalPerNum[numStr] ?: 0L
            val lay = layoffPerNum[numStr] ?: 0L
            retainedPerNum[numStr] = (tot - lay).coerceAtLeast(0L)
        }

        var totalBet = 0L
        var agentRetainedTotal = 0.0
        var agentRetainedWin = 0.0

        vouchers.forEach { v ->
            v.items.forEach { item ->
                totalBet += item.a
                val tot = totalPerNum[item.n] ?: 0L
                if (tot > 0L) {
                    val ret = retainedPerNum[item.n] ?: 0L
                    val scaled = item.a.toDouble() * (ret.toDouble() / tot.toDouble())
                    agentRetainedTotal += scaled
                    if (winNum.isNotEmpty() && item.n == winNum) {
                        agentRetainedWin += scaled
                    }
                }
            }
        }

        val commAmount = Math.round(agentRetainedTotal * (agent.rate / 100.0))
        val netAmount = (Math.round(agentRetainedTotal) - commAmount).coerceAtLeast(0L)
        val payout = if (agent.mult > 0.0 && winNum.isNotEmpty()) Math.round(agentRetainedWin * agent.mult) else 0L

        return AgentSummaryData(
            totalBet = totalBet,
            commission = commAmount,
            netAmount = netAmount,
            voucherCount = vouchers.size,
            payout = payout
        )
    }

    fun getAgent2DSettlementList(winNum: String): List<Agent2DSettlementItem> {
        val allVouchers = getFilteredVouchers()
        return _agents.value.map { agent ->
            val agentVouchers = allVouchers.filter { it.agentId == agent.id || it.agent == agent.name }
            var totalBet = 0L
            var winBetAmount = 0L
            agentVouchers.forEach { v ->
                v.items.forEach { item ->
                    totalBet += item.a
                    if (winNum.isNotEmpty() && item.n == winNum) {
                        winBetAmount += item.a
                    }
                }
            }
            val commission = Math.round(totalBet.toDouble() * (agent.rate / 100.0))
            val netAmount = (totalBet - commission).coerceAtLeast(0L)
            val payout = if (winNum.isNotEmpty() && agent.mult > 0.0) {
                Math.round(winBetAmount.toDouble() * agent.mult)
            } else 0L
            val profitLoss = totalBet - (commission + payout)
            Agent2DSettlementItem(
                agent = agent,
                voucherCount = agentVouchers.size,
                totalBet = totalBet,
                commission = commission,
                netAmount = netAmount,
                winBetAmount = winBetAmount,
                payout = payout,
                profitLoss = profitLoss
            )
        }
    }

    fun getAgent2DSettlements(winNum: String = getWinningNumberForCurrentSession()): List<Agent2DSettlement> {
        val cleanWin = winNum.trim().toAsciiDigits()
        val allVouchers = getFilteredVouchers()
        val sess = _sessions.value.firstOrNull { it.name == _selectedSession.value }
        val defaultRate = sess?.rate ?: 0.0
        val defaultMult = sess?.mult ?: 0.0

        val agentsList = _agents.value.toMutableList()
        val knownIds = agentsList.map { it.id }.toSet()
        val extraVouchers = allVouchers.filter { it.agentId !in knownIds }
        extraVouchers.groupBy { it.agentId to it.agent }.forEach { (pair, _) ->
            val (id, name) = pair
            agentsList.add(Agent(id = id, name = name, rate = defaultRate, mult = defaultMult))
        }

        return agentsList.map { agent ->
            val agentVouchers = allVouchers.filter { it.agentId == agent.id || it.agent == agent.name }
            var totalSales = 0L
            var winBetAmount = 0L
            agentVouchers.forEach { v ->
                v.items.forEach { item ->
                    totalSales += item.a
                    if (cleanWin.isNotEmpty() && item.n.toAsciiDigits() == cleanWin) {
                        winBetAmount += item.a
                    }
                }
            }
            val effRate = if (agent.rate > 0.0) agent.rate else defaultRate
            val effMult = if (agent.mult > 0.0) agent.mult else defaultMult
            val commissionAmount = Math.round(totalSales.toDouble() * (effRate / 100.0))
            val netSales = (totalSales - commissionAmount).coerceAtLeast(0L)
            val payoutAmount = if (cleanWin.isNotEmpty() && effMult > 0.0) {
                Math.round(winBetAmount.toDouble() * effMult)
            } else 0L
            val netBalance = netSales - payoutAmount

            Agent2DSettlement(
                agentId = agent.id,
                agentName = agent.name,
                commissionRate = effRate,
                multiplier = effMult,
                totalSales = totalSales,
                commissionAmount = commissionAmount,
                netSales = netSales,
                winningBetAmount = winBetAmount,
                payoutAmount = payoutAmount,
                netBalance = netBalance
            )
        }
    }

    /**
     * Calculates the Date-Wide Aggregated Daily Settlement (ရက်ချုပ်) across ALL rounds of [date].
     * Combines saved round settlements with any active rounds of [date] that have a winning number.
     */
    fun calculateAggregatedDailySettlementForDate(
        date: String = _selectedDate.value,
        includeUnsavedRoundsWithWin: Boolean = true
    ): DailySettlementEntity {
        val cleanDate = date.trim().toAsciiDigits()
        val savedForDate = _settled.value.filter { it.date.toAsciiDigits() == cleanDate }
        val savedBySession = savedForDate.associateBy { it.session }

        val candidateRounds = if (includeUnsavedRoundsWithWin) {
            val configured = _sessions.value.map { it.name.trim() }.filter { it.isNotEmpty() }
            val voucherRounds = _vouchers.value.filter { it.date.toAsciiDigits() == cleanDate }.map { it.session.trim() }
            val winRounds = _wins.value.filter { it.date.toAsciiDigits() == cleanDate }.map { it.session.trim() }
            val savedRounds = savedForDate.map { it.session.trim() }
            (configured + voucherRounds + winRounds + savedRounds)
                .filter { it.isNotEmpty() && "$cleanDate|$it" !in deletedRoundKeys }
                .distinct()
        } else {
            savedForDate.reversed().map { it.session.trim() }.filter { it.isNotEmpty() }.distinct()
        }

        var totalSales = 0L
        var totalComm = 0L
        var totalNet = 0L
        var totalPayout = 0L
        var totalProfitLoss = 0L
        val includedRounds = mutableListOf<String>()
        val winSummaries = mutableListOf<String>()

        for (roundName in candidateRounds) {
            val savedRec = savedBySession[roundName]
            if (savedRec != null) {
                totalSales += savedRec.totalRetained
                totalComm += savedRec.commission
                totalNet += savedRec.retainedNet
                totalPayout += savedRec.payout
                totalProfitLoss += savedRec.profitLoss
                includedRounds.add(roundName)
                winSummaries.add("$roundName:${savedRec.winningNumber.ifEmpty { "-" }}")
            } else if (includeUnsavedRoundsWithWin) {
                val winNum = _wins.value.firstOrNull { it.date.toAsciiDigits() == cleanDate && it.session == roundName }?.n?.trim().orEmpty()
                val hasVouchers = _vouchers.value.any { it.date.toAsciiDigits() == cleanDate && it.session == roundName }
                if (winNum.length == 2 && winNum.all { it.isDigit() } || hasVouchers) {
                    val calc = calculateSettlementForSession(date = cleanDate, session = roundName, winningNum = winNum)
                    if (calc.totalRetained > 0L || winNum.isNotEmpty()) {
                        totalSales += calc.totalRetained
                        totalComm += calc.commission
                        totalNet += calc.retainedNet
                        totalPayout += calc.payout
                        totalProfitLoss += calc.profitLoss
                        includedRounds.add(roundName)
                        winSummaries.add("$roundName:${winNum.ifEmpty { "-" }}")
                    }
                }
            }
        }

        return DailySettlementEntity(
            date = cleanDate,
            totalSales = totalSales,
            totalCommission = totalComm,
            netSales = totalNet,
            winningPayout = totalPayout,
            profitOrLoss = totalProfitLoss,
            roundsSummary = includedRounds.joinToString(", "),
            winningNumbersSummary = winSummaries.joinToString(", "),
            roundCount = includedRounds.size,
            timestamp = timeFormatter.format(Date()).toAsciiDigits()
        )
    }

    fun getDailySettlementForDate(date: String = _selectedDate.value): DailySettlementEntity? {
        val cleanDate = date.trim().toAsciiDigits()
        return _dailySettlements.value.firstOrNull { it.date.toAsciiDigits() == cleanDate }
            ?: repository.loadDailySettlementForDate(cleanDate)
    }

    fun saveDailySettlementForDate(
        date: String = _selectedDate.value,
        includeUnsavedRoundsWithWin: Boolean = true
    ): DailySettlementEntity? {
        val cleanDate = date.trim().toAsciiDigits()
        val aggregated = calculateAggregatedDailySettlementForDate(
            date = cleanDate,
            includeUnsavedRoundsWithWin = includeUnsavedRoundsWithWin
        )
        if (aggregated.roundCount == 0 && _settled.value.none { it.date.toAsciiDigits() == cleanDate }) {
            repository.deleteDailySettlementByDateSync(cleanDate)
            _dailySettlements.value = repository.loadAllDailySettlements()
            return null
        }
        repository.saveDailySettlement(aggregated)
        _dailySettlements.value = repository.loadAllDailySettlements()
        return aggregated
    }

    fun commitSettlement(calc: SettledCalculation) {
        val editing = _editingSettledRecord.value
        if (editing != null) {
            val updatedRecord = editing.copy(
                id = editing.id,
                date = _selectedDate.value,
                session = _selectedSession.value.ifBlank { editing.session },
                winningNumber = calc.winningNumber,
                totalRetained = calc.totalRetained,
                commission = calc.commission,
                retainedNet = calc.retainedNet,
                payout = calc.payout,
                profitLoss = calc.profitLoss
            )
            manuallyEditedRecordIds.add(editing.id)
            val updated = _settled.value.map { if (it.id == editing.id) updatedRecord else it }
            _settled.value = updated
            repository.updateSettledRecordById(editing.id, updatedRecord)
            saveDailySettlementForDate(date = updatedRecord.date, includeUnsavedRoundsWithWin = false)
            _editingSettledRecord.value = null
            soundManager.playSave()
            showToast("စာရင်းချုပ် ပြင်ဆင်ပြီးပါပြီ")
            return
        }
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        val activeDate = _selectedDate.value
        val roundKey = "$activeDate|${_selectedSession.value}"
        deletedRoundKeys.remove(roundKey)
        val record = SettledRecord(
            id = System.currentTimeMillis() + (nextSettledIdCounter++),
            date = activeDate,
            session = _selectedSession.value,
            winningNumber = calc.winningNumber,
            totalRetained = calc.totalRetained,
            commission = calc.commission,
            retainedNet = calc.retainedNet,
            payout = calc.payout,
            profitLoss = calc.profitLoss,
            timestamp = timeFormatter.format(Date())
        )
        val updated = (_settled.value + record).sortedWith(compareBy<SettledRecord> { it.id })
        manuallyEditedRecordIds.add(record.id)
        _settled.value = updated
        repository.saveSettled(updated)
        saveDailySettlementForDate(date = activeDate, includeUnsavedRoundsWithWin = true)
        soundManager.playSave()
        showToast("စာရင်းချုပ် သိမ်းဆည်းပြီးပါပြီ")
    }

    fun getLockedSettledRecordForCurrentSession(): SettledRecord? {
        val date = _selectedDate.value
        val sess = _selectedSession.value
        return _settled.value.firstOrNull {
            it.date == date && it.session == sess && it.id in manuallyEditedRecordIds
        }
    }

    /**
     * Locks and saves the current round's settlement and the Date-Wide Consolidated Daily Settlement (ရက်ချုပ်)
     * in Room DB (`settlements` and `daily_settlements`), then navigates to "ရက်ချုပ်အားလုံး" (Screen.SETTLEMENT).
     * Regardless of which round ("22.AM", "22.PM", etc.) the user is viewing for that date,
     * the saved "ရက်ချုပ်" summary remains consistently visible and accessible across all rounds of that day.
     */
    fun lockAndSaveBrakeSettlement() {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        if (!hasSavedWinningNumberForCurrentSession()) {
            showToast("ထွက်ဂဏန်း အရင်ထည့်ပါ")
            return
        }
        val date = _selectedDate.value
        val sess = _selectedSession.value
        val roundKey = "$date|$sess"
        deletedRoundKeys.remove(roundKey)

        val winNum = getWinningNumberForCurrentSession()
        val calc = calculateSettlementForSession(date = date, session = sess, winningNum = winNum)
        val existingAny = _settled.value.firstOrNull { it.date == date && it.session == sess }

        if (existingAny != null) {
            val lockedRec = existingAny.copy(
                winningNumber = calc.winningNumber,
                totalRetained = calc.totalRetained,
                commission = calc.commission,
                retainedNet = calc.retainedNet,
                payout = calc.payout,
                profitLoss = calc.profitLoss
            )
            manuallyEditedRecordIds.add(lockedRec.id)
            val updated = _settled.value.map { if (it.id == existingAny.id) lockedRec else it }
            _settled.value = updated
            repository.saveSettled(updated)
        } else {
            val newRec = SettledRecord(
                id = System.currentTimeMillis() + (nextSettledIdCounter++),
                date = date,
                session = sess,
                winningNumber = calc.winningNumber,
                totalRetained = calc.totalRetained,
                commission = calc.commission,
                retainedNet = calc.retainedNet,
                payout = calc.payout,
                profitLoss = calc.profitLoss,
                timestamp = timeFormatter.format(Date())
            )
            manuallyEditedRecordIds.add(newRec.id)
            val updated = (_settled.value + newRec).sortedWith(compareBy<SettledRecord> { it.id })
            _settled.value = updated
            repository.saveSettled(updated)
        }

        // Persist the Date-Wide Consolidated Daily Settlement (ရက်ချုပ်) scoped by primaryKey = [date]
        saveDailySettlementForDate(date = date, includeUnsavedRoundsWithWin = true)

        soundManager.playSave()
        showToast("ရက်ချုပ် စာရင်း သိမ်းဆည်းပြီးပါပြီ")
        navigateTo(Screen.SETTLEMENT)
    }

    /**
     * Strict Daily Settlement Persistence Guard ("ရက်ချုပ်" မသိမ်းမချင်း လုံးဝ မပြရန်):
     * Entering a winning number or calculating preview profit/loss must NOT automatically
     * populate or show any records inside the "ရက်ချုပ်" / "ရက်ချုပ်အားလုံး" screen.
     * Daily settlement records in `_settled` ONLY appear and are saved after the user explicitly
     * taps the "သိမ်းမည်" (Save Settlement) button (`lockAndSaveBrakeSettlement` or `commitSettlement`).
     */
    fun syncDailyRecordsFromCurrentDateRounds() {
        // Strict guard: never auto-insert unsaved rounds into _settled.
    }

    fun getDailySummaryTotalSales(): Long = _settled.value.sumOf { it.totalRetained }

    fun getDailySummaryTotalPayout(): Long = _settled.value.sumOf { it.payout }

    fun getDailySummaryTotalProfitLoss(): Long = _settled.value.sumOf { it.profitLoss }

    fun getTotalSettledProfitLoss(): Long = _settled.value.sumOf { it.profitLoss }

    fun startEditingSettledRecord(record: SettledRecord, navigateToHome: Boolean = false) {
        soundManager.playEdit()
        _editingSettledRecord.value = record
        _selectedDate.value = record.date
        if (record.session.isNotBlank()) {
            _selectedSession.value = record.session
        }
        if (navigateToHome) {
            navigateTo(Screen.HOME)
        }
    }

    fun cancelEditingSettledRecord() {
        _editingSettledRecord.value = null
        if (_currentScreen.value == Screen.HOME && screenHistory.size > 1) {
            goBack()
        }
    }

    fun saveEditedSettledRecord(
        date: String? = null,
        session: String? = null,
        winningNumber: String? = null,
        totalRetained: Long? = null,
        commission: Long? = null,
        payout: Long? = null
    ): Boolean {
        val existing = _editingSettledRecord.value ?: return false
        val targetDate = date?.trim()?.ifEmpty { existing.date } ?: existing.date
        val targetSession = session?.trim()?.ifEmpty { existing.session } ?: existing.session
        val targetWin = winningNumber?.trim() ?: existing.winningNumber

        val hasExplicitAmounts = totalRetained != null || commission != null || payout != null
        val hasVouchersForTarget = _vouchers.value.any { it.date == targetDate && it.session == targetSession }

        val finalTotalRetained: Long
        val finalCommission: Long
        val finalRetainedNet: Long
        val finalPayout: Long
        val finalProfitLoss: Long

        if (!hasExplicitAmounts && hasVouchersForTarget) {
            val calc = calculateSettlementForSession(
                date = targetDate,
                session = targetSession,
                winningNum = targetWin
            )
            finalTotalRetained = calc.totalRetained
            finalCommission = calc.commission
            finalRetainedNet = calc.retainedNet
            finalPayout = calc.payout
            finalProfitLoss = calc.profitLoss
        } else {
            finalTotalRetained = (totalRetained ?: existing.totalRetained).coerceAtLeast(0L)
            finalCommission = (commission ?: existing.commission).coerceAtLeast(0L)
            finalRetainedNet = (finalTotalRetained - finalCommission).coerceAtLeast(0L)
            finalPayout = (payout ?: existing.payout).coerceAtLeast(0L)
            finalProfitLoss = finalTotalRetained - (finalCommission + finalPayout)
        }

        val updatedRecord = existing.copy(
            id = existing.id,
            date = targetDate,
            session = targetSession,
            winningNumber = targetWin,
            totalRetained = finalTotalRetained,
            commission = finalCommission,
            retainedNet = finalRetainedNet,
            payout = finalPayout,
            profitLoss = finalProfitLoss
        )

        manuallyEditedRecordIds.add(existing.id)
        val updatedList = _settled.value.map { if (it.id == existing.id) updatedRecord else it }
        _settled.value = updatedList
        repository.updateSettledRecordById(existing.id, updatedRecord)
        saveDailySettlementForDate(date = targetDate, includeUnsavedRoundsWithWin = false)
        if (existing.date != targetDate) {
            saveDailySettlementForDate(date = existing.date, includeUnsavedRoundsWithWin = false)
        }
        _editingSettledRecord.value = null
        soundManager.playSave()
        showToast("စာရင်းချုပ် ပြင်ဆင်ပြီးပါပြီ")
        if (_currentScreen.value == Screen.HOME && screenHistory.size > 1) {
            goBack()
        }
        return true
    }

    fun updateSettledRecord(
        recordId: Long,
        date: String? = null,
        session: String? = null,
        winningNumber: String? = null,
        totalRetained: Long? = null,
        commission: Long? = null,
        payout: Long? = null
    ): Boolean {
        val existing = _settled.value.firstOrNull { it.id == recordId } ?: return false
        val targetDate = date?.trim()?.ifEmpty { existing.date } ?: existing.date
        val targetSession = session?.trim()?.ifEmpty { existing.session } ?: existing.session
        val targetWin = winningNumber?.trim() ?: existing.winningNumber
        val finalTotalRetained = (totalRetained ?: existing.totalRetained).coerceAtLeast(0L)
        val finalCommission = (commission ?: existing.commission).coerceAtLeast(0L)
        val finalRetainedNet = (finalTotalRetained - finalCommission).coerceAtLeast(0L)
        val finalPayout = (payout ?: existing.payout).coerceAtLeast(0L)
        val finalProfitLoss = finalTotalRetained - (finalCommission + finalPayout)

        val updatedRecord = existing.copy(
            id = existing.id,
            date = targetDate,
            session = targetSession,
            winningNumber = targetWin,
            totalRetained = finalTotalRetained,
            commission = finalCommission,
            retainedNet = finalRetainedNet,
            payout = finalPayout,
            profitLoss = finalProfitLoss
        )
        manuallyEditedRecordIds.add(recordId)
        val updatedList = _settled.value.map { if (it.id == recordId) updatedRecord else it }
        _settled.value = updatedList
        repository.updateSettledRecordById(recordId, updatedRecord)
        saveDailySettlementForDate(date = targetDate, includeUnsavedRoundsWithWin = false)
        if (existing.date != targetDate) {
            saveDailySettlementForDate(date = existing.date, includeUnsavedRoundsWithWin = false)
        }
        soundManager.playSave()
        showToast("စာရင်းချုပ် ပြင်ဆင်ပြီးပါပြီ")
        return true
    }

    fun clearSettledLedger() {
        _settled.value.forEach { rec ->
            deletedRoundKeys.add("${rec.date}|${rec.session}")
        }
        val rounds = getRoundSummariesForSelectedDate()
        rounds.forEach { r ->
            deletedRoundKeys.add("${r.date}|${r.session}")
        }
        _settled.value = emptyList()
        _dailySettlements.value = emptyList()
        repository.saveSettled(emptyList())
        repository.clearAllDailySettlementsSync()
        soundManager.playDelete()
        showToast("စာရင်းချုပ် မှတ်တမ်းများကို ရှင်းထုတ်ပြီးပါပြီ")
    }

    fun buildDailySettlementsCsvSummary(): String {
        val records = _settled.value
        val totalProfitLoss = records.sumOf { it.profitLoss }
        val signedGrandTotal = when {
            totalProfitLoss > 0L -> "+$totalProfitLoss"
            totalProfitLoss < 0L -> "$totalProfitLoss"
            else -> "0"
        }
        return buildString {
            appendLine("စဉ်,ရက်စွဲ,အပိုင်း,ပေါက်သီး,အမြတ်ငွေ")
            records.forEachIndexed { idx, rec ->
                val signedPl = when {
                    rec.profitLoss > 0L -> "+${rec.profitLoss}"
                    rec.profitLoss < 0L -> "${rec.profitLoss}"
                    else -> "0"
                }
                appendLine("${idx + 1},${rec.date},${rec.session},${rec.winningNumber.ifEmpty { "-" }},$signedPl")
            }
            appendLine("စုစုပေါင်းစာရင်းချုပ်,,,,$signedGrandTotal")
        }
    }

    fun buildDailySettlementsTextSummary(): String {
        val records = _settled.value
        val totalProfitLoss = records.sumOf { it.profitLoss }
        val signedGrandTotal = when {
            totalProfitLoss > 0L -> "+$totalProfitLoss"
            totalProfitLoss < 0L -> "$totalProfitLoss"
            else -> "0"
        }
        return buildString {
            appendLine("ရက်ချုပ်အားလုံး")
            appendLine("----------------------------------------")
            appendLine("စဉ် | ရက်စွဲ | အပိုင်း | အမြတ်ငွေ")
            appendLine("----------------------------------------")
            records.forEachIndexed { idx, rec ->
                val signedPl = when {
                    rec.profitLoss > 0L -> "+${rec.profitLoss}"
                    rec.profitLoss < 0L -> "${rec.profitLoss}"
                    else -> "0"
                }
                appendLine("${idx + 1} | ${rec.date} | ${rec.session} | $signedPl")
            }
            appendLine("----------------------------------------")
            appendLine("စုစုပေါင်းစာရင်းချုပ် = $signedGrandTotal")
        }
    }

    /**
     * Generates both TXT and CSV summaries of all daily settlements ("ရက်ချုပ်အားလုံး")
     * for local storage access and optionally shares via Android Share Intent (Intent.ACTION_SEND).
     */
    fun exportDailySettlementsToFile(
        context: android.content.Context,
        launchShare: Boolean = false,
        shareAsCsv: Boolean = false
    ): java.io.File {
        val csvContent = buildDailySettlementsCsvSummary()
        val txtContent = buildDailySettlementsTextSummary()

        val exportDir = context.getExternalFilesDir(null) ?: context.filesDir
        exportDir.mkdirs()
        val csvFile = java.io.File(exportDir, "daily_settlements_summary.csv")
        val txtFile = java.io.File(exportDir, "daily_settlements_summary.txt")
        csvFile.writeText(csvContent, Charsets.UTF_8)
        txtFile.writeText(txtContent, Charsets.UTF_8)
        soundManager.playSave()
        showToast("ဖိုင်သိမ်းပြီးပါပြီ: ${if (shareAsCsv) csvFile.name else txtFile.name}")
        if (launchShare) {
            val payload = if (shareAsCsv) csvContent else txtContent
            com.example.ui.components.ShareHelper.shareText(
                context = context,
                text = payload,
                title = "ရက်ချုပ်အားလုံး Export"
            )
        }
        return csvFile
    }

    /**
     * Exports the current session's ledger table data (တင်ကွက် / ဝယ်ရောင်း မှတ်တမ်းများ)
     * as both CSV and TXT files in local storage and optionally shares via Intent.ACTION_SEND.
     */
    fun exportCurrentSessionLedgerToFile(context: android.content.Context, launchShare: Boolean = false): java.io.File {
        val date = _selectedDate.value
        val sess = _selectedSession.value.ifBlank { "-" }
        val activeBrake = getEffectiveBrakeLimit().coerceAtLeast(0L)
        val summaries = getAggregatedNumbers()
        val winNum = getWinningNumberForCurrentSession()
        val calc = calculateSettlementForSession(date = date, session = sess, winningNum = winNum)
        val sessionLayoffs = getFilteredLayoffs()

        val keptItems = if (activeBrake > 0L) {
            summaries.mapNotNull { s ->
                val kept = minOf(s.netAmount, activeBrake)
                if (kept > 0L) s.n to kept else null
            }
        } else {
            emptyList()
        }
        val overflowItems = summaries.mapNotNull { s ->
            if (s.netAmount > activeBrake) s.n to (s.netAmount - activeBrake) else null
        }

        val csvContent = buildString {
            appendLine("အမျိုးအစား,ဂဏန်း,ပမာဏ,ရက်စွဲ,အပိုင်း")
            keptItems.forEach { (n, a) ->
                appendLine("လက်ခံကွက်,$n,$a,$date,$sess")
            }
            overflowItems.forEach { (n, a) ->
                appendLine("တင်ကွက်,$n,$a,$date,$sess")
            }
            sessionLayoffs.forEachIndexed { batchIdx, rec ->
                rec.items.forEach { itm ->
                    appendLine("တင်ပြီးမှတ်တမ်း #${batchIdx + 1},${itm.n},${itm.a},${rec.date},${rec.session}")
                }
            }
            appendLine("စုစုပေါင်း,,${calc.totalRetained},$date,$sess")
            appendLine("ကော်,,${calc.commission},$date,$sess")
            appendLine("နှုတ်ပြီး,,${calc.retainedNet},$date,$sess")
            appendLine("ပေါက်သီး,${winNum.ifEmpty { "-" }},${calc.payout},$date,$sess")
            appendLine("အမြတ်ငွေ,,${calc.profitLoss},$date,$sess")
        }

        val txtContent = buildString {
            appendLine("တင်ကွက် / ဝယ်ရောင်း မှတ်တမ်းများ")
            appendLine("နေ့စွဲ : $date | အပိုင်း : $sess | ဘရိတ် : $activeBrake")
            if (winNum.isNotEmpty()) {
                appendLine("ထွက်ဂဏန်း : $winNum")
            }
            appendLine("----------------------------------------")
            appendLine("[လက်ခံကွက် (${keptItems.size})]")
            keptItems.forEach { (n, a) -> appendLine("$n = $a") }
            appendLine("လက်ခံကွက် စုစုပေါင်း = ${keptItems.sumOf { it.second }}")
            appendLine("----------------------------------------")
            appendLine("[တင်ကွက် (${overflowItems.size})]")
            overflowItems.forEach { (n, a) -> appendLine("$n = $a") }
            appendLine("တင်ကွက် စုစုပေါင်း = ${overflowItems.sumOf { it.second }}")
            if (sessionLayoffs.isNotEmpty()) {
                appendLine("----------------------------------------")
                appendLine("[တင်ကွက်မှတ်တမ်း (${sessionLayoffs.size} ကြိမ်)]")
                sessionLayoffs.forEachIndexed { idx, rec ->
                    appendLine("${idx + 1}. (${rec.time}) စုစုပေါင်း = ${rec.total}")
                    rec.items.forEach { itm -> appendLine("   ${itm.n}=${itm.a}") }
                }
            }
            appendLine("----------------------------------------")
            appendLine("စုစုပေါင်း = ${calc.totalRetained}")
            appendLine("ကော် = ${calc.commission}")
            appendLine("နှုတ်ပြီး = ${calc.retainedNet}")
            appendLine("ပေါက်သီး = ${calc.payout}")
            val sign = if (calc.profitLoss > 0L) "+" else ""
            appendLine("အမြတ်ငွေ = $sign${calc.profitLoss}")
        }

        val exportDir = context.getExternalFilesDir(null) ?: context.filesDir
        exportDir.mkdirs()
        val csvFile = java.io.File(exportDir, "session_ledger_export.csv")
        val txtFile = java.io.File(exportDir, "session_ledger_export.txt")
        csvFile.writeText(csvContent, Charsets.UTF_8)
        txtFile.writeText(txtContent, Charsets.UTF_8)
        soundManager.playSave()
        showToast("မှတ်တမ်း Export သိမ်းပြီးပါပြီ: ${csvFile.name}")
        if (launchShare) {
            com.example.ui.components.ShareHelper.shareText(context, txtContent, "မှတ်တမ်း Export မျှဝေမည်")
        }
        return csvFile
    }

    fun deleteSettledRecord(recordId: Long) {
        if (recordId in deletedSettledIds) return
        val target = _settled.value.firstOrNull { it.id == recordId } ?: return
        deletedSettledIds.add(recordId)
        deletedRoundKeys.add("${target.date}|${target.session}")
        if (_editingSettledRecord.value?.id == recordId) {
            _editingSettledRecord.value = null
        }
        val updated = _settled.value.filter { it.id != recordId }
        _settled.value = updated
        repository.deleteSettledRecordById(recordId)
        saveDailySettlementForDate(date = target.date, includeUnsavedRoundsWithWin = false)
        soundManager.playDelete()
        showToast("စာရင်းချုပ်မှတ်တမ်း ဖျက်ပြီးပါပြီ")
    }

    // Blocked numbers
    fun addBlockedNumbers(nums: List<String>) {
        val validNums = nums.map { it.trim().toAsciiDigits() }.filter { it.length == 2 && it.all { c -> c.isDigit() } }
        if (validNums.isEmpty()) return
        val set = _blocked.value.toMutableSet()
        set.addAll(validNums)
        _blocked.value = set
        repository.saveBlocked(set)
        soundManager.playSave()
        showToast("မရဂဏန်းများ ထည့်သွင်းပြီးပါပြီ")
    }

    fun editBlockedNumber(oldNum: String, newNum: String) {
        val cleanNew = newNum.trim().toAsciiDigits()
        if (cleanNew.length != 2 || !cleanNew.all { it.isDigit() }) return
        val set = _blocked.value.toMutableSet()
        set.remove(oldNum)
        set.add(cleanNew)
        _blocked.value = set
        repository.saveBlocked(set)
        soundManager.playEdit()
        showToast("မရဂဏန်း ပြင်ဆင်ပြီးပါပြီ")
    }

    fun removeBlockedNumber(num: String) {
        if (!_blocked.value.contains(num)) return
        val set = _blocked.value.toMutableSet()
        set.remove(num)
        _blocked.value = set
        repository.saveBlocked(set)
        soundManager.playDelete()
        showToast("မရဂဏန်း ဖျက်ပြီးပါပြီ")
    }

    // Sessions (Rounds)
    fun addSession(name: String, rate: Double = 0.0, mult: Double = 0.0): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val activeDate = _selectedDate.value.toAsciiDigits()
        if (_sessions.value.any { it.name.equals(trimmed, ignoreCase = true) }) {
            showToast("အကြိမ် အမည် ထပ်နေပါသည်")
            return false
        }
        val wasSessionBlank = _selectedSession.value.isEmpty()
        val newSession = Session(trimmed, rate, mult, date = activeDate)
        val updated = _sessions.value + newSession
        _sessions.value = updated
        repository.saveSessionsForDate(activeDate, updated)
        if (_selectedSession.value.isEmpty() || !_sessions.value.any { it.name == _selectedSession.value }) {
            if (wasSessionBlank) {
                val unstagedAgents = repository.loadAgentsForDateAndRound(activeDate, "")
                if (unstagedAgents.isNotEmpty()) {
                    unstagedAgents.forEach { ag ->
                        repository.insertOrUpdateAgentForSession(
                            activeDate,
                            trimmed,
                            ag.copy(date = activeDate, roundNumber = trimmed)
                        )
                    }
                    repository.clearAgentsForSessionSync(activeDate, "")
                }
            }
            _selectedSession.value = trimmed
            refreshActiveAgentsForCurrentDraw(resetDraftOnSessionChange = false)
        }
        refreshAvailableRounds()
        soundManager.playSave()
        showToast("အကြိမ် အသစ် ထည့်ပြီးပါပြီ")
        return true
    }

    fun updateSessionRecord(targetId: Long?, oldName: String?, updatedSession: Session): Boolean {
        val existing = _sessions.value.firstOrNull { s ->
            (targetId != null && targetId > 0L && s.id == targetId) || (oldName != null && s.name == oldName)
        } ?: return false
        val trimmedName = updatedSession.name.trim()
        if (trimmedName.isEmpty()) return false
        if (_sessions.value.any { it.id != existing.id && it.name.equals(trimmedName, ignoreCase = true) }) {
            showToast("အကြိမ် အမည် ထပ်နေပါသည်")
            return false
        }
        val activeDate = _selectedDate.value.toAsciiDigits()
        val normalized = updatedSession.copy(id = existing.id, name = trimmedName, date = activeDate)
        val updated = _sessions.value.map { if (it.id == existing.id) normalized else it }
        _sessions.value = updated
        repository.updateSessionById(existing.id, normalized) ?: repository.saveSessionsForDate(activeDate, updated)

        if (existing.name != normalized.name) {
            val prevName = existing.name
            val newName = normalized.name
            repository.renameSessionAcrossDatabase(activeDate, prevName, newName)
            _vouchers.value = _vouchers.value.map { if (it.date.toAsciiDigits() == activeDate && it.session == prevName) it.copy(session = newName) else it }
            _wins.value = _wins.value.map { if (it.date.toAsciiDigits() == activeDate && it.session == prevName) it.copy(session = newName) else it }
            _settled.value = _settled.value.map { if (it.date.toAsciiDigits() == activeDate && it.session == prevName) it.copy(session = newName) else it }
            _layoffs.value = _layoffs.value.map { if (it.date.toAsciiDigits() == activeDate && it.session == prevName) it.copy(session = newName) else it }
            _agents.value = _agents.value.map { if (it.date.toAsciiDigits() == activeDate && it.roundNumber == prevName) it.copy(roundNumber = newName) else it }
        }

        if (_selectedSession.value == existing.name) {
            _selectedSession.value = normalized.name
            refreshActiveAgentsForCurrentDraw(resetDraftOnSessionChange = false)
        }
        refreshAvailableRounds()
        soundManager.playEdit()
        showToast("အကြိမ် ပြင်ဆင်ပြီးပါပြီ")
        return true
    }

    fun updateSessionById(sessionId: Long, updatedSession: Session): Boolean {
        return updateSessionRecord(targetId = sessionId, oldName = null, updatedSession = updatedSession)
    }

    fun updateSession(oldName: String, updatedSession: Session): Boolean {
        return updateSessionRecord(targetId = null, oldName = oldName, updatedSession = updatedSession)
    }

    fun deleteSessionRecord(targetId: Long?, name: String?): Boolean {
        val target = _sessions.value.firstOrNull { s ->
            (targetId != null && targetId > 0L && s.id == targetId) || (name != null && s.name == name)
        } ?: return false
        return deleteSessionById(target.id)
    }

    fun deleteSessionById(sessionId: Long): Boolean {
        val target = _sessions.value.firstOrNull { it.id == sessionId } ?: return false
        val activeDate = _selectedDate.value.toAsciiDigits()
        val updated = _sessions.value.filter { it.id != sessionId }
        _sessions.value = updated
        customRoundsByDate.values.forEach { it.remove(target.name) }
        repository.deleteSessionById(sessionId, activeDate) ?: repository.saveSessionsForDate(activeDate, updated)
        if (updated.isEmpty()) {
            _selectedSession.value = ""
        } else if (_selectedSession.value == target.name) {
            _selectedSession.value = updated.first().name
        }
        refreshActiveAgentsForCurrentDraw(resetDraftOnSessionChange = true)
        refreshAvailableRounds()
        soundManager.playDelete()
        showToast("အကြိမ် ဖျက်ပြီးပါပြီ")
        return true
    }

    fun deleteSession(name: String) {
        val target = _sessions.value.firstOrNull { it.name == name } ?: return
        deleteSessionById(target.id)
    }

    // Settings
    fun changePassword(current: String, newPass: String): Boolean {
        if (current.isBlank() || newPass.isBlank()) {
            return false
        }
        if (current != _settings.value.password) {
            showToast("လက်ရှိ စကားဝှက် မှားယွင်းနေပါသည်")
            return false
        }
        if (newPass.length < 4) {
            showToast("စကားဝှက် အနည်းဆုံး ၄ လုံး ရှိရမည်")
            return false
        }
        val updated = _settings.value.copy(password = newPass)
        _settings.value = updated
        repository.saveSettings(updated)
        soundManager.playSave()
        showToast("စကားဝှက် အသစ် ပြောင်းလဲပြီးပါပြီ")
        return true
    }

    fun resetAllData() {
        repository.resetAllData()
        loadAllData()
        _selectedSession.value = ""
        _currentBrakeInput.value = ""
        soundManager.playDelete()
        showToast("အချက်အလက်အားလုံး ပြန်လည်ရှင်းထုတ်ပြီးပါပြီ")
    }
}

typealias SharedLotteryViewModel = MainViewModel
typealias LotteryViewModel = MainViewModel
typealias Lottery2DViewModel = MainViewModel

data class SettledCalculation(
    val totalRetained: Long,
    val commission: Long,
    val retainedNet: Long,
    val multiplier: Double,
    val winningNumber: String,
    val winningRetained: Long,
    val payout: Long,
    val profitLoss: Long
)
