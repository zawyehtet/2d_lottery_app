package com.example.data

import java.util.concurrent.atomic.AtomicLong

private val betItemIdCounter = AtomicLong(1000L)
private val sessionIdCounter = AtomicLong(1L)

data class Agent(
    val id: Int,
    val name: String,
    val rate: Double = 0.0,
    val mult: Double = 0.0,
    val date: String = "",
    val roundNumber: String = ""
)

data class Session(
    val name: String,
    val rate: Double = 0.0,
    val mult: Double = 0.0,
    val id: Long = sessionIdCounter.incrementAndGet(),
    val date: String = "",
    val winningNumber: String? = null,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val sessionName: String get() = name
    val defaultCommission: Double get() = rate
    val defaultMultiplier: Double get() = mult
}

data class BetItem(
    val n: String,
    val a: Long,
    val id: Long = betItemIdCounter.incrementAndGet()
) {
    val number: String get() = n
    val amount: Long get() = a
}

typealias BetItemEntity = BetItem

data class Voucher(
    val id: Long,
    val no: Int,
    val agent: String,
    val agentId: Int = 0,
    val date: String,
    val session: String,
    val time: String,
    val items: List<BetItem>
) {
    val voucherNo: Int get() = no
    val sessionName: String get() = session
    val agentName: String get() = agent
    val formattedDateTime: String
        get() = if (time.contains("/")) time else if (time.isNotBlank()) "$date/$time" else date
    val totalAmount: Long
        get() = items.sumOf { it.a }
}

data class LayoffItem(
    val n: String,
    val a: Long
) {
    val number: String get() = n
    val amount: Long get() = a
}

data class LayoffRecord(
    val id: Long,
    val date: String,
    val session: String,
    val time: String,
    val limit: Long,
    val items: List<LayoffItem>,
    val total: Long,
    val recordNo: Int = 1
) {
    val sessionName: String get() = session
    val totalAmount: Long get() = total
    val betItems: List<BetItemEntity> get() = items.map { BetItem(it.n, it.a) }
}


data class WinRecord(
    val date: String,
    val session: String,
    val n: String
)

data class SettledRecord(
    val id: Long,
    val date: String,
    val session: String,
    val winningNumber: String,
    val totalRetained: Long,
    val commission: Long,
    val retainedNet: Long,
    val payout: Long,
    val profitLoss: Long,
    val timestamp: String
)

data class AppSettings(
    val password: String = "1234",
    val brakeLimit: Long = 0L,
    val soundEnabled: Boolean = true
)

data class AgentSummaryData(
    val totalBet: Long,
    val commission: Long,
    val netAmount: Long,
    val voucherCount: Int,
    val payout: Long
)

fun String.toAsciiDigits(): String {
    val myanmarDigits = "၀၁၂၃၄၅၆၇၈၉"
    val asciiDigits = "0123456789"
    return this.map { char ->
        val index = myanmarDigits.indexOf(char)
        if (index != -1) asciiDigits[index] else char
    }.joinToString("")
}

data class NumberSales2D(
    val number: String,
    val amount: Long
)

fun generateSorted2DLedger(currentSalesMap: Map<String, Long>): List<NumberSales2D> {
    // Generate all 100 numbers from "00" to "99"
    val allNumbers = (0..99).map { i ->
        val numStr = String.format(java.util.Locale.US, "%02d", i)
        NumberSales2D(
            number = numStr,
            amount = currentSalesMap[numStr] ?: 0L
        )
    }

    // Partition into active bets and 0-amount numbers
    val (activeBets, zeroBets) = allNumbers.partition { it.amount > 0L }

    // 1. Active Bets: Highest amount first, tie-break by number ascending
    val sortedActiveBets = activeBets.sortedWith(
        compareByDescending<NumberSales2D> { it.amount }
            .thenBy { it.number.toIntOrNull() ?: 0 }
    )

    // 2. Zero Bets: Ascending numeric order (00, 01, 02, ... 99)
    val sortedZeroBets = zeroBets.sortedBy { it.number.toIntOrNull() ?: 0 }

    // Combine: Active bets on top, followed by zero-amount numbers below
    return sortedActiveBets + sortedZeroBets
}

fun generateFull2DLedger(currentSalesMap: Map<String, Long>): List<NumberSales2D> =
    generateSorted2DLedger(currentSalesMap)



