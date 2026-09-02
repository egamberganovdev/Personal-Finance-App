package dev.egamberganov.finflow.domain.model

import dev.egamberganov.finflow.data.entity.AccountEntity
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.ScheduledPaymentEntity
import dev.egamberganov.finflow.data.entity.TransactionEntity
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.data.entity.ScheduledPaymentWithDetails
import java.text.NumberFormat
import java.util.Locale

data class AccountWithBalance(
    val account: AccountEntity,
    val currentBalance: Long,
    val totalIncome: Long,
    val totalExpense: Long,
    val transactionCount: Int
) {
    val id: Long get() = account.id
    val name: String get() = account.name
    val currency: String get() = account.currency
    val type: String get() = account.type
    val initialBalance: Long get() = account.initialBalance
    val colorHex: String get() = account.colorHex
}

data class FinancialSummary(
    val totalBalance: Long,
    val totalIncome: Long,
    val totalExpense: Long,
    val netChange: Long,
    val primaryCurrency: String
)

data class CategorySpend(
    val category: CategoryEntity,
    val totalAmount: Long,
    val percentage: Float,
    val transactionCount: Int
)

data class PeriodActivity(
    val periodLabel: String, // e.g. "W1", "Mon", "Jan"
    val income: Long,
    val expense: Long
)

enum class TimePeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    CUSTOM("Custom")
}

enum class TransactionFilterType(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense")
}

enum class DateFilterRange(val label: String) {
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

object CurrencyFormatter {
    fun formatAmount(amount: Long, currency: String, showSign: Boolean = false, isExpense: Boolean = false): String {
        val formattedNumber = NumberFormat.getNumberInstance(Locale.US).format(amount)
        val prefix = when {
            showSign && isExpense -> "-"
            showSign && !isExpense -> "+"
            else -> ""
        }

        return when (currency.uppercase()) {
            "USD" -> "$prefix$$formattedNumber"
            "EUR" -> "$prefix€$formattedNumber"
            "GBP" -> "$prefix£$formattedNumber"
            "RUB" -> "$prefix$formattedNumber ₽"
            "KZT" -> "$prefix$formattedNumber ₸"
            else -> "$prefix$formattedNumber $currency"
        }
    }

    fun formatAmountWithoutSymbol(amount: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(amount)
    }

    fun formatCompactAmount(amount: Long, currency: String): String {
        val absAmount = Math.abs(amount)
        val compactStr = when {
            absAmount >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", amount / 1_000_000_000.0)
            absAmount >= 1_000_000 -> String.format(Locale.US, "%.2fM", amount / 1_000_000.0)
            absAmount >= 1_000 -> String.format(Locale.US, "%.1fK", amount / 1_000.0)
            else -> amount.toString()
        }
        return compactStr
    }
}
