package dev.egamberganov.finflow

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.egamberganov.finflow.data.database.AppDatabase
import dev.egamberganov.finflow.data.entity.AccountEntity
import dev.egamberganov.finflow.data.entity.CategoryEntity
import dev.egamberganov.finflow.data.entity.ScheduledPaymentEntity
import dev.egamberganov.finflow.data.entity.TransactionEntity
import dev.egamberganov.finflow.data.repository.FinanceRepository
import dev.egamberganov.finflow.domain.model.AccountType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountManagementTest {

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
    fun verifyAllSixAccountTypesExistAndHaveValidResources() {
        val types = AccountType.entries
        assertEquals(6, types.size)

        val expectedKeys = setOf(
            "CASH",
            "BANK",
            "SAVINGS",
            "EWALLET",
            "INVESTMENT",
            "LOAN_DEBT"
        )
        assertEquals(expectedKeys, types.map { it.dbKey }.toSet())

        // Verify all string and icon resources resolve properly
        for (type in types) {
            val label = context.getString(type.stringResId)
            assertTrue("Label for ${type.name} should not be blank", label.isNotBlank())
            assertTrue("Icon resource for ${type.name} should be non-zero", type.iconResId != 0)
        }
    }

    @Test
    fun verifyAccountTypeFromStringMappingAndFallbacks() {
        assertEquals(AccountType.CASH, AccountType.fromString("CASH"))
        assertEquals(AccountType.BANK, AccountType.fromString("BANK"))
        assertEquals(AccountType.SAVINGS, AccountType.fromString("SAVINGS"))
        assertEquals(AccountType.EWALLET, AccountType.fromString("EWALLET"))
        assertEquals(AccountType.INVESTMENT, AccountType.fromString("INVESTMENT"))
        assertEquals(AccountType.LOAN_DEBT, AccountType.fromString("LOAN_DEBT"))

        // Case insensitivity and aliases
        assertEquals(AccountType.CASH, AccountType.fromString("cash"))
        assertEquals(AccountType.CASH, AccountType.fromString("wallet"))
        assertEquals(AccountType.BANK, AccountType.fromString("bank_account"))
        assertEquals(AccountType.BANK, AccountType.fromString("Bank Account"))
        assertEquals(AccountType.EWALLET, AccountType.fromString("e-wallet"))
        assertEquals(AccountType.EWALLET, AccountType.fromString("E_WALLET"))
        assertEquals(AccountType.LOAN_DEBT, AccountType.fromString("loan"))
        assertEquals(AccountType.LOAN_DEBT, AccountType.fromString("debt"))
        assertEquals(AccountType.LOAN_DEBT, AccountType.fromString("Loan / Debt"))

        // Legacy OTHER fallback to CASH
        assertEquals(AccountType.CASH, AccountType.fromString("OTHER"))

        // Unknown fallback to CASH
        assertEquals(AccountType.CASH, AccountType.fromString("unknown_random_type"))
        assertEquals(AccountType.CASH, AccountType.fromString(null))
    }

    @Test
    fun verifyAccountCreationAndListing() = runBlocking {
        val accountId = repository.createAccount(
            name = "My Savings Vault",
            type = AccountType.SAVINGS.dbKey,
            currency = "USD",
            initialBalance = 5000L,
            colorHex = "#30D158"
        )
        assertTrue(accountId > 0)

        val account = db.accountDao().getAccountByIdDirect(accountId)
        assertNotNull(account)
        assertEquals("My Savings Vault", account?.name)
        assertEquals("SAVINGS", account?.type)
        assertEquals("USD", account?.currency)
        assertEquals(5000L, account?.initialBalance)
    }

    @Test
    fun verifyAccountSafeDeletionAllowsWhenNoTransactionsOrScheduledPayments() = runBlocking {
        val accountId = repository.createAccount(
            name = "Empty Account",
            type = AccountType.CASH.dbKey,
            currency = "UZS",
            initialBalance = 0L,
            colorHex = "#5E5CE6"
        )
        val account = db.accountDao().getAccountByIdDirect(accountId)!!

        val canDelete = repository.canDeleteAccount(accountId)
        assertTrue("Account with no transactions or scheduled payments should be deletable", canDelete)

        val wasDeleted = repository.deleteAccount(account)
        assertTrue("deleteAccount should return true when deletion succeeds", wasDeleted)

        val remaining = db.accountDao().getAccountByIdDirect(accountId)
        assertNull(remaining)
    }

    @Test
    fun verifyAccountDeletionRefusedWhenTransactionsExist() = runBlocking {
        val accountId = repository.createAccount(
            name = "Active Checking",
            type = AccountType.BANK.dbKey,
            currency = "UZS",
            initialBalance = 100_000L,
            colorHex = "#0A84FF"
        )
        val account = db.accountDao().getAccountByIdDirect(accountId)!!

        // Insert dummy category and transaction
        val categoryId = db.categoryDao().insertCategory(
            CategoryEntity(
                name = "Groceries",
                type = "EXPENSE",
                iconName = "shopping",
                colorHex = "#FF9F0A"
            )
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                type = "EXPENSE",
                amount = 25_000L,
                currency = "UZS",
                categoryId = categoryId,
                accountId = accountId,
                dateMillis = System.currentTimeMillis(),
                note = "Store purchase"
            )
        )

        val txCount = db.transactionDao().getTransactionCountForAccount(accountId)
        assertEquals(1, txCount)

        val canDelete = repository.canDeleteAccount(accountId)
        assertFalse("Account with transaction must NOT be deletable", canDelete)

        val wasDeleted = repository.deleteAccount(account)
        assertFalse("deleteAccount should return false and block deletion", wasDeleted)

        // Ensure account was NOT deleted and historical data preserved
        val accountStillExists = db.accountDao().getAccountByIdDirect(accountId)
        assertNotNull(accountStillExists)
        assertEquals(1, db.transactionDao().getTransactionCountForAccount(accountId))
    }

    @Test
    fun verifyAccountDeletionRefusedWhenScheduledPaymentExists() = runBlocking {
        val accountId = repository.createAccount(
            name = "Loan Account",
            type = AccountType.LOAN_DEBT.dbKey,
            currency = "UZS",
            initialBalance = 0L,
            colorHex = "#FF3B30"
        )
        val account = db.accountDao().getAccountByIdDirect(accountId)!!

        val categoryId = db.categoryDao().insertCategory(
            CategoryEntity(
                name = "Debt Payment",
                type = "EXPENSE",
                iconName = "bills",
                colorHex = "#FF453A"
            )
        )

        db.scheduledPaymentDao().insertScheduledPayment(
            ScheduledPaymentEntity(
                name = "Monthly Car Loan",
                amount = 1_500_000L,
                currency = "UZS",
                categoryId = categoryId,
                accountId = accountId,
                frequency = "MONTHLY",
                nextPaymentDateMillis = System.currentTimeMillis() + 86400000L
            )
        )

        val schedCount = db.scheduledPaymentDao().getScheduledPaymentCountForAccount(accountId)
        assertEquals(1, schedCount)

        val canDelete = repository.canDeleteAccount(accountId)
        assertFalse("Account with scheduled payment must NOT be deletable", canDelete)

        val wasDeleted = repository.deleteAccount(account)
        assertFalse("deleteAccount should return false and block deletion", wasDeleted)

        // Ensure account was NOT deleted and scheduled payment preserved
        val accountStillExists = db.accountDao().getAccountByIdDirect(accountId)
        assertNotNull(accountStillExists)
        assertEquals(1, db.scheduledPaymentDao().getScheduledPaymentCountForAccount(accountId))
    }
}
