package com.lucky.lucky2d.logic

import com.lucky.lucky2d.model.BetAmountValidation
import com.lucky.lucky2d.model.MAX_BET_AMOUNT
import com.lucky.lucky2d.model.ParseResult
import com.lucky.lucky2d.model.ParsedBet

object PatternAlgorithms {

    val POWER_NUMBERS = listOf(
        "05", "50", "16", "61", "27", "72", "38", "83", "49", "94"
    )

    val NAKHAT_NUMBERS = listOf(
        "07", "70", "18", "81", "24", "42", "35", "53", "69", "96"
    )

    val BROTHER_NUMBERS = listOf(
        "01", "10", "12", "21", "23", "32", "34", "43", "45", "54",
        "56", "65", "67", "76", "78", "87", "89", "98", "90", "09"
    )

    val DOUBLES = listOf(
        "00", "11", "22", "33", "44", "55", "66", "77", "88", "99"
    )

    private const val MYANMAR_DIGITS = "၀၁၂၃၄၅၆၇၈၉"
    private const val ASCII_DIGITS = "0123456789"

    private val DIGITS_ONLY_REGEX = Regex("^\\d+$")
    private val PAREN_COMMENT_REGEX = Regex("\\([^)]*\\)")
    private val DIVIDER_LINE_REGEX = Regex("^[=\\-\\s]+$")
    private val SPACE_AROUND_EQ_SLASH_REGEX = Regex("\\s*([=/])\\s*")
    private val SPLIT_BET_REGEX = Regex("^(\\d{2})=(\\d+)/(\\d+)$")
    private val SINGLE_BET_REGEX = Regex("^(\\d{2})=(\\d+)$")

    private val EQ_R_SPLIT_REGEX = Regex("([=])(\\d+)[rR](\\d+)")
    private val DASH_TO_EQ_REGEX = Regex("((?:\\b\\d{2}[.,\\s]+)*\\b\\d{2})[-]+(?=\\d)")
    private val DOT_BEFORE_SPLIT_REGEX = Regex("((?:\\b\\d{2}[.,\\s]+)*\\b\\d{2})\\.(?=\\d+/\\d+)")
    private val SPACE_BEFORE_SPLIT_REGEX = Regex("((?:\\b\\d{2}[.,\\s]+)*\\b\\d{2})\\s+(?=\\d+/\\d+)")
    private val SPACE_BETWEEN_NUMS_REGEX = Regex("(\\b\\d{2})[\\s,]+(?=\\d{2}(?:[.\\s,]+\\d{2})*\\s*[=rR])")
    private val SPACE_AROUND_OPS_REGEX = Regex("\\s*([=rR/])\\s*")
    private val R_EQ_REGEX = Regex("([0-9.,\\s]+)[rR]=\\s*([0-9]+)")
    private val EQ_R_REGEX = Regex("([0-9.,\\s]+)=\\s*[rR]([0-9]+)")
    private val EQ_AMT_R_REGEX = Regex("([0-9.,\\s]+)=\\s*([0-9]+)[rR]")
    private val TOKEN_SPLIT_REGEX = Regex("[\\s;|]+|,(?=\\s|$|[0-9]{1,2}[.=rR]|[0-9][abtwABTW]|[pnsxPNSX][0-9])")
    private val NUM_SPLIT_REGEX = Regex("[.,\\s]+")
    private val DIGIT_LETTER_AMT_REGEX = Regex("^(\\d)([abtw])(\\d+)$")
    private val KWAY_AMT_REGEX = Regex("^(\\d{2,9})k(\\d+)$")
    private val LETTER_AMT_REGEX = Regex("^(?:\\d)?([pnsx])(\\d+)$")

    fun toAsciiDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = MYANMAR_DIGITS.indexOf(ch)
            sb.append(if (idx != -1) ASCII_DIGITS[idx] else ch)
        }
        return sb.toString()
    }

    fun validateBetAmount(rawInput: String?): BetAmountValidation {
        if (rawInput == null) return BetAmountValidation(false, "Empty")
        val trimmed = toAsciiDigits(rawInput).trim()
        if (trimmed.isEmpty()) return BetAmountValidation(false, "Empty")

        if (trimmed.contains('/')) {
            val parts = trimmed.split('/')
            if (parts.size != 2) return BetAmountValidation(false, "NonNumeric")
            val p1 = parts[0].trim()
            val p2 = parts[1].trim()
            if (p1.isEmpty() || p2.isEmpty()) return BetAmountValidation(false, "Empty")
            val v1 = validateBetAmount(p1)
            if (!v1.isValid) return v1
            val v2 = validateBetAmount(p2)
            if (!v2.isValid) return v2
            return BetAmountValidation(true)
        }

        val isNegative = trimmed.startsWith('-')
        val digitsPart = if (isNegative) trimmed.substring(1) else trimmed
        if (!DIGITS_ONLY_REGEX.matches(digitsPart)) {
            return BetAmountValidation(false, "NonNumeric")
        }
        if (isNegative) {
            return BetAmountValidation(false, "ZeroOrNegative")
        }
        val parsed = trimmed.toLongOrNull() ?: return BetAmountValidation(false, "TooLarge")
        if (parsed <= 0L) {
            return BetAmountValidation(false, "ZeroOrNegative")
        }
        if (parsed > MAX_BET_AMOUNT.toLong()) {
            return BetAmountValidation(false, "TooLarge")
        }
        return BetAmountValidation(true)
    }

    fun reverse2D(n: String): String {
        return if (n.length == 2) "${n[1]}${n[0]}" else n
    }

    fun getEvenEven(): List<String> {
        val evens = listOf('0', '2', '4', '6', '8')
        return buildList {
            for (d1 in evens) for (d2 in evens) add("$d1$d2")
        }
    }

    fun getOddOdd(): List<String> {
        val odds = listOf('1', '3', '5', '7', '9')
        return buildList {
            for (d1 in odds) for (d2 in odds) add("$d1$d2")
        }
    }

    fun getEvenOdd(): List<String> {
        val evens = listOf('0', '2', '4', '6', '8')
        val odds = listOf('1', '3', '5', '7', '9')
        return buildList {
            for (d1 in evens) for (d2 in odds) add("$d1$d2")
        }
    }

    fun getOddEven(): List<String> {
        val odds = listOf('1', '3', '5', '7', '9')
        val evens = listOf('0', '2', '4', '6', '8')
        return buildList {
            for (d1 in odds) for (d2 in evens) add("$d1$d2")
        }
    }

    fun getHead(d: String): List<String> {
        val ch = d.firstOrNull() ?: '0'
        return (0..9).map { "$ch$it" }
    }

    fun getTail(d: String): List<String> {
        val ch = d.firstOrNull() ?: '0'
        return (0..9).map { "$it$ch" }
    }

    fun getIncludes(d: String): List<String> {
        val ch = d.firstOrNull() ?: '0'
        val set = LinkedHashSet<String>()
        for (i in 0..9) {
            set.add("$ch$i")
            set.add("$i$ch")
        }
        return set.toList()
    }

    fun getBrake(brake: Int): List<String> {
        val target = ((brake % 10) + 10) % 10
        return buildList {
            for (i in 0..9) {
                for (j in 0..9) {
                    if ((i + j) % 10 == target) {
                        add("$i$j")
                    }
                }
            }
        }
    }

    fun getKway(digitsStr: String, withDoubles: Boolean): List<String> {
        val distinct = digitsStr.filter { it.isDigit() }.toList().distinct()
        if (distinct.size < 2) return emptyList()
        return buildList {
            for (i in distinct.indices) {
                for (j in distinct.indices) {
                    if (i != j || withDoubles) {
                        add("${distinct[i]}${distinct[j]}")
                    }
                }
            }
        }
    }

    fun generateNumbersForPattern(
        pattern: String,
        inputNumber: String,
        withReverse: Boolean,
        withDoublesForKway: Boolean = false
    ): List<String> {
        val cleanInput = toAsciiDigits(inputNumber)
        val baseList = when (pattern) {
            "ဒဲ့" -> if (cleanInput.length == 2) listOf(cleanInput) else emptyList()
            "ပါဝါ" -> POWER_NUMBERS
            "နက္ခတ်" -> NAKHAT_NUMBERS
            "ညီအစ်ကို" -> BROTHER_NUMBERS
            "အပူး" -> DOUBLES
            "စုံစုံ" -> getEvenEven()
            "မမ" -> getOddOdd()
            "စုံမ" -> getEvenOdd()
            "မစုံ" -> getOddEven()
            "ထိပ်" -> getHead(cleanInput.take(1).ifEmpty { "0" })
            "နောက်" -> getTail(cleanInput.take(1).ifEmpty { "0" })
            "အပါ" -> getIncludes(cleanInput.take(1).ifEmpty { "0" })
            "ဘရိတ်" -> getBrake(cleanInput.take(1).toIntOrNull() ?: 0)
            "ခွေ" -> getKway(cleanInput, withDoublesForKway)
            else -> if (cleanInput.length == 2) listOf(cleanInput) else emptyList()
        }

        if (!withReverse) return baseList

        val set = LinkedHashSet<String>()
        for (n in baseList) {
            set.add(n)
            set.add(reverse2D(n))
        }
        return set.toList()
    }

    fun parseBetInput(input: String): List<ParsedBet> {
        val trimmed = SPACE_AROUND_EQ_SLASH_REGEX.replace(toAsciiDigits(input).trim()) { it.groupValues[1] }
        if (trimmed.isEmpty()) return emptyList()

        SPLIT_BET_REGEX.matchEntire(trimmed)?.let { m ->
            val baseNumber = m.groupValues[1]
            val directAmount = m.groupValues[2].toIntOrNull() ?: 0
            val reverseAmount = m.groupValues[3].toIntOrNull() ?: 0
            val reverseNumber = reverse2D(baseNumber)
            return if (baseNumber == reverseNumber) {
                listOf(ParsedBet(baseNumber, directAmount + reverseAmount))
            } else {
                buildList {
                    add(ParsedBet(baseNumber, directAmount))
                    if (reverseAmount > 0) add(ParsedBet(reverseNumber, reverseAmount))
                }
            }
        }

        SINGLE_BET_REGEX.matchEntire(trimmed)?.let { m ->
            val baseNumber = m.groupValues[1]
            val amount = m.groupValues[2].toIntOrNull() ?: 0
            return listOf(ParsedBet(baseNumber, amount))
        }

        return emptyList()
    }

    private fun stripCommentAndGuideLines(text: String): String {
        return text.lines()
            .map { PAREN_COMMENT_REGEX.replace(it, "").trim() }
            .filter { line ->
                line.isNotEmpty() &&
                    !line.startsWith("(") &&
                    !line.endsWith(")") &&
                    !DIVIDER_LINE_REGEX.matches(line)
            }
            .joinToString("\n")
    }

    private fun normalizeQuickLines(text: String): List<String> {
        val cleaned = stripCommentAndGuideLines(toAsciiDigits(text))
            .replace("ဒဲ့ကျန်", "/")
            .replace("ဒဲ့", "=")
            .replace("ကျန်", "/")
            .replace("/r", "/")
            .replace("/R", "/")

        if (cleaned.isBlank()) return emptyList()

        return cleaned.lines().map { rawLine ->
            var l = rawLine.trim()
            l = EQ_R_SPLIT_REGEX.replace(l) { "${it.groupValues[1]}${it.groupValues[2]}/${it.groupValues[3]}" }
            l = DASH_TO_EQ_REGEX.replace(l) { "${it.groupValues[1]}=" }
            l = DOT_BEFORE_SPLIT_REGEX.replace(l) { "${it.groupValues[1]}=" }
            l = SPACE_BEFORE_SPLIT_REGEX.replace(l) { "${it.groupValues[1]}=" }
            l = SPACE_BETWEEN_NUMS_REGEX.replace(l) { "${it.groupValues[1]}." }
            l = SPACE_AROUND_OPS_REGEX.replace(l) { it.groupValues[1] }
            l = R_EQ_REGEX.replace(l) { "${it.groupValues[1]}R${it.groupValues[2]}" }
            l = EQ_R_REGEX.replace(l) { "${it.groupValues[1]}R${it.groupValues[2]}" }
            l = EQ_AMT_R_REGEX.replace(l) { "${it.groupValues[1]}R${it.groupValues[2]}" }
            l
        }
    }

    fun parseQuickEntryMode1(
        text: String,
        blocked: Set<String>,
        maxBet: Int = MAX_BET_AMOUNT
    ): ParseResult {
        val items = mutableListOf<ParsedBet>()
        var skipped = 0
        val blockedList = mutableListOf<String>()
        val invalidTokens = mutableListOf<String>()

        val normalizedLines = normalizeQuickLines(text)
        if (normalizedLines.isEmpty()) {
            return ParseResult(items, skipped, blockedList, invalidTokens)
        }

        val tokens = normalizedLines
            .flatMap { it.split(TOKEN_SPLIT_REGEX) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (token in tokens) {
            try {
                // Pattern 3: Equal and slash (e.g., 01.78=500/100 or 56=2000/1000)
                if (token.contains('=') && token.contains('/')) {
                    val eqParts = token.split('=')
                    if (eqParts.size == 2) {
                        val numPart = eqParts[0]
                        val amtParts = eqParts[1].split('/')
                        val directRaw = amtParts.getOrNull(0)
                        val reverseRaw = amtParts.getOrNull(1)
                        val directVal = validateBetAmount(directRaw)
                        val amtDirect = directRaw?.trim()?.toIntOrNull() ?: 0
                        val amtReverse = reverseRaw?.trim()?.toIntOrNull() ?: -1
                        val reverseValid = amtReverse == 0 ||
                            (validateBetAmount(reverseRaw).isValid && amtReverse <= maxBet)

                        val rawNumbers = numPart.split(NUM_SPLIT_REGEX).filter { it.isNotEmpty() }
                        val validNumbers = mutableListOf<String>()
                        for (raw in rawNumbers) {
                            if (raw.length == 2 && DIGITS_ONLY_REGEX.matches(raw)) {
                                validNumbers.add(raw)
                            } else {
                                invalidTokens.add(raw)
                            }
                        }

                        if (validNumbers.isEmpty() || !directVal.isValid || amtDirect > maxBet || !reverseValid) {
                            skipped++
                        } else {
                            for (n in validNumbers) {
                                if (blocked.contains(n)) blockedList.add(n)
                                else items.add(ParsedBet(n, amtDirect))

                                val rev = reverse2D(n)
                                if (rev == n) {
                                    if (validNumbers.size == 1 && amtReverse > 0) {
                                        if (blocked.contains(n)) blockedList.add(n)
                                        else items.add(ParsedBet(n, amtReverse))
                                    }
                                } else if (amtReverse > 0) {
                                    if (blocked.contains(rev)) blockedList.add(rev)
                                    else items.add(ParsedBet(rev, amtReverse))
                                }
                            }
                        }
                        continue
                    }
                }

                // Pattern 2: R notation (e.g., 01.78R500 or 33R3000)
                val rIndex = token.indexOfFirst { it == 'r' || it == 'R' }
                if (rIndex != -1) {
                    val numPart = token.substring(0, rIndex)
                    val amtPart = token.substring(rIndex + 1)
                    val amtVal = validateBetAmount(amtPart)
                    val amount = amtPart.trim().toIntOrNull() ?: 0
                    val rawNumbers = numPart.split(NUM_SPLIT_REGEX).filter { it.isNotEmpty() }
                    val validNumbers = mutableListOf<String>()
                    for (raw in rawNumbers) {
                        if (raw.length == 2 && DIGITS_ONLY_REGEX.matches(raw)) {
                            validNumbers.add(raw)
                        } else {
                            invalidTokens.add(raw)
                        }
                    }

                    if (validNumbers.isEmpty() || !amtVal.isValid || amount > maxBet) {
                        skipped++
                    } else {
                        for (n in validNumbers) {
                            val rev = reverse2D(n)
                            val listToBet = if (n == rev) listOf(n, n) else listOf(n, rev)
                            for (num in listToBet) {
                                if (blocked.contains(num)) blockedList.add(num)
                                else items.add(ParsedBet(num, amount))
                            }
                        }
                    }
                    continue
                }

                // Pattern 1: Same amount with '=' (e.g., 01.78=500)
                if (token.contains('=')) {
                    val parts = token.split('=')
                    if (parts.size == 2) {
                        val numPart = parts[0]
                        val amtVal = validateBetAmount(parts[1])
                        val amount = parts[1].trim().toIntOrNull() ?: 0
                        val rawNumbers = numPart.split(NUM_SPLIT_REGEX).filter { it.isNotEmpty() }
                        val validNumbers = mutableListOf<String>()
                        for (raw in rawNumbers) {
                            if (raw.length == 2 && DIGITS_ONLY_REGEX.matches(raw)) {
                                validNumbers.add(raw)
                            } else {
                                invalidTokens.add(raw)
                            }
                        }
                        if (validNumbers.isEmpty() || !amtVal.isValid || amount > maxBet) {
                            skipped++
                        } else {
                            for (n in validNumbers) {
                                if (blocked.contains(n)) blockedList.add(n)
                                else items.add(ParsedBet(n, amount))
                            }
                        }
                        continue
                    }
                }

                invalidTokens.add(token)
                skipped++
            } catch (_: Exception) {
                invalidTokens.add(token)
                skipped++
            }
        }

        return ParseResult(items, skipped, blockedList, invalidTokens)
    }

    fun parseQuickEntryMode2(
        text: String,
        blocked: Set<String>,
        maxBet: Int = MAX_BET_AMOUNT
    ): ParseResult {
        val items = mutableListOf<ParsedBet>()
        var skipped = 0
        val blockedList = mutableListOf<String>()
        val invalidTokens = mutableListOf<String>()

        val normalizedLines = normalizeQuickLines(text)
        if (normalizedLines.isEmpty()) {
            return ParseResult(items, skipped, blockedList, invalidTokens)
        }

        val tokens = normalizedLines
            .flatMap { it.split(TOKEN_SPLIT_REGEX) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (rawToken in tokens) {
            if (rawToken.startsWith("(") || rawToken.endsWith(")")) continue
            val token = rawToken.lowercase().trim()

            try {
                // Standard formats also supported in Fast Bet 2
                if (token.contains('=') || (token.contains('r') && token.firstOrNull()?.isDigit() == true)) {
                    val res = parseQuickEntryMode1(token, blocked, maxBet)
                    items.addAll(res.items)
                    skipped += res.skippedCount
                    blockedList.addAll(res.blockedSkipped)
                    invalidTokens.addAll(res.invalidTokens)
                    continue
                }

                // Digit + letter + amount: a (အပါ), b (ဘရိတ်), t (ထိပ်), w (နောက်)
                DIGIT_LETTER_AMT_REGEX.matchEntire(token)?.let { m ->
                    val digitChar = m.groupValues[1]
                    val letter = m.groupValues[2]
                    val rawAmt = m.groupValues[3]
                    val amtVal = validateBetAmount(rawAmt)
                    val amount = rawAmt.toIntOrNull() ?: 0

                    if (!amtVal.isValid || amount > maxBet) {
                        skipped++
                        invalidTokens.add(rawToken)
                    } else {
                        val nums = when (letter) {
                            "a" -> getIncludes(digitChar)
                            "b" -> getBrake(digitChar.toIntOrNull() ?: 0)
                            "t" -> getHead(digitChar)
                            "w" -> getTail(digitChar)
                            else -> emptyList()
                        }
                        for (n in nums) {
                            if (blocked.contains(n)) blockedList.add(n)
                            else items.add(ParsedBet(n, amount))
                        }
                    }
                    continue
                }

                // Digits (2..9) + k (ခွေ) + amount: e.g., 123k500
                KWAY_AMT_REGEX.matchEntire(token)?.let { m ->
                    val digitsStr = m.groupValues[1]
                    val rawAmt = m.groupValues[2]
                    val amtVal = validateBetAmount(rawAmt)
                    val amount = rawAmt.toIntOrNull() ?: 0
                    val nums = getKway(digitsStr, false)

                    if (!amtVal.isValid || amount > maxBet || nums.isEmpty()) {
                        skipped++
                        invalidTokens.add(rawToken)
                    } else {
                        for (n in nums) {
                            if (blocked.contains(n)) blockedList.add(n)
                            else items.add(ParsedBet(n, amount))
                        }
                    }
                    continue
                }

                // Letter + amount: p (ပါဝါ), n (နက္ခတ်), s (အပူး), x (ညီအစ်ကို)
                LETTER_AMT_REGEX.matchEntire(token)?.let { m ->
                    val letter = m.groupValues[1]
                    val rawAmt = m.groupValues[2]
                    val amtVal = validateBetAmount(rawAmt)
                    val amount = rawAmt.toIntOrNull() ?: 0

                    if (!amtVal.isValid || amount > maxBet) {
                        skipped++
                        invalidTokens.add(rawToken)
                    } else {
                        val nums = when (letter) {
                            "p" -> POWER_NUMBERS
                            "n" -> NAKHAT_NUMBERS
                            "s" -> DOUBLES
                            "x" -> BROTHER_NUMBERS
                            else -> emptyList()
                        }
                        for (n in nums) {
                            if (blocked.contains(n)) blockedList.add(n)
                            else items.add(ParsedBet(n, amount))
                        }
                    }
                    continue
                }

                invalidTokens.add(rawToken)
                skipped++
            } catch (_: Exception) {
                invalidTokens.add(rawToken)
                skipped++
            }
        }

        return ParseResult(items, skipped, blockedList, invalidTokens)
    }

    fun generateSorted2DLedger(currentSalesMap: Map<String, Int>): List<ParsedBet> {
        val allNumbers = (0..99).map { i ->
            val numStr = i.toString().padStart(2, '0')
            ParsedBet(numStr, currentSalesMap[numStr] ?: 0)
        }
        val activeBets = allNumbers.filter { it.a > 0 }
            .sortedWith(compareByDescending<ParsedBet> { it.a }.thenBy { it.n.toInt() })
        val zeroBets = allNumbers.filter { it.a == 0 }
            .sortedBy { it.n.toInt() }
        return activeBets + zeroBets
    }
}
