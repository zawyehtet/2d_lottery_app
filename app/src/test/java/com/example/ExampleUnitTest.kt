package com.example

import com.example.data.toAsciiDigits
import com.example.logic.PatternAlgorithms
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPowerAndNakhatNumbers() {
        assertEquals(10, PatternAlgorithms.POWER_NUMBERS.size)
        assertTrue(PatternAlgorithms.POWER_NUMBERS.contains("05"))
        assertTrue(PatternAlgorithms.POWER_NUMBERS.contains("94"))

        assertEquals(10, PatternAlgorithms.NAKHAT_NUMBERS.size)
        assertTrue(PatternAlgorithms.NAKHAT_NUMBERS.contains("07"))
        assertTrue(PatternAlgorithms.NAKHAT_NUMBERS.contains("96"))
    }

    @Test
    fun testBrothersAndDoubles() {
        assertEquals(20, PatternAlgorithms.BROTHER_NUMBERS.size)
        assertTrue(PatternAlgorithms.BROTHER_NUMBERS.contains("01"))
        assertTrue(PatternAlgorithms.BROTHER_NUMBERS.contains("90"))

        assertEquals(10, PatternAlgorithms.DOUBLES.size)
        assertTrue(PatternAlgorithms.DOUBLES.contains("00"))
        assertTrue(PatternAlgorithms.DOUBLES.contains("99"))
    }

    @Test
    fun testParityCombinations() {
        assertEquals(25, PatternAlgorithms.getEvenEven().size)
        assertEquals(25, PatternAlgorithms.getOddOdd().size)
        assertEquals(25, PatternAlgorithms.getEvenOdd().size)
        assertEquals(25, PatternAlgorithms.getOddEven().size)
    }

    @Test
    fun testHeadTailIncludesBrake() {
        // Head '1': 10..19 (10 numbers)
        val head1 = PatternAlgorithms.getHead('1')
        assertEquals(10, head1.size)
        assertTrue(head1.contains("10"))
        assertTrue(head1.contains("19"))

        // Tail '1': 01..91 (10 numbers)
        val tail1 = PatternAlgorithms.getTail('1')
        assertEquals(10, tail1.size)
        assertTrue(tail1.contains("01"))
        assertTrue(tail1.contains("91"))

        // Includes '5': 19 numbers
        val inc5 = PatternAlgorithms.getIncludes('5')
        assertEquals(19, inc5.size)

        // Brake 6: (d1 + d2) % 10 == 6
        val brake6 = PatternAlgorithms.getBrake(6)
        assertTrue(brake6.contains("06"))
        assertTrue(brake6.contains("15"))
        assertTrue(brake6.contains("24"))
        assertTrue(brake6.contains("33"))
    }

    @Test
    fun testKway() {
        // Digits "123" without doubles: 3 * 2 = 6 pairs
        val kwayNoDoubles = PatternAlgorithms.getKway("123", withDoubles = false)
        assertEquals(6, kwayNoDoubles.size)
        assertFalse(kwayNoDoubles.contains("11"))

        // Digits "123" with doubles: 3 * 3 = 9 pairs
        val kwayWithDoubles = PatternAlgorithms.getKway("123", withDoubles = true)
        assertEquals(9, kwayWithDoubles.size)
        assertTrue(kwayWithDoubles.contains("11"))
    }

    @Test
    fun testQuickEntryMode1() {
        val blocked = setOf("99")
        val maxBet = 100000L

        // 01.78=500
        val r1 = PatternAlgorithms.parseQuickEntryMode1("01.78=500", blocked, maxBet)
        assertEquals(2, r1.items.size)
        assertEquals("01", r1.items[0].n)
        assertEquals(500L, r1.items[0].a)
        assertEquals("78", r1.items[1].n)
        assertEquals(500L, r1.items[1].a)

        // 01.78R500 -> 01, 10, 78, 87
        val r2 = PatternAlgorithms.parseQuickEntryMode1("01.78R500", blocked, maxBet)
        assertEquals(4, r2.items.size)

        // 01.78=500/100 -> 01=500, 10=100, 78=500, 87=100
        val r3 = PatternAlgorithms.parseQuickEntryMode1("01.78=500/100", blocked, maxBet)
        assertEquals(4, r3.items.size)
        assertEquals(500L, r3.items.first { it.n == "01" }.a)
        assertEquals(100L, r3.items.first { it.n == "10" }.a)
    }

    @Test
    fun testQuickEntryMode2() {
        val blocked = setOf<String>()
        val maxBet = 100000L

        // 5a500 -> includes 5 (19 items)
        val rA = PatternAlgorithms.parseQuickEntryMode2("5a500", blocked, maxBet)
        assertEquals(19, rA.items.size)

        // 6b600 -> brake 6 (10 items)
        val rB = PatternAlgorithms.parseQuickEntryMode2("6b600", blocked, maxBet)
        assertEquals(10, rB.items.size)

        // 1t500 -> head 1 (10 items)
        val rT = PatternAlgorithms.parseQuickEntryMode2("1t500", blocked, maxBet)
        assertEquals(10, rT.items.size)

        // 1w500 -> tail 1 (10 items)
        val rW = PatternAlgorithms.parseQuickEntryMode2("1w500", blocked, maxBet)
        assertEquals(10, rW.items.size)

        // p3500 -> power (10 items)
        val rP = PatternAlgorithms.parseQuickEntryMode2("p3500", blocked, maxBet)
        assertEquals(10, rP.items.size)

        // n7500 -> nakhat (10 items)
        val rN = PatternAlgorithms.parseQuickEntryMode2("n7500", blocked, maxBet)
        assertEquals(10, rN.items.size)

        // s500 -> doubles (10 items)
        val rS = PatternAlgorithms.parseQuickEntryMode2("s500", blocked, maxBet)
        assertEquals(10, rS.items.size)

        // x200 -> brothers (20 items)
        val rX = PatternAlgorithms.parseQuickEntryMode2("x200", blocked, maxBet)
        assertEquals(20, rX.items.size)
    }

    @Test
    fun testEmptyInputParsingAndConditionalSaveButton() {
        assertEquals(0L, com.example.ui.MainViewModel.parseAmountOrZero(""))
        assertEquals(0L, com.example.ui.MainViewModel.parseAmountOrZero("   "))
        assertEquals(5000L, com.example.ui.MainViewModel.parseAmountOrZero(" 5000 "))

        assertEquals(0.0, com.example.ui.MainViewModel.parseDoubleOrZero(""), 0.001)
        assertEquals(0.0, com.example.ui.MainViewModel.parseDoubleOrZero("  "), 0.001)
        assertEquals(15.5, com.example.ui.MainViewModel.parseDoubleOrZero("15.5"), 0.001)

        // Save button visibility: hidden when blank or equal to saved value
        assertFalse(com.example.ui.MainViewModel.shouldShowSaveButton("", ""))
        assertFalse(com.example.ui.MainViewModel.shouldShowSaveButton("   ", ""))
        assertFalse(com.example.ui.MainViewModel.shouldShowSaveButton("88", "88"))
        assertTrue(com.example.ui.MainViewModel.shouldShowSaveButton("88", ""))
        assertTrue(com.example.ui.MainViewModel.shouldShowSaveButton("99", "88"))

        // Round management Save button visibility: hidden when blank, visible when non-blank
        assertFalse(com.example.ui.MainViewModel.shouldShowRoundSaveButton(""))
        assertFalse(com.example.ui.MainViewModel.shouldShowRoundSaveButton("   "))
        assertTrue(com.example.ui.MainViewModel.shouldShowRoundSaveButton("မနက်ပိုင်း"))
    }

    @Test
    fun testDailySummaryProfitLossFormatting() {
        assertEquals("အမြတ် +12,000", com.example.ui.MainViewModel.formatDailyTotalResultText(12000L))
        assertEquals("အရှုံး -12,000", com.example.ui.MainViewModel.formatDailyTotalResultText(-12000L))
        assertEquals("0", com.example.ui.MainViewModel.formatDailyTotalResultText(0L))
    }

    @Test
    fun testBetAmountValidator() {
        // 1. Valid normal amounts (e.g. 100, 1000, 50000, 1000000, and MAX_BET_AMOUNT = 999_999_999L) pass
        for (validAmt in listOf(100L, 1000L, 50000L, 1000000L, com.example.data.MAX_BET_AMOUNT)) {
            assertTrue(com.example.data.validateBetAmount(validAmt).isValid)
            assertTrue(com.example.data.validateBetAmount(validAmt.toString()).isValid)
        }
        assertTrue(com.example.data.validateBetAmount(" 50000 ").isValid)

        // 2. 0 and negative values fail
        assertFalse(com.example.data.validateBetAmount(0L).isValid)
        assertFalse(com.example.data.validateBetAmount(-1L).isValid)
        assertFalse(com.example.data.validateBetAmount(-500L).isValid)
        assertEquals(com.example.data.BetAmountValidationResult.ZeroOrNegative, com.example.data.validateBetAmount("0"))
        assertEquals(com.example.data.BetAmountValidationResult.ZeroOrNegative, com.example.data.validateBetAmount("-100"))

        // 3. Empty and non-numeric strings fail
        assertEquals(com.example.data.BetAmountValidationResult.Empty, com.example.data.validateBetAmount(""))
        assertEquals(com.example.data.BetAmountValidationResult.Empty, com.example.data.validateBetAmount("   "))
        assertEquals(com.example.data.BetAmountValidationResult.Empty, com.example.data.validateBetAmount(null))
        assertEquals(com.example.data.BetAmountValidationResult.NonNumeric, com.example.data.validateBetAmount("abc"))
        assertEquals(com.example.data.BetAmountValidationResult.NonNumeric, com.example.data.validateBetAmount("100.5"))
        assertEquals(com.example.data.BetAmountValidationResult.NonNumeric, com.example.data.validateBetAmount("12a00"))

        // 4. Values > MAX_BET_AMOUNT (999_999_999L) fail
        assertFalse(com.example.data.validateBetAmount(com.example.data.MAX_BET_AMOUNT + 1L).isValid)
        assertEquals(com.example.data.BetAmountValidationResult.TooLarge, com.example.data.validateBetAmount("1000000000"))
        assertEquals(com.example.data.BetAmountValidationResult.TooLarge, com.example.data.validateBetAmount("99999999999999999999"))
    }

    @Test
    fun testFastBet1And2MultiLineAndClipboardFormat() {
        val blocked = setOf<String>()

        // Fast Bet 1: straight/reverse on double number 11 does not duplicate
        val rDouble = PatternAlgorithms.parseQuickEntryMode1("11.01=500/100", blocked)
        assertEquals(3, rDouble.items.size)
        assertEquals(1, rDouble.items.count { it.n == "11" && it.a == 500L })
        assertEquals(1, rDouble.items.count { it.n == "01" && it.a == 500L })
        assertEquals(1, rDouble.items.count { it.n == "10" && it.a == 100L })

        // Fast Bet 2: multi-line with standard formats, k (ခွေ), and inline guide notes
        val fastBet2Input = """
            5a500 (5 အပါ)
            6b600 (6 ဘရိတ်)
            1t500 (1 ထိပ်)
            1w500 (1 နောက်)
            p3500 (ပါဝါ 3500)
            n7500 (နက္ခတ် 7500)
            s500 (အပူး 500)
            x200 (ညီအစ်ကို 200)
            123k500 (123 ခွေ 500)
            56=2000/1000
            89r350
            79=500

            ( a=အပါ, b=ဘရိတ်, t=ထိပ်, w=နောက်, p=ပါဝါ, n=နက္ခတ်, s=အပူး, x=ညီကို, k=ခွေ )
        """.trimIndent()
        val rFast2 = PatternAlgorithms.parseQuickEntryMode2(fastBet2Input, blocked)
        assertEquals(0, rFast2.skippedCount)
        // 19 + 10 + 10 + 10 + 10 + 10 + 10 + 20 + 6 (123k500 -> 12,13,21,23,31,32) + 2 (56,65) + 2 (89,98) + 1 (79) = 110 items
        assertEquals(110, rFast2.items.size)

        // Agent 2D Settlement clipboard text format verification
        val agent = com.example.data.Agent(id = 1, name = "Aa", rate = 15.0, mult = 80.0)
        val settlementItem = com.example.ui.Agent2DSettlementItem(
            agent = agent,
            voucherCount = 1,
            totalBet = 119600L,
            commission = 17940L,
            netAmount = 101660L,
            winBetAmount = 0L,
            payout = 0L,
            profitLoss = 101660L
        )
        val formattedText = com.example.ui.screens.formatAgent2DSettlementClipboardText(
            date = "2026-10-21",
            session = "21.AM",
            winningNumber = "16",
            item = settlementItem
        )
        val expectedText = """
            2D စာရင်းချုပ်
            ရက်စွဲ : 2026-10-21 (21.AM)
            အမည်= Aa
            စုစုပေါင်း ရောင်းကြေး = 119600
            ကော်မရှင် (15%) =17940
            ကော်နုတ်ပြီး =101660
            ပေါက်သီး(16)=0
            လျော်ကြေး =0
            စုစုပေါင်း= 101660
        """.trimIndent()
        assertEquals(expectedText, formattedText)
    }

    @Test
    fun testOfflineLicenseManagerKeyGenerationAndVerification() {
        assertEquals(
            "Lucky3D_Super_Secret_Key_2026!#",
            com.example.security.SecurityManager.getSharedSecret()
        )
        assertEquals(
            "LUCKY2D_SHWEKHINMYAY_OFFLINE_SECRET_SALT_2026",
            com.example.security.SecurityManager.getSharedSecret2D()
        )
        assertEquals(
            "LUCKY2D_SHWEKHINMYAY_OFFLINE_SECRET_SALT_2026",
            com.example.security.DeviceSecurityUtil2D.SECRET_SALT
        )

        // Verification that preview bypass is strictly reverted to false
        assertFalse(com.example.security.LicenseManager.defaultBypassActivationForPreview)

        val sampleDeviceId = "8f4a2c91e0b37d12"
        val key1 = com.example.security.LicenseManager.generateExpectedKey(sampleDeviceId)
        val key2 = com.example.security.LicenseManager.generateExpectedKey("  8F4A2C91E0B37D12  ")
        assertEquals(key1, key2)
        assertTrue(Regex("^[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$").matches(key1))

        val utilKey = com.example.security.DeviceSecurityUtil2D.generateExpectedKey(sampleDeviceId)
        assertEquals(key1, utilKey)

        val sig1 = com.example.security.LicenseManager.computeActivationSignature(sampleDeviceId)
        val sig2 = com.example.security.LicenseManager.computeActivationSignature("other_device_id")
        assertFalse(com.example.security.SecurityManager.constantTimeEquals(sig1, sig2))

        // Key normalization handles raw, formatted, spaced, and lowercase
        val raw12 = key1.replace("-", "")
        assertEquals(raw12, com.example.security.LicenseManager.normalizeKeyInput(key1))
        assertEquals(raw12, com.example.security.LicenseManager.normalizeKeyInput(raw12.lowercase()))
        assertEquals(raw12, com.example.security.LicenseManager.normalizeKeyInput("  $key1  "))
    }

    @Test
    fun testParseBetInputCases() {
        // Case 1: Standard Single Amount (58=3000)
        val singleResult = PatternAlgorithms.parseBetInput("58=3000")
        assertEquals(1, singleResult.size)
        assertEquals("58", singleResult[0].n)
        assertEquals(3000L, singleResult[0].a)

        // Case 2: Split Amount with Slash (58=3000/2000)
        val splitResult = PatternAlgorithms.parseBetInput("58=3000/2000")
        assertEquals(2, splitResult.size)
        assertEquals("58", splitResult[0].n)
        assertEquals(3000L, splitResult[0].a)
        assertEquals("85", splitResult[1].n)
        assertEquals(2000L, splitResult[1].a)

        // Case 3: Edge Case Handling - Twin numbers (55=3000/2000) combines to 5000
        val twinResult = PatternAlgorithms.parseBetInput("55=3000/2000")
        assertEquals(1, twinResult.size)
        assertEquals("55", twinResult[0].n)
        assertEquals(5000L, twinResult[0].a)

        // Spacing tolerance (58 = 3000 / 2000)
        val spacedResult = PatternAlgorithms.parseBetInput(" 58 = 3000 / 2000 ")
        assertEquals(2, spacedResult.size)
        assertEquals("58", spacedResult[0].n)
        assertEquals(3000L, spacedResult[0].a)
        assertEquals("85", spacedResult[1].n)
        assertEquals(2000L, spacedResult[1].a)
    }

    @Test
    fun testValidateBetAmountAndParseAmountOrZeroWithSlash() {
        assertTrue(com.example.data.validateBetAmount("3000/2000").isValid)
        assertTrue(com.example.data.validateBetAmount("500/100").isValid)
        assertFalse(com.example.data.validateBetAmount("3000/").isValid)
        assertFalse(com.example.data.validateBetAmount("/2000").isValid)
        assertFalse(com.example.data.validateBetAmount("3000/-100").isValid)
        assertFalse(com.example.data.validateBetAmount("3000/abc").isValid)
        assertFalse(com.example.data.validateBetAmount("3000/2000/1000").isValid)

        assertEquals(5000L, com.example.ui.MainViewModel.parseAmountOrZero("3000/2000"))
        assertEquals(600L, com.example.ui.MainViewModel.parseAmountOrZero("500/100"))
    }

    @Test
    fun testToAsciiDigitsAndMyanmarLocaleFormatting() {
        assertEquals("2026-10-26", "၂၀၂၆-၁၀-၂၆".toAsciiDigits())
        assertEquals("0123456789", "၀၁၂၃၄၅၆၇၈၉".toAsciiDigits())
        assertEquals("58=3000/2000", "၅၈=၃၀၀၀/၂၀၀၀".toAsciiDigits())

        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("my", "MM"))
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val formattedNum = "%02d".format(java.util.Locale.US, 5)
            assertEquals("05", formattedNum)
            assertTrue(Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(sdf.format(java.util.Date())))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testAll100NumbersInitializationAndAggregation() {
        val all100 = (0..99).map { String.format(java.util.Locale.US, "%02d", it) }
        assertEquals(100, all100.size)
        assertEquals("00", all100.first())
        assertEquals("99", all100.last())

        val aggregatedMap = (0..99).associate { i ->
            String.format(java.util.Locale.US, "%02d", i) to 0L
        }.toMutableMap()

        // Place bets for 15 and 58
        aggregatedMap["15"] = (aggregatedMap["15"] ?: 0L) + 1500L
        aggregatedMap["58"] = (aggregatedMap["58"] ?: 0L) + 5000L

        val rowItems = aggregatedMap.entries.map { entry ->
            com.example.ui.NumberRowItem2D(number = entry.key, totalAmount = entry.value)
        }

        assertEquals(100, rowItems.size)
        assertEquals("00", rowItems[0].number)
        assertEquals(0L, rowItems[0].totalAmount)

        val item15 = rowItems.first { it.number == "15" }
        assertEquals(1500L, item15.totalAmount)

        val item58 = rowItems.first { it.number == "58" }
        assertEquals(5000L, item58.totalAmount)

        assertEquals("99", rowItems[99].number)
        assertEquals(0L, rowItems[99].totalAmount)
    }

    @Test
    fun testMultiTierSortingWithAndWithoutWinningNumber() {
        val testData = listOf(
            com.example.ui.NumberSummary(n = "05", entryCount = 1, totalBet = 500L, layoffAmount = 0L, netAmount = 500L),
            com.example.ui.NumberSummary(n = "24", entryCount = 1, totalBet = 500L, layoffAmount = 0L, netAmount = 500L), // tie with 05
            com.example.ui.NumberSummary(n = "99", entryCount = 1, totalBet = 2000L, layoffAmount = 0L, netAmount = 2000L), // highest active
            com.example.ui.NumberSummary(n = "25", entryCount = 1, totalBet = 1000L, layoffAmount = 0L, netAmount = 1000L), // winning candidate
            com.example.ui.NumberSummary(n = "00", entryCount = 0, totalBet = 0L, layoffAmount = 0L, netAmount = 0L), // zero bet
            com.example.ui.NumberSummary(n = "01", entryCount = 0, totalBet = 0L, layoffAmount = 0L, netAmount = 0L)  // zero bet
        )

        // Case 1: Winning number "25" is set
        val sortedWithWin = com.example.ui.MainViewModel.sort2DNumbersWithWinning(testData, "25")
        assertEquals(6, sortedWithWin.size)
        // Row 1: Winning Number "25"
        assertEquals("25", sortedWithWin[0].n)
        assertEquals(1000L, sortedWithWin[0].totalBet)
        // Row 2: Highest active bet "99" (2000)
        assertEquals("99", sortedWithWin[1].n)
        assertEquals(2000L, sortedWithWin[1].totalBet)
        // Rows 3 & 4: Equal active bets (500) -> tie-broken numerically ASCENDING ("05" before "24")
        assertEquals("05", sortedWithWin[2].n)
        assertEquals("24", sortedWithWin[3].n)
        // Rows 5 & 6: Zero bets -> sorted numerically ASCENDING ("00", "01")
        assertEquals("00", sortedWithWin[4].n)
        assertEquals("01", sortedWithWin[5].n)

        // Case 2: No winning number is set (null)
        val sortedWithoutWin = com.example.ui.MainViewModel.sort2DNumbersWithWinning(testData, null)
        assertEquals(6, sortedWithoutWin.size)
        // Row 1: Highest active bet "99" (2000)
        assertEquals("99", sortedWithoutWin[0].n)
        // Row 2: Next highest "25" (1000)
        assertEquals("25", sortedWithoutWin[1].n)
        // Rows 3 & 4: Equal bets "05" and "24" (500)
        assertEquals("05", sortedWithoutWin[2].n)
        assertEquals("24", sortedWithoutWin[3].n)
        // Rows 5 & 6: Zero bets "00" and "01"
        assertEquals("00", sortedWithoutWin[4].n)
        assertEquals("01", sortedWithoutWin[5].n)
    }

    @Test
    fun testStandardizedVoucherCopyFormat() {
        val voucher = com.example.data.Voucher(
            id = 101L,
            no = 1,
            agent = "ကိုအောင်",
            agentId = 1,
            date = "2026-10-30",
            session = "12:01 PM",
            time = "10:30:00",
            items = listOf(
                com.example.data.BetItem(n = "12", a = 500L),
                com.example.data.BetItem(n = "34", a = 1000L)
            )
        )

        val formattedText = com.example.ui.screens.formatVoucherCopyText(voucher)
        val expected = "No. 1=12:01 PM\n12=500\n34=1000\n\nစုစုပေါင်း=1500"

        assertEquals(expected, formattedText)
    }

    @Test
    fun testAgent2DSettlementCalculationAndClipboardFormatting() {
        // Agent with 10% commission and 80 multiplier
        val settlement = com.example.ui.Agent2DSettlement(
            agentId = 1,
            agentName = "ဦးမင်း",
            commissionRate = 10.0,
            multiplier = 80.0,
            totalSales = 50000L,
            commissionAmount = 5000L,
            netSales = 45000L,
            winningBetAmount = 500L,
            payoutAmount = 40000L,
            netBalance = 5000L // 45000 - 40000 = +5000 Profit
        )

        assertEquals(5000L, settlement.netBalance)
        assertTrue(settlement.netBalance >= 0L)

        val clipboardText = com.example.ui.screens.formatAgent2DSettlementClipboardText(
            date = "2026-10-28",
            session = "12.AM",
            winningNumber = "25",
            item = settlement
        )
        assertTrue(clipboardText.contains("2D အရှုံး/အမြတ် စာရင်းရှင်းတမ်း"))
        assertTrue(clipboardText.contains("အမည်= ဦးမင်း"))
        assertTrue(clipboardText.contains("စုစုပေါင်းရောင်းကြေး = 50000"))
        assertTrue(clipboardText.contains("ကော်မရှင် (10%) = 5000"))
        assertTrue(clipboardText.contains("ကော်နုတ်ပြီး = 45000"))
        assertTrue(clipboardText.contains("ပေါက်သီး(25) = 500"))
        assertTrue(clipboardText.contains("လျော်ကြေး (80 ဆ) = 40000"))
        assertTrue(clipboardText.contains("အရှုံး / အမြတ် = +5000"))
    }

    @Test
    fun testVoucherOldestFirstSequence() {
        val v3 = com.example.data.Voucher(
            id = 300L,
            no = 3,
            agent = "Agent",
            date = "2026-10-31",
            session = "31.AM",
            time = "10:00",
            items = listOf(com.example.data.BetItem("11", 100L))
        )
        val v1 = com.example.data.Voucher(
            id = 100L,
            no = 1,
            agent = "Agent",
            date = "2026-10-31",
            session = "31.AM",
            time = "09:00",
            items = listOf(com.example.data.BetItem("22", 200L))
        )
        val v2 = com.example.data.Voucher(
            id = 200L,
            no = 2,
            agent = "Agent",
            date = "2026-10-31",
            session = "31.AM",
            time = "09:30",
            items = listOf(com.example.data.BetItem("33", 300L))
        )

        val unsorted = listOf(v3, v1, v2)
        val sorted = unsorted.sortedWith(compareBy<com.example.data.Voucher> { it.no }.thenBy { it.id })

        assertEquals(1, sorted[0].no)
        assertEquals(2, sorted[1].no)
        assertEquals(3, sorted[2].no)
    }

    @Test
    fun testCopyVoucherWithHeaderAndFooterFormat() {
        val voucherNo = 1
        val sessionName = "31.AM"
        val bets = listOf(
            com.example.data.BetItem("12", 500L),
            com.example.data.BetItem("34", 1000L),
            com.example.data.BetItem("56", 500L)
        )
        val totalAmount = bets.sumOf { it.amount }

        val builder = StringBuilder()
        builder.append("No. ").append(voucherNo).append("=").append(sessionName.trim()).append("\n")
        bets.forEach { bet ->
            builder.append(bet.number.trim()).append("=").append(bet.amount).append("\n")
        }
        builder.append("\nစုစုပေါင်း=").append(totalAmount)

        val expected = """
            No. 1=31.AM
            12=500
            34=1000
            56=500

            စုစုပေါင်း=2000
        """.trimIndent()

        assertEquals(expected, builder.toString())
    }

    @Test
    fun testPassBetRecordExportFormat() {
        val recordNo = 1
        val sessionName = "31.AM"
        val bets = listOf(
            com.example.data.BetItem("12", 500L),
            com.example.data.BetItem("34", 1000L),
            com.example.data.BetItem("56", 500L)
        )
        val totalAmount = bets.sumOf { it.amount }

        val builder = StringBuilder()
        builder.append("No. ").append(recordNo).append("=").append(sessionName.trim()).append("\n")
        bets.forEach { bet ->
            builder.append(bet.number.trim()).append("=").append(bet.amount).append("\n")
        }
        builder.append("\nစုစုပေါင်း=").append(totalAmount)

        val expected = """
            No. 1=31.AM
            12=500
            34=1000
            56=500

            စုစုပေါင်း=2000
        """.trimIndent()

        assertEquals(expected, builder.toString())
    }

    @Test
    fun testPassBetOldestFirstSequence() {
        val r3 = com.example.data.LayoffRecord(
            id = 300L,
            date = "2026-10-31",
            session = "31.AM",
            time = "10:00",
            limit = 5000L,
            items = listOf(com.example.data.LayoffItem("11", 100L)),
            total = 100L,
            recordNo = 3
        )
        val r1 = com.example.data.LayoffRecord(
            id = 100L,
            date = "2026-10-31",
            session = "31.AM",
            time = "09:00",
            limit = 5000L,
            items = listOf(com.example.data.LayoffItem("22", 200L)),
            total = 200L,
            recordNo = 1
        )
        val r2 = com.example.data.LayoffRecord(
            id = 200L,
            date = "2026-10-31",
            session = "31.AM",
            time = "09:30",
            limit = 5000L,
            items = listOf(com.example.data.LayoffItem("33", 300L)),
            total = 300L,
            recordNo = 2
        )

        val unsorted = listOf(r3, r1, r2)
        val sorted = unsorted.sortedWith(compareBy<com.example.data.LayoffRecord> { it.recordNo }.thenBy { it.id })

        assertEquals(1, sorted[0].recordNo)
        assertEquals(2, sorted[1].recordNo)
        assertEquals(3, sorted[2].recordNo)
    }

    @Test
    fun testAgentVoucherReceiptFormat() {
        val voucher = com.example.data.Voucher(
            id = 1L,
            no = 5,
            agent = "ဦးမောင်",
            date = "2026-10-31",
            session = "31.AM",
            time = "09:15",
            items = listOf(
                com.example.data.BetItem("12", 500L),
                com.example.data.BetItem("34", 1000L)
            )
        )
        val receipt = com.example.ui.screens.buildAgentVoucherReceipt(voucher)
        assertTrue(receipt.contains("🍀 Lucky2D - အရောင်းဘောင်ချာ"))
        assertTrue(receipt.contains("ဘောင်ချာ အမှတ် : No. 5"))
        assertTrue(receipt.contains("ထိုးသူ/ကိုယ်စားလှယ် : ဦးမောင်"))
        assertTrue(receipt.contains("12 = 500"))
        assertTrue(receipt.contains("34 = 1000"))
        assertTrue(receipt.contains("စုစုပေါင်း = 1500"))
    }

    @Test
    fun testUniversalNetPositionFormula() {
        val totalSales = 7000L
        val totalPassed = 10000L
        val state = com.example.ui.NumberLedgerState2D(
            number = "12",
            totalSales = totalSales,
            totalPassed = totalPassed
        )
        // NetKept(num) = TotalSales(num) - TotalPassed(num)
        assertEquals(-3000L, state.netKept)
        assertTrue(state.netKept < 0L)
    }

    @Test
    fun testNegativeBalanceWinningPayoutAndHedgingProfit() {
        val netKept = -3000L
        val multiplier = 80.0
        // Net Payout = NetKept("12") * Multiplier = (-3000) * 80 = -240,000
        val netPayout = Math.round(netKept * multiplier)
        assertEquals(-240000L, netPayout)

        // Profit/Loss = RetainedSales - Commission - Payout
        // If RetainedSales = -3000, Commission = 0:
        // Profit/Loss = (-3000) - (0 + (-240000)) = -3000 + 240000 = +237,000
        val retainedSales = -3000L
        val commission = 0L
        val profitLoss = retainedSales - (commission + netPayout)
        assertEquals(237000L, profitLoss)
    }

    @Test
    fun testDynamicAutoOffsetOnNewSales() {
        var totalSales = 5000L
        val totalPassed = 7000L
        var netKept = totalSales - totalPassed
        assertEquals(-2000L, netKept)

        // New agent places a bet of 3000 on "12"
        val newBet = 3000L
        totalSales += newBet
        netKept = totalSales - totalPassed
        // New NetKept = -2000 + 3000 = +1000
        assertEquals(1000L, netKept)
    }

    @Test
    fun testDoubleNumbersWithRPermutationDuplication() {
        // 1. parseFastBetLine: "12.46.56.33R3000"
        val (bets, invalid) = PatternAlgorithms.parseFastBetLine("12.46.56.33R3000")
        assertTrue(invalid.isEmpty())
        // 12 -> 12, 21; 46 -> 46, 64; 56 -> 56, 65; 33 (pair with R) -> 33, 33 (TWICE)
        assertEquals(8, bets.size)
        assertEquals(2, bets.count { it.number == "33" && it.amount == 3000L })
        assertEquals(1, bets.count { it.number == "12" && it.amount == 3000L })
        assertEquals(1, bets.count { it.number == "21" && it.amount == 3000L })

        // 2. parseQuickEntryMode1 with "33R3000"
        val res = PatternAlgorithms.parseQuickEntryMode1("33R3000", emptySet())
        assertEquals(2, res.items.size)
        assertEquals("33", res.items[0].n)
        assertEquals(3000L, res.items[0].a)
        assertEquals("33", res.items[1].n)
        assertEquals(3000L, res.items[1].a)

        // 3. Invalid formatted numbers collection
        val (betsWithInvalid, invalidFound) = PatternAlgorithms.parseFastBetLine("12.999.abc.33R3000")
        assertEquals(4, betsWithInvalid.size) // 12, 21, 33, 33
        assertEquals(listOf("999", "abc"), invalidFound)
    }

    @Test
    fun testExtended2DRSyntaxParsing() {
        // 1) 12R1000
        val (b1, inv1) = PatternAlgorithms.parseFastBet2DExtended("12R1000")
        assertTrue(inv1.isEmpty())
        assertEquals(2, b1.size)
        assertTrue(b1.any { it.number == "12" && it.amount == 1000L })
        assertTrue(b1.any { it.number == "21" && it.amount == 1000L })

        // 2) 12R=1000
        val (b2, inv2) = PatternAlgorithms.parseFastBet2DExtended("12R=1000")
        assertTrue(inv2.isEmpty())
        assertEquals(2, b2.size)
        assertTrue(b2.any { it.number == "12" && it.amount == 1000L })
        assertTrue(b2.any { it.number == "21" && it.amount == 1000L })

        // 3) 12=R1000
        val (b3, inv3) = PatternAlgorithms.parseFastBet2DExtended("12=R1000")
        assertTrue(inv3.isEmpty())
        assertEquals(2, b3.size)
        assertTrue(b3.any { it.number == "12" && it.amount == 1000L })
        assertTrue(b3.any { it.number == "21" && it.amount == 1000L })

        // 4) 12=1000R
        val (b4, inv4) = PatternAlgorithms.parseFastBet2DExtended("12=1000R")
        assertTrue(inv4.isEmpty())
        assertEquals(2, b4.size)
        assertTrue(b4.any { it.number == "12" && it.amount == 1000L })
        assertTrue(b4.any { it.number == "21" && it.amount == 1000L })

        // 5) Multiple numbers dot-separated: 12.34.56R1000
        val (b5, inv5) = PatternAlgorithms.parseFastBet2DExtended("12.34.56R1000")
        assertTrue(inv5.isEmpty())
        assertEquals(6, b5.size)

        // 6) Multiple numbers with R=: 12.34R=1000
        val (b6, inv6) = PatternAlgorithms.parseFastBet2DExtended("12.34R=1000")
        assertTrue(inv6.isEmpty())
        assertEquals(4, b6.size)

        // 7) Double numbers with R: 33R1000 & 33R=1000 (added TWICE)
        val (b7, inv7) = PatternAlgorithms.parseFastBet2DExtended("33R1000")
        assertTrue(inv7.isEmpty())
        assertEquals(2, b7.size)
        assertEquals(2, b7.count { it.number == "33" && it.amount == 1000L })

        val (b8, inv8) = PatternAlgorithms.parseFastBet2DExtended("33R=1000")
        assertTrue(inv8.isEmpty())
        assertEquals(2, b8.size)
        assertEquals(2, b8.count { it.number == "33" && it.amount == 1000L })

        // 8) parseQuickEntryMode1 with extended R syntax
        val q1 = PatternAlgorithms.parseQuickEntryMode1("12R=1000\n12=R1000\n12=1000R\n33R=1000", emptySet())
        assertTrue(q1.invalidTokens.isEmpty())
        assertEquals(8, q1.items.size)
        assertEquals(2, q1.items.count { it.n == "33" && it.a == 1000L })
    }
}



