package com.example.data.imports

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Represents a single row parsed from an import file (CSV or XLSX).
 */
data class ImportRow(
    val name: String,
    val phone: String,
    val balance: Double
)

/**
 * Result of parsing an import file.
 */
data class ImportResult(
    val rows: List<ImportRow>,
    val errors: List<String>
)

/**
 * Parses contact & balance data from CSV or XLSX files.
 */
object ImportParser {

    /**
     * Parse a CSV file from the given [uri] using [context]'s ContentResolver.
     * Expected columns: name (required), phone (optional), balance (required, > 0)
     * Supports comma or tab delimiters, with or without header row.
     */
    fun parseCsv(context: Context, uri: Uri): ImportResult {
        val rows = mutableListOf<ImportRow>()
        val errors = mutableListOf<String>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult(emptyList(), listOf("تعذر فتح الملف"))

            val reader = BufferedReader(InputStreamReader(inputStream))
            val allLines = reader.readLines()
            reader.close()

            if (allLines.isEmpty()) {
                return ImportResult(emptyList(), listOf("الملف فارغ"))
            }

            // Detect delimiter by checking first line
            val delimiter = detectDelimiter(allLines.first())
            val lines = allLines.toMutableList()

            // Check if first line is a header
            val firstLine = lines.first().trim().lowercase()
            val hasHeader = firstLine.contains("اسم") ||
                    firstLine.contains("الاسم") ||
                    firstLine.contains("name") ||
                    firstLine.contains("phone") ||
                    firstLine.contains("هاتف") ||
                    firstLine.contains("رقم") ||
                    firstLine.contains("balance") ||
                    firstLine.contains("دين") ||
                    firstLine.contains("رصيد") ||
                    firstLine.contains("مبلغ")

            if (hasHeader) {
                lines.removeAt(0)
            }

            for ((index, line) in lines.withIndex()) {
                val trimmed = line.trim()
                if (trimmed.isBlank()) continue

                val parts = splitLine(trimmed, delimiter)
                if (parts.size < 2) {
                    errors.add("السطر ${index + 2}: لا يحتوي على أعمدة كافية")
                    continue
                }

                val name = parts[0].trim().trim('"', '\'')
                val phone = if (parts.size >= 2) parts[1].trim().trim('"', '\'') else ""
                val balanceStr = if (parts.size >= 3) parts[2].trim().trim('"', '\'') else parts[1].trim().trim('"', '\'')

                if (name.isBlank()) {
                    errors.add("السطر ${index + 2}: اسم الزبون فارغ")
                    continue
                }

                val balance = parseArabicNumber(balanceStr)
                if (balance == null || balance <= 0) {
                    errors.add("السطر ${index + 2}: مبلغ الدين غير صالح ($balanceStr)")
                    continue
                }

                rows.add(ImportRow(name = name, phone = phone, balance = balance))
            }

        } catch (e: Exception) {
            return ImportResult(emptyList(), listOf("خطأ في قراءة الملف: ${e.localizedMessage}"))
        }

        return ImportResult(rows = rows, errors = errors)
    }

    /**
     * Parse an XLSX file from the given [uri].
     */
    fun parseXlsx(context: Context, uri: Uri): ImportResult {
        val rows = mutableListOf<ImportRow>()
        val errors = mutableListOf<String>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult(emptyList(), listOf("تعذر فتح الملف"))

            val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            // Determine if first row is a header
            val firstRow = sheet.getRow(0)
            var startRow = 0
            if (firstRow != null) {
                val firstCellValue = firstRow.getCell(0)?.toString()?.trim()?.lowercase() ?: ""
                if (firstCellValue.contains("اسم") ||
                    firstCellValue.contains("الاسم") ||
                    firstCellValue.contains("name") ||
                    firstCellValue.contains("phone") ||
                    firstCellValue.contains("رقم") ||
                    firstCellValue.contains("balance") ||
                    firstCellValue.contains("دين") ||
                    firstCellValue.contains("رصيد")
                ) {
                    startRow = 1
                }
            }

            for (i in startRow..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val nameCell = row.getCell(0)
                val phoneCell = row.getCell(1)
                val balanceCell = row.getCell(2) ?: row.getCell(1) // fallback if only 2 columns

                val name = nameCell?.toString()?.trim() ?: ""
                val phone = phoneCell?.toString()?.trim() ?: ""
                val balanceStr = balanceCell?.toString()?.trim() ?: ""

                if (name.isBlank()) {
                    errors.add("السطر ${i + 1}: اسم الزبون فارغ")
                    continue
                }

                val balance = parseArabicNumber(balanceStr)
                if (balance == null || balance <= 0) {
                    errors.add("السطر ${i + 1}: مبلغ الدين غير صالح ($balanceStr)")
                    continue
                }

                rows.add(ImportRow(name = name, phone = phone, balance = balance))
            }

            workbook.close()
            inputStream.close()

        } catch (e: Exception) {
            return ImportResult(emptyList(), listOf("خطأ في قراءة ملف Excel: ${e.localizedMessage}"))
        }

        return ImportResult(rows = rows, errors = errors)
    }

    /**
     * Detect delimiter: tab or comma.
     */
    private fun detectDelimiter(line: String): Char {
        return if (line.contains('\t')) '\t' else ','
    }

    /**
     * Split a line by delimiter, respecting quoted fields.
     */
    private fun splitLine(line: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' || char == '\'' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    parts.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        parts.add(current.toString())
        return parts
    }

    /**
     * Parse Arabic and Western number strings to Double.
     * Handles: "10000", "10,000", "١٠٠٠٠", "10 ألف", "خمسة", etc.
     */
    private fun parseArabicNumber(text: String): Double? {
        if (text.isBlank()) return null

        var normalized = text.trim()
            .replace(",", "")
            .replace(" ", "")
            .replace("٬", "") // Arabic thousands separator

        // Handle written numbers (basic Arabic number words)
        val writtenNumbers = mapOf(
            "واحد" to 1.0, "اثنان" to 2.0, "اثنين" to 2.0, "ثلاثة" to 3.0, "ثلاث" to 3.0,
            "أربعة" to 4.0, "أربع" to 4.0, "خمسة" to 5.0, "خمس" to 5.0,
            "ستة" to 6.0, "ست" to 6.0, "سبعة" to 7.0, "سبع" to 7.0,
            "ثمانية" to 8.0, "ثمان" to 8.0, "تسعة" to 9.0, "تسع" to 9.0,
            "عشرة" to 10.0, "عشر" to 10.0,
            "عشرين" to 20.0, "عشرون" to 20.0,
            "ثلاثين" to 30.0, "ثلاثون" to 30.0,
            "أربعين" to 40.0, "أربعون" to 40.0,
            "خمسين" to 50.0, "خمسون" to 50.0,
            "مائة" to 100.0, "مئة" to 100.0, "ماية" to 100.0,
            "مائتين" to 200.0, "ميتين" to 200.0,
            "ألف" to 1000.0,
            "ألفين" to 2000.0,
            "مليون" to 1000000.0
        )

        // Check for "X ألف" or "X مية" patterns
        for ((word, multiplier) in writtenNumbers.entries.reversed()) {
            if (normalized.endsWith(word)) {
                val prefix = normalized.removeSuffix(word).trim()
                if (prefix.isNotBlank()) {
                    val prefixNum = parseArabicNumberSimple(prefix) ?: continue
                    return prefixNum * multiplier
                }
            }
        }

        // Check simple written number
        val simpleMatch = writtenNumbers.entries.firstOrNull { (word, _) ->
            normalized == word
        }
        if (simpleMatch != null) return simpleMatch.value

        // Parse as numeral (Arabic or Western)
        return parseArabicNumberSimple(normalized)
    }

    /**
     * Parse a simple numeric string (Arabic or Western digits) to Double.
     */
    private fun parseArabicNumberSimple(text: String): Double? {
        // Check for Arabic-Indic digits (٠-٩)
        val zeroCode = '0'.code
        val arabicZeroCode = '٠'.code
        val persianZeroCode = '۰'.code

        val arabicDigits = text.filter { it in '٠'..'٩' }
        if (arabicDigits.isNotEmpty()) {
            val converted = text.map { c ->
                if (c in '٠'..'٩') (zeroCode + (c.code - arabicZeroCode)).toChar() else c
            }.joinToString("")
            return converted.toDoubleOrNull()
        }

        // Check for Persian digits (۰-۹)
        val persianDigits = text.filter { it in '۰'..'۹' }
        if (persianDigits.isNotEmpty()) {
            val converted = text.map { c ->
                if (c in '۰'..'۹') (zeroCode + (c.code - persianZeroCode)).toChar() else c
            }.joinToString("")
            return converted.toDoubleOrNull()
        }

        // Standard Western digits
        return text.toDoubleOrNull()
    }
}
