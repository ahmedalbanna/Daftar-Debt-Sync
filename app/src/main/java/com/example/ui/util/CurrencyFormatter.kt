package com.example.ui.util

import java.text.DecimalFormat

object CurrencyFormatter {
    private val decimalFormat = DecimalFormat("#,##0")

    fun formatAmount(amount: Double, currencySymbol: String = "د.ع"): String {
        val formatted = decimalFormat.format(amount)
        return "$formatted $currencySymbol"
    }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "لا يوجد حركات"
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd - hh:mm a", java.util.Locale("ar"))
        return sdf.format(java.util.Date(timestamp))
    }
}
