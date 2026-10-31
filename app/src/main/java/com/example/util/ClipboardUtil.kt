package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.example.data.BetItemEntity
import com.example.data.Voucher

/**
 * Standardized Voucher Copy Utility for both 2D and 3D.
 * Strictly guarantees Header, Body, and Footer are concatenated.
 */
fun copyVoucherWithHeaderAndFooter(
    context: Context,
    voucherNo: Int,
    sessionOrRound: String, // "31.AM" for 2D or "23" for 3D
    items: List<BetItemEntity>, // List of bets with .number and .amount
    totalAmount: Long
) {
    val builder = StringBuilder()

    // 1. MANDATORY HEADER: No. [အမှတ်စဉ်]=[အကြိမ်]
    builder.append("No. ").append(voucherNo).append("=").append(sessionOrRound.trim()).append("\n")

    // 2. ITEM LINES: [ဂဏန်း]=[ငွေ]
    items.forEach { bet ->
        builder.append(bet.number.trim()).append("=").append(bet.amount).append("\n")
    }

    // 3. MANDATORY FOOTER: Blank line divider + စုစုပေါင်း=[ငွေပမာဏ စုစုပေါင်း]
    builder.append("\nစုစုပေါင်း=").append(totalAmount)

    val finalClipboardText = builder.toString()

    // Copy directly to system clipboard
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Voucher No. $voucherNo", finalClipboardText)
    clipboard.setPrimaryClip(clip)

    Toast.makeText(context, "No. $voucherNo တင်ကွက်စာရင်း ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
}

/**
 * Overload accepting domain Voucher model
 */
fun copyVoucherWithHeaderAndFooter(
    context: Context,
    voucher: Voucher
) {
    copyVoucherWithHeaderAndFooter(
        context = context,
        voucherNo = voucher.no,
        sessionOrRound = voucher.session,
        items = voucher.items,
        totalAmount = voucher.totalAmount
    )
}

/**
 * Standardized Pass Bet / Layoff Export Copy Utility for 2D & 3D.
 * Strictly guarantees:
 * Line 1: No. [အမှတ်စဉ်]=[အကြိမ်ရေ]
 * Middle Lines: [ဂဏန်း]=[ငွေ]
 * Blank line divider
 * Footer: စုစုပေါင်း=[ငွေပမာဏ စုစုပေါင်း]
 */
fun copyPassBetsRecordToClipboard(
    context: Context,
    recordNo: Int,
    sessionName: String,
    items: List<BetItemEntity>,
    totalAmount: Long
) {
    val builder = StringBuilder()

    // 1. Line 1: Strictly No. [RecordNo]=[SessionName]
    builder.append("No. ").append(recordNo).append("=").append(sessionName.trim()).append("\n")

    // 2. Middle Lines: [ဂဏန်း]=[ငွေ]
    items.forEach { bet ->
        builder.append(bet.number.trim()).append("=").append(bet.amount).append("\n")
    }

    // 3. Footer: Blank line divider + စုစုပေါင်း=[TotalAmount]
    builder.append("\nစုစုပေါင်း=").append(totalAmount)

    val finalClipboardText = builder.toString()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Pass Bet No. $recordNo", finalClipboardText)
    clipboard.setPrimaryClip(clip)

    Toast.makeText(context, "No. $recordNo တင်ကွက်စာရင်း ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
}

@JvmName("copyPassBetsRecordFromLayoffItemsToClipboard")
fun copyPassBetsRecordToClipboard(
    context: Context,
    recordNo: Int,
    sessionName: String,
    items: List<com.example.data.LayoffItem>,
    totalAmount: Long
) {
    val betItems = items.map { com.example.data.BetItem(it.n, it.a) }
    copyPassBetsRecordToClipboard(context, recordNo, sessionName, betItems, totalAmount)
}

fun copyPassBetsRecordToClipboard(
    context: Context,
    record: com.example.data.LayoffRecord,
    recordNo: Int = record.recordNo
) {
    copyPassBetsRecordToClipboard(
        context = context,
        recordNo = recordNo,
        sessionName = record.session,
        items = record.items,
        totalAmount = record.total
    )
}

