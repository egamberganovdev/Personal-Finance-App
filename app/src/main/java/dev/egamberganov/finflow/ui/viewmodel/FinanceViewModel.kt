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
import dev.egamberganov.finflow.domain.model.CurrencyFormatter
import dev.egamberganov.finflow.domain.model.DateFilterRange
import dev.egamberganov.finflow.domain.model.FinancialSummary
import dev.egamberganov.finflow.domain.model.PeriodActivity
import dev.egamberganov.finflow.domain.model.ScheduledPaymentCalculator
import dev.egamberganov.finflow.domain.model.ScheduledPaymentStatus
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

    private val _filterAccountId = MutableStateFlow<Long?>(null)
    val filterAccountId: StateFlow<Long?> = _filterAccountId.asStateFlow()

    private val _filterCategoryId = MutableStateFlow<Long?>(null)
    val filterCategoryId: StateFlow<Long?> = _filterCategoryId.asStateFlow()

    private val _transactionDateRange = MutableStateFlow(DateFilterRange.ALL_TIME)
    val transactionDateRange: StateFlow<DateFilterRange> = _transactionDateRange.asStateFlow()

    private val _customStartDateMillis = MutableStateFlow<Long?>(null)
    val customStartDateMillis: StateFlow<Long?> = _customStartDateMillis.asStateFlow()

    private val _customEndDateMillis = MutableStateFlow<Long?>(null)
    val customEndDateMillis: StateFlow<Long?> = _customEndDateMillis.asStateFlow()

    private val _minAmount = MutableStateFlow<Long?>(null)
    val minAmount: StateFlow<Long?> = _minAmount.asStateFlow()

    private val _maxAmount = MutableStateFlow<Long?>(null)
    val maxAmount: StateFlow<Long?> = _maxAmount.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Filters Count (excluding default ALL_TIME and ALL)
    val activeFiltersCount: StateFlow<Int> = combine(
        listOf<kotlinx.coroutines.flow.Flow<*>>(
            _transactionFilterType,
            _filterAccountId,
            _filterCategoryId,
            _transactionDateRange,
            _minAmount,
            _maxAmount
        )
    ) { args ->
        val type = args[0] as TransactionFilterType
        val accountId = args[1] as Long?
        val categoryId = args[2] as Long?
        val dateRange = args[3] as DateFilterRange
        val min = args[4] as Long?
        val max = args[5] as Long?
        var count = 0
        if (type != TransactionFilterType.ALL) count++
        if (accountId != null) count++
        if (categoryId != null) count++
        if (dateRange != DateFilterRange.ALL_TIME) count++
        if (min != null || max != null) count++
        count
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Filtered Transactions
    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        listOf<kotlinx.coroutines.flow.Flow<*>>(
            repository.currentContextTransactions,
            _transactionFilterType,
            _filterAccountId,
            _filterCategoryId,
            _transactionDateRange,
            _customStartDateMillis,
            _customEndDateMillis,
            _minAmount,
            _maxAmount,
            _searchQuery
        )
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val transactions = args[0] as List<TransactionWithDetails>
        val typeFilter = args[1] as TransactionFilterType
        val filterAccId = args[2] as Long?
        val catId = args[3] as Long?
        val dateRange = args[4] as DateFilterRange
        val customStart = args[5] as Long?
        val customEnd = args[6] as Long?
        val minAmt = args[7] as Long?
        val maxAmt = args[8] as Long?
        val search = args[9] as String

        val (startMillis, endMillis) = calculateDateBounds(dateRange, customStart, customEnd)

        transactions.filter { item ->
            // 1. Transaction Type
            val matchesType = when (typeFilter) {
                TransactionFilterType.ALL -> true
                TransactionFilterType.INCOME -> item.transaction.type == "INCOME"
                TransactionFilterType.EXPENSE -> item.transaction.type == "EXPENSE"
                TransactionFilterType.TRANSFER -> item.transaction.type == "TRANSFER"
            }

            // 2. Account Filter (matches primary or destination account for transfers)
            val matchesAccount = if (filterAccId == null) {
                true
            } else {
                item.transaction.accountId == filterAccId || item.transaction.toAccountId == filterAccId
            }

            // 3. Category Filter (Transfers have no category; when catId == null, transfers are included)
            val matchesCategory = if (catId == null) {
                true
            } else {
                item.transaction.categoryId == catId
            }

            // 4. Date Range
            val matchesDate = item.transaction.dateMillis in startMillis..endMillis

            // 5. Amount Range
            val matchesAmount = (minAmt == null || item.transaction.amount >= minAmt) &&
                    (maxAmt == null || item.transaction.amount <= maxAmt)

            // 6. Search Query (case-insensitive, all fields)
            val matchesSearch = if (search.isBlank()) true else {
                val q = search.trim().lowercase()
                val note = item.transaction.note?.lowercase() ?: ""
                val catName = item.category?.name?.lowercase() ?: ""
                val accName = item.account?.name?.lowercase() ?: ""
                val toAccName = item.toAccount?.name?.lowercase() ?: ""
                val amountRaw = item.transaction.amount.toString()
                val currency = item.transaction.currency.lowercase()
                val formattedAmount = CurrencyFormatter.formatAmount(item.transaction.amount, item.transaction.currency).lowercase()
                val type = item.transaction.type.lowercase()
                val typeLabel = when (item.transaction.type) {
                    "INCOME" -> "income"
                    "EXPENSE" -> "expense"
                    "TRANSFER" -> "transfer"
                    else -> ""
                }
                val convertedAmountRaw = item.transaction.convertedAmount?.toString() ?: ""
                val formattedConverted = item.transaction.convertedAmount?.let {
                    CurrencyFormatter.formatAmount(it, item.toAccount?.currency ?: item.transaction.currency).lowercase()
                } ?: ""

                note.contains(q) ||
                catName.contains(q) ||
                accName.contains(q) ||
                toAccName.contains(q) ||
                amountRaw.contains(q) ||
                currency.contains(q) ||
                formattedAmount.contains(q) ||
                type.contains(q) ||
                typeLabel.contains(q) ||
                convertedAmountRaw.contains(q) ||
                formattedConverted.contains(q)
            }

            matchesType && matchesAccount && matchesCategory && matchesDate && matchesAmount && matchesSearch
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
        .map { list ->
            list.filter { it.scheduledPayment.isActive }
                .sortedWith(
                    compareBy<ScheduledPaymentWithDetails> {
                        when (ScheduledPaymentCalculator.calculateStatus(it.scheduledPayment.nextPaymentDateMillis).status) {
                            ScheduledPaymentStatus.OVERDUE -> 0
                            ScheduledPaymentStatus.DUE_TODAY -> 1
                            ScheduledPaymentStatus.UPCOMING -> 2
                        }
                    }.thenBy { it.scheduledPayment.nextPaymentDateMillis }
                )
        }
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

    fun setFilterAccountId(accountId: Long?) {
        _filterAccountId.value = accountId
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

    fun setCustomDateRange(startMillis: Long?, endMillis: Long?) {
        _customStartDateMillis.value = startMillis
        _customEndDateMillis.value = endMillis
        _transactionDateRange.value = DateFilterRange.CUSTOM
    }

    fun setAmountRange(min: Long?, max: Long?) {
        _minAmount.value = min
        _maxAmount.value = max
    }

    fun applyAdvancedFilters(
        type: TransactionFilterType,
        accountId: Long?,
        categoryId: Long?,
        dateRange: DateFilterRange,
        customStartMillis: Long?,
        customEndMillis: Long?,
        minAmount: Long?,
        maxAmount: Long?
    ) {
        _transactionFilterType.value = type
        _filterAccountId.value = accountId
        _filterCategoryId.value = categoryId
        _transactionDateRange.value = dateRange
        _customStartDateMillis.value = customStartMillis
        _customEndDateMillis.value = customEndMillis
        _minAmount.value = minAmount
        _maxAmount.value = maxAmount
    }

    fun clearAllFilters() {
        _transactionFilterType.value = TransactionFilterType.ALL
        _filterAccountId.value = null
        _filterCategoryId.value = null
        _transactionDateRange.value = DateFilterRange.ALL_TIME
        _customStartDateMillis.value = null
        _customEndDateMillis.value = null
        _minAmount.value = null
        _maxAmount.value = null
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun resetAllFiltersAndSearch() {
        clearSearchQuery()
        clearAllFilters()
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

            val isOneTime = payment.frequency.trim().uppercase() in listOf("ONCE", "ONE_TIME")
            if (isOneTime) {
                repository.updateScheduledPayment(
                    payment.copy(isActive = false)
                )
            } else {
                val nextDate = ScheduledPaymentCalculator.calculateNextDate(
                    payment.nextPaymentDateMillis,
                    payment.frequency
                )
                repository.updateScheduledPayment(
                    payment.copy(nextPaymentDateMillis = nextDate)
                )
            }
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

    companion object {
        fun calculateDateBounds(
            dateRange: DateFilterRange,
            customStart: Long?,
            customEnd: Long?
        ): Pair<Long, Long> {
            return when (dateRange) {
                DateFilterRange.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
                DateFilterRange.TODAY -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis
                    Pair(start, end)
                }
                DateFilterRange.THIS_WEEK -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_WEEK, 6)
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis
                    Pair(start, end)
                }
                DateFilterRange.THIS_MONTH -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis
                    Pair(start, end)
                }
                DateFilterRange.THIS_YEAR -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis
                    Pair(start, end)
                }
                DateFilterRange.CUSTOM -> {
                    val start = customStart?.let {
                        Calendar.getInstance().apply {
                            timeInMillis = it
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    } ?: 0L

                    val end = customEnd?.let {
                        Calendar.getInstance().apply {
                            timeInMillis = it
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                    } ?: Long.MAX_VALUE

                    Pair(start, end)
                }
            }
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
