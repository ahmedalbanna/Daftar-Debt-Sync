package com.example.ui.util

import com.example.data.entity.TransactionType

/**
 * Holds the parsed result from speech-to-text.
 */
data class SpeechParseResult(
    val customerName: String?,
    val amount: Double?,
    val type: TransactionType?,
    val description: String
)

/**
 * Parses Arabic speech-to-text output to extract transaction fields.
 *
 * Expected patterns:
 * - "<name> دين <amount>" or "<name> عليه دين <amount>"
 * - "<name> تسديد <amount>" or "<name> دفعة <amount>"
 * - "<name> دفع <amount>"
 * - "<amount> دين <name>"
 *
 * Currency words (ريال, دينار, د.ع, ل.س) are optional.
 */
object SpeechParser {

    // Number words in Arabic — normalized without و prefix
    private val numberWords = mapOf(
        "واحد" to 1, "واحدة" to 1,
        "اثنان" to 2, "اثنين" to 2, "اثنتين" to 2,
        "ثلاثة" to 3, "ثلاث" to 3,
        "أربعة" to 4, "أربع" to 4,
        "خمسة" to 5, "خمس" to 5,
        "ستة" to 6, "ست" to 6,
        "سبعة" to 7, "سبع" to 7,
        "ثمانية" to 8, "ثمان" to 8,
        "تسعة" to 9, "تسع" to 9,
        "عشرة" to 10, "عشر" to 10,
        "أحدعشر" to 11, "أحد عشر" to 11,
        "اثناعشر" to 12, "اثنا عشر" to 12,
        "عشرين" to 20, "عشرون" to 20,
        "ثلاثين" to 30, "ثلاثون" to 30,
        "أربعين" to 40, "أربعون" to 40,
        "خمسين" to 50, "خمسون" to 50,
        "ستين" to 60, "ستون" to 60,
        "سبعين" to 70, "سبعون" to 70,
        "ثمانين" to 80, "ثمانون" to 80,
        "تسعين" to 90, "تسعون" to 90,
        "مائة" to 100, "مئة" to 100, "ماية" to 100, "مية" to 100,
        "مائتين" to 200, "ميتين" to 200,
        "ألف" to 1000,
        "ألفين" to 2000,
        "مليون" to 1000000
    )

    // Currency indicators to ignore
    private val currencyWords = setOf(
        "ريال", "ريالات", "ريالاً", "دينار", "دنانير", "ديناراً",
        "د.ع", "د.ع.", "ل.س", "ل.س.", "$", "دولار"
    )

    // Transaction type indicators
    private val debtIndicators = listOf(
        "دين", "عليه", "عليها", "مدين",
        "شاري", "مشتريات", "بضاعة", "بضاعه"
    )

    private val paymentIndicators = listOf(
        "تسديد", "دفعة", "دفع", "دفعت", "قبض", "قبضت",
        "حساب", "محاسبة", "تسوية", "مسدد"
    )

    /**
     * Parse a speech-to-text result string.
     */
    fun parse(text: String, existingCustomerNames: List<String> = emptyList()): SpeechParseResult {
        val normalized = normalizeText(text)
        val words = normalized.split(" ").filter { it.isNotBlank() }

        // Remove currency words
        val filteredWords = words.filter { it !in currencyWords }

        // Determine transaction type
        val type = detectTransactionType(filteredWords.joinToString(" "))

        // Extract amount and identify which word indices belong to the amount expression
        val amountResult = extractAmountWithIndices(filteredWords)
        val amount = amountResult.value
        val amountIndices = amountResult.indices.toSet()

        // Extract customer name: words that are not type indicators and not amount words
        val remainingWords = mutableListOf<Pair<Int, String>>()
        for ((index, word) in filteredWords.withIndex()) {
            if (index in amountIndices) continue
            val lowerWord = word.lowercase()

            // Skip type indicators
            val indicators = if (type == TransactionType.DEBT) debtIndicators else paymentIndicators
            if (lowerWord in indicators) continue

            remainingWords.add(index to word)
        }

        // Join remaining words as customer name
        val customerNameCandidate = if (remainingWords.isNotEmpty()) {
            remainingWords.joinToString(" ") { it.second }
        } else {
            null
        }

        // Try to match against existing customers
        val customerName = matchExistingCustomer(customerNameCandidate, existingCustomerNames)
            ?: customerNameCandidate

        // The description
        val description = buildDescription(customerName, amount, type)

        return SpeechParseResult(
            customerName = customerName,
            amount = amount,
            type = type,
            description = description
        )
    }

    private fun normalizeText(text: String): String {
        return text.trim()
            .replace(Regex("[،,!?.;:،؟!\\[\\](){}\"]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun detectTransactionType(text: String): TransactionType {
        val lowerText = text.lowercase()

        // Check payment first (more specific)
        for (indicator in paymentIndicators) {
            if (lowerText.contains(indicator)) {
                return TransactionType.PAYMENT
            }
        }

        // Check debt
        for (indicator in debtIndicators) {
            if (lowerText.contains(indicator)) {
                return TransactionType.DEBT
            }
        }

        // Default to DEBT if no indicator found
        return TransactionType.DEBT
    }

    /**
     * Result of amount extraction including which word indices belong to the amount.
     */
    private data class AmountResult(
        val value: Double?,
        val indices: List<Int>
    )

    /**
     * Extract the amount and return which word indices are part of it.
     *
     * Handles Arabic compound numbers like:
     * - "خمسة وعشرين" = 25 (accumulated small numbers)
     * - "خمسة وعشرين ألف" = 25 × 1000 = 25000
     * - "مية ألف" = 100 × 1000 = 100000
     * - "ألف" alone = 1000
     * - "ألفين" = 2000
     */
    private fun extractAmountWithIndices(words: List<String>): AmountResult {
        val usedIndices = mutableListOf<Int>()
        var total = 0.0
        var accumulator = 0.0 // Accumulates successive small numbers before a multiplier
        var foundNumber = false

        var i = 0
        while (i < words.size) {
            val word = words[i]
            val cleanWord = stripAndPrefix(word)

            // Check if next word is a multiplier — if so, treat current word as base number
            if (i + 1 < words.size) {
                val nextClean = stripAndPrefix(words[i + 1])
                val nextMultiplier = parseMultiplier(nextClean)
                if (nextMultiplier != null) {
                    val baseValue = parseNumericWord(cleanWord)
                    if (baseValue != null) {
                        // "مية ألف" or "خمسة وعشرين ألف"
                        // Current + accumulated base, then multiply
                        val combined = accumulator + baseValue
                        total += combined * nextMultiplier
                        accumulator = 0.0
                        usedIndices.add(i)
                        usedIndices.add(i + 1)
                        foundNumber = true
                        i += 2
                        continue
                    }
                }
            }

            val numericValue = parseNumericWord(cleanWord)
            if (numericValue != null) {
                usedIndices.add(i)
                accumulator += numericValue
                foundNumber = true
                i++
                continue
            }

            val multiplierWord = stripAndPrefix(word)
            val multiplier = parseMultiplier(multiplierWord)
            if (multiplier != null) {
                if (accumulator > 0) {
                    // "خمسة وعشرين ألف" → accumulator=25, × 1000 = 25000
                    total += accumulator * multiplier
                    accumulator = 0.0
                } else {
                    // "ألف" or "ألفين" alone → 1000
                    total += multiplier
                }
                usedIndices.add(i)
                foundNumber = true
                i++
                continue
            }

            // Non-numeric, non-multiplier word — flush accumulator if present
            if (accumulator > 0) {
                total += accumulator
                accumulator = 0.0
            }
            i++
        }

        // Flush remaining accumulator
        if (accumulator > 0) {
            total += accumulator
        }

        return AmountResult(
            value = if (foundNumber) total else null,
            indices = usedIndices
        )
    }

    /**
     * Strip Arabic "و" (and) prefix from a word. Also strip other common prefixes.
     */
    private fun stripAndPrefix(word: String): String {
        var result = word
        if (result.startsWith("و") && result.length > 1) {
            result = result.substring(1)
        }
        if (result.startsWith("ف") && result.length > 1 && result[1] in 'ا'..'ي') {
            result = result.substring(1)
        }
        return result
    }

    private fun parseNumericWord(word: String): Double? {
        // Check if it's a written number word
        val written = numberWords[word.lowercase()]
        if (written != null) return written.toDouble()

        // Check for Arabic digits
        val zeroCode = '0'.code
        val arabicZeroCode = '٠'.code
        val persianZeroCode = '۰'.code

        val arabicDigits = word.filter { it in '٠'..'٩' || it in '۰'..'۹' }
        if (arabicDigits.isNotEmpty()) {
            val converted = word.map { c ->
                when {
                    c in '٠'..'٩' -> (zeroCode + (c.code - arabicZeroCode)).toChar()
                    c in '۰'..'۹' -> (zeroCode + (c.code - persianZeroCode)).toChar()
                    else -> c
                }
            }.joinToString("")
            val clean = converted.filter { it.isDigit() || it == '.' }
            return clean.toDoubleOrNull()
        }

        // Standard digits
        val clean = word.filter { it.isDigit() || it == '.' }
        return clean.toDoubleOrNull()
    }

    private fun parseMultiplier(word: String): Double? {
        return when (word.lowercase()) {
            "مية", "مائة", "مئة", "ماية" -> 100.0
            "مائتين", "ميتين" -> 200.0
            "ألف", "الف", "آلاف" -> 1000.0
            "ألفين" -> 2000.0
            "مليون" -> 1000000.0
            else -> null
        }
    }

    private fun matchExistingCustomer(
        candidateName: String?,
        existingNames: List<String>
    ): String? {
        if (candidateName == null || existingNames.isEmpty()) return null

        val lowerCandidate = candidateName.lowercase().trim()

        // Try exact match
        val exactMatch = existingNames.find { it.lowercase() == lowerCandidate }
        if (exactMatch != null) return exactMatch

        // Try partial match (candidate is contained in a known customer name)
        val partialMatch = existingNames.find { it.lowercase().contains(lowerCandidate) }
        if (partialMatch != null) return partialMatch

        // Try reverse (known name contains candidate)
        val reverseMatch = existingNames.find { lowerCandidate.contains(it.lowercase()) }
        if (reverseMatch != null) return reverseMatch

        return null
    }

    private fun buildDescription(
        customerName: String?,
        amount: Double?,
        type: TransactionType?
    ): String {
        val parts = mutableListOf<String>()
        if (customerName != null) parts.add(customerName)
        if (amount != null) parts.add("${amount.toLong()} ريال")
        parts.add("(إدخال صوتي)")
        return parts.joinToString(" - ")
    }
}
