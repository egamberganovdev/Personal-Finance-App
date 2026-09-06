package dev.egamberganov.finflow

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.egamberganov.finflow.data.database.AppDatabase
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.TransactionEntity
import dev.egamberganov.finflow.data.repository.FinanceRepository
import dev.egamberganov.finflow.domain.model.AccountType
import dev.egamberganov.finflow.domain.model.DateFilterRange
import dev.egamberganov.finflow.domain.model.TransactionFilterType
import dev.egamberganov.finflow.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionsFeatureTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel
    private lateinit var context: Context

    private var cashAccId: Long = 0
    private var bankAccId: Long = 0
    private var foodCatId: Long = 0
    private var salaryCatId: Long = 0

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db)

        // Seed basic accounts
        cashAccId = repository.createAccount("Cash Wallet", AccountType.CASH.dbKey, "USD", 1000L, "#10B981")
        bankAccId = repository.createAccount("Main Bank", AccountType.BANK.dbKey, "USD", 5000L, "#3B82F6")

        // Seed basic categories
        foodCatId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Groceries & Food", iconName = "food", colorHex = "#F59E0B", type = "EXPENSE")
        )
        salaryCatId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Monthly Salary", iconName = "salary", colorHex = "#10B981", type = "INCOME")
        )

        viewModel = FinanceViewModel(repository)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun verifySearchFiltersByNoteCategoryAndAccount() = runBlocking {
        val now = System.currentTimeMillis()

        // Insert 3 transactions
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "EXPENSE",
                amount = 45L,
                currency = "USD",
                categoryId = foodCatId,
                accountId = cashAccId,
                dateMillis = now,
                note = "Supermarket organic milk"
            )
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "INCOME",
                amount = 3000L,
                currency = "USD",
                categoryId = salaryCatId,
                accountId = bankAccId,
                dateMillis = now,
                note = "Tech company paycheck"
            )
        )
        repository.createTransfer(
            fromAccountId = bankAccId,
            toAccountId = cashAccId,
            amount = 200L,
            dateMillis = now,
            note = "ATM withdrawal"
        )

        // 1. Search note "organic" (case-insensitive "ORGANIC")
        viewModel.setSearchQuery("ORGANIC")
        var results = viewModel.filteredTransactions.first { it.isNotEmpty() }
        assertEquals(1, results.size)
        assertEquals(45L, results[0].transaction.amount)

        // 2. Search category "Salary"
        viewModel.setSearchQuery("salary")
        results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals(3000L, results[0].transaction.amount)

        // 3. Search transfer note "withdrawal"
        viewModel.setSearchQuery("withdrawal")
        results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals("TRANSFER", results[0].transaction.type)
        assertEquals(200L, results[0].transaction.amount)

        // 4. Search amount "3000"
        viewModel.setSearchQuery("3000")
        results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals(3000L, results[0].transaction.amount)

        // 5. Clear search restores all 3
        viewModel.clearSearchQuery()
        val allResults = viewModel.filteredTransactions.first { it.size == 3 }
        assertEquals(3, allResults.size)
    }

    @Test
    fun verifyFilterByType() = runBlocking {
        val now = System.currentTimeMillis()

        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "EXPENSE",
                amount = 50L,
                currency = "USD",
                categoryId = foodCatId,
                accountId = cashAccId,
                dateMillis = now
            )
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "INCOME",
                amount = 1500L,
                currency = "USD",
                categoryId = salaryCatId,
                accountId = bankAccId,
                dateMillis = now
            )
        )
        repository.createTransfer(
            fromAccountId = cashAccId,
            toAccountId = bankAccId,
            amount = 100L,
            dateMillis = now,
            note = null
        )

        // Filter TRANSFER
        viewModel.setTransactionFilterType(TransactionFilterType.TRANSFER)
        var results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals("TRANSFER", results[0].transaction.type)

        // Filter EXPENSE
        viewModel.setTransactionFilterType(TransactionFilterType.EXPENSE)
        results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals("EXPENSE", results[0].transaction.type)

        // Filter INCOME
        viewModel.setTransactionFilterType(TransactionFilterType.INCOME)
        results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals("INCOME", results[0].transaction.type)

        // Filter ALL
        viewModel.setTransactionFilterType(TransactionFilterType.ALL)
        results = viewModel.filteredTransactions.first { it.size == 3 }
        assertEquals(3, results.size)
    }

    @Test
    fun verifyFilterByAccountMatchesBothSourceAndDestination() = runBlocking {
        val now = System.currentTimeMillis()

        // Cash expense
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "EXPENSE",
                amount = 20L,
                currency = "USD",
                categoryId = foodCatId,
                accountId = cashAccId,
                dateMillis = now
            )
        )
        // Bank income
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "INCOME",
                amount = 2000L,
                currency = "USD",
                categoryId = salaryCatId,
                accountId = bankAccId,
                dateMillis = now
            )
        )
        // Transfer from Bank to Cash
        repository.createTransfer(
            fromAccountId = bankAccId,
            toAccountId = cashAccId,
            amount = 300L,
            dateMillis = now,
            note = null
        )

        // Filter by Cash account -> Should include Cash expense AND the Transfer!
        viewModel.setFilterAccountId(cashAccId)
        val cashResults = viewModel.filteredTransactions.first { it.size == 2 }
        assertEquals(2, cashResults.size)
        assertTrue(cashResults.any { it.transaction.type == "EXPENSE" })
        assertTrue(cashResults.any { it.transaction.type == "TRANSFER" })

        // Filter by Bank account -> Should include Bank income AND the Transfer!
        viewModel.setFilterAccountId(bankAccId)
        val bankResults = viewModel.filteredTransactions.first { it.size == 2 }
        assertEquals(2, bankResults.size)
        assertTrue(bankResults.any { it.transaction.type == "INCOME" })
        assertTrue(bankResults.any { it.transaction.type == "TRANSFER" })
    }

    @Test
    fun verifyAmountRangeFilter() = runBlocking {
        val now = System.currentTimeMillis()

        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "EXPENSE",
                amount = 10L,
                currency = "USD",
                categoryId = foodCatId,
                accountId = cashAccId,
                dateMillis = now
            )
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "EXPENSE",
                amount = 50L,
                currency = "USD",
                categoryId = foodCatId,
                accountId = cashAccId,
                dateMillis = now
            )
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "EXPENSE",
                amount = 200L,
                currency = "USD",
                categoryId = foodCatId,
                accountId = cashAccId,
                dateMillis = now
            )
        )

        // Min 30, Max 100 -> only the 50L transaction should match
        viewModel.setAmountRange(min = 30L, max = 100L)
        val results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals(1, results.size)
        assertEquals(50L, results[0].transaction.amount)
    }

    @Test
    fun verifyCombinedAndFiltersWorkSimultaneously() = runBlocking {
        val now = System.currentTimeMillis()

        // 1. Food expense from Cash, 50 USD
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "EXPENSE",
                amount = 50L,
                currency = "USD",
                categoryId = foodCatId,
                accountId = cashAccId,
                dateMillis = now,
                note = "Dinner at bistro"
            )
        )
        // 2. Food expense from Bank, 150 USD
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "EXPENSE",
                amount = 150L,
                currency = "USD",
                categoryId = foodCatId,
                accountId = bankAccId,
                dateMillis = now,
                note = "Family restaurant"
            )
        )
        // 3. Salary income to Bank, 2000 USD
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "INCOME",
                amount = 2000L,
                currency = "USD",
                categoryId = salaryCatId,
                accountId = bankAccId,
                dateMillis = now,
                note = "Monthly salary"
            )
        )

        // Apply combined filters:
        // Type = EXPENSE, Account = Cash, Category = Food
        viewModel.applyAdvancedFilters(
            type = TransactionFilterType.EXPENSE,
            accountId = cashAccId,
            categoryId = foodCatId,
            dateRange = DateFilterRange.ALL_TIME,
            customStartMillis = null,
            customEndMillis = null,
            minAmount = null,
            maxAmount = null
        )

        var results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals(1, results.size)
        assertEquals(50L, results[0].transaction.amount)

        // Search within this already filtered set: query "bistro" -> matches
        viewModel.setSearchQuery("bistro")
        results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals(1, results.size)

        // Search query "family" (which is on the bank expense) -> 0 matches because Account is Cash!
        viewModel.setSearchQuery("family")
        val emptyResults = viewModel.filteredTransactions.first { it.isEmpty() }
        assertEquals(0, emptyResults.size)

        // Clearing search restores the 1 filtered result
        viewModel.clearSearchQuery()
        results = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals(1, results.size)

        // Clearing filters but keeping search query preserves search
        viewModel.setSearchQuery("Family")
        viewModel.clearAllFilters()
        val restoredSearch = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals(1, restoredSearch.size)
        assertEquals(150L, restoredSearch[0].transaction.amount)
    }

    @Test
    fun verifyDateBoundsCalculation() {
        // Today bounds
        val (todayStart, todayEnd) = FinanceViewModel.calculateDateBounds(DateFilterRange.TODAY, null, null)
        val now = System.currentTimeMillis()
        assertTrue(now in todayStart..todayEnd)

        // Custom bounds
        val customStart = 1000000000L
        val customEnd = 2000000000L
        val (boundStart, boundEnd) = FinanceViewModel.calculateDateBounds(
            DateFilterRange.CUSTOM,
            customStart,
            customEnd
        )
        assertTrue(boundStart <= customStart)
        assertTrue(boundEnd >= customEnd)
    }
}
