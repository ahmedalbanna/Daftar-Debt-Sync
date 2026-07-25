package com.example.ui.util

object ArabicNumerals {
    private val map = mapOf(
        '0' to '٠', '1' to '١', '2' to '٢', '3' to '٣',
        '4' to '٤', '5' to '٥', '6' to '٦', '7' to '٧',
        '8' to '٨', '9' to '٩'
    )

    fun convert(input: String): String =
        input.map { map[it] ?: it }.joinToString("")

    fun formatCurrency(amount: Double, symbol: String = "د.ع"): String {
        val formatted = String.format("%,.0f", amount)
        return "${convert(formatted)} $symbol"
    }

    fun formatPercent(percent: Int): String {
        return "${convert(percent.toString())}%"
    }
}
