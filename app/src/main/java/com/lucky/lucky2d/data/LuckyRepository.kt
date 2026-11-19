package com.lucky.lucky2d.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lucky.lucky2d.logic.PatternAlgorithms
import com.lucky.lucky2d.model.Agent
import com.lucky.lucky2d.model.AppSettings
import com.lucky.lucky2d.model.DailySettlement
import com.lucky.lucky2d.model.LayoffRecord
import com.lucky.lucky2d.model.Session
import com.lucky.lucky2d.model.SettledRecord
import com.lucky.lucky2d.model.Voucher
import com.lucky.lucky2d.model.WinRecord
import java.io.File

class LuckyRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val storageDir: File = File(appContext.filesDir, "lucky2d_store").apply {
        if (!exists()) mkdirs()
    }

    companion object {
        private const val PREFS_NAME = "lucky2d_data_prefs"
        private const val KEY_AGENTS = "lucky2d_agents"
        private const val KEY_SESSIONS = "lucky2d_sessions"
        private const val KEY_VOUCHERS = "lucky2d_vouchers"
        private const val KEY_LAYOFFS = "lucky2d_layoffs"
        private const val KEY_BLOCKED = "lucky2d_blocked"
        private const val KEY_WINS = "lucky2d_wins"
        private const val KEY_SETTLED = "lucky2d_settled"
        private const val KEY_DAILY_SETTLED = "lucky2d_daily_settled"
        private const val KEY_SETTINGS = "lucky2d_settings"
        private const val KEY_LAST_SESSION = "last_active_session"
    }

    private fun readRaw(key: String): String? {
        val file = File(storageDir, "$key.json")
        if (file.exists()) {
            try {
                val text = file.readText(Charsets.UTF_8)
                if (text.isNotBlank()) return text
            } catch (_: Exception) {
                // Fall back to SharedPreferences
            }
        }
        return prefs.getString(key, null)
    }

    private fun writeRaw(key: String, json: String) {
        prefs.edit().putString(key, json).apply()
        try {
            val target = File(storageDir, "$key.json")
            val tmp = File(storageDir, "$key.json.tmp")
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(target)) {
                target.writeText(json, Charsets.UTF_8)
                tmp.delete()
            }
        } catch (_: Exception) {
            // SharedPreferences mirror already written
        }
    }

    private fun deleteKey(key: String) {
        prefs.edit().remove(key).apply()
        try {
            File(storageDir, "$key.json").delete()
        } catch (_: Exception) {
            // Ignore
        }
    }

    fun loadSettings(): AppSettings {
        return try {
            val raw = readRaw(KEY_SETTINGS) ?: return AppSettings().also { saveSettings(it) }
            gson.fromJson(raw, AppSettings::class.java) ?: AppSettings()
        } catch (_: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        writeRaw(KEY_SETTINGS, gson.toJson(settings))
    }

    fun getLastActiveSession(): String {
        return prefs.getString(KEY_LAST_SESSION, "") ?: ""
    }

    fun saveLastActiveSession(session: String) {
        prefs.edit().putString(KEY_LAST_SESSION, session.trim()).apply()
    }

    fun loadSessionsForDate(date: String): List<Session> {
        val asciiDate = PatternAlgorithms.toAsciiDigits(date)
        return try {
            val dateKey = "${KEY_SESSIONS}_$asciiDate"
            val raw = readRaw(dateKey)
            if (!raw.isNullOrBlank()) {
                val type = object : TypeToken<List<Session>>() {}.type
                val parsed: List<Session>? = gson.fromJson(raw, type)
                if (parsed != null) return parsed
            }
            val all = loadAllSessions()
            val filtered = all.filter { PatternAlgorithms.toAsciiDigits(it.date) == asciiDate }
            if (filtered.isNotEmpty()) return filtered

            val now = System.currentTimeMillis()
            val defaults = listOf(
                Session(id = now, name = "a", rate = 15.0, mult = 80, date = asciiDate),
                Session(id = now + 1, name = "p", rate = 15.0, mult = 80, date = asciiDate)
            )
            saveSessionsForDate(asciiDate, defaults)
            defaults
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSessionsForDate(date: String, sessions: List<Session>) {
        val asciiDate = PatternAlgorithms.toAsciiDigits(date)
        writeRaw("${KEY_SESSIONS}_$asciiDate", gson.toJson(sessions))
        val others = loadAllSessions().filter { PatternAlgorithms.toAsciiDigits(it.date) != asciiDate }
        writeRaw(KEY_SESSIONS, gson.toJson(others + sessions))
    }

    private fun loadAllSessions(): List<Session> {
        return try {
            val raw = readRaw(KEY_SESSIONS) ?: return emptyList()
            val type = object : TypeToken<List<Session>>() {}.type
            gson.fromJson<List<Session>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadAgents(): List<Agent> {
        return try {
            val raw = readRaw(KEY_AGENTS) ?: return emptyList()
            val type = object : TypeToken<List<Agent>>() {}.type
            gson.fromJson<List<Agent>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAgents(agents: List<Agent>) {
        writeRaw(KEY_AGENTS, gson.toJson(agents))
    }

    fun loadVouchers(): List<Voucher> {
        return try {
            val raw = readRaw(KEY_VOUCHERS) ?: return emptyList()
            val type = object : TypeToken<List<Voucher>>() {}.type
            gson.fromJson<List<Voucher>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveVouchers(vouchers: List<Voucher>) {
        writeRaw(KEY_VOUCHERS, gson.toJson(vouchers))
    }

    fun loadLayoffs(): List<LayoffRecord> {
        return try {
            val raw = readRaw(KEY_LAYOFFS) ?: return emptyList()
            val type = object : TypeToken<List<LayoffRecord>>() {}.type
            gson.fromJson<List<LayoffRecord>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLayoffs(layoffs: List<LayoffRecord>) {
        writeRaw(KEY_LAYOFFS, gson.toJson(layoffs))
    }

    fun loadBlocked(): Set<String> {
        return try {
            val raw = readRaw(KEY_BLOCKED) ?: return emptySet()
            val type = object : TypeToken<List<String>>() {}.type
            (gson.fromJson<List<String>>(raw, type) ?: emptyList()).toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun saveBlocked(blocked: Set<String>) {
        writeRaw(KEY_BLOCKED, gson.toJson(blocked.sorted()))
    }

    fun loadWins(): List<WinRecord> {
        return try {
            val raw = readRaw(KEY_WINS) ?: return emptyList()
            val type = object : TypeToken<List<WinRecord>>() {}.type
            gson.fromJson<List<WinRecord>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveWins(wins: List<WinRecord>) {
        writeRaw(KEY_WINS, gson.toJson(wins))
    }

    fun loadSettled(): List<SettledRecord> {
        return try {
            val raw = readRaw(KEY_SETTLED) ?: return emptyList()
            val type = object : TypeToken<List<SettledRecord>>() {}.type
            gson.fromJson<List<SettledRecord>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSettled(records: List<SettledRecord>): List<DailySettlement> {
        writeRaw(KEY_SETTLED, gson.toJson(records))
        val daily = rebuildDailySettlements(records)
        writeRaw(KEY_DAILY_SETTLED, gson.toJson(daily))
        return daily
    }

    fun loadDailySettlements(): List<DailySettlement> {
        return try {
            val raw = readRaw(KEY_DAILY_SETTLED) ?: return emptyList()
            val type = object : TypeToken<List<DailySettlement>>() {}.type
            gson.fromJson<List<DailySettlement>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun rebuildDailySettlements(records: List<SettledRecord>): List<DailySettlement> {
        val grouped = records.groupBy { it.date }
        return grouped.map { (date, items) ->
            val ordered = items.reversed()
            DailySettlement(
                date = date,
                totalSales = items.sumOf { it.totalRetained },
                totalCommission = items.sumOf { it.commission },
                netSales = items.sumOf { it.retainedNet },
                winningPayout = items.sumOf { it.payout },
                profitOrLoss = items.sumOf { it.profitLoss },
                roundsSummary = ordered.joinToString(", ") { it.session },
                winningNumbersSummary = ordered.joinToString(", ") { "${it.session}:${it.winningNumber.ifBlank { "-" }}" },
                roundCount = items.size,
                timestamp = items.firstOrNull()?.timestamp ?: ""
            )
        }
    }

    fun resetAllData() {
        listOf(
            KEY_AGENTS,
            KEY_SESSIONS,
            KEY_VOUCHERS,
            KEY_LAYOFFS,
            KEY_BLOCKED,
            KEY_WINS,
            KEY_SETTLED,
            KEY_DAILY_SETTLED,
            KEY_LAST_SESSION
        ).forEach { deleteKey(it) }

        val allKeys = prefs.all.keys.toList()
        for (k in allKeys) {
            if (k.startsWith("${KEY_SESSIONS}_")) {
                deleteKey(k)
            }
        }
        try {
            storageDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("${KEY_SESSIONS}_")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Ignore
        }
    }
}
