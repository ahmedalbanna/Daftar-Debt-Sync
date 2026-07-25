package com.example.ui.util

import java.text.DecimalFormat

object CurrencyFormatter {
    private val map = mapOf(
        '0' to '٠', '1' to '١', '2' to '٢', '3' to '٣',
        '4' to '٤', '5' to '٥', '6' to '٦', '7' to '٧',
        '8' to '٨', '9' to '٩'
    )

    private val decimalFormat = DecimalFormat("#,##0")

    fun convertToArabicNumerals(input: String): String =
        input.map { map[it] ?: it }.joinToString("")

    fun formatAmount(amount: Double, currencySymbol: String = "د.ع"): String {
        val formatted = decimalFormat.format(amount)
        return "${convertToArabicNumerals(formatted)} $currencySymbol"
    }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "لا يوجد حركات"
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd - hh:mm a", java.util.Locale("ar"))
        val formattedDate = sdf.format(java.util.Date(timestamp))
        return convertToArabicNumerals(formattedDate)
    }

    fun formatPhone(phone: String): String {
        // Keep phone numbers in western digits for standard dialing/recognition, 
        // or convert if purely for display. Usually better to keep western for copy-paste.
        return phone
    }
}
