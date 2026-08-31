package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.AccountEntity
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ScheduledPaymentEntity
import com.example.data.entity.ScheduledPaymentWithDetails
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionWithDetails
import com.example.domain.model.AccountWithBalance
import com.example.domain.model.FinancialSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class FinanceRepository(private val database: AppDatabase) {
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
            val accountTx = transactions.filter { it.transaction.accountId == account.id }
            val totalIncome = accountTx
                .filter { it.transaction.type == "INCOME" }
                .sumOf { it.transaction.amount }
            val totalExpense = accountTx
                .filter { it.transaction.type == "EXPENSE" }
                .sumOf { it.transaction.amount }
            val currentBalance = account.initialBalance + totalIncome - totalExpense

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
            transactions.filter { it.transaction.accountId == selectedId }
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
        currency: String,
        initialBalance: Long
    ): Long {
        initializeDefaultDataIfNeeded()

        val account = AccountEntity(
            name = accountName.trim().ifEmpty { "My Wallet" },
            type = "Cash",
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
    suspend fun createAccount(name: String, currency: String, initialBalance: Long, colorHex: String): Long {
        val account = AccountEntity(
            name = name.trim(),
            type = "Cash",
            currency = currency.trim(),
            initialBalance = initialBalance,
            colorHex = colorHex
        )
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        // If the deleted account was selected, reset to All Accounts
        val currentSettings = appSettingsDao.getSettingsDirect()
        if (currentSettings?.selectedAccountId == account.id) {
            appSettingsDao.setSelectedAccountId(null)
        }
        accountDao.deleteAccount(account)
    }

    // Transaction Operations
    suspend fun createTransaction(
        type: String,
        amount: Long,
        currency: String,
        categoryId: Long,
        accountId: Long,
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
            dateMillis = dateMillis,
            note = note?.trim()?.ifEmpty { null },
            attachmentUri = attachmentUri?.trim()?.ifEmpty { null }
        )
        return transactionDao.insertTransaction(transaction)
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
