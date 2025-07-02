package ng.wimika.samplebankapp.utils

object CurrencyFormatter {
    fun format(amount: Double): String {
        return "₦${String.format("%,.0f", amount)}"
    }
    
    fun format(amount: Int): String {
        return "₦${String.format("%,d", amount)}"
    }
} 