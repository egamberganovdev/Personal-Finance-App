package dev.egamberganov.finflow

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.egamberganov.finflow.data.database.AppDatabase
import dev.egamberganov.finflow.data.entity.AccountEntity
import dev.egamberganov.finflow.data.repository.FinanceRepository
import dev.egamberganov.finflow.domain.model.AccountType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransferFeatureTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun verifyTransferSameCurrencyUpdatesBalancesAccurately() = runBlocking {
        // Setup: Source account with 1,000,000 UZS, Destination with 200,000 UZS
        val fromAccId = repository.createAccount("Cash Wallet", AccountType.CASH.dbKey, "UZS", 1_000_000L, "#10B981")
        val toAccId = repository.createAccount("Bank Card", AccountType.BANK.dbKey, "UZS", 200_000L, "#3B82F6")

        assertEquals(1_000_000L, repository.getAccountBalance(fromAccId))
        assertEquals(200_000L, repository.getAccountBalance(toAccId))

        // Transfer 300,000 UZS
        val result = repository.createTransfer(
            fromAccountId = fromAccId,
            toAccountId = toAccId,
            amount = 300_000L,
            dateMillis = System.currentTimeMillis(),
            note = "ATM deposit"
        )
        assertTrue(result.isSuccess)

        // Check balances
        val fromBalance = repository.getAccountBalance(fromAccId)
        val toBalance = repository.getAccountBalance(toAccId)

        assertEquals(700_000L, fromBalance)
        assertEquals(500_000L, toBalance)

        // Verify transaction record
        val txId = result.getOrThrow()
        val tx = db.transactionDao().getTransactionById(txId)
        assertNotNull(tx)
        assertEquals("TRANSFER", tx!!.type)
        assertEquals(300_000L, tx.amount)
        assertEquals("UZS", tx.currency)
        assertEquals(fromAccId, tx.accountId)
        assertEquals(toAccId, tx.toAccountId)
        assertEquals(300_000L, tx.convertedAmount)
    }

    @Test
    fun verifyTransferCrossCurrencyUpdatesBalancesWithConversion() = runBlocking {
        // Setup: Source USD account with $500, Destination UZS account with 0 UZS
        val usdAccId = repository.createAccount("USD Savings", AccountType.SAVINGS.dbKey, "USD", 500L, "#6366F1")
        val uzsAccId = repository.createAccount("UZS Bank", AccountType.BANK.dbKey, "UZS", 0L, "#3B82F6")

        // Transfer 100 USD at rate 12800 -> 1,280,000 UZS
        val rate = 12800.0
        val convertedAmount = 1_280_000L

        val result = repository.createTransfer(
            fromAccountId = usdAccId,
            toAccountId = uzsAccId,
            amount = 100L,
            dateMillis = System.currentTimeMillis(),
            note = "Exchanged USD to UZS",
            exchangeRate = rate,
            convertedAmount = convertedAmount
        )
        assertTrue(result.isSuccess)

        // Verify source balance deducted in USD, destination credited in UZS
        assertEquals(400L, repository.getAccountBalance(usdAccId))
        assertEquals(1_280_000L, repository.getAccountBalance(uzsAccId))

        // Verify stored transaction entity
        val tx = db.transactionDao().getTransactionById(result.getOrThrow())
        assertNotNull(tx)
        assertEquals("TRANSFER", tx!!.type)
        assertEquals(100L, tx.amount)
        assertEquals("USD", tx.currency)
        assertEquals(usdAccId, tx.accountId)
        assertEquals(uzsAccId, tx.toAccountId)
        assertEquals(rate, tx.exchangeRate!!, 0.001)
        assertEquals(1_280_000L, tx.convertedAmount)
    }

    @Test
    fun verifyTransferDoesNotAlterIncomeOrExpenseTotals() = runBlocking {
        val acc1Id = repository.createAccount("Account 1", AccountType.CASH.dbKey, "UZS", 500_000L, "#10B981")
        val acc2Id = repository.createAccount("Account 2", AccountType.BANK.dbKey, "UZS", 100_000L, "#3B82F6")

        // Execute transfer
        repository.createTransfer(
            fromAccountId = acc1Id,
            toAccountId = acc2Id,
            amount = 150_000L,
            dateMillis = System.currentTimeMillis(),
            note = "Transfer between my accounts"
        )

        val summary = repository.currentFinancialSummary.first()
        // Transfer is neither income nor expense
        assertEquals(0L, summary.totalIncome)
        assertEquals(0L, summary.totalExpense)
        assertEquals(0L, summary.netChange)
        // Total balance across all accounts remains conserved (500k + 100k = 600k)
        assertEquals(600_000L, summary.totalBalance)
    }

    @Test
    fun verifyTransferValidationPreventsInvalidOperations() = runBlocking {
        val acc1Id = repository.createAccount("Wallet", AccountType.CASH.dbKey, "UZS", 50_000L, "#10B981")
        val acc2Id = repository.createAccount("Card", AccountType.BANK.dbKey, "UZS", 0L, "#3B82F6")

        // 1. Same account transfer must fail
        val sameAccResult = repository.createTransfer(
            fromAccountId = acc1Id,
            toAccountId = acc1Id,
            amount = 10_000L,
            dateMillis = System.currentTimeMillis(),
            note = "Invalid same account"
        )
        assertFalse(sameAccResult.isSuccess)

        // 2. Non-positive amount must fail
        val zeroResult = repository.createTransfer(
            fromAccountId = acc1Id,
            toAccountId = acc2Id,
            amount = 0L,
            dateMillis = System.currentTimeMillis(),
            note = "Invalid zero amount"
        )
        assertFalse(zeroResult.isSuccess)

        val negativeResult = repository.createTransfer(
            fromAccountId = acc1Id,
            toAccountId = acc2Id,
            amount = -5000L,
            dateMillis = System.currentTimeMillis(),
            note = "Invalid negative amount"
        )
        assertFalse(negativeResult.isSuccess)

        // 3. Insufficient balance must fail
        val overdrawResult = repository.createTransfer(
            fromAccountId = acc1Id,
            toAccountId = acc2Id,
            amount = 100_000L, // balance is only 50k
            dateMillis = System.currentTimeMillis(),
            note = "Overdraft attempt"
        )
        assertFalse(overdrawResult.isSuccess)

        // Balances must be untouched
        assertEquals(50_000L, repository.getAccountBalance(acc1Id))
        assertEquals(0L, repository.getAccountBalance(acc2Id))
    }

    @Test
    fun verifyTransferDeletionReversesBalanceChanges() = runBlocking {
        val acc1Id = repository.createAccount("Wallet", AccountType.CASH.dbKey, "UZS", 500_000L, "#10B981")
        val acc2Id = repository.createAccount("Savings", AccountType.SAVINGS.dbKey, "UZS", 100_000L, "#8B5CF6")

        val txId = repository.createTransfer(
            fromAccountId = acc1Id,
            toAccountId = acc2Id,
            amount = 200_000L,
            dateMillis = System.currentTimeMillis(),
            note = "Transfer to save"
        ).getOrThrow()

        assertEquals(300_000L, repository.getAccountBalance(acc1Id))
        assertEquals(300_000L, repository.getAccountBalance(acc2Id))

        // Delete the transfer
        repository.deleteTransactionById(txId)

        // Balances must be completely restored
        assertEquals(500_000L, repository.getAccountBalance(acc1Id))
        assertEquals(100_000L, repository.getAccountBalance(acc2Id))
    }

    @Test
    fun verifyAccountCannotBeDeletedIfItHasTransfers() = runBlocking {
        val acc1Id = repository.createAccount("Account 1", AccountType.CASH.dbKey, "UZS", 200_000L, "#10B981")
        val acc2Id = repository.createAccount("Account 2", AccountType.BANK.dbKey, "UZS", 50_000L, "#3B82F6")

        repository.createTransfer(
            fromAccountId = acc1Id,
            toAccountId = acc2Id,
            amount = 30_000L,
            dateMillis = System.currentTimeMillis(),
            note = "Transfer"
        )

        // Both accounts have transaction history now and must NOT be deletable
        assertFalse("Source account with transfers should not be deletable", repository.canDeleteAccount(acc1Id))
        assertFalse("Destination account with transfers should not be deletable", repository.canDeleteAccount(acc2Id))
    }
}
