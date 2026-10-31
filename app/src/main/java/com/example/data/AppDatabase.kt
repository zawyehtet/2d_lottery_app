package com.example.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "vouchers",
    indices = [Index(value = ["date", "roundNumber"])]
)
data class VoucherEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "voucherId")
    val id: Long = 0,
    val agentName: String,
    val date: String,           // Format: "YYYY-MM-DD"
    val roundNumber: String,    // Format: "22.AM", "11", etc.
    val betsJson: String,       // Serialized bet rows
    val totalAmount: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val voucherNo: Int = 1,
    val agentId: Int = 0,
    val formattedTime: String = ""
) {
    val voucherId: Long
        get() = id
    val sessionName: String
        get() = roundNumber
    val formattedDateTime: String
        get() = if (formattedTime.contains("/")) formattedTime else if (formattedTime.isNotBlank()) "$date/$formattedTime" else date

    fun toDomainVoucher(): Voucher {
        val items = mutableListOf<BetItem>()
        try {
            val arr = JSONArray(betsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val savedId = obj.optLong("id", 0L)
                val amt = obj.optLong("a", 0L)
                val num = obj.optString("n", "").toAsciiDigits()
                if (num.isNotEmpty() && validateBetAmount(amt).isValid) {
                    items.add(
                        if (savedId > 0L) BetItem(n = num, a = amt, id = savedId)
                        else BetItem(n = num, a = amt)
                    )
                }
            }
        } catch (_: Exception) {
        }
        return Voucher(
            id = id,
            no = voucherNo,
            agent = agentName,
            agentId = agentId,
            date = date.toAsciiDigits(),
            session = roundNumber,
            time = formattedTime.toAsciiDigits(),
            items = items
        )
    }

    companion object {
        fun fromDomainVoucher(v: Voucher): VoucherEntity {
            val itemsArr = JSONArray()
            v.items.forEach { itm ->
                itemsArr.put(
                    JSONObject().apply {
                        put("id", itm.id)
                        put("n", itm.n.toAsciiDigits())
                        put("a", itm.a)
                    }
                )
            }
            return VoucherEntity(
                id = v.id,
                agentName = v.agent,
                date = v.date.toAsciiDigits(),
                roundNumber = v.session,
                betsJson = itemsArr.toString(),
                totalAmount = v.totalAmount,
                timestamp = v.id,
                voucherNo = v.no,
                agentId = v.agentId,
                formattedTime = v.time.toAsciiDigits()
            )
        }
    }
}

@Entity(
    tableName = "winning_numbers",
    indices = [Index(value = ["date", "roundNumber"], unique = true)]
)
data class WinningNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val roundNumber: String,
    val winningNumber: String   // e.g., "47"
) {
    fun toDomainWinRecord(): WinRecord = WinRecord(
        date = date.toAsciiDigits(),
        session = roundNumber,
        n = winningNumber.toAsciiDigits()
    )

    companion object {
        fun fromDomainWinRecord(w: WinRecord, id: Long = 0L): WinningNumberEntity = WinningNumberEntity(
            id = id,
            date = w.date.toAsciiDigits(),
            roundNumber = w.session,
            winningNumber = w.n.toAsciiDigits()
        )
    }
}

@Entity(
    tableName = "settlements",
    indices = [Index(value = ["date", "roundNumber"], unique = true)]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val roundNumber: String,
    val totalSales: Long,
    val totalCommission: Long,
    val netSales: Long,
    val winningPayout: Long,
    val profitOrLoss: Long,
    val winningNumber: String = "",
    val timestamp: String = ""
) {
    fun toDomainSettledRecord(): SettledRecord = SettledRecord(
        id = id,
        date = date.toAsciiDigits(),
        session = roundNumber,
        winningNumber = winningNumber.toAsciiDigits(),
        totalRetained = totalSales,
        commission = totalCommission,
        retainedNet = netSales,
        payout = winningPayout,
        profitLoss = profitOrLoss,
        timestamp = timestamp.toAsciiDigits()
    )

    companion object {
        fun fromDomainSettledRecord(r: SettledRecord): SettlementEntity = SettlementEntity(
            id = r.id,
            date = r.date.toAsciiDigits(),
            roundNumber = r.session,
            totalSales = r.totalRetained,
            totalCommission = r.commission,
            netSales = r.retainedNet,
            winningPayout = r.payout,
            profitOrLoss = r.profitLoss,
            winningNumber = r.winningNumber.toAsciiDigits(),
            timestamp = r.timestamp.toAsciiDigits()
        )
    }
}

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundName: String,
    val commissionPercent: Double,
    val multiplier: Double
) {
    fun toDomainSession(): Session = Session(
        name = roundName,
        rate = commissionPercent,
        mult = multiplier,
        id = id
    )

    companion object {
        fun fromDomainSession(s: Session): SessionEntity = SessionEntity(
            id = s.id,
            roundName = s.name,
            commissionPercent = s.rate,
            multiplier = s.mult
        )
    }
}

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers WHERE date = :date AND roundNumber = :round ORDER BY voucherNo ASC, voucherId ASC")
    fun getVouchersByDateAndRound(date: String, round: String): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers WHERE date = :date AND roundNumber = :round ORDER BY voucherNo ASC, voucherId ASC")
    fun getVouchersByDateAndRoundSync(date: String, round: String): List<VoucherEntity>

    @Query("SELECT * FROM vouchers WHERE date = :date AND roundNumber = :round ORDER BY voucherNo ASC")
    fun getVouchersBySessionOldestFirst(date: String, round: String): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers WHERE date = :date AND roundNumber = :round ORDER BY voucherNo ASC")
    fun getVouchersBySessionOldestFirstSync(date: String, round: String): List<VoucherEntity>

    @Query("SELECT DISTINCT roundNumber FROM vouchers WHERE date = :date")
    fun getRoundsByDate(date: String): Flow<List<String>>

    @Query("SELECT DISTINCT roundNumber FROM vouchers WHERE date = :date")
    fun getRoundsByDateSync(date: String): List<String>

    @Query("SELECT * FROM vouchers ORDER BY voucherNo ASC, voucherId ASC")
    fun getAllVouchersSync(): List<VoucherEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVoucherSync(voucher: VoucherEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllVouchersSync(vouchers: List<VoucherEntity>)

    @Query("UPDATE vouchers SET roundNumber = :newName WHERE roundNumber = :oldName")
    fun renameRoundSync(oldName: String, newName: String)

    @Query("DELETE FROM vouchers WHERE voucherId = :id")
    fun deleteVoucherByIdSync(id: Long)

    @Query("DELETE FROM vouchers")
    fun clearAllVouchersSync()
}

@Dao
interface WinningNumberDao {
    @Query("SELECT * FROM winning_numbers WHERE date = :date AND roundNumber = :round LIMIT 1")
    fun getWinningNumber(date: String, round: String): Flow<WinningNumberEntity?>

    @Query("SELECT * FROM winning_numbers WHERE date = :date AND roundNumber = :round LIMIT 1")
    fun getWinningNumberSync(date: String, round: String): WinningNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWinningNumber(win: WinningNumberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateWinningNumberSync(win: WinningNumberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllWinningNumbersSync(wins: List<WinningNumberEntity>)

    @Query("UPDATE winning_numbers SET roundNumber = :newName WHERE roundNumber = :oldName")
    fun renameRoundSync(oldName: String, newName: String)

    @Query("SELECT * FROM winning_numbers ORDER BY id ASC")
    fun getAllWinningNumbersSync(): List<WinningNumberEntity>

    @Query("SELECT DISTINCT roundNumber FROM winning_numbers WHERE date = :date")
    fun getRoundsByDateSync(date: String): List<String>

    @Query("DELETE FROM winning_numbers WHERE date = :date AND roundNumber = :round")
    fun deleteWinningNumberSync(date: String, round: String)

    @Query("DELETE FROM winning_numbers")
    fun clearAllWinningNumbersSync()
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE date = :date AND roundNumber = :round LIMIT 1")
    fun getSettlement(date: String, round: String): Flow<SettlementEntity?>

    @Query("SELECT * FROM settlements WHERE date = :date AND roundNumber = :round LIMIT 1")
    fun getSettlementSync(date: String, round: String): SettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateSettlementSync(settlement: SettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSettlementsSync(settlements: List<SettlementEntity>)

    @Query("UPDATE settlements SET roundNumber = :newName WHERE roundNumber = :oldName")
    fun renameRoundSync(oldName: String, newName: String)

    @Query("SELECT * FROM settlements ORDER BY id ASC")
    fun getAllSettlementsSync(): List<SettlementEntity>

    @Query("SELECT DISTINCT roundNumber FROM settlements WHERE date = :date")
    fun getRoundsByDateSync(date: String): List<String>

    @Query("DELETE FROM settlements WHERE id = :id")
    fun deleteSettlementByIdSync(id: Long)

    @Query("DELETE FROM settlements")
    fun clearAllSettlementsSync()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY id ASC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY id ASC")
    fun getAllSessionsSync(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSessionsSync(sessions: List<SessionEntity>)

    @Query("DELETE FROM sessions")
    fun clearAllSessionsSync()
}

@Entity(
    tableName = "pass_bet_records",
    indices = [Index(value = ["date", "sessionName"])]
)
data class PassBetRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordNo: Int = 1,
    val sessionName: String,
    val date: String,
    val totalAmount: Long,
    val betsJson: String = "",
    val time: String = "",
    val limit: Long = 0L
) {
    val session: String get() = sessionName
    val total: Long get() = totalAmount

    fun toDomainLayoffRecord(): LayoffRecord {
        val items = mutableListOf<LayoffItem>()
        try {
            val arr = JSONArray(betsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                items.add(LayoffItem(obj.getString("n"), obj.getLong("a")))
            }
        } catch (_: Exception) {}
        return LayoffRecord(
            id = id,
            date = date,
            session = sessionName,
            time = time,
            limit = limit,
            items = items,
            total = totalAmount,
            recordNo = recordNo
        )
    }

    companion object {
        fun fromDomainLayoffRecord(rec: LayoffRecord): PassBetRecordEntity {
            val arr = JSONArray()
            rec.items.forEach { itm ->
                arr.put(JSONObject().apply {
                    put("n", itm.n)
                    put("a", itm.a)
                })
            }
            return PassBetRecordEntity(
                id = rec.id,
                recordNo = rec.recordNo,
                sessionName = rec.session,
                date = rec.date,
                totalAmount = rec.total,
                betsJson = arr.toString(),
                time = rec.time,
                limit = rec.limit
            )
        }
    }
}

@Dao
interface PassBetRecordDao {
    @Query("SELECT * FROM pass_bet_records WHERE sessionName = :session AND date = :date ORDER BY recordNo ASC")
    fun getPassBetRecordsOldestFirst(session: String, date: String): Flow<List<PassBetRecordEntity>>

    @Query("SELECT * FROM pass_bet_records WHERE sessionName = :session AND date = :date ORDER BY recordNo ASC")
    fun getPassBetRecordsOldestFirstSync(session: String, date: String): List<PassBetRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPassBetRecordSync(record: PassBetRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllPassBetRecordsSync(records: List<PassBetRecordEntity>)

    @Query("DELETE FROM pass_bet_records WHERE id = :id")
    fun deletePassBetRecordByIdSync(id: Long)

    @Query("DELETE FROM pass_bet_records")
    fun clearAllPassBetRecordsSync()
}

typealias AgentEntity = com.example.data.local.entity.AgentEntity
typealias AgentDao = com.example.data.local.dao.AgentDao
typealias DailySettlementEntity = com.example.data.local.entity.DailySettlementEntity
typealias DailySettlementDao = com.example.data.local.dao.DailySettlementDao

@Database(
    entities = [
        AgentEntity::class,
        VoucherEntity::class,
        WinningNumberEntity::class,
        SettlementEntity::class,
        DailySettlementEntity::class,
        SessionEntity::class,
        PassBetRecordEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
    abstract fun voucherDao(): VoucherDao
    abstract fun winningNumberDao(): WinningNumberDao
    abstract fun settlementDao(): SettlementDao
    abstract fun dailySettlementDao(): DailySettlementDao
    abstract fun sessionDao(): SessionDao
    abstract fun passBetRecordDao(): PassBetRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lucky2d_room_database.db"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
