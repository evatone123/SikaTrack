package com.example.domain

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(
        amount: Double,
        currencyCode: String = "GH₵",
        hideBalances: Boolean = false,
        showSign: Boolean = false
    ): String {
        if (hideBalances) {
            return "••••••"
        }

        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val absVal = kotlin.math.abs(amount)
        val formattedNumber = formatter.format(absVal)

        return when {
            amount < 0 -> "-$currencyCode $formattedNumber"
            showSign && amount > 0 -> "+$currencyCode $formattedNumber"
            else -> "$currencyCode $formattedNumber"
        }
    }

    fun formatCompact(
        amount: Double,
        currencyCode: String = "GH₵"
    ): String {
        val absVal = kotlin.math.abs(amount)
        val prefix = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000 -> "$prefix$currencyCode ${(absVal / 1_000_000.0).formatDecimals(1)}M"
            absVal >= 1_000 -> "$prefix$currencyCode ${(absVal / 1_000.0).formatDecimals(1)}K"
            else -> "$prefix$currencyCode ${absVal.formatDecimals(2)}"
        }
    }

    private fun Double.formatDecimals(digits: Int): String {
        return "%.${digits}f".format(Locale.US, this)
    }
}
