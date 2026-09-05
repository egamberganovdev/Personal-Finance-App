package dev.egamberganov.finflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.egamberganov.finflow.data.entity.AccountEntity
import dev.egamberganov.finflow.data.entity.AppSettingsEntity
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.ScheduledPaymentEntity
import dev.egamberganov.finflow.data.entity.ScheduledPaymentWithDetails
import dev.egamberganov.finflow.data.entity.TransactionEntity
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.data.repository.FinanceRepository
import dev.egamberganov.finflow.domain.model.AccountType
import dev.egamberganov.finflow.domain.model.AccountWithBalance
import dev.egamberganov.finflow.domain.model.CategorySpend
import dev.egamberganov.finflow.domain.model.DateFilterRange
import dev.egamberganov.finflow.domain.model.FinancialSummary
import dev.egamberganov.finflow.domain.model.PeriodActivity
import dev.egamberganov.finflow.domain.model.TimePeriod
import dev.egamberganov.finflow.domain.model.TransactionFilterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    // Settings & Onboarding
    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isOnboardingCompleted: StateFlow<Boolean> = repository.appSettings
        .map { it?.isOnboardingCompleted == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Accounts
    val allAccounts: StateFlow<List<AccountWithBalance>> = repository.accountsWithBalance
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedAccountId: StateFlow<Long?> = repository.selectedAccountId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val financialSummary: StateFlow<FinancialSummary> = repository.currentFinancialSummary
        .stateIn(viewModelScope, SharingStarted.Eagerly, FinancialSummary(0, 0, 0, 0, "UZS"))

    // Categories
    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> = repository.expenseCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> = repository.incomeCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Transactions Filtering & Search State
    private val _transactionFilterType = MutableStateFlow(TransactionFilterType.ALL)
    val transactionFilterType: StateFlow<TransactionFilterType> = _transactionFilterType.asStateFlow()

    private val _transactionDateRange = MutableStateFlow(DateFilterRange.THIS_MONTH)
    val transactionDateRange: StateFlow<DateFilterRange> = _transactionDateRange.asStateFlow()

    private val _filterCategoryId = MutableStateFlow<Long?>(null)
    val filterCategoryId: StateFlow<Long?> = _filterCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered Transactions
    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        repository.currentContextTransactions,
        _transactionFilterType,
        _transactionDateRange,
        _filterCategoryId,
        _searchQuery
    ) { transactions, typeFilter, dateRange, categoryId, search ->
        val startOfRangeMillis: Long = when (dateRange) {
            DateFilterRange.THIS_WEEK -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            DateFilterRange.THIS_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            DateFilterRange.THIS_YEAR -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            DateFilterRange.ALL_TIME -> 0L
        }

        transactions.filter { item ->
            val matchesType = when (typeFilter) {
                TransactionFilterType.ALL -> true
                TransactionFilterType.INCOME -> item.transaction.type == "INCOME"
                TransactionFilterType.EXPENSE -> item.transaction.type == "EXPENSE"
                TransactionFilterType.TRANSFER -> item.transaction.type == "TRANSFER"
            }
            val matchesDate = item.transaction.dateMillis >= startOfRangeMillis
            val matchesCategory = categoryId == null || item.transaction.categoryId == categoryId
            val matchesSearch = if (search.isBlank()) true else {
                val q = search.trim().lowercase()
                val catName = item.category?.name?.lowercase() ?: ""
                val note = item.transaction.note?.lowercase() ?: ""
                val accName = item.account?.name?.lowercase() ?: ""
                val toAccName = item.toAccount?.name?.lowercase() ?: ""
                catName.contains(q) || note.contains(q) || accName.contains(q) || toAccName.contains(q) || item.transaction.amount.toString().contains(q)
            }
            matchesType && matchesDate && matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Recent 5 Transactions for Home Screen
    val recentTransactions: StateFlow<List<TransactionWithDetails>> = repository.currentContextTransactions
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Scheduled Payments
    val allScheduledPayments: StateFlow<List<ScheduledPaymentWithDetails>> = repository.currentContextScheduledPayments
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val upcomingPayments: StateFlow<List<ScheduledPaymentWithDetails>> = allScheduledPayments
        .map { list -> list.filter { it.scheduledPayment.isActive } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Statistics Period & Aggregations
    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    private val statsPeriodTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        repository.currentContextTransactions,
        _selectedPeriod
    ) { transactions, period ->
        val cal = Calendar.getInstance()
        val startMillis = when (period) {
            TimePeriod.WEEK -> {
                cal.apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            TimePeriod.MONTH -> {
                cal.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            TimePeriod.YEAR -> {
                cal.apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            TimePeriod.CUSTOM -> 0L
        }
        transactions.filter { it.transaction.dateMillis >= startMillis }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val expenseCategorySpends: StateFlow<List<CategorySpend>> = statsPeriodTransactions.combine(expenseCategories) { txs, _ ->
        val expenseTxs = txs.filter { it.transaction.type == "EXPENSE" }
        val totalExpense = expenseTxs.sumOf { it.transaction.amount }

        expenseTxs
            .groupBy { it.category }
            .mapNotNull { (category, list) ->
                if (category == null) return@mapNotNull null
                val sum = list.sumOf { it.transaction.amount }
                val percentage = if (totalExpense > 0) (sum.toFloat() / totalExpense) * 100f else 0f
                CategorySpend(category, sum, percentage, list.size)
            }
            .sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incomeCategorySpends: StateFlow<List<CategorySpend>> = statsPeriodTransactions.combine(incomeCategories) { txs, _ ->
        val incomeTxs = txs.filter { it.transaction.type == "INCOME" }
        val totalIncome = incomeTxs.sumOf { it.transaction.amount }

        incomeTxs
            .groupBy { it.category }
            .mapNotNull { (category, list) ->
                if (category == null) return@mapNotNull null
                val sum = list.sumOf { it.transaction.amount }
                val percentage = if (totalIncome > 0) (sum.toFloat() / totalIncome) * 100f else 0f
                CategorySpend(category, sum, percentage, list.size)
            }
            .sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val periodActivities: StateFlow<List<PeriodActivity>> = combine(
        statsPeriodTransactions,
        _selectedPeriod
    ) { txs, period ->
        when (period) {
            TimePeriod.WEEK -> {
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val cal = Calendar.getInstance()
                days.mapIndexed { index, label ->
                    val dayOfWeekCalendar = (index + 2) % 7
                    val dayTxs = txs.filter {
                        cal.timeInMillis = it.transaction.dateMillis
                        val day = cal.get(Calendar.DAY_OF_WEEK)
                        day == if (dayOfWeekCalendar == 0) 7 else dayOfWeekCalendar
                    }
                    PeriodActivity(
                        periodLabel = label,
                        income = dayTxs.filter { it.transaction.type == "INCOME" }.sumOf { it.transaction.amount },
                        expense = dayTxs.filter { it.transaction.type == "EXPENSE" }.sumOf { it.transaction.amount }
                    )
                }
            }
            TimePeriod.MONTH -> {
                listOf("W1", "W2", "W3", "W4").mapIndexed { index, label ->
                    val weekTxs = txs.filter {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.dateMillis }
                        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                        when (index) {
                            0 -> dayOfMonth in 1..7
                            1 -> dayOfMonth in 8..14
                            2 -> dayOfMonth in 15..21
                            else -> dayOfMonth >= 22
                        }
                    }
                    PeriodActivity(
                        periodLabel = label,
                        income = weekTxs.filter { it.transaction.type == "INCOME" }.sumOf { it.transaction.amount },
                        expense = weekTxs.filter { it.transaction.type == "EXPENSE" }.sumOf { it.transaction.amount }
                    )
                }
            }
            TimePeriod.YEAR -> {
                listOf("Q1", "Q2", "Q3", "Q4").mapIndexed { index, label ->
                    val qTxs = txs.filter {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.dateMillis }
                        val month = cal.get(Calendar.MONTH)
                        month in (index * 3)..((index * 3) + 2)
                    }
                    PeriodActivity(
                        periodLabel = label,
                        income = qTxs.filter { it.transaction.type == "INCOME" }.sumOf { it.transaction.amount },
                        expense = qTxs.filter { it.transaction.type == "EXPENSE" }.sumOf { it.transaction.amount }
                    )
                }
            }
            TimePeriod.CUSTOM -> {
                listOf(
                    PeriodActivity(
                        "Total",
                        income = txs.filter { it.transaction.type == "INCOME" }.sumOf { it.transaction.amount },
                        expense = txs.filter { it.transaction.type == "EXPENSE" }.sumOf { it.transaction.amount }
                    )
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
        }
    }

    // Onboarding
    fun completeOnboarding(
        accountName: String,
        accountType: String = AccountType.CASH.dbKey,
        currency: String,
        initialBalance: Long
    ) {
        viewModelScope.launch {
            repository.completeOnboarding(accountName, accountType, currency, initialBalance)
        }
    }

    // Filter Setters
    fun setTransactionFilterType(type: TransactionFilterType) {
        _transactionFilterType.value = type
    }

    fun setTransactionDateRange(range: DateFilterRange) {
        _transactionDateRange.value = range
    }

    fun setFilterCategoryId(categoryId: Long?) {
        _filterCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    // Transaction CRUD
    fun addTransaction(
        type: String,
        amount: Long,
        currency: String,
        categoryId: Long?,
        accountId: Long,
        dateMillis: Long,
        note: String?,
        attachmentUri: String?
    ) {
        viewModelScope.launch {
            repository.createTransaction(
                type = type,
                amount = amount,
                currency = currency,
                categoryId = categoryId,
                accountId = accountId,
                dateMillis = dateMillis,
                note = note?.trim()?.ifEmpty { null },
                attachmentUri = attachmentUri?.trim()?.ifEmpty { null }
            )
        }
    }

    fun addTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        dateMillis: Long,
        note: String?,
        attachmentUri: String? = null,
        exchangeRate: Double? = null,
        convertedAmount: Long? = null
    ) {
        viewModelScope.launch {
            repository.createTransfer(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = amount,
                dateMillis = dateMillis,
                note = note,
                attachmentUri = attachmentUri,
                exchangeRate = exchangeRate,
                convertedAmount = convertedAmount
            )
        }
    }

    suspend fun createTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        dateMillis: Long,
        note: String?,
        attachmentUri: String? = null,
        exchangeRate: Double? = null,
        convertedAmount: Long? = null
    ): Result<Long> {
        return repository.createTransfer(
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = amount,
            dateMillis = dateMillis,
            note = note,
            attachmentUri = attachmentUri,
            exchangeRate = exchangeRate,
            convertedAmount = convertedAmount
        )
    }

    suspend fun fetchExchangeRate(fromCurrency: String, toCurrency: String): Result<Double> {
        return repository.getExchangeRate(fromCurrency, toCurrency)
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Account CRUD
    fun selectAccount(accountId: Long?) {
        viewModelScope.launch {
            repository.selectAccount(accountId)
        }
    }

    fun createAccount(
        name: String,
        type: String = AccountType.CASH.dbKey,
        currency: String,
        initialBalance: Long,
        colorHex: String
    ) {
        viewModelScope.launch {
            repository.createAccount(name, type, currency, initialBalance, colorHex)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = repository.deleteAccount(account)
            onResult?.invoke(success)
        }
    }

    // Category CRUD
    fun createCategory(name: String, type: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            repository.createCategory(name, type, iconName, colorHex)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Scheduled Payments
    fun createScheduledPayment(
        name: String,
        amount: Long,
        currency: String,
        categoryId: Long,
        accountId: Long,
        frequency: String,
        nextPaymentDateMillis: Long,
        note: String?,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            repository.createScheduledPayment(
                name = name,
                amount = amount,
                currency = currency,
                categoryId = categoryId,
                accountId = accountId,
                frequency = frequency,
                nextPaymentDateMillis = nextPaymentDateMillis,
                note = note?.trim()?.ifEmpty { null },
                isActive = isActive
            )
        }
    }

    fun updateScheduledPayment(payment: ScheduledPaymentEntity) {
        viewModelScope.launch {
            repository.updateScheduledPayment(payment)
        }
    }

    fun deleteScheduledPayment(payment: ScheduledPaymentEntity) {
        viewModelScope.launch {
            repository.deleteScheduledPayment(payment)
        }
    }

    fun toggleScheduledPaymentActive(payment: ScheduledPaymentEntity) {
        viewModelScope.launch {
            repository.toggleScheduledPaymentActive(payment.id, !payment.isActive)
        }
    }

    fun recordScheduledPaymentAsTransaction(payment: ScheduledPaymentEntity) {
        viewModelScope.launch {
            repository.createTransaction(
                type = "EXPENSE",
                amount = payment.amount,
                currency = payment.currency,
                categoryId = payment.categoryId,
                accountId = payment.accountId,
                dateMillis = System.currentTimeMillis(),
                note = "Recurring payment: ${payment.name}",
                attachmentUri = null
            )

            val cal = Calendar.getInstance().apply {
                timeInMillis = payment.nextPaymentDateMillis
            }
            when (payment.frequency) {
                "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                "YEARLY" -> cal.add(Calendar.YEAR, 1)
            }

            repository.updateScheduledPayment(
                payment.copy(nextPaymentDateMillis = cal.timeInMillis)
            )
        }
    }

    // App Settings
    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun updateLanguage(code: String) {
        viewModelScope.launch {
            repository.setLanguageCode(code)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
        }
    }
}

class FinanceViewModelFactory(
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
