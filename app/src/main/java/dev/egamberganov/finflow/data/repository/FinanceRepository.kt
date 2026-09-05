package dev.egamberganov.finflow.data.repository

import dev.egamberganov.finflow.data.database.AppDatabase
import dev.egamberganov.finflow.data.entity.AccountEntity
import dev.egamberganov.finflow.data.entity.AppSettingsEntity
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.ScheduledPaymentEntity
import dev.egamberganov.finflow.data.entity.ScheduledPaymentWithDetails
import dev.egamberganov.finflow.data.entity.TransactionEntity
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.data.network.ExchangeRateService
import dev.egamberganov.finflow.domain.model.AccountType
import dev.egamberganov.finflow.domain.model.AccountWithBalance
import dev.egamberganov.finflow.domain.model.FinancialSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class FinanceRepository(
    private val database: AppDatabase,
    private val exchangeRateService: ExchangeRateService = ExchangeRateService()
) {
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val scheduledPaymentDao = database.scheduledPaymentDao()
    private val appSettingsDao = database.appSettingsDao()

    val appSettings: Flow<AppSettingsEntity?> = appSettingsDao.getSettings()
    val selectedAccountId: Flow<Long?> = appSettings.map { it?.selectedAccountId }
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allTransactions: Flow<List<TransactionWithDetails>> = transactionDao.getAllTransactionsWithDetails()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val expenseCategories: Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType("EXPENSE")
    val incomeCategories: Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType("INCOME")
    val allScheduledPayments: Flow<List<ScheduledPaymentWithDetails>> = scheduledPaymentDao.getAllScheduledPaymentsWithDetails()

    // Reactive Accounts With Calculated Balances
    val accountsWithBalance: Flow<List<AccountWithBalance>> = combine(
        allAccounts,
        allTransactions
    ) { accounts, transactions ->
        accounts.map { account ->
            val accountTx = transactions.filter {
                it.transaction.accountId == account.id || it.transaction.toAccountId == account.id
            }
            val totalIncome = accountTx
                .filter { it.transaction.type == "INCOME" && it.transaction.accountId == account.id }
                .sumOf { it.transaction.amount }
            val totalExpense = accountTx
                .filter { it.transaction.type == "EXPENSE" && it.transaction.accountId == account.id }
                .sumOf { it.transaction.amount }
            val totalTransferOut = accountTx
                .filter { it.transaction.type == "TRANSFER" && it.transaction.accountId == account.id }
                .sumOf { it.transaction.amount }
            val totalTransferIn = accountTx
                .filter { it.transaction.type == "TRANSFER" && it.transaction.toAccountId == account.id }
                .sumOf { it.transaction.convertedAmount ?: it.transaction.amount }

            val currentBalance = account.initialBalance + totalIncome - totalExpense - totalTransferOut + totalTransferIn

            AccountWithBalance(
                account = account,
                currentBalance = currentBalance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                transactionCount = accountTx.size
            )
        }
    }

    // Selected Account Context (null = All Accounts)
    val selectedAccountWithBalance: Flow<AccountWithBalance?> = combine(
        appSettings,
        accountsWithBalance
    ) { settings, accounts ->
        val selectedId = settings?.selectedAccountId
        if (selectedId != null) {
            accounts.find { it.account.id == selectedId }
        } else {
            null
        }
    }

    // Filtered Transactions based on Selected Account
    val currentContextTransactions: Flow<List<TransactionWithDetails>> = combine(
        appSettings,
        allTransactions
    ) { settings, transactions ->
        val selectedId = settings?.selectedAccountId
        if (selectedId != null) {
            transactions.filter {
                it.transaction.accountId == selectedId || it.transaction.toAccountId == selectedId
            }
        } else {
            transactions
        }
    }

    // Financial Summary for Selected Account or All Accounts
    val currentFinancialSummary: Flow<FinancialSummary> = combine(
        selectedAccountWithBalance,
        accountsWithBalance,
        currentContextTransactions
    ) { selectedAccount, allAccountsBalance, transactions ->
        val totalIncome = transactions
            .filter { it.transaction.type == "INCOME" }
            .sumOf { it.transaction.amount }
        val totalExpense = transactions
            .filter { it.transaction.type == "EXPENSE" }
            .sumOf { it.transaction.amount }
        val netChange = totalIncome - totalExpense

        val (totalBalance, currency) = if (selectedAccount != null) {
            Pair(selectedAccount.currentBalance, selectedAccount.account.currency)
        } else {
            val primaryCurr = allAccountsBalance.firstOrNull()?.account?.currency ?: "UZS"
            val totalBal = allAccountsBalance.sumOf { it.currentBalance }
            Pair(totalBal, primaryCurr)
        }

        FinancialSummary(
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netChange = netChange,
            primaryCurrency = currency
        )
    }

    // Filtered Scheduled Payments
    val currentContextScheduledPayments: Flow<List<ScheduledPaymentWithDetails>> = combine(
        appSettings,
        allScheduledPayments
    ) { settings, payments ->
        val selectedId = settings?.selectedAccountId
        if (selectedId != null) {
            payments.filter { it.scheduledPayment.accountId == selectedId }
        } else {
            payments
        }
    }

    // Initialize Default Data (Categories and Settings)
    suspend fun initializeDefaultDataIfNeeded() {
        if (appSettingsDao.getSettingsDirect() == null) {
            appSettingsDao.insertOrUpdateSettings(
                AppSettingsEntity(
                    id = 1,
                    isOnboardingCompleted = false,
                    selectedAccountId = null,
                    themeMode = "SYSTEM",
                    languageCode = "en",
                    notificationsEnabled = true
                )
            )
        }

        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                // Expense
                CategoryEntity(name = "Food", type = "EXPENSE", iconName = "food", colorHex = "#FF9500", isDefault = true),
                CategoryEntity(name = "Transport", type = "EXPENSE", iconName = "transport", colorHex = "#5E5CE6", isDefault = true),
                CategoryEntity(name = "Shopping", type = "EXPENSE", iconName = "shopping", colorHex = "#EC4899", isDefault = true),
                CategoryEntity(name = "Bills", type = "EXPENSE", iconName = "bills", colorHex = "#EF4444", isDefault = true),
                CategoryEntity(name = "Entertainment", type = "EXPENSE", iconName = "entertainment", colorHex = "#8B5CF6", isDefault = true),
                CategoryEntity(name = "Health", type = "EXPENSE", iconName = "health", colorHex = "#10B981", isDefault = true),
                CategoryEntity(name = "Other", type = "EXPENSE", iconName = "other", colorHex = "#6B7280", isDefault = true),
                // Income
                CategoryEntity(name = "Salary", type = "INCOME", iconName = "salary", colorHex = "#10B981", isDefault = true),
                CategoryEntity(name = "Freelance", type = "INCOME", iconName = "freelance", colorHex = "#3B82F6", isDefault = true),
                CategoryEntity(name = "Business", type = "INCOME", iconName = "business", colorHex = "#F59E0B", isDefault = true),
                CategoryEntity(name = "Gift", type = "INCOME", iconName = "gift", colorHex = "#EC4899", isDefault = true),
                CategoryEntity(name = "Other", type = "INCOME", iconName = "other", colorHex = "#6B7280", isDefault = true)
            )
            categoryDao.insertAllCategories(defaultCategories)
        }
    }

    // Onboarding Account Setup
    suspend fun completeOnboarding(
        accountName: String,
        accountType: String = AccountType.CASH.dbKey,
        currency: String,
        initialBalance: Long
    ): Long {
        initializeDefaultDataIfNeeded()

        val account = AccountEntity(
            name = accountName.trim().ifEmpty { "My Wallet" },
            type = accountType.trim().ifEmpty { AccountType.CASH.dbKey },
            currency = currency.trim().ifEmpty { "UZS" },
            initialBalance = initialBalance,
            colorHex = "#5E5CE6"
        )
        val newAccountId = accountDao.insertAccount(account)

        appSettingsDao.setSelectedAccountId(newAccountId)
        appSettingsDao.setOnboardingCompleted(true)

        return newAccountId
    }

    // Account Operations
    suspend fun createAccount(
        name: String,
        type: String = AccountType.CASH.dbKey,
        currency: String,
        initialBalance: Long,
        colorHex: String
    ): Long {
        val account = AccountEntity(
            name = name.trim(),
            type = type.trim(),
            currency = currency.trim(),
            initialBalance = initialBalance,
            colorHex = colorHex
        )
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun getTransactionCountForAccount(accountId: Long): Int {
        return transactionDao.getTransactionCountForAccount(accountId)
    }

    suspend fun canDeleteAccount(accountId: Long): Boolean {
        val txCount = transactionDao.getTransactionCountForAccount(accountId)
        val scheduledCount = scheduledPaymentDao.getScheduledPaymentCountForAccount(accountId)
        return txCount == 0 && scheduledCount == 0
    }

    suspend fun deleteAccount(account: AccountEntity): Boolean {
        // Safe account deletion: refuse to delete if transactions or scheduled payments exist
        if (!canDeleteAccount(account.id)) {
            return false
        }
        // If the deleted account was selected, reset to All Accounts
        val currentSettings = appSettingsDao.getSettingsDirect()
        if (currentSettings?.selectedAccountId == account.id) {
            appSettingsDao.setSelectedAccountId(null)
        }
        accountDao.deleteAccount(account)
        return true
    }

    // Transaction Operations
    suspend fun getExchangeRate(fromCurrency: String, toCurrency: String): Result<Double> {
        return exchangeRateService.getExchangeRate(fromCurrency, toCurrency)
    }

    suspend fun getAccountBalance(accountId: Long): Long {
        val account = accountDao.getAccountByIdDirect(accountId) ?: return 0L
        val allTx = transactionDao.getAllTransactionsList()
        val fromTx = allTx.filter { it.accountId == accountId || it.toAccountId == accountId }
        val income = fromTx.filter { it.type == "INCOME" && it.accountId == accountId }.sumOf { it.amount }
        val expense = fromTx.filter { it.type == "EXPENSE" && it.accountId == accountId }.sumOf { it.amount }
        val transferOut = fromTx.filter { it.type == "TRANSFER" && it.accountId == accountId }.sumOf { it.amount }
        val transferIn = fromTx.filter { it.type == "TRANSFER" && it.toAccountId == accountId }.sumOf { it.convertedAmount ?: it.amount }
        return account.initialBalance + income - expense - transferOut + transferIn
    }

    suspend fun createTransaction(
        type: String,
        amount: Long,
        currency: String,
        categoryId: Long?,
        accountId: Long,
        toAccountId: Long? = null,
        exchangeRate: Double? = null,
        convertedAmount: Long? = null,
        dateMillis: Long,
        note: String?,
        attachmentUri: String?
    ): Long {
        val transaction = TransactionEntity(
            type = type,
            amount = amount,
            currency = currency,
            categoryId = categoryId,
            accountId = accountId,
            toAccountId = toAccountId,
            exchangeRate = exchangeRate,
            convertedAmount = convertedAmount,
            dateMillis = dateMillis,
            note = note?.trim()?.ifEmpty { null },
            attachmentUri = attachmentUri?.trim()?.ifEmpty { null }
        )
        return transactionDao.insertTransaction(transaction)
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
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Transfer amount must be greater than zero"))
        }
        if (fromAccountId == toAccountId) {
            return Result.failure(IllegalArgumentException("Source and destination accounts must be different"))
        }

        val fromAccount = accountDao.getAccountByIdDirect(fromAccountId)
            ?: return Result.failure(IllegalArgumentException("Source account not found"))
        val toAccount = accountDao.getAccountByIdDirect(toAccountId)
            ?: return Result.failure(IllegalArgumentException("Destination account not found"))

        val currentBalance = getAccountBalance(fromAccountId)
        if (amount > currentBalance) {
            return Result.failure(IllegalStateException("Insufficient funds in source account"))
        }

        val isDifferentCurrency = !fromAccount.currency.equals(toAccount.currency, ignoreCase = true)
        val finalRate: Double?
        val finalConvertedAmount: Long?

        if (isDifferentCurrency) {
            if (exchangeRate == null || exchangeRate <= 0.0) {
                return Result.failure(IllegalArgumentException("Exchange rate required for different currencies"))
            }
            if (convertedAmount == null || convertedAmount <= 0) {
                return Result.failure(IllegalArgumentException("Converted amount must be greater than zero"))
            }
            finalRate = exchangeRate
            finalConvertedAmount = convertedAmount
        } else {
            finalRate = null
            finalConvertedAmount = amount
        }

        val transaction = TransactionEntity(
            type = "TRANSFER",
            amount = amount,
            currency = fromAccount.currency,
            categoryId = null,
            accountId = fromAccountId,
            toAccountId = toAccountId,
            exchangeRate = finalRate,
            convertedAmount = finalConvertedAmount,
            dateMillis = dateMillis,
            note = note?.trim()?.ifEmpty { null },
            attachmentUri = attachmentUri?.trim()?.ifEmpty { null }
        )
        val id = transactionDao.insertTransaction(transaction)
        return Result.success(id)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    // Category Operations
    suspend fun createCategory(name: String, type: String, iconName: String, colorHex: String): Long {
        val category = CategoryEntity(
            name = name.trim(),
            type = type,
            iconName = iconName,
            colorHex = colorHex,
            isDefault = false
        )
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    // Scheduled Payment Operations
    suspend fun createScheduledPayment(
        name: String,
        amount: Long,
        currency: String,
        categoryId: Long,
        accountId: Long,
        frequency: String,
        nextPaymentDateMillis: Long,
        note: String?,
        isActive: Boolean = true
    ): Long {
        val payment = ScheduledPaymentEntity(
            name = name.trim(),
            amount = amount,
            currency = currency,
            categoryId = categoryId,
            accountId = accountId,
            frequency = frequency,
            nextPaymentDateMillis = nextPaymentDateMillis,
            note = note?.trim()?.ifEmpty { null },
            isActive = isActive
        )
        return scheduledPaymentDao.insertScheduledPayment(payment)
    }

    suspend fun updateScheduledPayment(payment: ScheduledPaymentEntity) {
        scheduledPaymentDao.updateScheduledPayment(payment)
    }

    suspend fun deleteScheduledPayment(payment: ScheduledPaymentEntity) {
        scheduledPaymentDao.deleteScheduledPayment(payment)
    }

    suspend fun toggleScheduledPaymentActive(id: Long, isActive: Boolean) {
        scheduledPaymentDao.toggleActive(id, isActive)
    }

    // Settings Operations
    suspend fun selectAccount(accountId: Long?) {
        appSettingsDao.setSelectedAccountId(accountId)
    }

    suspend fun setThemeMode(mode: String) {
        appSettingsDao.setThemeMode(mode)
    }

    suspend fun setLanguageCode(code: String) {
        appSettingsDao.setLanguageCode(code)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appSettingsDao.setNotificationsEnabled(enabled)
    }
}
