package com.lucky.lucky2d.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lucky.lucky2d.data.LuckyRepository
import com.lucky.lucky2d.logic.PatternAlgorithms
import com.lucky.lucky2d.model.Agent
import com.lucky.lucky2d.model.Agent2DSettlement
import com.lucky.lucky2d.model.AgentSummaryData
import com.lucky.lucky2d.model.AppSettings
import com.lucky.lucky2d.model.BetItem
import com.lucky.lucky2d.model.DailySettlement
import com.lucky.lucky2d.model.LayoffItem
import com.lucky.lucky2d.model.LayoffRecord
import com.lucky.lucky2d.model.NumberAgentBreakdownItem
import com.lucky.lucky2d.model.NumberSummary
import com.lucky.lucky2d.model.Session
import com.lucky.lucky2d.model.SessionSettlementCalc
import com.lucky.lucky2d.model.SettledRecord
import com.lucky.lucky2d.model.Voucher
import com.lucky.lucky2d.model.WinRecord
import com.lucky.lucky2d.utils.CleanAudio2D
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.roundToInt

class LotteryViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repo = LuckyRepository(appContext)
    private val idCounter = AtomicLong(System.currentTimeMillis() * 1000L)

    var selectedDate by mutableStateOf(todayIso())
        private set

    var selectedSession by mutableStateOf("")
        private set

    var sessions by mutableStateOf<List<Session>>(emptyList())
        private set

    var agents by mutableStateOf<List<Agent>>(emptyList())
        private set

    var vouchers by mutableStateOf<List<Voucher>>(emptyList())
        private set

    var layoffs by mutableStateOf<List<LayoffRecord>>(emptyList())
        private set

    var blocked by mutableStateOf<Set<String>>(emptySet())
        private set

    var wins by mutableStateOf<List<WinRecord>>(emptyList())
        private set

    var settled by mutableStateOf<List<SettledRecord>>(emptyList())
        private set

    var dailySettlements by mutableStateOf<List<DailySettlement>>(emptyList())
        private set

    var settings by mutableStateOf(AppSettings())
        private set

    var currentBrakeInput by mutableStateOf("")
        private set

    var entryAgent by mutableStateOf<Agent?>(null)
        private set

    var entryItems by mutableStateOf<List<BetItem>>(emptyList())
        private set

    var voucherFilterAgent by mutableStateOf("ALL")
        private set

    var voucherFilterAgentId by mutableStateOf<Long?>(null)
        private set

    var toastMessage by mutableStateOf<String?>(null)
        private set

    private var toastJob: Job? = null

    init {
        loadAllState()
    }

    private fun loadAllState() {
        val loadedSettings = repo.loadSettings()
        settings = loadedSettings
        CleanAudio2D.isSoundEnabled = loadedSettings.soundEnabled
        currentBrakeInput = if (loadedSettings.brakeLimit > 0) loadedSettings.brakeLimit.toString() else ""

        val loadedSessions = repo.loadSessionsForDate(selectedDate)
        sessions = loadedSessions
        agents = repo.loadAgents()
        vouchers = repo.loadVouchers()
        layoffs = repo.loadLayoffs()
        blocked = repo.loadBlocked()
        wins = repo.loadWins()
        settled = repo.loadSettled()
        dailySettlements = repo.loadDailySettlements()

        val lastSess = repo.getLastActiveSession()
        selectedSession = when {
            lastSess.isNotBlank() && loadedSessions.any { it.name == lastSess } -> lastSess
            loadedSessions.isNotEmpty() -> loadedSessions.first().name
            else -> ""
        }
    }

    fun nextId(): Long = idCounter.incrementAndGet()

    fun showToast(msg: String) {
        toastJob?.cancel()
        toastMessage = msg
        toastJob = viewModelScope.launch {
            delay(2500)
            if (toastMessage == msg) {
                toastMessage = null
            }
        }
    }

    val availableRounds: List<String>
        get() {
            val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
            val configured = sessions.map { it.name.trim() }.filter { it.isNotEmpty() }
            val vRounds = vouchers.filter { PatternAlgorithms.toAsciiDigits(it.date) == cleanDate }.map { it.session }
            val wRounds = wins.filter { PatternAlgorithms.toAsciiDigits(it.date) == cleanDate }.map { it.session }
            val lRounds = layoffs.filter { PatternAlgorithms.toAsciiDigits(it.date) == cleanDate }.map { it.session }
            val sRounds = settled.filter { PatternAlgorithms.toAsciiDigits(it.date) == cleanDate }.map { it.session }
            return (configured + vRounds + wRounds + lRounds + sRounds).distinct()
        }

    val hasRound: Boolean
        get() = selectedSession.isNotBlank() && availableRounds.contains(selectedSession)

    val activeVouchers: List<Voucher>
        get() {
            val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
            return vouchers
                .filter { PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession }
                .sortedWith(compareByDescending<Voucher> { it.no }.thenByDescending { it.id })
        }

    val currentWinningNumber: String
        get() {
            val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
            return wins.firstOrNull {
                PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession
            }?.n ?: ""
        }

    fun setDate(newDate: String) {
        val clean = PatternAlgorithms.toAsciiDigits(newDate)
        selectedDate = clean
        val dateSessions = repo.loadSessionsForDate(clean)
        sessions = dateSessions
        if (dateSessions.isNotEmpty() && (selectedSession.isBlank() || dateSessions.none { it.name == selectedSession })) {
            selectedSession = dateSessions.first().name
        }
    }

    fun setSession(sess: String) {
        val trimmed = sess.trim()
        selectedSession = trimmed
        repo.saveLastActiveSession(trimmed)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(settings)
        settings = updated
        CleanAudio2D.isSoundEnabled = updated.soundEnabled
        repo.saveSettings(updated)
        if (updated.brakeLimit > 0) {
            currentBrakeInput = updated.brakeLimit.toString()
        } else if (updated.brakeLimit == 0 && currentBrakeInput.isNotEmpty()) {
            currentBrakeInput = ""
        }
    }

    fun updateBrakeInput(raw: String) {
        currentBrakeInput = PatternAlgorithms.toAsciiDigits(raw).filter { it.isDigit() }
    }

    fun saveBrakeLimit() {
        val limit = currentBrakeInput.toIntOrNull() ?: 0
        val updated = settings.copy(brakeLimit = limit)
        settings = updated
        repo.saveSettings(updated)
        CleanAudio2D.playClick(appContext)
        showToast("ဘရိတ် သတ်မှတ်ချက် သိမ်းဆည်းပြီးပါပြီ")
    }

    fun setWinningNumber(num: String) {
        val cleanNum = PatternAlgorithms.toAsciiDigits(num).trim().padStart(2, '0').takeLast(2)
        val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
        if (cleanNum.length != 2 || !cleanNum.all { it.isDigit() }) return
        if (!hasRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }

        val newRecord = WinRecord(date = cleanDate, session = selectedSession, n = cleanNum)
        val filtered = wins.filterNot {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession
        }
        val updated = filtered + newRecord
        wins = updated
        repo.saveWins(updated)
        CleanAudio2D.playSuccess(appContext)
        showToast("ထွက်ဂဏန်း ($cleanNum) သိမ်းဆည်းပြီးပါပြီ")
    }

    fun deleteWinRecord(record: WinRecord) {
        val updated = wins.filterNot {
            PatternAlgorithms.toAsciiDigits(it.date) == PatternAlgorithms.toAsciiDigits(record.date) &&
                it.session == record.session
        }
        wins = updated
        repo.saveWins(updated)
        CleanAudio2D.playClick(appContext)
        showToast("ဖျက်ပြီးပါပြီ")
    }

    // --- Agent CRUD ---

    fun addAgent(name: String, rate: Double, mult: Int, phoneNumber: String = "") {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val nextAgentId = (agents.maxOfOrNull { it.id } ?: 0L) + 1L
        val newAgent = Agent(
            id = nextAgentId,
            name = cleanName,
            rate = rate,
            mult = mult,
            date = PatternAlgorithms.toAsciiDigits(selectedDate),
            roundNumber = selectedSession,
            phoneNumber = phoneNumber.trim()
        )
        val updated = agents + newAgent
        agents = updated
        repo.saveAgents(updated)
        CleanAudio2D.playClick(appContext)
        showToast("ကော်မရှင် အသစ် ထည့်သွင်းပြီးပါပြီ")
    }

    fun updateAgent(agent: Agent) {
        val normalized = agent.copy(
            name = agent.name.trim(),
            phoneNumber = agent.phoneNumber.trim()
        )
        val updated = agents.map { if (it.id == normalized.id) normalized else it }
        agents = updated
        repo.saveAgents(updated)
        if (entryAgent?.id == normalized.id) {
            entryAgent = normalized
        }
        CleanAudio2D.playClick(appContext)
        showToast("ပြင်ဆင်ပြီးပါပြီ")
    }

    fun deleteAgent(agentId: Long) {
        val target = agents.firstOrNull { it.id == agentId } ?: return
        val updatedAgents = agents.filterNot { it.id == agentId }
        agents = updatedAgents
        repo.saveAgents(updatedAgents)

        if (entryAgent?.id == agentId) {
            entryAgent = null
        }

        val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
        val updatedVouchers = vouchers.filterNot { v ->
            PatternAlgorithms.toAsciiDigits(v.date) == cleanDate &&
                v.session == selectedSession &&
                (v.agentId == agentId || v.agent == target.name)
        }
        vouchers = updatedVouchers
        repo.saveVouchers(updatedVouchers)
        CleanAudio2D.playClick(appContext)
        showToast("ဖျက်ပြီးပါပြီ")
    }

    // --- Number Entry Draft ---

    fun startNumberEntry(agent: Agent) {
        entryAgent = agent
        entryItems = emptyList()
    }

    fun selectEntryAgent(agent: Agent) {
        entryAgent = agent
    }

    fun addEntryItems(items: List<BetItem>): List<Pair<String, Int>> {
        if (!hasRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return emptyList()
        }
        if (entryAgent == null) {
            showToast("ထိုးသူ ရွေးချယ်ပါ")
            return emptyList()
        }
        val valid = items.filter { !blocked.contains(it.n) && it.a > 0 }
        if (valid.isEmpty()) return emptyList()

        val updatedDraft = entryItems + valid
        entryItems = updatedDraft
        CleanAudio2D.playClick(appContext)

        // Check if brakeLimit > 0 and any newly added number now exceeds brake limit
        val limit = settings.brakeLimit
        if (limit <= 0) return emptyList()

        val existingMap = getAll100Numbers().associate { it.n to it.netAmount }.toMutableMap()
        for (item in updatedDraft) {
            existingMap[item.n] = (existingMap[item.n] ?: 0) + item.a
        }
        val touchedNumbers = valid.map { it.n }.distinct()
        return touchedNumbers
            .map { n -> n to (existingMap[n] ?: 0) }
            .filter { it.second > limit }
    }

    fun editEntryItem(index: Int, newNum: String, newAmt: Int): Boolean {
        if (index !in entryItems.indices) return false
        val cleanNum = PatternAlgorithms.toAsciiDigits(newNum).padStart(2, '0').takeLast(2)
        if (blocked.contains(cleanNum)) {
            showToast("$cleanNum သည် မရဂဏန်း ဖြစ်ပါသည်")
            return false
        }
        val mutable = entryItems.toMutableList()
        mutable[index] = mutable[index].copy(n = cleanNum, a = newAmt)
        entryItems = mutable
        CleanAudio2D.playClick(appContext)
        return true
    }

    fun deleteEntryItem(index: Int) {
        if (index !in entryItems.indices) return
        val mutable = entryItems.toMutableList()
        mutable.removeAt(index)
        entryItems = mutable
        CleanAudio2D.playClick(appContext)
    }

    fun clearEntryItems() {
        entryItems = emptyList()
    }

    fun commitCurrentVoucher(): Voucher? {
        if (!hasRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return null
        }
        val agent = entryAgent
        if (agent == null) {
            showToast("ထိုးသူ ရွေးချယ်ပါ")
            return null
        }
        if (entryItems.isEmpty()) {
            showToast("ထိုးမည့် ဂဏန်းများ ထည့်သွင်းပါ")
            return null
        }

        val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
        val sessionVouchers = vouchers.filter {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession
        }
        val nextNo = (sessionVouchers.maxOfOrNull { it.no } ?: 0) + 1

        val newVoucher = Voucher(
            id = System.currentTimeMillis(),
            no = nextNo,
            agent = agent.name,
            agentId = agent.id,
            date = cleanDate,
            session = selectedSession,
            time = nowFormattedTime(),
            items = entryItems.map { it.copy(n = PatternAlgorithms.toAsciiDigits(it.n)) }
        )

        val updated = vouchers + newVoucher
        vouchers = updated
        repo.saveVouchers(updated)
        entryItems = emptyList()
        CleanAudio2D.playSuccess(appContext)
        showToast("ဘောင်ချာ (No.$nextNo) သိမ်းဆည်းပြီးပါပြီ")
        return newVoucher
    }

    // --- Vouchers Management ---

    fun openAgentVouchersFilter(agentId: Long?, agentName: String?) {
        voucherFilterAgentId = agentId
        voucherFilterAgent = agentName ?: "ALL"
    }

    fun clearVoucherFilter() {
        voucherFilterAgent = "ALL"
        voucherFilterAgentId = null
    }

    fun deleteVoucher(voucherId: Long) {
        val updated = vouchers.filterNot { it.id == voucherId }
        vouchers = updated
        repo.saveVouchers(updated)
        CleanAudio2D.playClick(appContext)
        showToast("ဘောင်ချာ ဖျက်ပြီးပါပြီ")
    }

    fun editVoucherNumber(itemId: Long, newNum: String, newAmt: Int) {
        val cleanNum = PatternAlgorithms.toAsciiDigits(newNum).padStart(2, '0').takeLast(2)
        var changed = false
        val updated = vouchers.map { v ->
            if (v.items.any { it.id == itemId }) {
                changed = true
                v.copy(items = v.items.map { if (it.id == itemId) it.copy(n = cleanNum, a = newAmt) else it })
            } else {
                v
            }
        }
        if (changed) {
            vouchers = updated
            repo.saveVouchers(updated)
            CleanAudio2D.playClick(appContext)
            showToast("ဂဏန်း ပြင်ဆင်ပြီးပါပြီ")
        }
    }

    fun deleteVoucherNumber(itemId: Long) {
        var changed = false
        val updated = vouchers.mapNotNull { v ->
            if (v.items.any { it.id == itemId }) {
                changed = true
                val remaining = v.items.filterNot { it.id == itemId }
                if (remaining.isEmpty()) null else v.copy(items = remaining)
            } else {
                v
            }
        }
        if (changed) {
            vouchers = updated
            repo.saveVouchers(updated)
            CleanAudio2D.playClick(appContext)
            showToast("ဂဏန်း ဖျက်ပြီးပါပြီ")
        }
    }

    // --- Blocked Numbers ---

    fun toggleBlockedNumber(num: String) {
        val clean = PatternAlgorithms.toAsciiDigits(num).trim().padStart(2, '0').takeLast(2)
        if (clean.length != 2 || !clean.all { it.isDigit() }) return
        val next = blocked.toMutableSet()
        if (next.contains(clean)) next.remove(clean) else next.add(clean)
        blocked = next
        repo.saveBlocked(next)
    }

    // --- Sessions Management ---

    fun addSession(name: String, rate: Double, mult: Int): Boolean {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return false
        val newSess = Session(
            id = nextId(),
            name = cleanName,
            rate = rate,
            mult = mult,
            date = PatternAlgorithms.toAsciiDigits(selectedDate)
        )
        val updated = sessions + newSess
        sessions = updated
        repo.saveSessionsForDate(selectedDate, updated)
        if (selectedSession.isBlank()) {
            setSession(cleanName)
        }
        CleanAudio2D.playClick(appContext)
        showToast("အကြိမ်ရေ ($cleanName) အသစ်ထည့်ပြီးပါပြီ")
        return true
    }

    fun deleteSessionRecord(targetId: Long, name: String) {
        val updated = sessions.filterNot { it.id == targetId || it.name == name }
        sessions = updated
        repo.saveSessionsForDate(selectedDate, updated)
        if (selectedSession == name) {
            selectedSession = updated.firstOrNull()?.name ?: ""
        }
        CleanAudio2D.playClick(appContext)
        showToast("ဖျက်ပြီးပါပြီ")
    }

    // --- Layoff / Brake Management ---

    fun executeLayoff(isAll: Boolean) {
        val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
        val limit = currentBrakeInput.toIntOrNull() ?: settings.brakeLimit
        if (limit <= 0) {
            showToast("ဘရိတ်ပမာဏ အရင်သတ်မှတ်ပါ")
            return
        }

        val full = getAll100Numbers()
        val overflows = full
            .filter { it.netAmount > limit }
            .map { LayoffItem(n = it.n, a = it.netAmount - limit) }
            .sortedWith(compareByDescending<LayoffItem> { it.a }.thenBy { it.n })

        if (overflows.isEmpty()) {
            showToast("ဘရိတ်ကျော် တင်ကွက် မရှိပါ")
            return
        }

        val itemsToLayoff = if (isAll) overflows else overflows.take(30)
        val sessionLayoffs = layoffs.filter {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession
        }
        val nextNo = (sessionLayoffs.maxOfOrNull { it.recordNo } ?: 0) + 1
        val total = itemsToLayoff.sumOf { it.a }

        val newRecord = LayoffRecord(
            id = System.currentTimeMillis(),
            date = cleanDate,
            session = selectedSession,
            time = nowFormattedTime(),
            limit = limit,
            items = itemsToLayoff,
            total = total,
            recordNo = nextNo
        )

        val updated = layoffs + newRecord
        layoffs = updated
        repo.saveLayoffs(updated)
        CleanAudio2D.playSuccess(appContext)
        showToast("တင်ကွက် No.$nextNo (${itemsToLayoff.size} ကွက်) တင်ပြီးပါပြီ")
    }

    fun deleteLayoffRecord(id: Long) {
        val updated = layoffs.filterNot { it.id == id }
        layoffs = updated
        repo.saveLayoffs(updated)
        CleanAudio2D.playClick(appContext)
        showToast("တင်ကွက် မှတ်တမ်း ဖျက်ပြီးပါပြီ")
    }

    // --- Settlement Calculations ---

    fun calculateSettlementForSession(
        dateStr: String = selectedDate,
        sessionName: String = selectedSession,
        winningNum: String = currentWinningNumber
    ): SessionSettlementCalc {
        val cleanDate = PatternAlgorithms.toAsciiDigits(dateStr)
        val sessionVouchers = vouchers.filter {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == sessionName
        }
        val sessionLayoffs = layoffs.filter {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == sessionName
        }

        val totalSales = sessionVouchers.sumOf { v -> v.items.sumOf { it.a.toLong() } }
        val totalPassed = sessionLayoffs.sumOf { it.total.toLong() }
        val totalRetained = totalSales - totalPassed

        // Calculate commission & payout grouped by agent
        var totalCommission = 0L
        var payout = 0L
        val validWin = winningNum.length == 2 && winningNum.all { it.isDigit() }

        for (ag in agents) {
            val agVouchers = sessionVouchers.filter { it.agentId == ag.id || it.agent == ag.name }
            if (agVouchers.isEmpty()) continue
            val agTotal = agVouchers.sumOf { v -> v.items.sumOf { it.a.toLong() } }
            totalCommission += (agTotal * (ag.rate / 100.0)).roundToInt().toLong()

            if (validWin) {
                var agWinBet = 0L
                for (v in agVouchers) {
                    for (item in v.items) {
                        if (item.n == winningNum) agWinBet += item.a.toLong()
                    }
                }
                payout += agWinBet * ag.mult.toLong()
            }
        }

        val retainedNet = totalRetained - totalCommission
        val profitLoss = retainedNet - payout

        return SessionSettlementCalc(
            totalRetained = totalRetained,
            commission = totalCommission,
            retainedNet = retainedNet,
            payout = payout,
            profitLoss = profitLoss
        )
    }

    fun lockAndSaveBrakeSettlement() {
        if (!hasRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
        val calc = calculateSettlementForSession(cleanDate, selectedSession, currentWinningNumber)

        val newRecord = SettledRecord(
            id = System.currentTimeMillis(),
            date = cleanDate,
            session = selectedSession,
            winningNumber = currentWinningNumber.ifBlank { "-" },
            totalRetained = calc.totalRetained,
            commission = calc.commission,
            retainedNet = calc.retainedNet,
            payout = calc.payout,
            profitLoss = calc.profitLoss,
            timestamp = nowFormattedTime()
        )

        val filtered = settled.filterNot {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession
        }
        val updated = listOf(newRecord) + filtered
        settled = updated
        dailySettlements = repo.saveSettled(updated)
        CleanAudio2D.playSuccess(appContext)
        showToast("ရက်ချုပ် စာရင်း ($selectedSession) သိမ်းဆည်းပြီးပါပြီ")
    }

    fun deleteSettledRecord(recordId: Long) {
        val updated = settled.filterNot { it.id == recordId }
        settled = updated
        dailySettlements = repo.saveSettled(updated)
        CleanAudio2D.playClick(appContext)
        showToast("ဖျက်ပြီးပါပြီ")
    }

    fun clearSettledLedger() {
        settled = emptyList()
        dailySettlements = repo.saveSettled(emptyList())
        CleanAudio2D.playClick(appContext)
        showToast("ရက်ချုပ်အားလုံး ရှင်းပြီးပါပြီ")
    }

    fun resetAllData() {
        repo.resetAllData()
        agents = emptyList()
        vouchers = emptyList()
        layoffs = emptyList()
        blocked = emptySet()
        wins = emptyList()
        settled = emptyList()
        dailySettlements = emptyList()
        entryAgent = null
        entryItems = emptyList()
        currentBrakeInput = ""
        val defSessions = repo.loadSessionsForDate(selectedDate)
        sessions = defSessions
        selectedSession = defSessions.firstOrNull()?.name ?: ""
        CleanAudio2D.playClick(appContext)
        showToast("အချက်အလက်အားလုံး အသစ်ပြန်စပြီးပါပြီ")
    }

    // --- Aggregation Helpers ---

    fun getAll100Numbers(): List<NumberSummary> {
        val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
        val sessionVouchers = vouchers.filter {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession
        }
        val sessionLayoffs = layoffs.filter {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession
        }

        val salesMap = IntArray(100)
        val countMap = IntArray(100)
        val layoffMap = IntArray(100)

        for (v in sessionVouchers) {
            for (item in v.items) {
                val idx = PatternAlgorithms.toAsciiDigits(item.n).toIntOrNull()
                if (idx != null && idx in 0..99) {
                    salesMap[idx] += item.a
                    countMap[idx] += 1
                }
            }
        }

        for (l in sessionLayoffs) {
            for (item in l.items) {
                val idx = PatternAlgorithms.toAsciiDigits(item.n).toIntOrNull()
                if (idx != null && idx in 0..99) {
                    layoffMap[idx] += item.a
                }
            }
        }

        return (0..99).map { i ->
            val num = i.toString().padStart(2, '0')
            val totalBet = salesMap[i]
            val layoffAmt = layoffMap[i]
            NumberSummary(
                n = num,
                entryCount = countMap[i],
                totalBet = totalBet,
                layoffAmount = layoffAmt,
                netAmount = totalBet - layoffAmt
            )
        }
    }

    fun sort2DNumbersWithWinning(all: List<NumberSummary>, winningNum: String?): List<NumberSummary> {
        val cleanWin = winningNum?.trim()
        val hasWin = !cleanWin.isNullOrEmpty() && cleanWin.length == 2 && cleanWin.all { it.isDigit() }

        val winItem = if (hasWin) {
            all.firstOrNull { it.n == cleanWin } ?: NumberSummary(cleanWin!!, 0, 0, 0, 0)
        } else null

        val pool = if (hasWin) all.filterNot { it.n == cleanWin } else all
        val active = pool.filter { it.totalBet > 0 }
            .sortedWith(compareByDescending<NumberSummary> { it.totalBet }.thenBy { it.n })
        val zero = pool.filter { it.totalBet <= 0 }
            .sortedBy { it.n }

        return if (winItem != null) listOf(winItem) + active + zero else active + zero
    }

    fun getAgentSummaryData(agent: Agent, winNum: String = currentWinningNumber): AgentSummaryData {
        val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
        val agVouchers = vouchers.filter { v ->
            PatternAlgorithms.toAsciiDigits(v.date) == cleanDate &&
                v.session == selectedSession &&
                (v.agentId == agent.id || v.agent == agent.name)
        }

        val totalBet = agVouchers.sumOf { v -> v.items.sumOf { it.a } }
        val commission = (totalBet * (agent.rate / 100.0)).roundToInt()
        val netAmount = totalBet - commission

        var payout = 0L
        if (winNum.length == 2 && winNum.all { it.isDigit() }) {
            var winBet = 0L
            for (v in agVouchers) {
                for (item in v.items) {
                    if (item.n == winNum) winBet += item.a.toLong()
                }
            }
            payout = winBet * agent.mult.toLong()
        }

        return AgentSummaryData(
            totalBet = totalBet,
            commission = commission,
            netAmount = netAmount,
            voucherCount = agVouchers.size,
            payout = payout
        )
    }

    fun getAgent2DSettlements(winNum: String): List<Agent2DSettlement> {
        val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
        val sessionVouchers = vouchers.filter {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession
        }

        return agents.mapNotNull { ag ->
            val agVouchers = sessionVouchers.filter { it.agentId == ag.id || it.agent == ag.name }
            if (agVouchers.isEmpty()) return@mapNotNull null

            val totalSales = agVouchers.sumOf { v -> v.items.sumOf { it.a } }
            val commissionAmount = (totalSales * (ag.rate / 100.0)).roundToInt()
            val netSales = totalSales - commissionAmount

            var winningBetAmount = 0
            if (winNum.length == 2 && winNum.all { it.isDigit() }) {
                for (v in agVouchers) {
                    for (item in v.items) {
                        if (item.n == winNum) winningBetAmount += item.a
                    }
                }
            }
            val payoutAmount = winningBetAmount.toLong() * ag.mult.toLong()
            val netBalance = netSales.toLong() - payoutAmount

            Agent2DSettlement(
                agentId = ag.id,
                agentName = ag.name,
                commissionRate = ag.rate,
                multiplier = ag.mult,
                totalSales = totalSales,
                commissionAmount = commissionAmount,
                netSales = netSales,
                winningBetAmount = winningBetAmount,
                payoutAmount = payoutAmount,
                netBalance = netBalance
            )
        }
    }

    fun getNumberAgentBreakdown(targetNum: String): List<NumberAgentBreakdownItem> {
        val cleanDate = PatternAlgorithms.toAsciiDigits(selectedDate)
        val sessionVouchers = vouchers.filter {
            PatternAlgorithms.toAsciiDigits(it.date) == cleanDate && it.session == selectedSession
        }

        return agents.mapNotNull { ag ->
            val agVouchers = sessionVouchers.filter { it.agentId == ag.id || it.agent == ag.name }
            val totalAgentSales = agVouchers.sumOf { v -> v.items.sumOf { it.a } }
            var betOnNumber = 0
            var entryCount = 0
            for (v in agVouchers) {
                for (item in v.items) {
                    if (item.n == targetNum) {
                        betOnNumber += item.a
                        entryCount++
                    }
                }
            }
            if (betOnNumber <= 0 && totalAgentSales <= 0) return@mapNotNull null

            val commissionOnNumber = (betOnNumber * (ag.rate / 100.0)).roundToInt()
            val totalSalesCommission = (totalAgentSales * (ag.rate / 100.0)).roundToInt()
            val winningPayout = betOnNumber.toLong() * ag.mult.toLong()

            NumberAgentBreakdownItem(
                agentId = ag.id,
                agentName = ag.name,
                rate = ag.rate,
                mult = ag.mult,
                entryCount = entryCount,
                betOnNumber = betOnNumber,
                totalAgentSales = totalAgentSales,
                commissionOnNumber = commissionOnNumber,
                totalSalesCommission = totalSalesCommission,
                winningPayout = winningPayout
            )
        }.filter { it.betOnNumber > 0 }
    }

    fun buildDailySettlementsTextSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("----------------------------")
        sb.appendLine("Lucky2D - ရက်ချုပ်အားလုံး မှတ်တမ်း")
        sb.appendLine("----------------------------")
        settled.forEachIndexed { idx, r ->
            val sign = when {
                r.profitLoss > 0 -> "+"
                r.profitLoss < 0 -> "-"
                else -> ""
            }
            sb.appendLine("${idx + 1}. ${r.date} (${r.session}) | ပေါက်သီး: ${r.winningNumber} | အမြတ်ငွေ: $sign${abs(r.profitLoss)}")
        }
        sb.appendLine("----------------------------")
        val totalPL = settled.sumOf { it.profitLoss }
        val sign = when {
            totalPL > 0 -> "+"
            totalPL < 0 -> "-"
            else -> ""
        }
        sb.appendLine("စုစုပေါင်း အမြတ်ငွေ : $sign${abs(totalPL)}")
        sb.append("----------------------------")
        return sb.toString()
    }

    companion object {
        fun todayIso(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        fun nowFormattedTime(): String {
            return SimpleDateFormat("yyyy.MM.dd/HH:mm:ss", Locale.US).format(Date())
        }

        fun formatDotDate(dateStr: String): String {
            val parts = dateStr.split("-")
            return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else dateStr
        }

        fun formatRate(rate: Double): String {
            return if (rate == rate.toLong().toDouble()) rate.toLong().toString() else rate.toString()
        }
    }
}
