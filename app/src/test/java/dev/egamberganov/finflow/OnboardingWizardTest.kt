package dev.egamberganov.finflow

import android.content.Context
import android.content.res.Configuration
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.egamberganov.finflow.data.database.AppDatabase
import dev.egamberganov.finflow.data.repository.FinanceRepository
import dev.egamberganov.finflow.domain.model.AccountType
import dev.egamberganov.finflow.ui.screens.OnboardingStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnboardingWizardTest {

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
    fun testOnboardingStepsSequence() {
        assertEquals(6, OnboardingStep.TOTAL_STEPS)
        assertEquals(OnboardingStep.WELCOME, OnboardingStep.entries[0])
        assertEquals(OnboardingStep.LANGUAGE, OnboardingStep.entries[1])
        assertEquals(OnboardingStep.CURRENCY, OnboardingStep.entries[2])
        assertEquals(OnboardingStep.THEME, OnboardingStep.entries[3])
        assertEquals(OnboardingStep.ACCOUNT, OnboardingStep.entries[4])
        assertEquals(OnboardingStep.READY, OnboardingStep.entries[5])

        assertEquals(1, OnboardingStep.WELCOME.stepIndex)
        assertEquals(2, OnboardingStep.LANGUAGE.stepIndex)
        assertEquals(3, OnboardingStep.CURRENCY.stepIndex)
        assertEquals(4, OnboardingStep.THEME.stepIndex)
        assertEquals(5, OnboardingStep.ACCOUNT.stepIndex)
        assertEquals(6, OnboardingStep.READY.stepIndex)
    }

    @Test
    fun testCompleteOnboardingWithCustomAccountAndCurrency() = runBlocking {
        // Complete onboarding with EUR, Bank account, and 1,500 initial balance
        repository.completeOnboarding(
            accountName = "Main Checking",
            accountType = AccountType.BANK.dbKey,
            currency = "EUR",
            initialBalance = 150000L
        )

        val settings = repository.appSettings.first()
        assertNotNull(settings)
        assertTrue(settings!!.isOnboardingCompleted)
        assertNotNull(settings.selectedAccountId)

        val accounts = repository.allAccounts.first()
        assertEquals(1, accounts.size)
        val firstAccount = accounts.first()
        assertEquals("Main Checking", firstAccount.name)
        assertEquals(AccountType.BANK.dbKey, firstAccount.type)
        assertEquals("EUR", firstAccount.currency)
        assertEquals(150000L, firstAccount.initialBalance)

        val accountsWithBal = repository.accountsWithBalance.first()
        val firstAccWithBal = accountsWithBal.first()
        assertEquals(150000L, firstAccWithBal.currentBalance)

        // Categories should have been pre-seeded
        val categories = repository.allCategories.first()
        assertTrue("Categories should be seeded during onboarding", categories.isNotEmpty())
    }

    @Test
    fun testOnboardingStringsEnglish() {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.ENGLISH)
        val localizedContext = context.createConfigurationContext(config)

        val welcomeTitle = localizedContext.getString(R.string.onboarding_welcome_title)
        val startNow = localizedContext.getString(R.string.btn_start_now)
        val langTitle = localizedContext.getString(R.string.onboarding_language_title)
        val currTitle = localizedContext.getString(R.string.onboarding_currency_title)
        val themeTitle = localizedContext.getString(R.string.onboarding_theme_title)
        val accTitle = localizedContext.getString(R.string.onboarding_account_title)
        val readyTitle = localizedContext.getString(R.string.onboarding_ready_title)

        assertEquals("Welcome to FinFlow", welcomeTitle)
        assertEquals("Start Now", startNow)
        assertEquals("Choose your language", langTitle)
        assertEquals("Choose your main currency", currTitle)
        assertEquals("Choose appearance", themeTitle)
        assertEquals("Create your first account", accTitle)
        assertEquals("You're all set!", readyTitle)
    }

    @Test
    fun testOnboardingStringsUzbek() {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale("uz"))
        val localizedContext = context.createConfigurationContext(config)

        val welcomeTitle = localizedContext.getString(R.string.onboarding_welcome_title)
        val startNow = localizedContext.getString(R.string.btn_start_now)
        val langTitle = localizedContext.getString(R.string.onboarding_language_title)
        val currTitle = localizedContext.getString(R.string.onboarding_currency_title)
        val themeTitle = localizedContext.getString(R.string.onboarding_theme_title)
        val accTitle = localizedContext.getString(R.string.onboarding_account_title)
        val readyTitle = localizedContext.getString(R.string.onboarding_ready_title)

        assertEquals("FinFlow ga Xush Kelibsiz", welcomeTitle)
        assertEquals("Boshlash", startNow)
        assertEquals("Tilni tanlang", langTitle)
        assertEquals("Asosiy valyutani tanlang", currTitle)
        assertEquals("Mavzuni tanlang", themeTitle)
        assertEquals("Birinchi hisobingizni yarating", accTitle)
        assertEquals("Hammasi tayyor!", readyTitle)
    }

    @Test
    fun testOnboardingStringsRussian() {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale("ru"))
        val localizedContext = context.createConfigurationContext(config)

        val welcomeTitle = localizedContext.getString(R.string.onboarding_welcome_title)
        val startNow = localizedContext.getString(R.string.btn_start_now)
        val langTitle = localizedContext.getString(R.string.onboarding_language_title)
        val currTitle = localizedContext.getString(R.string.onboarding_currency_title)
        val themeTitle = localizedContext.getString(R.string.onboarding_theme_title)
        val accTitle = localizedContext.getString(R.string.onboarding_account_title)
        val readyTitle = localizedContext.getString(R.string.onboarding_ready_title)

        assertEquals("Добро пожаловать в FinFlow", welcomeTitle)
        assertEquals("Начать", startNow)
        assertEquals("Выберите язык", langTitle)
        assertEquals("Выберите основную валюту", currTitle)
        assertEquals("Выберите тему оформления", themeTitle)
        assertEquals("Создайте ваш первый счет", accTitle)
        assertEquals("Всё готово!", readyTitle)
    }
}
