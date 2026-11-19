package com.lucky.lucky2d.model

const val MAX_BET_AMOUNT: Int = 999_999_999

data class Agent(
    val id: Long,
    val name: String,
    val rate: Double,
    val mult: Int,
    val date: String,
    val roundNumber: String,
    val phoneNumber: String = ""
)

data class Session(
    val id: Long,
    val name: String,
    val rate: Double,
    val mult: Int,
    val date: String,
    val winningNumber: String? = null,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BetItem(
    val id: Long,
    val n: String, // "00".."99"
    val a: Int     // amount
)

data class Voucher(
    val id: Long,
    val no: Int,
    val agent: String,
    val agentId: Long,
    val date: String,
    val session: String,
    val time: String,
    val items: List<BetItem>
)

data class LayoffItem(
    val n: String,
    val a: Int
)

data class LayoffRecord(
    val id: Long,
    val date: String,
    val session: String,
    val time: String,
    val limit: Int,
    val items: List<LayoffItem>,
    val total: Int,
    val recordNo: Int
)

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

data class DailySettlement(
    val date: String,
    val totalSales: Long,
    val totalCommission: Long,
    val netSales: Long,
    val winningPayout: Long,
    val profitOrLoss: Long,
    val roundsSummary: String,
    val winningNumbersSummary: String,
    val roundCount: Int,
    val timestamp: String
)

data class AppSettings(
    val password: String = "1234",
    val passwordLockEnabled: Boolean = false,
    val brakeLimit: Int = 0,
    val soundEnabled: Boolean = true
)

data class NumberSummary(
    val n: String,
    val entryCount: Int,
    val totalBet: Int,
    val layoffAmount: Int,
    val netAmount: Int
)

data class NumberAgentBreakdownItem(
    val agentId: Long,
    val agentName: String,
    val rate: Double,
    val mult: Int,
    val entryCount: Int,
    val betOnNumber: Int,
    val totalAgentSales: Int,
    val commissionOnNumber: Int,
    val totalSalesCommission: Int,
    val winningPayout: Long
)

data class AgentSummaryData(
    val totalBet: Int,
    val commission: Int,
    val netAmount: Int,
    val voucherCount: Int,
    val payout: Long
)

data class Agent2DSettlement(
    val agentId: Long,
    val agentName: String,
    val commissionRate: Double,
    val multiplier: Int,
    val totalSales: Int,
    val commissionAmount: Int,
    val netSales: Int,
    val winningBetAmount: Int,
    val payoutAmount: Long,
    val netBalance: Long
)

data class SessionSettlementCalc(
    val totalRetained: Long,
    val commission: Long,
    val retainedNet: Long,
    val payout: Long,
    val profitLoss: Long
)

data class ParsedBet(
    val n: String,
    val a: Int
)

data class ParseResult(
    val items: List<ParsedBet>,
    val skippedCount: Int,
    val blockedSkipped: List<String>,
    val invalidTokens: List<String>
)

data class BetAmountValidation(
    val isValid: Boolean,
    val error: String? = null
)

data class RejectedAlertInfo(
    val blockedNumbers: List<String>,
    val overLimitEntries: List<Pair<String, Int>> = emptyList(),
    val brakeLimit: Int = 0
)
