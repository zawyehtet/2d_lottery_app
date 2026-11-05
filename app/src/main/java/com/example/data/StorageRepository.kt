package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class StorageRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lucky2d_storage", Context.MODE_PRIVATE)
    val database: AppDatabase = AppDatabase.getInstance(context)
    val agentDao: AgentDao = database.agentDao()
    val voucherDao: VoucherDao = database.voucherDao()
    val winningNumberDao: WinningNumberDao = database.winningNumberDao()
    val settlementDao: SettlementDao = database.settlementDao()
    val dailySettlementDao: DailySettlementDao = database.dailySettlementDao()
    val sessionDao: SessionDao = database.sessionDao()
    val session2DDao: Session2DDao = database.session2DDao()
    val passBetRecordDao: PassBetRecordDao = database.passBetRecordDao()

    companion object {
        private const val KEY_AGENTS = "lucky2d_agents"
        private const val KEY_SESSIONS = "lucky2d_sessions"
        private const val KEY_SESSIONS_MIGRATED_V1 = "lucky2d_sessions_migrated_v1"
        private const val KEY_SETTINGS_MIGRATED_V1 = "lucky2d_settings_migrated_v1"
        private const val KEY_VOUCHERS = "lucky2d_vouchers"
        private const val KEY_LAYOFFS = "lucky2d_layoffs"
        private const val KEY_BLOCKED = "lucky2d_blocked"
        private const val KEY_WINS = "lucky2d_wins"
        private const val KEY_SETTLED = "lucky2d_settled"
        private const val KEY_DAILY_SETTLED = "lucky2d_daily_settled"
        private const val KEY_SETTINGS = "lucky2d_settings"

        private val DEFAULT_ROUND_NAMES_12AM = setOf("12.AM", "12AM")
        private val DEFAULT_ROUND_NAMES_4PM = setOf("4.PM", "4PM")
    }

    init {
        migrateLegacyDefaultsIfNeeded()
    }

    private fun isUntouchedDefaultSessions(list: List<Session>): Boolean {
        if (list.size != 2) return false
        val first = list[0]
        val second = list[1]
        return first.name in DEFAULT_ROUND_NAMES_12AM &&
            first.rate == 15.0 &&
            first.mult == 80.0 &&
            second.name in DEFAULT_ROUND_NAMES_4PM &&
            second.rate == 15.0 &&
            second.mult == 80.0
    }

    private fun parseSessionsJson(jsonStr: String): List<Session> {
        val list = mutableListOf<Session>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Session(
                        name = obj.getString("name"),
                        rate = obj.optDouble("rate", 0.0),
                        mult = obj.optDouble("mult", 0.0),
                        id = obj.optLong("id", System.currentTimeMillis() + i),
                        date = obj.optString("date", ""),
                        winningNumber = obj.optString("winningNumber", "").ifEmpty { null },
                        isClosed = obj.optBoolean("isClosed", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun migrateLegacyDefaultsIfNeeded() {
        try {
            if (!prefs.getBoolean(KEY_SESSIONS_MIGRATED_V1, false)) {
                val legacySessionsJson = prefs.getString(KEY_SESSIONS, null)
                if (!legacySessionsJson.isNullOrEmpty()) {
                    val parsed = parseSessionsJson(legacySessionsJson)
                    if (isUntouchedDefaultSessions(parsed)) {
                        prefs.edit()
                            .remove(KEY_SESSIONS)
                            .putBoolean(KEY_SESSIONS_MIGRATED_V1, true)
                            .commit()
                        sessionDao.clearAllSessionsSync()
                        session2DDao.clearAllSessionsSync()
                    } else {
                        prefs.edit().putBoolean(KEY_SESSIONS_MIGRATED_V1, true).commit()
                    }
                } else {
                    prefs.edit().putBoolean(KEY_SESSIONS_MIGRATED_V1, true).commit()
                }
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error migrating legacy sessions", e)
        }
    }

    // Sessions (Rounds) - strictly composite-bound by Date
    fun loadSessionsForDate(date: String): List<Session> {
        migrateLegacyDefaultsIfNeeded()
        val asciiDate = date.toAsciiDigits()
        return try {
            val roomSessions = session2DDao.getSessionsByDateSync(asciiDate).map { it.toDomainSession() }
            if (roomSessions.isNotEmpty()) {
                return roomSessions.sortedBy { it.id }
            }
            // Fallback: check prefs for this specific date
            val dateKey = "${KEY_SESSIONS}_$asciiDate"
            val jsonStr = prefs.getString(dateKey, null)
            if (!jsonStr.isNullOrEmpty()) {
                val list = parseSessionsJson(jsonStr)
                if (list.isNotEmpty()) {
                    session2DDao.insertAllSessionsSync(list.map { Session2DEntity.fromDomainSession(it, asciiDate) })
                    return list.sortedBy { it.id }
                }
            }
            emptyList()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading sessions for date $asciiDate", e)
            emptyList()
        }
    }

    fun loadSessions(): List<Session> {
        migrateLegacyDefaultsIfNeeded()
        return try {
            val all2D = session2DDao.getAllSessionsSync()
            if (all2D.isNotEmpty()) {
                return all2D.map { it.toDomainSession() }.sortedBy { it.id }
            }
            val roomSessions = sessionDao.getAllSessionsSync()
            if (roomSessions.isNotEmpty()) {
                val mapped = roomSessions.map { it.toDomainSession() }.sortedBy { it.id }
                if (isUntouchedDefaultSessions(mapped)) {
                    sessionDao.clearAllSessionsSync()
                    emptyList()
                } else {
                    mapped
                }
            } else {
                val legacyJson = prefs.getString(KEY_SESSIONS, null)
                if (!legacyJson.isNullOrEmpty()) {
                    val parsed = parseSessionsJson(legacyJson)
                    if (isUntouchedDefaultSessions(parsed)) {
                        emptyList()
                    } else {
                        parsed
                    }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading all sessions", e)
            emptyList()
        }
    }

    fun saveSessionsForDate(date: String, sessions: List<Session>) {
        val asciiDate = date.toAsciiDigits()
        try {
            val dateKey = "${KEY_SESSIONS}_$asciiDate"
            val arr = JSONArray()
            sessions.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("rate", s.rate)
                    put("mult", s.mult)
                    put("date", asciiDate)
                    put("winningNumber", s.winningNumber ?: "")
                    put("isClosed", s.isClosed)
                }
                arr.put(obj)
            }
            prefs.edit().putString(dateKey, arr.toString()).apply()

            val existingInRoom = session2DDao.getSessionsByDateSync(asciiDate)
            existingInRoom.forEach { existing ->
                if (sessions.none { it.id == existing.id }) {
                    session2DDao.deleteSessionByIdSync(existing.id)
                }
            }
            sessions.forEach { s ->
                val entity = Session2DEntity.fromDomainSession(s, asciiDate)
                session2DDao.insertSessionSync(entity)
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving sessions for date $asciiDate", e)
        }
    }

    fun saveSessions(sessions: List<Session>) {
        try {
            val arr = JSONArray()
            sessions.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("rate", s.rate)
                    put("mult", s.mult)
                    put("date", s.date)
                    put("winningNumber", s.winningNumber ?: "")
                    put("isClosed", s.isClosed)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
            sessions.groupBy { it.date.ifEmpty { "GLOBAL" } }.forEach { (d, list) ->
                if (d != "GLOBAL") {
                    saveSessionsForDate(d, list)
                }
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving sessions", e)
        }
    }

    fun updateSessionById(sessionId: Long, updatedSession: Session): List<Session>? {
        val targetEntity = session2DDao.getSessionByIdSync(sessionId)
        val date = (targetEntity?.date ?: updatedSession.date).toAsciiDigits()
        val current = if (date.isNotEmpty()) loadSessionsForDate(date) else loadSessions()
        val target = current.firstOrNull { it.id == sessionId } ?: return null
        val oldName = target.name
        val newName = updatedSession.name
        val normalized = updatedSession.copy(id = sessionId, date = date)

        session2DDao.updateSessionByIdSync(sessionId, newName, normalized.rate, normalized.mult)
        val updated = current.map { s ->
            if (s.id == sessionId) normalized else s
        }
        if (date.isNotEmpty()) {
            saveSessionsForDate(date, updated)
        } else {
            saveSessions(updated)
        }
        if (oldName.isNotBlank() && newName.isNotBlank() && oldName != newName) {
            renameSessionAcrossDatabase(date, oldName, newName)
        }
        return updated
    }

    fun renameSessionAcrossDatabase(date: String, oldName: String, newName: String) {
        if (oldName.isBlank() || newName.isBlank() || oldName == newName) return
        val asciiDate = date.toAsciiDigits()
        try {
            if (asciiDate.isNotEmpty()) {
                voucherDao.renameRoundForDateSync(asciiDate, oldName, newName)
                winningNumberDao.renameRoundForDateSync(asciiDate, oldName, newName)
                settlementDao.renameRoundForDateSync(asciiDate, oldName, newName)
                agentDao.renameRoundForDateSync(asciiDate, oldName, newName)
                passBetRecordDao.renameRoundForDateSync(asciiDate, oldName, newName)

                val currentVouchers = loadVouchers()
                val updatedVouchers = currentVouchers.map {
                    if (it.date.toAsciiDigits() == asciiDate && it.session == oldName) it.copy(session = newName) else it
                }
                saveVouchers(updatedVouchers)

                val currentWins = loadWins()
                val updatedWins = currentWins.map {
                    if (it.date.toAsciiDigits() == asciiDate && it.session == oldName) it.copy(session = newName) else it
                }
                saveWins(updatedWins)

                val currentSettled = loadSettled()
                val updatedSettled = currentSettled.map {
                    if (it.date.toAsciiDigits() == asciiDate && it.session == oldName) it.copy(session = newName) else it
                }
                saveSettled(updatedSettled)

                val currentLayoffs = loadLayoffs()
                val updatedLayoffs = currentLayoffs.map {
                    if (it.date.toAsciiDigits() == asciiDate && it.session == oldName) it.copy(session = newName) else it
                }
                saveLayoffs(updatedLayoffs)
            } else {
                voucherDao.renameRoundSync(oldName, newName)
                winningNumberDao.renameRoundSync(oldName, newName)
                settlementDao.renameRoundSync(oldName, newName)
                agentDao.renameRoundSync(oldName, newName)
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error renaming session across database", e)
        }
    }

    fun deleteSessionById(sessionId: Long, date: String = ""): List<Session>? {
        val asciiDate = date.toAsciiDigits()
        session2DDao.deleteSessionByIdSync(sessionId)
        sessionDao.clearAllSessionsSync()
        val current = if (asciiDate.isNotEmpty()) loadSessionsForDate(asciiDate) else loadSessions()
        val updated = current.filter { it.id != sessionId }
        if (asciiDate.isNotEmpty()) {
            saveSessionsForDate(asciiDate, updated)
        } else {
            saveSessions(updated)
        }
        return updated
    }

    // Agents (Draw-Scoped to [date, roundNumber, agentId])
    fun getAgentsByDateAndRound(date: String, round: String): kotlinx.coroutines.flow.Flow<List<AgentEntity>> =
        agentDao.getAgentsByDateAndRound(date.toAsciiDigits(), round)

    fun loadAgentsForDateAndRound(date: String, round: String): List<Agent> {
        val asciiDate = date.toAsciiDigits()
        return try {
            agentDao.getAgentsByDateAndRoundSync(asciiDate, round).map { it.toDomainAgent() }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading agents for $asciiDate / $round", e)
            emptyList()
        }
    }

    fun insertOrUpdateAgentForSession(date: String, round: String, agent: Agent): List<Agent> {
        val asciiDate = date.toAsciiDigits()
        return try {
            val entity = AgentEntity.fromDomainAgent(agent, date = asciiDate, round = round)
            agentDao.insertOrUpdateAgentSync(entity)
            syncAllAgentsToPrefs()
            loadAgentsForDateAndRound(asciiDate, round)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error inserting/updating agent", e)
            emptyList()
        }
    }

    suspend fun insertOrUpdateAgent(agent: AgentEntity) {
        agentDao.insertOrUpdateAgent(agent.copy(date = agent.date.toAsciiDigits()))
        syncAllAgentsToPrefs()
    }

    fun deleteAgentBySessionSync(date: String, round: String, agentId: Int): List<Agent> {
        val asciiDate = date.toAsciiDigits()
        return try {
            agentDao.deleteAgentBySessionSync(asciiDate, round, agentId)
            syncAllAgentsToPrefs()
            loadAgentsForDateAndRound(asciiDate, round)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error deleting agent by session", e)
            emptyList()
        }
    }

    suspend fun deleteAgentBySession(date: String, round: String, agentId: Int) {
        agentDao.deleteAgentBySession(date.toAsciiDigits(), round, agentId)
        syncAllAgentsToPrefs()
    }

    suspend fun clearAgentsForSession(date: String, round: String) {
        agentDao.clearAgentsForSession(date.toAsciiDigits(), round)
        syncAllAgentsToPrefs()
    }

    fun clearAgentsForSessionSync(date: String, round: String) {
        agentDao.clearAgentsForSessionSync(date.toAsciiDigits(), round)
        syncAllAgentsToPrefs()
    }

    private fun syncAllAgentsToPrefs() {
        try {
            val allEntities = agentDao.getAllAgentsSync()
            val arr = JSONArray()
            allEntities.forEach { entity ->
                val obj = JSONObject().apply {
                    put("id", entity.agentId)
                    put("name", entity.name)
                    put("rate", entity.commissionRate)
                    put("mult", entity.multiplier)
                    put("date", entity.date)
                    put("roundNumber", entity.roundNumber)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_AGENTS, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error syncing agents to prefs", e)
        }
    }

    fun loadAgents(): List<Agent> {
        return try {
            val jsonStr = prefs.getString(KEY_AGENTS, null)
            if (jsonStr.isNullOrEmpty()) {
                agentDao.getAllAgentsSync().map { it.toDomainAgent() }
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<Agent>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Agent(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            rate = obj.optDouble("rate", 0.0),
                            mult = obj.optDouble("mult", 0.0),
                            date = obj.optString("date", "").toAsciiDigits(),
                            roundNumber = obj.optString("roundNumber", "")
                        )
                    )
                }
                val isOldDefaults = list.size == 3 &&
                    list.getOrNull(0)?.name == "ကိုအောင်" &&
                    list.getOrNull(1)?.name == "မသီတာ" &&
                    list.getOrNull(2)?.name == "ကိုဇော်"
                if (isOldDefaults) {
                    saveAgents(emptyList())
                    emptyList()
                } else {
                    agentDao.clearAllAgentsSync()
                    agentDao.insertAllAgentsSync(list.map { AgentEntity.fromDomainAgent(it) })
                    list
                }
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading agents", e)
            emptyList()
        }
    }

    fun saveAgents(agents: List<Agent>) {
        try {
            val arr = JSONArray()
            agents.forEach { a ->
                val obj = JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("rate", a.rate)
                    put("mult", a.mult)
                    put("date", a.date)
                    put("roundNumber", a.roundNumber)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_AGENTS, arr.toString()).apply()
            agentDao.clearAllAgentsSync()
            agentDao.insertAllAgentsSync(agents.map { AgentEntity.fromDomainAgent(it) })
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving agents", e)
        }
    }

    // Vouchers
    fun loadVouchers(): List<Voucher> {
        return try {
            val jsonStr = prefs.getString(KEY_VOUCHERS, null)
            if (jsonStr.isNullOrEmpty()) {
                voucherDao.getAllVouchersSync().map { it.toDomainVoucher() }.sortedWith(compareBy<Voucher> { it.no }.thenBy { it.id })
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<Voucher>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val itemsArr = obj.getJSONArray("items")
                    val items = mutableListOf<BetItem>()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        val savedId = itemObj.optLong("id", 0L)
                        val amt = itemObj.optLong("a", 0L)
                        if (validateBetAmount(amt).isValid) {
                            items.add(
                                if (savedId > 0L) {
                                    BetItem(
                                        n = itemObj.getString("n").toAsciiDigits(),
                                        a = amt,
                                        id = savedId
                                    )
                                } else {
                                    BetItem(
                                        n = itemObj.getString("n").toAsciiDigits(),
                                        a = amt
                                    )
                                }
                            )
                        }
                    }
                    list.add(
                        Voucher(
                            id = obj.getLong("id"),
                            no = obj.getInt("no"),
                            agent = obj.getString("agent"),
                            agentId = obj.optInt("agentId", 0),
                            date = obj.getString("date").toAsciiDigits(),
                            session = obj.getString("session"),
                            time = obj.getString("time").toAsciiDigits(),
                            items = items
                        )
                    )
                }
                val sortedList = list.sortedWith(compareBy<Voucher> { it.no }.thenBy { it.id })
                voucherDao.clearAllVouchersSync()
                voucherDao.insertAllVouchersSync(sortedList.map { VoucherEntity.fromDomainVoucher(it) })
                sortedList
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading vouchers", e)
            emptyList()
        }
    }

    fun saveVouchers(vouchers: List<Voucher>) {
        try {
            val arr = JSONArray()
            vouchers.forEach { v ->
                val obj = JSONObject().apply {
                    put("id", v.id)
                    put("no", v.no)
                    put("agent", v.agent)
                    put("agentId", v.agentId)
                    put("date", v.date)
                    put("session", v.session)
                    put("time", v.time)
                    val itemsArr = JSONArray()
                    v.items.forEach { itm ->
                        val itemObj = JSONObject().apply {
                            put("id", itm.id)
                            put("n", itm.n)
                            put("a", itm.a)
                        }
                        itemsArr.put(itemObj)
                    }
                    put("items", itemsArr)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_VOUCHERS, arr.toString()).apply()
            voucherDao.clearAllVouchersSync()
            voucherDao.insertAllVouchersSync(vouchers.map { VoucherEntity.fromDomainVoucher(it) })
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving vouchers", e)
        }
    }

    /**
     * Deletes the entire Voucher matching [voucherId] along with all of its associated BetItems (CASCADE).
     */
    fun deleteVoucherById(voucherId: Long): List<Voucher>? {
        val current = loadVouchers()
        if (current.none { it.id == voucherId }) return null
        val updated = current.filter { it.id != voucherId }
        voucherDao.deleteVoucherByIdSync(voucherId)
        saveVouchers(updated)
        return updated
    }

    /**
     * Deletes all vouchers belonging to [agentId] or [agentName] ONLY within [date] and [round].
     * Never affects vouchers from other dates or sessions.
     */
    fun deleteVouchersByAgentAndSessionSync(date: String, round: String, agentId: Int, agentName: String): List<Voucher> {
        val asciiDate = date.toAsciiDigits().trim()
        val current = loadVouchers()
        val updated = current.filterNot { v ->
            v.date.toAsciiDigits().trim() == asciiDate &&
                v.session.trim() == round.trim() &&
                (v.agentId == agentId || v.agent == agentName)
        }
        try {
            voucherDao.deleteVouchersByAgentAndSessionSync(asciiDate, round.trim(), agentId, agentName)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error deleting vouchers by agent and session", e)
        }
        saveVouchers(updated)
        return updated
    }

    /**
     * Deletes ONLY the single number entry matching [numberId] (equivalent to DELETE ... WHERE id = :numberId).
     * Never deletes the parent Voucher object even when its last number is removed.
     */
    fun deleteNumberById(numberId: Long): List<Voucher>? {
        val current = loadVouchers()
        var found = false
        val updated = current.map { v ->
            if (v.items.any { it.id == numberId }) {
                found = true
                v.copy(items = v.items.filter { it.id != numberId })
            } else {
                v
            }
        }
        if (!found) return null
        saveVouchers(updated)
        return updated
    }

    /**
     * Updates ONLY the single number entry matching [numberId], preserving its unique id and parent Voucher.
     */
    fun updateNumberById(numberId: Long, newNumber: String, newAmount: Long): List<Voucher>? {
        if (!validateBetAmount(newAmount).isValid) return null
        val current = loadVouchers()
        var found = false
        val updated = current.map { v ->
            if (v.items.any { it.id == numberId }) {
                found = true
                v.copy(
                    items = v.items.map { item ->
                        if (item.id == numberId) item.copy(n = newNumber, a = newAmount) else item
                    }
                )
            } else {
                v
            }
        }
        if (!found) return null
        saveVouchers(updated)
        return updated
    }

    /**
     * Appends a single number entry to an existing Voucher (including an empty voucher).
     */
    fun addNumberToVoucher(voucherId: Long, item: BetItem): List<Voucher>? {
        if (!validateBetAmount(item.a).isValid) return null
        val current = loadVouchers()
        var found = false
        val updated = current.map { v ->
            if (v.id == voucherId) {
                found = true
                v.copy(items = v.items + item)
            } else {
                v
            }
        }
        if (!found) return null
        saveVouchers(updated)
        return updated
    }

    // Layoff Records
    fun loadLayoffs(): List<LayoffRecord> {
        return try {
            val jsonStr = prefs.getString(KEY_LAYOFFS, null)
            if (jsonStr.isNullOrEmpty()) {
                emptyList()
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<LayoffRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val itemsArr = obj.getJSONArray("items")
                    val items = mutableListOf<LayoffItem>()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        items.add(
                            LayoffItem(
                                n = itemObj.getString("n").toAsciiDigits(),
                                a = itemObj.getLong("a")
                            )
                        )
                    }
                    val savedRecordNo = obj.optInt("recordNo", 0)
                    val recordNo = if (savedRecordNo > 0) savedRecordNo else (i + 1)
                    list.add(
                        LayoffRecord(
                            id = obj.getLong("id"),
                            date = obj.getString("date").toAsciiDigits(),
                            session = obj.getString("session"),
                            time = obj.getString("time").toAsciiDigits(),
                            limit = obj.getLong("limit"),
                            items = items,
                            total = obj.getLong("total"),
                            recordNo = recordNo
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading layoffs", e)
            emptyList()
        }
    }

    fun saveLayoffs(layoffs: List<LayoffRecord>) {
        try {
            val arr = JSONArray()
            layoffs.forEach { l ->
                val obj = JSONObject().apply {
                    put("id", l.id)
                    put("recordNo", l.recordNo)
                    put("date", l.date)
                    put("session", l.session)
                    put("time", l.time)
                    put("limit", l.limit)
                    put("total", l.total)
                    val itemsArr = JSONArray()
                    l.items.forEach { itm ->
                        val itemObj = JSONObject().apply {
                            put("n", itm.n)
                            put("a", itm.a)
                        }
                        itemsArr.put(itemObj)
                    }
                    put("items", itemsArr)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_LAYOFFS, arr.toString()).apply()
            passBetRecordDao.clearAllPassBetRecordsSync()
            passBetRecordDao.insertAllPassBetRecordsSync(layoffs.map { PassBetRecordEntity.fromDomainLayoffRecord(it) })
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving layoffs", e)
        }
    }

    // Blocked numbers
    fun loadBlocked(): Set<String> {
        return try {
            val jsonStr = prefs.getString(KEY_BLOCKED, null)
            if (jsonStr.isNullOrEmpty()) {
                emptySet()
            } else {
                val arr = JSONArray(jsonStr)
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                set
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading blocked numbers", e)
            emptySet()
        }
    }

    fun saveBlocked(blocked: Set<String>) {
        try {
            val arr = JSONArray()
            blocked.forEach { arr.put(it) }
            prefs.edit().putString(KEY_BLOCKED, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving blocked numbers", e)
        }
    }

    // Wins (winning numbers)
    fun loadWins(): List<WinRecord> {
        return try {
            val jsonStr = prefs.getString(KEY_WINS, null)
            if (jsonStr.isNullOrEmpty()) {
                winningNumberDao.getAllWinningNumbersSync().map { it.toDomainWinRecord() }
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<WinRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        WinRecord(
                            date = obj.getString("date").toAsciiDigits(),
                            session = obj.getString("session"),
                            n = obj.getString("n").toAsciiDigits()
                        )
                    )
                }
                winningNumberDao.clearAllWinningNumbersSync()
                winningNumberDao.insertAllWinningNumbersSync(
                    list.map { WinningNumberEntity.fromDomainWinRecord(it) }
                )
                list
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading wins", e)
            emptyList()
        }
    }

    fun saveWins(wins: List<WinRecord>) {
        try {
            val arr = JSONArray()
            wins.forEach { w ->
                val obj = JSONObject().apply {
                    put("date", w.date.toAsciiDigits())
                    put("session", w.session)
                    put("n", w.n.toAsciiDigits())
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_WINS, arr.toString()).apply()
            winningNumberDao.clearAllWinningNumbersSync()
            winningNumberDao.insertAllWinningNumbersSync(
                wins.map { WinningNumberEntity.fromDomainWinRecord(it) }
            )
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving wins", e)
        }
    }

    // Settled Records
    fun loadSettled(): List<SettledRecord> {
        return try {
            val jsonStr = prefs.getString(KEY_SETTLED, null)
            if (jsonStr.isNullOrEmpty()) {
                settlementDao.getAllSettlementsSync().map { it.toDomainSettledRecord() }
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<SettledRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        SettledRecord(
                            id = obj.getLong("id"),
                            date = obj.getString("date").toAsciiDigits(),
                            session = obj.getString("session"),
                            winningNumber = obj.getString("winningNumber").toAsciiDigits(),
                            totalRetained = obj.getLong("totalRetained"),
                            commission = obj.getLong("commission"),
                            retainedNet = obj.getLong("retainedNet"),
                            payout = obj.getLong("payout"),
                            profitLoss = obj.getLong("profitLoss"),
                            timestamp = obj.getString("timestamp").toAsciiDigits()
                        )
                    )
                }
                settlementDao.clearAllSettlementsSync()
                settlementDao.insertAllSettlementsSync(
                    list.map { SettlementEntity.fromDomainSettledRecord(it) }
                )
                list
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading settled records", e)
            emptyList()
        }
    }

    fun saveSettled(records: List<SettledRecord>) {
        try {
            val arr = JSONArray()
            records.forEach { r ->
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("date", r.date)
                    put("session", r.session)
                    put("winningNumber", r.winningNumber)
                    put("totalRetained", r.totalRetained)
                    put("commission", r.commission)
                    put("retainedNet", r.retainedNet)
                    put("payout", r.payout)
                    put("profitLoss", r.profitLoss)
                    put("timestamp", r.timestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_SETTLED, arr.toString()).apply()
            settlementDao.clearAllSettlementsSync()
            settlementDao.insertAllSettlementsSync(
                records.map { SettlementEntity.fromDomainSettledRecord(it) }
            )
            rebuildDailySettlementsFromRecordsSync(records)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving settled records", e)
        }
    }

    private fun rebuildDailySettlementsFromRecordsSync(records: List<SettledRecord>) {
        try {
            val grouped = records.groupBy { it.date }
            dailySettlementDao.clearAllDailySettlementsSync()
            val dailyList = grouped.map { (date, dateRecords) ->
                val ordered = dateRecords.reversed()
                DailySettlementEntity(
                    date = date,
                    totalSales = dateRecords.sumOf { it.totalRetained },
                    totalCommission = dateRecords.sumOf { it.commission },
                    netSales = dateRecords.sumOf { it.retainedNet },
                    winningPayout = dateRecords.sumOf { it.payout },
                    profitOrLoss = dateRecords.sumOf { it.profitLoss },
                    roundsSummary = ordered.joinToString(", ") { it.session },
                    winningNumbersSummary = ordered.joinToString(", ") { "${it.session}:${it.winningNumber.ifEmpty { "-" }}" },
                    roundCount = dateRecords.size,
                    timestamp = dateRecords.firstOrNull()?.timestamp ?: ""
                )
            }
            if (dailyList.isNotEmpty()) {
                dailySettlementDao.insertAllDailySettlementsSync(dailyList)
            }
            syncAllDailySettlementsToPrefs()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error rebuilding daily settlements", e)
        }
    }

    private fun syncAllDailySettlementsToPrefs() {
        try {
            val allDaily = dailySettlementDao.getAllDailySettlementsSync()
            val arr = JSONArray()
            allDaily.forEach { d ->
                val obj = JSONObject().apply {
                    put("date", d.date)
                    put("totalSales", d.totalSales)
                    put("totalCommission", d.totalCommission)
                    put("netSales", d.netSales)
                    put("winningPayout", d.winningPayout)
                    put("profitOrLoss", d.profitOrLoss)
                    put("roundsSummary", d.roundsSummary)
                    put("winningNumbersSummary", d.winningNumbersSummary)
                    put("roundCount", d.roundCount)
                    put("timestamp", d.timestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_DAILY_SETTLED, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error syncing daily settlements to prefs", e)
        }
    }

    fun getDailySettlementByDate(date: String): kotlinx.coroutines.flow.Flow<DailySettlementEntity?> =
        dailySettlementDao.getDailySettlementByDate(date.toAsciiDigits())

    fun getAllDailySettlements(): kotlinx.coroutines.flow.Flow<List<DailySettlementEntity>> =
        dailySettlementDao.getAllDailySettlements()

    fun loadDailySettlementForDate(date: String): DailySettlementEntity? {
        val asciiDate = date.toAsciiDigits()
        return try {
            dailySettlementDao.getDailySettlementByDateSync(asciiDate)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading daily settlement for $asciiDate", e)
            null
        }
    }

    fun loadAllDailySettlements(): List<DailySettlementEntity> {
        return try {
            val fromRoom = dailySettlementDao.getAllDailySettlementsSync()
            if (fromRoom.isNotEmpty()) {
                fromRoom.map { it.copy(date = it.date.toAsciiDigits()) }
            } else {
                val jsonStr = prefs.getString(KEY_DAILY_SETTLED, null)
                if (!jsonStr.isNullOrEmpty()) {
                    val arr = JSONArray(jsonStr)
                    val list = mutableListOf<DailySettlementEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            DailySettlementEntity(
                                date = obj.getString("date").toAsciiDigits(),
                                totalSales = obj.optLong("totalSales", 0L),
                                totalCommission = obj.optLong("totalCommission", 0L),
                                netSales = obj.optLong("netSales", 0L),
                                winningPayout = obj.optLong("winningPayout", 0L),
                                profitOrLoss = obj.optLong("profitOrLoss", 0L),
                                roundsSummary = obj.optString("roundsSummary", ""),
                                winningNumbersSummary = obj.optString("winningNumbersSummary", ""),
                                roundCount = obj.optInt("roundCount", 0),
                                timestamp = obj.optString("timestamp", "").toAsciiDigits()
                            )
                        )
                    }
                    if (list.isNotEmpty()) {
                        dailySettlementDao.insertAllDailySettlementsSync(list)
                    }
                    list
                } else {
                    val settled = loadSettled()
                    if (settled.isNotEmpty()) {
                        rebuildDailySettlementsFromRecordsSync(settled)
                        dailySettlementDao.getAllDailySettlementsSync()
                    } else {
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading all daily settlements", e)
            emptyList()
        }
    }

    fun saveDailySettlement(entity: DailySettlementEntity) {
        try {
            dailySettlementDao.insertOrUpdateDailySettlementSync(entity.copy(date = entity.date.toAsciiDigits()))
            syncAllDailySettlementsToPrefs()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving daily settlement for ${entity.date}", e)
        }
    }

    fun deleteDailySettlementByDateSync(date: String) {
        val asciiDate = date.toAsciiDigits()
        try {
            dailySettlementDao.deleteDailySettlementByDateSync(asciiDate)
            syncAllDailySettlementsToPrefs()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error deleting daily settlement for $asciiDate", e)
        }
    }

    fun clearAllDailySettlementsSync() {
        try {
            dailySettlementDao.clearAllDailySettlementsSync()
            prefs.edit().remove(KEY_DAILY_SETTLED).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error clearing daily settlements", e)
        }
    }

    /**
     * Deletes ONLY the single daily settled record matching [recordId] (DELETE ... WHERE id = :recordId).
     * Never clears the whole table.
     */
    fun deleteSettledRecordById(recordId: Long): List<SettledRecord>? {
        val current = loadSettled()
        if (current.none { it.id == recordId }) return null
        val updated = current.filter { it.id != recordId }
        settlementDao.deleteSettlementByIdSync(recordId)
        saveSettled(updated)
        return updated
    }

    fun deleteById(recordId: Long): List<SettledRecord>? = deleteSettledRecordById(recordId)

    /**
     * Updates ONLY the single daily settled record matching [recordId], keeping the exact same id
     * and leaving all other records unchanged.
     */
    fun updateSettledRecordById(recordId: Long, updatedRecord: SettledRecord): List<SettledRecord>? {
        val current = loadSettled()
        if (current.none { it.id == recordId }) return null
        val updated = current.map { r ->
            if (r.id == recordId) updatedRecord.copy(id = recordId) else r
        }
        saveSettled(updated)
        return updated
    }

    fun updateById(recordId: Long, updatedRecord: SettledRecord): List<SettledRecord>? =
        updateSettledRecordById(recordId, updatedRecord)

    // Composite (date, roundNumber) reactive and synchronous Room DAO queries
    fun getVouchersByDateAndRound(date: String, round: String): kotlinx.coroutines.flow.Flow<List<VoucherEntity>> =
        voucherDao.getVouchersByDateAndRound(date.toAsciiDigits(), round)

    fun getRoundsByDate(date: String): kotlinx.coroutines.flow.Flow<List<String>> =
        voucherDao.getRoundsByDate(date.toAsciiDigits())

    fun getWinningNumber(date: String, round: String): kotlinx.coroutines.flow.Flow<WinningNumberEntity?> =
        winningNumberDao.getWinningNumber(date.toAsciiDigits(), round)

    fun getSettlement(date: String, round: String): kotlinx.coroutines.flow.Flow<SettlementEntity?> =
        settlementDao.getSettlement(date.toAsciiDigits(), round)

    fun getDistinctHistoricalRoundsByDateSync(date: String): List<String> {
        val asciiDate = date.toAsciiDigits()
        return try {
            val agentRounds = agentDao.getRoundsByDateSync(asciiDate)
            val voucherRounds = voucherDao.getRoundsByDateSync(asciiDate)
            val winRounds = winningNumberDao.getRoundsByDateSync(asciiDate)
            val settlementRounds = settlementDao.getRoundsByDateSync(asciiDate)
            val layoffRounds = loadLayoffs().filter { it.date.toAsciiDigits() == asciiDate }.map { it.session }
            (agentRounds + voucherRounds + winRounds + settlementRounds + layoffRounds)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading historical rounds for date $asciiDate", e)
            emptyList()
        }
    }

    // App Settings
    fun loadSettings(): AppSettings {
        return try {
            val migrated = prefs.getBoolean(KEY_SETTINGS_MIGRATED_V1, false)
            val jsonStr = prefs.getString(KEY_SETTINGS, null)
            if (jsonStr.isNullOrEmpty()) {
                val defaults = AppSettings()
                prefs.edit().putBoolean(KEY_SETTINGS_MIGRATED_V1, true).apply()
                saveSettings(defaults)
                defaults
            } else {
                val obj = JSONObject(jsonStr)
                val password = obj.optString("password", "1234")
                val brakeLimit = obj.optLong("brakeLimit", 0L)
                val soundEnabled = obj.optBoolean("soundEnabled", true)
                val loaded = AppSettings(
                    password = password,
                    brakeLimit = brakeLimit,
                    soundEnabled = soundEnabled
                )
                if (!migrated) {
                    prefs.edit().putBoolean(KEY_SETTINGS_MIGRATED_V1, true).apply()
                }
                loaded
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading settings", e)
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        try {
            val obj = JSONObject().apply {
                put("password", settings.password)
                put("brakeLimit", settings.brakeLimit)
                put("soundEnabled", settings.soundEnabled)
            }
            prefs.edit()
                .putString(KEY_SETTINGS, obj.toString())
                .putBoolean(KEY_SETTINGS_MIGRATED_V1, true)
                .apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving settings", e)
        }
    }

    fun loadVouchersForDateAndRound(date: String, round: String): List<Voucher> {
        val asciiDate = date.toAsciiDigits()
        return try {
            voucherDao.getVouchersByDateAndRoundSync(asciiDate, round).map { it.toDomainVoucher() }
        } catch (e: Exception) {
            loadVouchers().filter { it.date.toAsciiDigits() == asciiDate && it.session == round }
        }
    }

    fun loadWinningNumberForDateAndRound(date: String, round: String): WinRecord? {
        val asciiDate = date.toAsciiDigits()
        return try {
            winningNumberDao.getWinningNumberSync(asciiDate, round)?.toDomainWinRecord()
        } catch (e: Exception) {
            loadWins().firstOrNull { it.date.toAsciiDigits() == asciiDate && it.session == round }
        }
    }

    fun loadSettlementForDateAndRound(date: String, round: String): SettledRecord? {
        val asciiDate = date.toAsciiDigits()
        return try {
            settlementDao.getSettlementSync(asciiDate, round)?.toDomainSettledRecord()
        } catch (e: Exception) {
            loadSettled().firstOrNull { it.date.toAsciiDigits() == asciiDate && it.session == round }
        }
    }

    fun resetAllData() {
        try {
            prefs.edit()
                .clear()
                .putBoolean(KEY_SESSIONS_MIGRATED_V1, true)
                .putBoolean(KEY_SETTINGS_MIGRATED_V1, true)
                .apply()
            agentDao.clearAllAgentsSync()
            voucherDao.clearAllVouchersSync()
            winningNumberDao.clearAllWinningNumbersSync()
            settlementDao.clearAllSettlementsSync()
            dailySettlementDao.clearAllDailySettlementsSync()
            sessionDao.clearAllSessionsSync()
            session2DDao.clearAllSessionsSync()
            passBetRecordDao.clearAllPassBetRecordsSync()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error resetting data", e)
        }
    }
}
