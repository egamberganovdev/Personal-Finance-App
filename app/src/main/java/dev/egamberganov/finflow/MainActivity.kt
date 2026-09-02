package dev.egamberganov.finflow

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.egamberganov.finflow.data.database.AppDatabase
import dev.egamberganov.finflow.data.entity.AccountEntity
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.ScheduledPaymentWithDetails
import dev.egamberganov.finflow.data.entity.TransactionWithDetails
import dev.egamberganov.finflow.data.repository.FinanceRepository
import dev.egamberganov.finflow.ui.components.AccountSelectorSheet
import dev.egamberganov.finflow.ui.components.AddTransactionSheet
import dev.egamberganov.finflow.ui.components.AppBottomNavBar
import dev.egamberganov.finflow.ui.components.CreateAccountDialog
import dev.egamberganov.finflow.ui.components.CreateCategoryDialog
import dev.egamberganov.finflow.ui.components.CreateScheduledPaymentDialog
import dev.egamberganov.finflow.ui.components.InfoContentDialog
import dev.egamberganov.finflow.ui.components.LanguageSelectionDialog
import dev.egamberganov.finflow.ui.components.ManageAccountsDialog
import dev.egamberganov.finflow.ui.components.ManageCategoriesDialog
import dev.egamberganov.finflow.ui.components.ManageScheduledPaymentsDialog
import dev.egamberganov.finflow.ui.components.NavScreen
import dev.egamberganov.finflow.ui.components.ThemeSelectionDialog
import dev.egamberganov.finflow.ui.components.TransactionDetailDialog
import dev.egamberganov.finflow.ui.screens.HomeScreen
import dev.egamberganov.finflow.ui.screens.MenuScreen
import dev.egamberganov.finflow.ui.screens.OnboardingScreen
import dev.egamberganov.finflow.ui.screens.StatisticsScreen
import dev.egamberganov.finflow.ui.screens.TransactionsScreen
import dev.egamberganov.finflow.ui.theme.PersonalFinanceTheme
import dev.egamberganov.finflow.ui.viewmodel.FinanceViewModel
import dev.egamberganov.finflow.ui.viewmodel.FinanceViewModelFactory
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = FinanceRepository(database)
        val factory = FinanceViewModelFactory(repository)

        setContent {
            val viewModel: FinanceViewModel = viewModel(factory = factory)
            val appSettings by viewModel.appSettings.collectAsState()

            ProvideLocalizedResources(languageCode = appSettings?.languageCode ?: "en") {
                PersonalFinanceTheme(themeMode = appSettings?.themeMode ?: "SYSTEM") {
                    FinanceAppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ProvideLocalizedResources(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val locale = remember(languageCode) {
        when (languageCode.lowercase()) {
            "uz" -> Locale("uz")
            "ru" -> Locale("ru")
            else -> Locale("en")
        }
    }

    val currentConfig = LocalConfiguration.current
    val localizedConfig = remember(locale, currentConfig) {
        Configuration(currentConfig).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }

    val localizedContext = remember(locale, context) {
        context.createConfigurationContext(localizedConfig)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig
    ) {
        content()
    }
}

@Composable
fun FinanceAppRoot(viewModel: FinanceViewModel) {
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val selectedAccount = allAccounts.find { it.account.id == selectedAccountId }

    val financialSummary by viewModel.financialSummary.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val upcomingPayments by viewModel.upcomingPayments.collectAsState()
    val allScheduledPayments by viewModel.allScheduledPayments.collectAsState()

    val allCategories by viewModel.allCategories.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    val transactionFilterType by viewModel.transactionFilterType.collectAsState()
    val transactionDateRange by viewModel.transactionDateRange.collectAsState()
    val filterCategoryId by viewModel.filterCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val periodActivities by viewModel.periodActivities.collectAsState()
    val expenseCategorySpends by viewModel.expenseCategorySpends.collectAsState()
    val incomeCategorySpends by viewModel.incomeCategorySpends.collectAsState()

    val appSettings by viewModel.appSettings.collectAsState()

    // Navigation State
    var currentScreen by remember { mutableStateOf(NavScreen.HOME) }

    // Dialog & Sheet States
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var selectedDetailTransaction by remember { mutableStateOf<TransactionWithDetails?>(null) }

    var showAccountSelectorSheet by remember { mutableStateOf(false) }
    var showManageAccountsDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showCreateAccountDialog by remember { mutableStateOf(false) }

    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    var showManageScheduledPaymentsDialog by remember { mutableStateOf(false) }
    var editingScheduledPayment by remember { mutableStateOf<ScheduledPaymentWithDetails?>(null) }
    var showCreateScheduledPaymentDialog by remember { mutableStateOf(false) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (!isOnboardingCompleted) {
        OnboardingScreen(
            onComplete = { accountName, currency, initialBalance ->
                viewModel.completeOnboarding(accountName, currency, initialBalance)
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                AppBottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen },
                    onAddClick = {
                        editingTransaction = null
                        showAddTransactionSheet = true
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    NavScreen.HOME -> {
                        HomeScreen(
                            summary = financialSummary,
                            selectedAccount = selectedAccount,
                            recentTransactions = recentTransactions,
                            upcomingPayments = upcomingPayments,
                            onOpenAccountSelector = { showAccountSelectorSheet = true },
                            onViewAllTransactions = { currentScreen = NavScreen.TRANSACTIONS },
                            onTransactionClick = { tx -> selectedDetailTransaction = tx },
                            onScheduledPaymentClick = { payment ->
                                editingScheduledPayment = payment
                                showCreateScheduledPaymentDialog = true
                            }
                        )
                    }

                    NavScreen.TRANSACTIONS -> {
                        TransactionsScreen(
                            transactions = filteredTransactions,
                            categories = allCategories,
                            typeFilter = transactionFilterType,
                            dateRange = transactionDateRange,
                            selectedCategoryId = filterCategoryId,
                            searchQuery = searchQuery,
                            onTypeFilterChange = { viewModel.setTransactionFilterType(it) },
                            onDateRangeChange = { viewModel.setTransactionDateRange(it) },
                            onCategoryFilterChange = { viewModel.setFilterCategoryId(it) },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onTransactionClick = { tx -> selectedDetailTransaction = tx }
                        )
                    }

                    NavScreen.STATISTICS -> {
                        StatisticsScreen(
                            selectedPeriod = selectedPeriod,
                            periodActivities = periodActivities,
                            expenseCategorySpends = expenseCategorySpends,
                            incomeCategorySpends = incomeCategorySpends,
                            primaryCurrency = financialSummary.primaryCurrency,
                            onPeriodChange = { viewModel.setSelectedPeriod(it) }
                        )
                    }

                    NavScreen.MENU -> {
                        MenuScreen(
                            accounts = allAccounts,
                            categoriesCount = allCategories.size,
                            scheduledPaymentsCount = allScheduledPayments.count { it.scheduledPayment.isActive },
                            appSettings = appSettings,
                            onOpenManageAccounts = { showManageAccountsDialog = true },
                            onOpenManageCategories = { showManageCategoriesDialog = true },
                            onOpenManageScheduledPayments = { showManageScheduledPaymentsDialog = true },
                            onOpenAppearanceDialog = { showThemeDialog = true },
                            onOpenLanguageDialog = { showLanguageDialog = true },
                            onToggleNotifications = { viewModel.updateNotificationsEnabled(it) },
                            onOpenContactSupport = { showSupportDialog = true },
                            onOpenTermsOfUse = { showTermsDialog = true },
                            onOpenAbout = { showAboutDialog = true }
                        )
                    }
                }
            }
        }

        // Add / Edit Transaction Sheet
        if (showAddTransactionSheet) {
            AddTransactionSheet(
                accounts = allAccounts,
                selectedAccountId = selectedAccountId,
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories,
                editingTransaction = editingTransaction,
                onSaveTransaction = { type, amount, currency, categoryId, accountId, dateMillis, note, attachmentUri ->
                    if (editingTransaction != null) {
                        viewModel.updateTransaction(
                            editingTransaction!!.transaction.copy(
                                type = type,
                                amount = amount,
                                currency = currency,
                                categoryId = categoryId,
                                accountId = accountId,
                                dateMillis = dateMillis,
                                note = note,
                                attachmentUri = attachmentUri
                            )
                        )
                    } else {
                        viewModel.addTransaction(
                            type = type,
                            amount = amount,
                            currency = currency,
                            categoryId = categoryId,
                            accountId = accountId,
                            dateMillis = dateMillis,
                            note = note,
                            attachmentUri = attachmentUri
                        )
                    }
                    showAddTransactionSheet = false
                    editingTransaction = null
                },
                onOpenScheduledPayment = {
                    showAddTransactionSheet = false
                    showCreateScheduledPaymentDialog = true
                },
                onDismiss = {
                    showAddTransactionSheet = false
                    editingTransaction = null
                }
            )
        }

        // Account Context Selector Sheet
        if (showAccountSelectorSheet) {
            AccountSelectorSheet(
                accounts = allAccounts,
                selectedAccountId = selectedAccountId,
                onSelectAccount = { viewModel.selectAccount(it) },
                onAddAccountClick = {
                    editingAccount = null
                    showCreateAccountDialog = true
                },
                onManageAccountsClick = { showManageAccountsDialog = true },
                onDismiss = { showAccountSelectorSheet = false }
            )
        }

        // Transaction Detail Modal
        if (selectedDetailTransaction != null) {
            TransactionDetailDialog(
                item = selectedDetailTransaction!!,
                onEdit = {
                    editingTransaction = selectedDetailTransaction
                    selectedDetailTransaction = null
                    showAddTransactionSheet = true
                },
                onDelete = {
                    viewModel.deleteTransaction(selectedDetailTransaction!!.transaction)
                    selectedDetailTransaction = null
                },
                onDismiss = { selectedDetailTransaction = null }
            )
        }

        // Manage Accounts Modal
        if (showManageAccountsDialog) {
            ManageAccountsDialog(
                accounts = allAccounts,
                onAddAccount = {
                    editingAccount = null
                    showCreateAccountDialog = true
                },
                onEditAccount = { acc ->
                    editingAccount = acc
                    showCreateAccountDialog = true
                },
                onDeleteAccount = { acc -> viewModel.deleteAccount(acc) },
                onDismiss = { showManageAccountsDialog = false }
            )
        }

        // Create / Edit Account Dialog
        if (showCreateAccountDialog) {
            CreateAccountDialog(
                editingAccount = editingAccount,
                onSave = { name, currency, initialBalance, colorHex ->
                    if (editingAccount != null) {
                        viewModel.updateAccount(
                            editingAccount!!.copy(
                                name = name,
                                currency = currency,
                                initialBalance = initialBalance,
                                colorHex = colorHex
                            )
                        )
                    } else {
                        viewModel.createAccount(name, currency, initialBalance, colorHex)
                    }
                    showCreateAccountDialog = false
                    editingAccount = null
                },
                onDismiss = {
                    showCreateAccountDialog = false
                    editingAccount = null
                }
            )
        }

        // Manage Categories Modal
        if (showManageCategoriesDialog) {
            ManageCategoriesDialog(
                allCategories = allCategories,
                onAddCategory = {
                    editingCategory = null
                    showCreateCategoryDialog = true
                },
                onEditCategory = { cat ->
                    editingCategory = cat
                    showCreateCategoryDialog = true
                },
                onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
                onDismiss = { showManageCategoriesDialog = false }
            )
        }

        // Create / Edit Category Dialog
        if (showCreateCategoryDialog) {
            CreateCategoryDialog(
                editingCategory = editingCategory,
                onSave = { name, type, iconName, colorHex ->
                    if (editingCategory != null) {
                        viewModel.updateCategory(
                            editingCategory!!.copy(
                                name = name,
                                type = type,
                                iconName = iconName,
                                colorHex = colorHex
                            )
                        )
                    } else {
                        viewModel.createCategory(name, type, iconName, colorHex)
                    }
                    showCreateCategoryDialog = false
                    editingCategory = null
                },
                onDismiss = {
                    showCreateCategoryDialog = false
                    editingCategory = null
                }
            )
        }

        // Manage Scheduled Payments Modal
        if (showManageScheduledPaymentsDialog) {
            ManageScheduledPaymentsDialog(
                scheduledPayments = allScheduledPayments,
                onAddScheduledPayment = {
                    editingScheduledPayment = null
                    showCreateScheduledPaymentDialog = true
                },
                onEditScheduledPayment = { payment ->
                    editingScheduledPayment = payment
                    showCreateScheduledPaymentDialog = true
                },
                onDeleteScheduledPayment = { payment ->
                    viewModel.deleteScheduledPayment(payment.scheduledPayment)
                },
                onToggleActive = { payment ->
                    viewModel.toggleScheduledPaymentActive(payment.scheduledPayment)
                },
                onRecordAsTransaction = { payment ->
                    viewModel.recordScheduledPaymentAsTransaction(payment.scheduledPayment)
                },
                onDismiss = { showManageScheduledPaymentsDialog = false }
            )
        }

        // Create / Edit Scheduled Payment Dialog
        if (showCreateScheduledPaymentDialog) {
            CreateScheduledPaymentDialog(
                accounts = allAccounts,
                categories = allCategories,
                editingPayment = editingScheduledPayment,
                onSave = { name, amount, currency, categoryId, accountId, frequency, nextPaymentDateMillis, note, isActive ->
                    if (editingScheduledPayment != null) {
                        viewModel.updateScheduledPayment(
                            editingScheduledPayment!!.scheduledPayment.copy(
                                name = name,
                                amount = amount,
                                currency = currency,
                                categoryId = categoryId,
                                accountId = accountId,
                                frequency = frequency,
                                nextPaymentDateMillis = nextPaymentDateMillis,
                                note = note,
                                isActive = isActive
                            )
                        )
                    } else {
                        viewModel.createScheduledPayment(
                            name = name,
                            amount = amount,
                            currency = currency,
                            categoryId = categoryId,
                            accountId = accountId,
                            frequency = frequency,
                            nextPaymentDateMillis = nextPaymentDateMillis,
                            note = note,
                            isActive = isActive
                        )
                    }
                    showCreateScheduledPaymentDialog = false
                    editingScheduledPayment = null
                },
                onDismiss = {
                    showCreateScheduledPaymentDialog = false
                    editingScheduledPayment = null
                }
            )
        }

        // Theme Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = appSettings?.themeMode ?: "System",
                onSelectTheme = { viewModel.updateThemeMode(it) },
                onDismiss = { showThemeDialog = false }
            )
        }

        // Language Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = appSettings?.languageCode ?: "en",
                onSelectLanguage = { viewModel.updateLanguage(it) },
                onDismiss = { showLanguageDialog = false }
            )
        }

        // Support Dialog
        if (showSupportDialog) {
            InfoContentDialog(
                title = "Contact Support",
                content = "Need help or have feedback? Reach out to our team at support@financeapp.local or visit our community documentation.",
                onDismiss = { showSupportDialog = false }
            )
        }

        // Terms Dialog
        if (showTermsDialog) {
            InfoContentDialog(
                title = "Terms of Use",
                content = "This Personal Finance application operates 100% locally on your device. Your financial logs, account balances, and category allocations never leave your hardware. No external tracking or telemetry is gathered.",
                onDismiss = { showTermsDialog = false }
            )
        }

        // About Dialog
        if (showAboutDialog) {
            InfoContentDialog(
                title = "About Personal Finance Manager",
                content = "Personal Finance Manager V1\n\nDesigned for intuitive cash and expense tracking, account balance management, upcoming scheduled payment monitoring, and deep statistical financial clarity.",
                onDismiss = { showAboutDialog = false }
            )
        }
    }
}
