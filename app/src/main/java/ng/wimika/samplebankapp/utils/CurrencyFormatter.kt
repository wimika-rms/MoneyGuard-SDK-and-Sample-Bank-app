package ng.wimika.samplebankapp.utils

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    private val formatter = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
        currency = Currency.getInstance("NGN")
        maximumFractionDigits = 0
    }

    fun format(amount: Double): String {
        return formatter.format(amount.toLong())
    }

    fun format(amount: Int): String {
        return formatter.format(amount.toLong())
    }


    fun format(amount: Long): String {
        return formatter.format(amount)
    }
}