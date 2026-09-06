package dev.egamberganov.finflow.domain.model

import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.data.entity.AccountEntity
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.ScheduledPaymentEntity
import dev.egamberganov.finflow.data.entity.TransactionEntity
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.data.entity.ScheduledPaymentWithDetails
import java.text.NumberFormat
import java.util.Locale

enum class AccountType(
    val dbKey: String,
    val stringResId: Int,
    val iconResId: Int
) {
    CASH("CASH", R.string.account_type_cash, R.drawable.ic_fin_wallet),
    BANK("BANK", R.string.account_type_bank, R.drawable.ic_fin_bank),
    SAVINGS("SAVINGS", R.string.account_type_savings, R.drawable.ic_fin_savings),
    EWALLET("EWALLET", R.string.account_type_ewallet, R.drawable.ic_fin_ewallet),
    INVESTMENT("INVESTMENT", R.string.account_type_investment, R.drawable.ic_fin_investment),
    LOAN_DEBT("LOAN_DEBT", R.string.account_type_loan_debt, R.drawable.ic_fin_loan);

    companion object {
        fun fromString(value: String?): AccountType {
            if (value.isNullOrBlank()) return CASH
            val normalized = value.trim().uppercase()
            return when (normalized) {
                "CASH", "WALLET" -> CASH
                "BANK", "BANK_ACCOUNT", "BANK ACCOUNT" -> BANK
                "SAVINGS", "SAVING" -> SAVINGS
                "EWALLET", "E-WALLET", "E_WALLET", "MOBILE" -> EWALLET
                "INVESTMENT", "INVEST" -> INVESTMENT
                "LOAN_DEBT", "LOAN", "DEBT", "LOAN / DEBT" -> LOAN_DEBT
                // Backward-compatible safe mapping for legacy "OTHER"
                "OTHER" -> CASH
                else -> CASH
            }
        }
    }
}

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
    val accountType: AccountType get() = AccountType.fromString(account.type)
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
    EXPENSE("Expense"),
    TRANSFER("Transfer")
}

enum class DateFilterRange(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
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

enum class ScheduledPaymentStatus {
    OVERDUE,
    DUE_TODAY,
    UPCOMING
}

data class ScheduledPaymentStatusInfo(
    val status: ScheduledPaymentStatus,
    val daysDifference: Long // negative if overdue (e.g. -2 for 2 days overdue), 0 if due today, positive if upcoming (e.g. 3 for in 3 days)
)

object ScheduledPaymentCalculator {
    fun calculateStatus(
        nextPaymentDateMillis: Long,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): ScheduledPaymentStatusInfo {
        val calTarget = java.util.Calendar.getInstance().apply {
            timeInMillis = nextPaymentDateMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val calRef = java.util.Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffMillis = calTarget.timeInMillis - calRef.timeInMillis
        val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)

        val status = when {
            diffDays < 0 -> ScheduledPaymentStatus.OVERDUE
            diffDays == 0L -> ScheduledPaymentStatus.DUE_TODAY
            else -> ScheduledPaymentStatus.UPCOMING
        }
        return ScheduledPaymentStatusInfo(status, diffDays)
    }

    fun calculateNextDate(currentNextDateMillis: Long, frequency: String): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = currentNextDateMillis
        }
        when (frequency.trim().uppercase()) {
            "DAILY" -> cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> cal.add(java.util.Calendar.MONTH, 1)
            "YEARLY" -> cal.add(java.util.Calendar.YEAR, 1)
            else -> cal.add(java.util.Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }
}
