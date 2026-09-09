package dev.egamberganov.finflow.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.domain.model.AccountType
import dev.egamberganov.finflow.domain.model.CurrencyFormatter
import dev.egamberganov.finflow.ui.theme.BrandPrimary
import dev.egamberganov.finflow.ui.theme.BrandPrimaryLight
import dev.egamberganov.finflow.ui.theme.BrandPrimaryVariant
import dev.egamberganov.finflow.ui.theme.appColors

enum class OnboardingStep(val stepIndex: Int) {
    WELCOME(1),
    LANGUAGE(2),
    CURRENCY(3),
    THEME(4),
    ACCOUNT(5),
    READY(6);

    companion object {
        const val TOTAL_STEPS = 6
    }
}

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val nameResId: Int
)

data class LanguageOption(
    val code: String,
    val nativeName: String,
    val englishName: String
)

data class ThemeOption(
    val mode: String,
    val titleResId: Int,
    val descResId: Int,
    val iconResId: Int
)

@Composable
fun OnboardingScreen(
    currentLanguage: String = "en",
    currentTheme: String = "SYSTEM",
    onLanguageChange: (String) -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onComplete: (accountName: String, accountType: String, currency: String, initialBalance: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }

    // Persistent onboarding state
    var selectedLanguage by rememberSaveable { mutableStateOf(currentLanguage) }
    var selectedCurrency by rememberSaveable { mutableStateOf("UZS") }
    var selectedTheme by rememberSaveable { mutableStateOf(currentTheme) }
    var accountName by rememberSaveable { mutableStateOf("") }
    var selectedAccountType by rememberSaveable { mutableStateOf(AccountType.CASH) }
    var initialBalanceText by rememberSaveable { mutableStateOf("0") }
    var accountNameError by remember { mutableStateOf<String?>(null) }

    val defaultAccountName = stringResource(R.string.onboarding_default_account_name)
    LaunchedEffect(Unit) {
        if (accountName.isEmpty()) {
            accountName = defaultAccountName
        }
    }

    // Android System Back Navigation
    BackHandler(enabled = currentStep != OnboardingStep.WELCOME) {
        currentStep = when (currentStep) {
            OnboardingStep.LANGUAGE -> OnboardingStep.WELCOME
            OnboardingStep.CURRENCY -> OnboardingStep.LANGUAGE
            OnboardingStep.THEME -> OnboardingStep.CURRENCY
            OnboardingStep.ACCOUNT -> OnboardingStep.THEME
            OnboardingStep.READY -> OnboardingStep.ACCOUNT
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
        }
    }

    val availableCurrencies = listOf(
        CurrencyOption("UZS", "so'm", R.string.curr_uzs_name),
        CurrencyOption("USD", "$", R.string.curr_usd_name),
        CurrencyOption("EUR", "€", R.string.curr_eur_name),
        CurrencyOption("RUB", "₽", R.string.curr_rub_name),
        CurrencyOption("GBP", "£", R.string.curr_gbp_name),
        CurrencyOption("KZT", "₸", R.string.curr_kzt_name)
    )

    val availableLanguages = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("uz", "O'zbekcha", "Uzbek"),
        LanguageOption("ru", "Русский", "Russian")
    )

    val availableThemes = listOf(
        ThemeOption("SYSTEM", R.string.theme_system, R.string.onboarding_theme_system_desc, R.drawable.ic_nav_settings),
        ThemeOption("LIGHT", R.string.theme_light, R.string.onboarding_theme_light_desc, R.drawable.ic_sys_light_theme),
        ThemeOption("DARK", R.string.theme_dark, R.string.onboarding_theme_dark_desc, R.drawable.ic_sys_dark_theme)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Step Navigation Bar & Progress Indicator
        OnboardingHeader(
            currentStep = currentStep,
            onBackClick = {
                currentStep = when (currentStep) {
                    OnboardingStep.LANGUAGE -> OnboardingStep.WELCOME
                    OnboardingStep.CURRENCY -> OnboardingStep.LANGUAGE
                    OnboardingStep.THEME -> OnboardingStep.CURRENCY
                    OnboardingStep.ACCOUNT -> OnboardingStep.THEME
                    OnboardingStep.READY -> OnboardingStep.ACCOUNT
                    OnboardingStep.WELCOME -> OnboardingStep.WELCOME
                }
            }
        )

        // Animated Content for Step Transitions
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                val forward = targetState.stepIndex > initialState.stepIndex
                if (forward) {
                    (slideInHorizontally(animationSpec = tween(280)) { width -> width } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> -width } + fadeOut(animationSpec = tween(280)))
                } else {
                    (slideInHorizontally(animationSpec = tween(280)) { width -> -width } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> width } + fadeOut(animationSpec = tween(280)))
                }
            },
            modifier = Modifier.weight(1f),
            label = "OnboardingStepTransition"
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> {
                    WelcomeStepContent(
                        onStartClick = { currentStep = OnboardingStep.LANGUAGE }
                    )
                }
                OnboardingStep.LANGUAGE -> {
                    LanguageStepContent(
                        languages = availableLanguages,
                        selectedLanguage = selectedLanguage,
                        onSelectLanguage = { code ->
                            selectedLanguage = code
                            onLanguageChange(code)
                        },
                        onBackClick = { currentStep = OnboardingStep.WELCOME },
                        onContinueClick = { currentStep = OnboardingStep.CURRENCY }
                    )
                }
                OnboardingStep.CURRENCY -> {
                    CurrencyStepContent(
                        currencies = availableCurrencies,
                        selectedCurrency = selectedCurrency,
                        onSelectCurrency = { selectedCurrency = it },
                        onBackClick = { currentStep = OnboardingStep.LANGUAGE },
                        onContinueClick = { currentStep = OnboardingStep.THEME }
                    )
                }
                OnboardingStep.THEME -> {
                    ThemeStepContent(
                        themes = availableThemes,
                        selectedTheme = selectedTheme,
                        onSelectTheme = { mode ->
                            selectedTheme = mode
                            onThemeChange(mode)
                        },
                        onBackClick = { currentStep = OnboardingStep.CURRENCY },
                        onContinueClick = { currentStep = OnboardingStep.ACCOUNT }
                    )
                }
                OnboardingStep.ACCOUNT -> {
                    AccountStepContent(
                        accountName = accountName,
                        onAccountNameChange = {
                            accountName = it
                            if (it.isNotBlank()) accountNameError = null
                        },
                        accountNameError = accountNameError,
                        selectedType = selectedAccountType,
                        onSelectType = { selectedAccountType = it },
                        selectedCurrency = selectedCurrency,
                        availableCurrencies = availableCurrencies.map { it.code },
                        onSelectCurrency = { selectedCurrency = it },
                        initialBalanceText = initialBalanceText,
                        onInitialBalanceChange = { if (it.all { c -> c.isDigit() }) initialBalanceText = it },
                        onBackClick = { currentStep = OnboardingStep.THEME },
                        onContinueClick = {
                            if (accountName.trim().isBlank()) {
                                accountNameError = context.getString(R.string.enter_account_name_error)
                                Toast.makeText(context, context.getString(R.string.enter_account_name_error), Toast.LENGTH_SHORT).show()
                                return@AccountStepContent
                            }
                            currentStep = OnboardingStep.READY
                        }
                    )
                }
                OnboardingStep.READY -> {
                    ReadyStepContent(
                        languageCode = selectedLanguage,
                        currency = selectedCurrency,
                        themeMode = selectedTheme,
                        accountName = accountName.trim().ifEmpty { defaultAccountName },
                        accountType = selectedAccountType,
                        initialBalance = initialBalanceText.toLongOrNull() ?: 0L,
                        onBackClick = { currentStep = OnboardingStep.ACCOUNT },
                        onGetStartedClick = {
                            val finalBalance = initialBalanceText.toLongOrNull() ?: 0L
                            val finalName = accountName.trim().ifEmpty { defaultAccountName }
                            onComplete(finalName, selectedAccountType.dbKey, selectedCurrency, finalBalance)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(
    currentStep: OnboardingStep,
    onBackClick: () -> Unit
) {
    val progress = currentStep.stepIndex.toFloat() / OnboardingStep.TOTAL_STEPS.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep != OnboardingStep.WELCOME) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("onboarding_back_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_act_back),
                        contentDescription = stringResource(R.string.btn_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Step Indicator Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.appColors.brand.copy(alpha = 0.12f)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step_progress, currentStep.stepIndex, OnboardingStep.TOTAL_STEPS),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.appColors.brand,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress line
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = 24.dp),
            color = MaterialTheme.appColors.brand,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}

// STEP 1 — WELCOME
@Composable
private fun WelcomeStepContent(
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Hero Branding
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(BrandPrimaryLight, BrandPrimaryVariant)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fin_wallet),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Value Propositions
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureHighlightRow(
                        iconResId = R.drawable.ic_fin_wallet,
                        title = stringResource(R.string.onboarding_feature_accounts_title),
                        description = stringResource(R.string.onboarding_feature_accounts_desc)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    FeatureHighlightRow(
                        iconResId = R.drawable.ic_tx_recurring,
                        title = stringResource(R.string.onboarding_feature_scheduled_title),
                        description = stringResource(R.string.onboarding_feature_scheduled_desc)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    FeatureHighlightRow(
                        iconResId = R.drawable.ic_sys_info,
                        title = stringResource(R.string.onboarding_feature_privacy_title),
                        description = stringResource(R.string.onboarding_feature_privacy_desc)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("onboarding_start_now_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.brand)
        ) {
            Text(
                text = stringResource(R.string.btn_start_now),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// STEP 2 — LANGUAGE
@Composable
private fun LanguageStepContent(
    languages: List<LanguageOption>,
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.onboarding_language_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_language_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                languages.forEach { lang ->
                    val isSelected = selectedLanguage.equals(lang.code, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectLanguage(lang.code) }
                            .testTag("onboarding_language_${lang.code}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.appColors.brand.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang.code.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = lang.englishName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.appColors.brand,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        StepNavigationButtons(
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}

// STEP 3 — CURRENCY
@Composable
private fun CurrencyStepContent(
    currencies: List<CurrencyOption>,
    selectedCurrency: String,
    onSelectCurrency: (String) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.onboarding_currency_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_currency_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currencies.forEach { curr ->
                    val isSelected = selectedCurrency.equals(curr.code, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectCurrency(curr.code) }
                            .testTag("onboarding_currency_${curr.code}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.appColors.brand.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = curr.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = curr.code,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(curr.nameResId),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.appColors.brand,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        StepNavigationButtons(
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}

// STEP 4 — THEME / APPEARANCE
@Composable
private fun ThemeStepContent(
    themes: List<ThemeOption>,
    selectedTheme: String,
    onSelectTheme: (String) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.onboarding_theme_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_theme_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                themes.forEach { theme ->
                    val isSelected = selectedTheme.equals(theme.mode, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTheme(theme.mode) }
                            .testTag("onboarding_theme_${theme.mode.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.appColors.brand.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = theme.iconResId),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(theme.titleResId),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(theme.descResId),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.appColors.brand,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        StepNavigationButtons(
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}

// STEP 5 — FIRST ACCOUNT
@Composable
private fun AccountStepContent(
    accountName: String,
    onAccountNameChange: (String) -> Unit,
    accountNameError: String?,
    selectedType: AccountType,
    onSelectType: (AccountType) -> Unit,
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onSelectCurrency: (String) -> Unit,
    initialBalanceText: String,
    onInitialBalanceChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.onboarding_account_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_account_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Account Name
                    Text(
                        text = stringResource(R.string.account_name),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = onAccountNameChange,
                        isError = accountNameError != null,
                        supportingText = if (accountNameError != null) {
                            { Text(text = accountNameError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_account_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.appColors.brand,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Account Type Grid (6 types)
                    Text(
                        text = stringResource(R.string.account_type),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountType.entries.chunked(3).forEach { rowTypes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTypes.forEach { accType ->
                                    val isSelected = selectedType == accType
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectType(accType) }
                                            .testTag("onboarding_account_type_${accType.dbKey}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.appColors.brand.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = accType.iconResId),
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(accType.stringResId),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Currency Selector Row
                    Text(
                        text = stringResource(R.string.currency),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableCurrencies.forEach { curr ->
                            val isSelected = selectedCurrency == curr
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectCurrency(curr) }
                                    .testTag("onboarding_account_currency_$curr"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = curr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Initial Balance
                    Text(
                        text = stringResource(R.string.initial_balance),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = onInitialBalanceChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_initial_balance_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.appColors.brand,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        StepNavigationButtons(
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}

// STEP 6 — READY / COMPLETION
@Composable
private fun ReadyStepContent(
    languageCode: String,
    currency: String,
    themeMode: String,
    accountName: String,
    accountType: AccountType,
    initialBalance: Long,
    onBackClick: () -> Unit,
    onGetStartedClick: () -> Unit
) {
    val languageLabel = when (languageCode.lowercase()) {
        "uz" -> "O'zbekcha"
        "ru" -> "Русский"
        else -> "English"
    }

    val themeLabel = when (themeMode.uppercase()) {
        "LIGHT" -> stringResource(R.string.theme_light)
        "DARK" -> stringResource(R.string.theme_dark)
        else -> stringResource(R.string.theme_system)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Success Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.appColors.income.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sys_success),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.income,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.onboarding_ready_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboarding_ready_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Summary Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_summary_card"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_summary_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    SummaryRow(
                        label = stringResource(R.string.onboarding_summary_language),
                        value = languageLabel
                    )

                    SummaryRow(
                        label = stringResource(R.string.onboarding_summary_currency),
                        value = currency
                    )

                    SummaryRow(
                        label = stringResource(R.string.onboarding_summary_theme),
                        value = themeLabel
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // First Account Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.onboarding_summary_account),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = accountType.iconResId),
                                    contentDescription = null,
                                    tint = MaterialTheme.appColors.brand,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$accountName (${stringResource(accountType.stringResId)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.onboarding_summary_balance),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatAmount(initialBalance, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.appColors.income
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onGetStartedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_get_started_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.brand)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_get_started),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_review_back_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Text(
                    text = stringResource(R.string.btn_back),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FeatureHighlightRow(
    iconResId: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.appColors.brand.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = MaterialTheme.appColors.brand,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StepNavigationButtons(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .testTag("onboarding_nav_back_button"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Text(
                text = stringResource(R.string.btn_back),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .weight(1.8f)
                .height(54.dp)
                .testTag("onboarding_continue_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.brand)
        ) {
            Text(
                text = stringResource(R.string.btn_continue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

