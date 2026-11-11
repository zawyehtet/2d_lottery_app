package com.example.logic

import com.example.data.toAsciiDigits

data class ParsedBetItem(
    val number: String,
    val amount: Long
)

/**
 * FastBetParser2D:
 * Dedicated parser for Fast Bet (အမြန်ထိုး 1 & 2) inputs supporting:
 *  - Twin numbers ("00", "11", ... "99") with split amounts ("00=2000/1000", "00=2000ဒဲ့ကျန်1000",
 *    "00=2000/R1000", "00=2000R1000", "00-2000/1000", "00.2000/1000", "00 2000/1000"):
 *    BOTH amounts are captured (e.g. 00=2000 and 00=1000).
 *  - Non-twin numbers with split amounts ("12=2000/1000"):
 *    amount1 is mapped to direct ("12"), amount2 is mapped to reverse ("21").
 *  - Single amounts ("12=1000", "00=1000").
 *  - Same amount with R ("12R1000", "00R1000").
 */
object FastBetParser2D {

    fun parseInputText(rawText: String): List<ParsedBetItem> {
        val result = mutableListOf<ParsedBetItem>()
        if (rawText.isBlank()) return result

        val cleaned = normalizeFastBetText(rawText)
        val lines = cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }

        for (line in lines) {
            val tokens = line.split(Regex("[\\s;|]+")).map { it.trim() }.filter { it.isNotEmpty() }
            for (token in tokens) {
                // Split amounts: token contains '=' and '/'
                if (token.contains("=") && token.contains("/")) {
                    val eqParts = token.split("=")
                    if (eqParts.size == 2) {
                        val numPart = eqParts[0]
                        val amtParts = eqParts[1].split("/")
                        val amtDirect = amtParts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                        val amtReverse = amtParts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                        val numbers = extractNumbers(numPart)

                        for (num in numbers) {
                            val rev = reverse2D(num)
                            if (amtDirect > 0L) {
                                result.add(ParsedBetItem(num, amtDirect))
                            }
                            if (amtReverse > 0L) {
                                if (num == rev) {
                                    // Twin number: capture both amounts for the twin number!
                                    result.add(ParsedBetItem(num, amtReverse))
                                } else {
                                    result.add(ParsedBetItem(rev, amtReverse))
                                }
                            }
                        }
                        continue
                    }
                }

                // R notation: token contains 'r' / 'R'
                if (token.contains("r", ignoreCase = true)) {
                    val rIndex = token.indexOfFirst { it.equals('r', ignoreCase = true) }
                    val numPart = token.substring(0, rIndex)
                    val amtPart = token.substring(rIndex + 1)
                    val amount = amtPart.trim().toLongOrNull() ?: 0L
                    val numbers = extractNumbers(numPart)

                    if (amount > 0L) {
                        for (num in numbers) {
                            val rev = reverse2D(num)
                            result.add(ParsedBetItem(num, amount))
                            if (num == rev) {
                                result.add(ParsedBetItem(num, amount))
                            } else {
                                result.add(ParsedBetItem(rev, amount))
                            }
                        }
                    }
                    continue
                }

                // Standard single amount: token contains '='
                if (token.contains("=")) {
                    val parts = token.split("=")
                    if (parts.size == 2) {
                        val numPart = parts[0]
                        val amount = parts[1].trim().toLongOrNull() ?: 0L
                        val numbers = extractNumbers(numPart)
                        if (amount > 0L) {
                            for (num in numbers) {
                                result.add(ParsedBetItem(num, amount))
                            }
                        }
                        continue
                    }
                }
            }
        }

        return result
    }

    private fun reverse2D(num: String): String =
        if (num.length == 2) "${num[1]}${num[0]}" else num

    private fun extractNumbers(numPart: String): List<String> {
        val raw = numPart.split(Regex("[.,\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
        val valid = mutableListOf<String>()
        for (item in raw) {
            if (item.length == 2 && item.all { it.isDigit() }) {
                valid.add(item)
            }
        }
        return valid
    }

    fun normalizeFastBetText(text: String): String {
        return text.toAsciiDigits()
            .replace("ဒဲ့ကျန်", "/")
            .replace("ဒဲ့", "=")
            .replace("ကျန်", "/")
            .replace(Regex("/[rR]"), "/")
            .lines()
            .joinToString("\n") { rawLine ->
                var l = rawLine.trim()
                // Normalize R between two amounts, e.g. 00=2000R1000 -> 00=2000/1000
                l = l.replace(Regex("([=])(\\d+)[rR](\\d+)"), "$1$2/$3")
                // Normalize 00-2000/1000 or 00.2000/1000 or 00 2000/1000 into 00=2000/1000
                l = l.replace(Regex("((?:\\b\\d{2}[.,\\s]+)*\\b\\d{2})[-]+(?=\\d)"), "$1=")
                l = l.replace(Regex("((?:\\b\\d{2}[.,\\s]+)*\\b\\d{2})[.](?=\\d+/\\d+)"), "$1=")
                l = l.replace(Regex("((?:\\b\\d{2}[.,\\s]+)*\\b\\d{2})\\s+(?=\\d+/\\d+)"), "$1=")
                // Normalize multiple numbers separated by spaces before '=' or 'R', e.g. 01 78=500 -> 01.78=500
                l = l.replace(Regex("(\\b\\d{2})[\\s,]+(?=\\d{2}(?:[.\\s,]+\\d{2})*\\s*[=rR])"), "$1.")
                // Normalize spaces around '=' or '/' or 'R'
                l = l.replace(Regex("\\s*([=rR/])\\s*"), "$1")
                l = l.replace(Regex("([0-9.,\\s]+)[rR]=\\s*([0-9]+)"), "$1R$2")
                l = l.replace(Regex("([0-9.,\\s]+)=\\s*[rR]([0-9]+)"), "$1R$2")
                l = l.replace(Regex("([0-9.,\\s]+)=\\s*([0-9]+)[rR]"), "$1R$2")
                l
            }
    }
}
